package org.trade.match;

import org.trade.enums.Direction;
import org.trade.model.trade.OrderEntity;

import java.util.Comparator;
import java.util.TreeMap;

// 订单列表
public class OrderBook {
    public final Direction direction;
    public final TreeMap<OrderKey, OrderEntity> book;

    // 买盘排序
    private static final Comparator<OrderKey> SORT_BUY = new Comparator<OrderKey>() {
        @Override
        public int compare(OrderKey o1, OrderKey o2) {
            // 价格高在前:
            int i = o2.price().compareTo(o1.price());

            // 价格相等的看seq
            return i == 0 ? Long.compare(o1.sequenceId(), o2.sequenceId()) : i;
        }
    };

    // 卖盘排序
    private static final Comparator<OrderKey> SORT_SELL = new Comparator<OrderKey>() {
        @Override
        public int compare(OrderKey o1, OrderKey o2) {
            // 价格低在前:
            int i = o1.price().compareTo(o2.price());

            // 价格相等的看seq
            return i == 0 ? Long.compare(o1.sequenceId(), o2.sequenceId()) : i;
        }
    };

    public OrderBook(Direction direction) {
        this.direction = direction;
        this.book = new TreeMap<>(direction == Direction.BUY ? SORT_BUY : SORT_SELL);
    }

    public OrderEntity getFirst() {
        return this.book.isEmpty() ? null : this.book.firstEntry().getValue();
    }

    public boolean remove(OrderEntity order) {
        return this.book.remove(new OrderKey(order.sequenceId, order.price)) != null;
    }

    public boolean add(OrderEntity order) {
        return this.book.put(new OrderKey(order.sequenceId, order.price), order) == null;
    }
}
