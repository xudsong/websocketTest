package com.xudasong.test.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.server.ServerEndpoint;

@Component
@ServerEndpoint(value = "/release/websocket")
@Slf4j
public class WebSocketClient {
}
