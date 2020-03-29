package com.xudasong.test.client;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPubSub;

@Component
@Slf4j
public class WebSocketSubscribe extends JedisPubSub{

    //redis订阅频道
    private static final String REDIS_CMD_CHANEL = "websocketCmdChannel";

    @Override
    public void onMessage(String channel,String message){
        log.info("频道：{}，收到消息：{}",channel,message);
        try {
            if (WebSocketSubscribe.REDIS_CMD_CHANEL.equals(channel)){
                WebSocketCmd cmd = JSONObject.parseObject(message,WebSocketCmd.class);
                WebSocketClient
            }
        }
    }

}
