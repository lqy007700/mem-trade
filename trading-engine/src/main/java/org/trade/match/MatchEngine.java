package org.trade.match;

import org.trade.enums.Direction;
import org.trade.enums.OrderStatus;
import org.trade.model.trade.OrderEntity;

import java.math.BigDecimal;

public class MatchEngine {
    // 买盘
    public final OrderBook buyBook = new OrderBook(Direction.BUY);

    // 卖盘
    public final OrderBook sellBook = new OrderBook(Direction.SELL);

    // 最新成交价
    public BigDecimal marketPrice = BigDecimal.ZERO;

    // 上次的序号
    private long sequenceId;

    /**
     * taker 订单
     *
     * @param seq
     * @param order
     * @return
     */
    public MatchResult processOrder(long seq, OrderEntity order) {
        switch (order.direction) {
            case BUY -> {
                return processOrder(seq, order, this.sellBook, this.buyBook);
            }
            case SELL -> {
                return processOrder(seq, order, this.buyBook, this.sellBook);
            }
            default -> throw new IllegalArgumentException("Invalid direction.");
        }
    }

    /**
     * 撮合
     *
     * @param sequenceId  序号
     * @param takerOrder  输入的订单
     * @param makerBook   挂单
     * @param anotherBook 和交易方向一致的挂单，用于存放剩余未成交的数据
     * @return
     */
    private MatchResult processOrder(long sequenceId, OrderEntity takerOrder, OrderBook makerBook, OrderBook anotherBook) {
        this.sequenceId = sequenceId;
        long ts = takerOrder.createdAt;
        MatchResult matchResult = new MatchResult(takerOrder);
        BigDecimal takerUnfilledQuantity = takerOrder.quantity;

        // 撮合流程
        while (true) {
            OrderEntity makerOrder = makerBook.getFirst();

            // 对手盘不存在
            if (makerOrder == null) {
                break;
            }

            // 价格不匹配
            if (takerOrder.direction == Direction.BUY && takerOrder.price.compareTo(makerOrder.price) < 0) {
                // 买入订单价格比卖盘第一档价格低:
                break;
            } else if (takerOrder.direction == Direction.SELL && takerOrder.price.compareTo(makerOrder.price) > 0) {
                // 卖出订单价格比买盘第一档价格高:
                break;
            }

            // 以maker价格成交
            this.marketPrice = makerOrder.price;
            // 待成交数量为两者较小值: 取maker的未成交数量
            BigDecimal matchedQuantity = takerUnfilledQuantity.min(makerOrder.unfilledQuantity);
            // 成交记录
            matchResult.add(makerOrder.price, matchedQuantity, makerOrder);
            // 更新成交后的订单数量:
            takerUnfilledQuantity = takerUnfilledQuantity.subtract(matchedQuantity);

            // 更新maker未成交订单数量
            BigDecimal makerUnfilledQuantity = makerOrder.unfilledQuantity.subtract(takerUnfilledQuantity);

            // 对手盘完全成交后，从订单簿中删除:
            if (makerUnfilledQuantity.signum() == 0) {
                // 更新订单
                makerOrder.updateOrder(makerUnfilledQuantity, OrderStatus.FULLY_FILLED, ts);
                // 从对手盘上移除
                makerBook.remove(makerOrder);
            } else {
                // 对手盘部分成交:
                makerOrder.updateOrder(makerUnfilledQuantity, OrderStatus.PARTIAL_FILLED, ts);
            }

            if (takerUnfilledQuantity.signum() == 0) {
                takerOrder.updateOrder(takerUnfilledQuantity, OrderStatus.FULLY_FILLED, ts);
                break;
            }
        }

        // 订单未完全成交，剩余的放入订单列表
        if (takerUnfilledQuantity.signum() > 0) {
            OrderStatus status = OrderStatus.PARTIAL_FILLED;
            if (takerOrder.quantity.compareTo(takerUnfilledQuantity) == 0) {
                status = OrderStatus.PENDING;
            }

            takerOrder.updateOrder(takerUnfilledQuantity, status, ts);
            anotherBook.add(takerOrder);
        }
        return matchResult;
    }
}
