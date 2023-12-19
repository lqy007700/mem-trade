package org.trade.enums;

/**
 * 买卖方向
 */
public enum Direction {
    BUY(1), SELL(0);

    public final int value;

    /**
     * Get negate direction.
     */
    public Direction negate() {
        return this == BUY ? SELL : BUY;
    }

    Direction(int value) {
        this.value = value;
    }

    public static Direction of(int val) {
        if (val == 1) {
            return BUY;
        }
        if (val == 0) {
            return SELL;
        }
        throw new IllegalArgumentException("Invalid Direction value.");
    }
}
