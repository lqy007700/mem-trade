package org.trade.store;

import org.springframework.beans.factory.annotation.Autowired;
import org.trade.db.DbTemplate;
import org.trade.message.event.AbstractEvent;
import org.trade.messaging.MessageTypes;
import org.trade.model.support.EntitySupport;
import org.trade.model.trade.EventEntity;
import org.trade.support.LoggerSupport;

import java.util.List;
import java.util.stream.Collectors;


public class StoreService extends LoggerSupport {

    @Autowired
    MessageTypes messageTypes;

    @Autowired
    DbTemplate dbTemplate;

    public List<AbstractEvent> loadEventsFromDb(long lastEventId) {
        List<EventEntity> events = this.dbTemplate.from(EventEntity.class).where("sequenceId > ?", lastEventId)
                .orderBy("sequenceId").limit(100000).list();
        return events.stream().map(event -> (AbstractEvent) messageTypes.deserialize(event.data))
                .collect(Collectors.toList());
    }

    public void insertIgnore(List<? extends EntitySupport> list) {
        dbTemplate.insertIgnore(list);
    }
}
