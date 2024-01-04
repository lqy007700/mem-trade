package org.trade.model.quotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.trade.model.support.AbstractBarEntity;

/**
 * Store bars of day.
 */
@Entity
@Table(name = "day_bars")
public class DayBarEntity extends AbstractBarEntity {

}
