package org.trade.clearing;

import org.trade.assets.AssetService;
import org.trade.assets.Transfer;
import org.trade.enums.AssetEnum;
import org.trade.match.MatchDetailRecord;
import org.trade.match.MatchResult;
import org.trade.model.trade.OrderEntity;
import org.trade.order.OrderService;
import org.trade.support.LoggerSupport;

import java.math.BigDecimal;

public class ClearingService extends LoggerSupport {
    final AssetService assetService;
    final OrderService orderService;

    public ClearingService(AssetService assetService, OrderService orderService) {
        this.assetService = assetService;
        this.orderService = orderService;
    }

    public void clearMatchResult(MatchResult result) {
        OrderEntity taker = result.takerOrder;
        switch (taker.direction) {
            case BUY -> {
                // 买入按照maker价格成交
                // Taker冻结的金额是按照Taker订单的报价冻结的
                // 解冻后，部分差额要退回至Taker可用余额：
                for (MatchDetailRecord detail : result.matchDetails) {
                    OrderEntity maker = detail.makerOrder();
                    BigDecimal quantity = detail.quantity();
                    // 实际买入价格比报价低
                    if (taker.price.compareTo(maker.price) > 0) {
                        // 退回差额
                        BigDecimal diff = taker.price.subtract(maker.price).multiply(quantity);
                        assetService.unfreeze(taker.userId, AssetEnum.USD, diff);
                    }
                    // 买方usd转入卖方账户
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, taker.userId, maker.userId, AssetEnum.USD, maker.price.multiply(quantity));
                    // 卖方btc转入买方账户
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, maker.userId, taker.userId, AssetEnum.BTC, quantity);

                    // 删除已成交的订单maker
                    if (maker.unfilledQuantity.signum() == 0) {
                        orderService.removeOrder(maker.id);
                    }
                }
                // 删除已成交的订单taker
                if (taker.unfilledQuantity.signum() == 0) {
                    orderService.removeOrder(taker.id);
                }
            }
            case SELL -> {
                for (MatchDetailRecord detail : result.matchDetails) {
                    OrderEntity maker = detail.makerOrder();
                    BigDecimal quantity = detail.quantity();
                    // 卖方BTC转入买方账户
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, taker.userId, maker.userId, AssetEnum.BTC, quantity);
                    // 买方USD转入卖方账户
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, maker.userId, taker.userId, AssetEnum.USD, maker.price.multiply(quantity));

                    // 删除已成交的订单maker
                    if (maker.unfilledQuantity.signum() == 0) {
                        orderService.removeOrder(maker.id);
                    }
                }

                // 删除已成交的订单taker
                if (taker.unfilledQuantity.signum() == 0) {
                    orderService.removeOrder(taker.id);
                }
            }
            default -> throw new IllegalArgumentException("Invalid direction.");
        }
    }

    // 撤单
    public void clearCancelOrder(OrderEntity order) {
        switch (order.direction) {
            case BUY -> {
                // 买入订单取消，解冻USDT
                assetService.unfreeze(order.userId, AssetEnum.USD, order.price.multiply(order.unfilledQuantity));
            }
            case SELL -> {
                // 卖出订单取消，解冻BTC
                assetService.unfreeze(order.userId, AssetEnum.BTC, order.unfilledQuantity);
            }
            default -> throw new IllegalArgumentException("Invalid direction.");
        }
        orderService.removeOrder(order.id);
    }
}
