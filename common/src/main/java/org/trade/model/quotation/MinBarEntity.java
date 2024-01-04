package org.trade.model.quotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.trade.model.support.AbstractBarEntity;

/**
 * Store bars of minute.
 */
@Entity
@Table(name = "min_bars")
public class MinBarEntity extends AbstractBarEntity {

}
