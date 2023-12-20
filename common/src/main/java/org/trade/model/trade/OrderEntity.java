package org.trade.model.trade;

import org.trade.enums.Direction;
import org.trade.enums.OrderStatus;

import java.math.BigDecimal;

public class OrderEntity {
    // 订单id 定序id 用户id
    public Long id;
    public long sequenceId;
    public Long userId;

    // 价格 / 方向 / 状态
    public BigDecimal price;
    public Direction direction;
    public OrderStatus status;

    // 订单数量 未成交数量
    public BigDecimal quantity;
    public BigDecimal unfilledQuantity;

    // 创建和更新时间:
    public long createdAt;
    public long updatedAt;

    private int version;

    public void updateOrder(BigDecimal unfilledQuantity, OrderStatus status, long updatedAt) {
        this.version++;
        this.unfilledQuantity = unfilledQuantity;
        this.status = status;
        this.updatedAt = updatedAt;
        this.version++;
    }
}
