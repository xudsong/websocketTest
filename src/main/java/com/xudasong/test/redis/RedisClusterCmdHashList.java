package com.xudasong.test.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RedisClusterCmdHashList {

    protected static final String HashKeyName = "avr";

    @Autowired
    private RedisClusterUtil redisClusterUtil;

    public Set<String> getHashFields()throws Exception{
        return redisClusterUtil.hget(HashKeyName);
    }

    public String getHashValueByField(String field)throws Exception{
        return redisClusterUtil.hget(HashKeyName,field);
    }

    public Long removeKey(String field)throws Exception{
        return redisClusterUtil.hdel(HashKeyName, field);
    }

}
