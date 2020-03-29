package com.xudasong.test;

import com.xudasong.test.client.WebSocketClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication
@EnableWebSocket
public class WebSocketTestApplication {
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(WebSocketTestApplication.class);
        ConfigurableApplicationContext configurableApplicationContext = springApplication.run(args);
        //解决websocket不能注入的问题
        WebSocketClient.setApplicationContext(configurableApplicationContext);
    }
}
