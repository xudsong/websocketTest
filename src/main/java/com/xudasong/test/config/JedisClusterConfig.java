package com.xudasong.test.config;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class JedisClusterConfig {

    @Autowired
    private RedisClusterProperties redisClusterProperties;

    @Bean
    public JedisCluster getJedisCluster(){
        String[] serverArray = redisClusterProperties.getClusterNode().split(",");
        Set<HostAndPort> nodes = new HashSet<>();
        for (String ipPort : serverArray){
            String[] ipPortPair = ipPort.split(":");
            nodes.add(new HostAndPort(ipPortPair[0].trim(),Integer.valueOf(ipPortPair[1].trim())));
        }
        return new JedisCluster(nodes,redisClusterProperties.getExpireSeconds(),1000,3,redisClusterProperties.getPwd(),new GenericObjectPoolConfig());
    }

}
