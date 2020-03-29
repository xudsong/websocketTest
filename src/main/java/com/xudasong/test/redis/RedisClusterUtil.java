package com.xudasong.test.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class RedisClusterUtil {

    private static final String LOCK_SUCESS = "OK";
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";
    private static final Long RELEASE_SUCESS = 1L;

    @Autowired
    private JedisCluster jedisCluster;

    public String setex(String key,String value,Integer seconds){
        String statusCode = jedisCluster.setex(key,seconds,value);
        return statusCode;
    }

    public Long setnx(String key,String value)throws Exception{
        return jedisCluster.setnx(key,value);
    }

    public String set(String key,String value)throws Exception{
        return jedisCluster.set(key,value);
    }

    public Long hset(String key,String field,String value)throws Exception{
        Long result = jedisCluster.hset(key,field,value);
        return result;
    }

    public Set<String> hget(String key)throws Exception{
        return jedisCluster.hkeys(key);
    }

    public String hget(String key,String field)throws Exception{
        return jedisCluster.hget(key,field);
    }

    public Long hdel(String key,String ...fields)throws Exception{
        return jedisCluster.hdel(key,fields);
    }

    public Boolean isKeyExist(String key)throws Exception{
        Boolean result = false;
        result = jedisCluster.exists(key);
        return result;
    }

    public String setString(String key,String value)throws Exception{
        return jedisCluster.set(key,value);
    }

    public String getString(String key)throws Exception{
        String value = "";
        value = jedisCluster.get(key);
        return value;
    }

    public Long leftPushStringToList(String key,String... value)throws Exception{
        Long result = null;
        result = jedisCluster.lpush(key,value);
        log.info("RedisCluster:leftPushStringToList:{}",jedisCluster.lrange(key,0,100));
        return result;
    }

    public Long rightPushStringToList(String key,String... value)throws Exception{
        Long result = null;
        result = jedisCluster.rpush(key,value);
        log.info("RedisCluster:rightPushStringToList:{}",jedisCluster.lrange(key,0,100));
        return result;
    }

    public Long removeValueFromList(String key,String value)throws Exception{
        Long result = null;
        result = jedisCluster.lrem(key,0,value);
        return result;
    }

    public Long deleteKey(String key)throws Exception{
        Long result = null;
        result = jedisCluster.del(key);
        return result;
    }

    public String lTrimKeyFromList(String key,long start,long end)throws Exception{
        String result = null;
        result = jedisCluster.ltrim(key,start,end);
        return result;
    }

    public String leftPopStringFromList(String key)throws Exception{
        String pop = null;
        pop = jedisCluster.lpop(key);
        log.info("RedisCluster:leftPopStringFromList:{}",jedisCluster.lrange(key,0,100));
        return pop;
    }

    public String rightPopStringFromList(String key)throws Exception{
        String pop = null;
        pop = jedisCluster.rpop(key);
        log.info("RedisCluster:rightPopStringFromList:{}",jedisCluster.lrange(key,0,100));
        return pop;
    }

    public Long expireTime(String key,Integer second)throws Exception{
        Long result = null;
        result = jedisCluster.expire(key,second);
        return result;
    }

    public List<String> hvals(String key){
        return jedisCluster.hvals(key);
    }

    public Set<String> hkeys(String key){
        return jedisCluster.hkeys(key);
    }

    public Long setAdd(String key,String member){
        return jedisCluster.sadd(key,member);
    }

    public void setDel(String key){
        jedisCluster.srem(key);
    }

    public void setDel(String key,String value){
        jedisCluster.srem(key,value);
    }

    public Set<String> setSelect(String key){
        Set<String> set = jedisCluster.smembers(key);
        return set;
    }

    public boolean setIsMemberExist(String key,String member){
        return jedisCluster.sismember(key,member);
    }

    public boolean tryGetDistributedLock(String lockKey,String value,int expireTime){
        String result = jedisCluster.set(lockKey,value,SET_IF_NOT_EXIST,SET_WITH_EXPIRE_TIME,expireTime);
        if (LOCK_SUCESS.equals(result)){
            return true;
        }
        return false;
    }

    public boolean releaseDistributedLock(String lockKey,String value){
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = jedisCluster.eval(script, Collections.singletonList(lockKey),Collections.singletonList(value));
        if (RELEASE_SUCESS.equals(result)){
            return true;
        }
        return false;
    }

}
