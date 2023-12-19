package org.trade.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.trade.assets.AssetService;
import org.trade.enums.AssetEnum;
import org.trade.enums.Direction;
import org.trade.model.trade.OrderEntity;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class OrderService {

    final AssetService assetService;

    public OrderService(@Autowired AssetService assetService) {
        this.assetService = assetService;
    }

    // 跟踪所有活动订单: Order ID => OrderEntity
    final ConcurrentMap<Long, OrderEntity> activeOrders = new ConcurrentHashMap<>();

    // 跟踪用户活动订单: User ID => Map(Order ID => OrderEntity)
    final ConcurrentMap<Long, ConcurrentMap<Long, OrderEntity>> userOrders = new ConcurrentHashMap<>();

    /**
     * 创建订单
     *
     * @param seqId
     * @param ts
     * @param orderId
     * @param userId
     * @param direction
     * @param price
     * @param quantity
     * @return
     */
    public OrderEntity createOrder(long seqId, long ts, Long orderId, Long userId,
                                   Direction direction, BigDecimal price, BigDecimal quantity) {
        switch (direction) {
            // 冻结 USDT
            case BUY -> {
                if (!assetService.tryFreeze(userId, AssetEnum.USD, price.multiply(quantity))) {
                    return null;
                }
            }
            // 冻结 BTC
            case SELL -> {
                if (!assetService.tryFreeze(userId, AssetEnum.BTC, quantity)) {
                    return null;
                }
            }
            default -> throw new IllegalArgumentException("Invalid direction.");
        }

        OrderEntity order = new OrderEntity();
        order.id = orderId;
        order.sequenceId = seqId;
        order.userId = userId;
        order.price = price;
        order.quantity = quantity;
        order.unfilledQuantity = quantity;
        order.createdAt = order.updatedAt = ts;

        //  存储订单数据
        this.activeOrders.put(orderId, order);
        ConcurrentMap<Long, OrderEntity> uOrders = this.userOrders.get(userId);
        if (uOrders == null) {
            uOrders = new ConcurrentHashMap<>();
            this.userOrders.put(userId, uOrders);
        }
        uOrders.put(orderId, order);
        return order;
    }

    /**
     * 删除订单
     *
     * @param orderId
     */
    public void removeOrder(Long orderId) {
        OrderEntity remove = this.activeOrders.remove(orderId);
        if (remove == null) {
            throw new IllegalArgumentException("Order not found by orderId in active orders: " + orderId);
        }

        ConcurrentMap<Long, OrderEntity> uOrders = this.userOrders.get(remove.userId);
        if (uOrders == null) {
            throw new IllegalArgumentException("User orders not found by userId: " + remove.userId);
        }

        if (uOrders.remove(orderId) == null) {
            throw new IllegalArgumentException("Order not found by orderId in user orders: " + orderId);
        }
    }

    public OrderEntity getOrder(Long orderId) {
        return this.activeOrders.get(orderId);
    }

    public ConcurrentMap<Long, OrderEntity> getUserOrders(Long userId) {
        return this.userOrders.get(userId);
    }
}
