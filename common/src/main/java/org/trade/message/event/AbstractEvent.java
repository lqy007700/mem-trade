package org.trade.message.event;

import jakarta.annotation.Nullable;
import org.trade.message.AbstractMessage;

public class AbstractEvent extends AbstractMessage {
    public long sequenceId;
    public long previousId;

    @Nullable
    public String uniqueId;
}
