package org.trade.model.quotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.trade.model.support.AbstractBarEntity;

/**
 * Store bars of hour.
 */
@Entity
@Table(name = "hour_bars")
public class HourBarEntity extends AbstractBarEntity {

}
