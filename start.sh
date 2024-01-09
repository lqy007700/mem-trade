#!/bin/bash

# 启动第一个 Spring Boot 项目
cd /Users/lqy007700/Data/code/java-application/trade/config
mvn spring-boot:run &

cd /Users/lqy007700/Data/code/java-application/trade/trading-api
mvn spring-boot:run &

cd /Users/lqy007700/Data/code/java-application/trade/trading-engine
mvn spring-boot:run &

cd /Users/lqy007700/Data/code/java-application/trade/trading-sequencer
mvn spring-boot:run &

cd /Users/lqy007700/Data/code/java-application/trade/quotation
mvn spring-boot:run &

cd /Users/lqy007700/Data/code/java-application/trade/push
mvn spring-boot:run &

cd /Users/lqy007700/Data/code/java-application/trade/ui
mvn spring-boot:run &

cd /Users/lqy007700/Data/code/java-application/trade/build/bot
./bot.py --email=user0@example.com --password=password0

