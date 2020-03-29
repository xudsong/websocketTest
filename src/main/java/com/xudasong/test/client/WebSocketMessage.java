package com.xudasong.test.client;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class WebSocketMessage {

    public void onUserCall(WebSocketCmd cmd,WebSocketClient client){
        log.info("onUserCall start:cmd={},client={}",cmd, JSON.toJSONString(client));
        String userUnionId = "";
        if (!StringUtils.isEmpty(cmd.getUnionId())){
            userUnionId = cmd.getUnionId();
        }
        //标识该websocket连接id
        client.setUserId(userUnionId);
        //校验是否重新登录
        if (!client.onlogin()){
            log.info("onUserCall 重复登录：{}",cmd);
            client.sendErrorCmd(WebSocketCmd.CmdRepeatLogin,"重复登录",userUnionId);
            return;
        }
        //业务处理

        log.info("onUserCall end,userUnionId:{}",userUnionId);
    }

}
