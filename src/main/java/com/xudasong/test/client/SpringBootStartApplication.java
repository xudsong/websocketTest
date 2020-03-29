package com.xudasong.test.client;

import com.xudasong.test.WebSocketTestApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

public class SpringBootStartApplication extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder){
        //注意这里需要指向原先用main方法执行的Application启动类
        return builder.sources(WebSocketTestApplication.class);
    }
}
