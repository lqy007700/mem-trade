package org.trade.bean;

import org.trade.enums.MatchType;

import java.math.BigDecimal;

public record SimpleMatchDetailRecord(BigDecimal price, BigDecimal quantity, MatchType type) {
}
