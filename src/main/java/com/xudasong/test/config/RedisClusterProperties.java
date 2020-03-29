package com.xudasong.test.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedisClusterProperties {
    private String clusterNode;
    private String pwd;
    private Integer expireSeconds;
}
