package org.trade.message.event;

import java.util.List;

import org.trade.message.AbstractMessage;
import org.trade.model.quotation.TickEntity;

public class TickMessage extends AbstractMessage {

    public long sequenceId;

    public List<TickEntity> ticks;

}
