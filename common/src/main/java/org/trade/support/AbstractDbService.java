package org.trade.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.trade.db.DbTemplate;

public abstract class AbstractDbService extends LoggerSupport {
    @Autowired
    protected DbTemplate db;
}
