package org.trade;

import org.springframework.beans.factory.annotation.Autowired;
import org.trade.assets.AssetService;
import org.trade.clearing.ClearingService;
import org.trade.enums.Direction;
import org.trade.enums.MatchType;
import org.trade.match.MatchDetailRecord;
import org.trade.match.MatchEngine;
import org.trade.match.MatchResult;
import org.trade.message.ApiResultMessage;
import org.trade.message.NotificationMessage;
import org.trade.message.event.*;
import org.trade.model.quotation.TickEntity;
import org.trade.model.trade.MatchDetailEntity;
import org.trade.model.trade.OrderEntity;
import org.trade.order.OrderService;
import org.trade.store.StoreService;
import org.trade.support.LoggerSupport;

import java.time.ZoneId;


import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TradingEngineService extends LoggerSupport {

    @Autowired(required = false)
    ZoneId zoneId = ZoneId.systemDefault();

    @Autowired
    AssetService assetService;
    @Autowired
    OrderService orderService;
    @Autowired
    ClearingService clearingService;

    @Autowired
    MatchEngine matchEngine;

    @Autowired
    StoreService storeService;

    private Queue<List<OrderEntity>> orderQueue = new ConcurrentLinkedQueue<>();
    private Queue<List<MatchDetailEntity>> matchQueue = new ConcurrentLinkedQueue<>();
    private Queue<ApiResultMessage> apiResultQueue = new ConcurrentLinkedQueue<>();

    private Queue<TickMessage> tickQueue = new ConcurrentLinkedQueue<>();
    private Queue<NotificationMessage> notificationQueue = new ConcurrentLinkedQueue<>();


    private boolean orderBookChanged = false;

    private boolean fatalError = false;

    private long lastSequenceId = 0;


    /**
     * 处理kafka消息
     *
     * @param messages
     */
    public void processMessages(List<AbstractEvent> messages) {
        this.orderBookChanged = false;
        for (AbstractEvent message : messages) {
            this.processEvent(message);
        }
    }

    /**
     * 处理事件
     *
     * @param event
     */
    private void processEvent(AbstractEvent event) {
        if (this.fatalError) {
            return;
        }

        if (event.sequenceId <= this.lastSequenceId) {
            logger.warn("skip duplicate event: {}", event);
            return;
        }

        if (event.previousId > this.lastSequenceId) {
            logger.warn("event lost: expected previous id {} but actual {} for event {}", this.lastSequenceId, event.previousId, event);
            List<AbstractEvent> events = this.storeService.loadEventsFromDb(this.lastSequenceId);
            if (events.isEmpty()) {
                logger.error("cannot load lost event from db.");
                panic();
                return;
            }
            for (AbstractEvent e : events) {
                this.processEvent(e);
            }
            return;
        }

        if (event instanceof OrderRequestEvent) {
            createOrder((OrderRequestEvent) event);
        } else if (event instanceof OrderCancelEvent) {
            cancelOrder((OrderCancelEvent) event);
        } else if (event instanceof TransferEvent) {
            transfer((TransferEvent) event);
        }

    }

    private void panic() {
        logger.error("application panic. exit now...");
        this.fatalError = true;
        System.exit(1);
    }

    private void transfer(TransferEvent event) {
    }

    private void cancelOrder(OrderCancelEvent event) {
    }

    /**
     * 创建订单事件
     */
    private void createOrder(OrderRequestEvent event) {
        ZonedDateTime zdt = Instant.ofEpochMilli(event.createdAt).atZone(zoneId);
        int year = zdt.getYear();
        int month = zdt.getMonth().getValue();
        long orderId = event.sequenceId * 10000 + (year * 100L + month);

        OrderEntity order = orderService.createOrder(event.sequenceId, event.createdAt, orderId, event.userId, event.direction, event.price, event.quantity);
        if (order == null) {
            logger.warn("create order failed.");
            this.apiResultQueue.add(ApiResultMessage.createOrderFailed(event.refId, event.createdAt));
            return;
        }

        // 撮合
        MatchResult matchResult = matchEngine.processOrder(event.sequenceId, order);
        // 清算
        clearingService.clearMatchResult(matchResult);

        // 推送成功结果,注意必须复制一份OrderEntity,因为将异步序列化:
        this.apiResultQueue.add(ApiResultMessage.orderSuccess(event.refId, order, event.createdAt));
        // todo
        this.orderBookChanged = true;
        // 收集Notification:
        List<NotificationMessage> notifications = new ArrayList<>();
        notifications.add(createNotification(event.createdAt, "order", event.userId, order));

        // 撮合清算结果
        // 收集已完成的OrderEntity并生成MatchDetailEntity, TickEntity:
        if (!matchResult.matchDetails.isEmpty()) {

            List<OrderEntity> closedOrder = new ArrayList<>();
            List<MatchDetailEntity> matchDetails = new ArrayList<>();
            List<TickEntity> ticks = new ArrayList<>();
            if (matchResult.takerOrder.status.isFinalStatus) {
                closedOrder.add(matchResult.takerOrder);
            }

            for (MatchDetailRecord detail : matchResult.matchDetails) {
                OrderEntity maker = detail.makerOrder();
                notifications.add(createNotification(event.createdAt, "order_matched", maker.userId, maker.copy()));
                if (maker.status.isFinalStatus) {
                    closedOrder.add(maker);
                }
                MatchDetailEntity takerDetail = generateMatchDetailEntity(event.sequenceId, event.createdAt, detail, true);
                MatchDetailEntity makerDetail = generateMatchDetailEntity(event.sequenceId, event.createdAt, detail, false);
                matchDetails.add(takerDetail);
                matchDetails.add(makerDetail);

                TickEntity tick = new TickEntity();
                tick.sequenceId = event.sequenceId;
                tick.takerOrderId = detail.takerOrder().id;
                tick.makerOrderId = detail.makerOrder().id;
                tick.price = detail.price();
                tick.quantity = detail.quantity();
                tick.takerDirection = detail.takerOrder().direction == Direction.BUY;
                tick.createdAt = event.createdAt;
                ticks.add(tick);
            }
            // 异步写入数据库:
            this.orderQueue.add(closedOrder);
            this.matchQueue.add(matchDetails);

            // 异步发送Tick消息:
            TickMessage msg = new TickMessage();
            msg.sequenceId = event.sequenceId;
            msg.createdAt = event.createdAt;
            msg.ticks = ticks;
            this.tickQueue.add(msg);
            // 异步通知OrderMatch:
            this.notificationQueue.addAll(notifications);
        }
    }

    private NotificationMessage createNotification(long ts, String type, Long userId, Object data) {
        NotificationMessage msg = new NotificationMessage();
        msg.createdAt = ts;
        msg.type = type;
        msg.userId = userId;
        msg.data = data;
        return msg;
    }

    MatchDetailEntity generateMatchDetailEntity(long sequenceId, long timestamp, MatchDetailRecord detail, boolean forTaker) {
        MatchDetailEntity d = new MatchDetailEntity();
        d.sequenceId = sequenceId;
        d.orderId = forTaker ? detail.takerOrder().id : detail.makerOrder().id;
        d.counterOrderId = forTaker ? detail.makerOrder().id : detail.takerOrder().id;
        d.direction = forTaker ? detail.takerOrder().direction : detail.makerOrder().direction;
        d.price = detail.price();
        d.quantity = detail.quantity();
        d.type = forTaker ? MatchType.TAKER : MatchType.MAKER;
        d.userId = forTaker ? detail.takerOrder().userId : detail.makerOrder().userId;
        d.counterUserId = forTaker ? detail.makerOrder().userId : detail.takerOrder().userId;
        d.createdAt = timestamp;
        return d;
    }
}