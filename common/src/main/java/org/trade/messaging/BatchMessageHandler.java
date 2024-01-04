package org.trade.messaging;

import org.trade.message.AbstractMessage;

import java.util.List;


@FunctionalInterface
public interface BatchMessageHandler<T extends AbstractMessage> {

    void processMessages(List<T> messages);

}
