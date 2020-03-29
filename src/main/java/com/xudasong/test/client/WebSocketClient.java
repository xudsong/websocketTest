package com.xudasong.test.client;

import com.alibaba.fastjson.JSON;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import com.xudasong.test.redis.RedisClusterCmdHashList;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisCluster;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@ServerEndpoint(value = "/release/websocket")
@Slf4j
@Data
public class WebSocketClient {

    //当前机器的连接数
    private static int websocketCount = 0;

    //心跳检查开关
    private static boolean isCheckHeartbeat = true;

    //上次心跳时间
    private Long lastHeartbeatTime;

    //心跳间隔
    private static final int HEARTBEATINTERVAL = 10*1000;

    //检测没收到心跳导致离线的次数
    private static final int DETECTOFFLINEINTERVAL = 2;
    //标识连接用户
    private String userId;
    //登录超时时间，10秒
    private static final int LOGOUTTIME = 10*1000;

    //redis中标识已登录用户id
    private static final String LOGINEDUSERS = "loginedUsers";

    private Session session = null;
    private WebSocketMessage webSocketMessage;

    //已登录的连接
    private static ConcurrentHashMap<String,WebSocketClient> loginedClientsMap = new ConcurrentHashMap<>();

    //concurrent包的线程安全set，用来存放每个客户端对应的Websocket对象
    private static CopyOnWriteArraySet<WebSocketClient> unknowClients = new CopyOnWriteArraySet<>();

    private static ApplicationContext applicationContext;

    public static void setApplicationContext(ApplicationContext applicationContext){
        WebSocketClient.applicationContext = applicationContext;
        startCheckRedisCmd();
        startCheckHeartbeat();
    }

    private static void startCheckRedisCmd(){
        final RedisClusterCmdHashList chl = applicationContext.getBean(RedisClusterCmdHashList.class);
        //创建一个线程从redis读取指令集，看指令对应websocket是否本机
        //此线程只会在启动时创建一次
        new Thread(new Runnable() {
            @Override
            public void run() {
                log.info("setApplicationContext start ...");
                while (true){
                    //从redis中获取所有事件
                    Set<String> fields = null;
                    try {
                        fields = chl.getHashFields();
                    }catch (Exception e){
                        log.error("WebSocketClient startCheckRedisCmd getHashFields failed",e);
                        try {
                            Thread.sleep(1000);
                        }catch (Exception ex){
                            log.error("startCheckRedisCmd getHashFields sleep failed,{}",ex);
                        }
                    }
                    //为空忽略
                    if (null == fields || fields.isEmpty()){
                        try {
                            Thread.sleep(1000);
                        }catch (Exception e){
                            log.error("setApplicationContext,{}",e);
                        }
                        continue;
                    }

                    //遍历所有事件，并检查websocket连接是否在本机
                    for (String field:fields){
                        String value = null;
                        try {
                            value = chl.getHashValueByField(field);
                        }catch (Exception e){
                            log.error("WebSocketClient startCheckRedisCmd getHashFields failed,field={},{}",field,e);
                            continue;
                        }
                        WebSocketCmd cmd = JSON.parseObject(value,WebSocketCmd.class);
                        if (cmd == null){
                            log.error("根据key获取cmd失败，field：{}",field);
                            try {
                                chl.removeKey(field);
                            }catch (Exception e){
                                log.error("WebSocketClient startCheckRedisCmd removeKey failed,field={},{}",field,e);
                            }
                            continue;
                        }

                        //判断是否超时
                        if (System.currentTimeMillis() - cmd.getTime() > 20*1000){
                            log.error("命令：field={} value={} 处理失败，超时",field,value);
                            try {
                                chl.removeKey(field);
                            }catch (Exception e){
                                log.error("WebSocketClient startCheckRedisCmd removeKey failed,field={},{}",field,e);
                                continue;
                            }
                        }
                        try {
                            //根据事件类型处理相应的业务逻辑
                            if (onRedisCmd(cmd)){
                                chl.removeKey(field);
                            }
                        }catch (Exception e){
                            log.error("WebSocketClient startCheckRedisCmd removeKey failed,field={},{}",field,e);
                        }
                    }
                    try {
                        Thread.sleep(300);
                    }catch (Exception e){
                        log.error("setApplicationContext,{}",e);
                    }

                }
            }
        }).start();
    }

    /**
     * 心跳检测
     */
    private static void startCheckHeartbeat(){
        log.info("startCheckHeartbeat start...");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isCheckHeartbeat){
                    try {
                        Iterator<Map.Entry<String,WebSocketClient>> iterator = loginedClientsMap.entrySet().iterator();
                        while (iterator.hasNext()){
                            WebSocketClient webSocketClient = iterator.next().getValue();
                            if (null != webSocketClient){
                                webSocketClient.checkHeartbeat();
                            }
                        }
                        Thread.sleep(1000);
                    }catch (Exception e){
                        log.warn("startCheckHeartbeat",e);
                    }
                }
            }
        });
        thread.start();
    }

    private void checkHeartbeat(){
        long interval = System.currentTimeMillis()-lastHeartbeatTime;
        if (interval < HEARTBEATINTERVAL){
            return;
        }
        if (interval >= DETECTOFFLINEINTERVAL * HEARTBEATINTERVAL){
            log.error("userId:{},连续{}次未收到心跳，websocket异常中断",userId,DETECTOFFLINEINTERVAL);
            close();
            onclose();
            return;
        }
        WebSocketCmd webSocketCmd = new WebSocketCmd();
        webSocketCmd.setCmd(WebSocketCmd.CmdPing);
        webSocketCmd.setTime(System.currentTimeMillis());
        webSocketCmd.setUnionId(userId);
        sendMessage(JSON.toJSONString(webSocketCmd));
    }

    @OnClose
    public void onclose(){
        log.info("onclose,userId:{}",userId);
        --websocketCount;
        unknowClients.remove(this);
        if (StringUtils.isEmpty(userId)){
            log.warn("未标识websocket断开连接，移除成功");
            return;
        }else {
            log.info("已标识websocket断开连接，需移除并通知");
        }
        loginedClientsMap.remove(userId);
    }

    /**
     * 建立连接，需要前端把客户端信息带过来
     * @param clientInfo
     * @param session
     */
    @OnOpen
    public void onOpen(@PathParam("clientInfo") String clientInfo,Session session){
        log.info("open");
        this.session = session;
        webSocketMessage = applicationContext.getBean(WebSocketMessage.class);
        unknowClients.add(this);
        ++websocketCount;
        log.info("有新连接加入！当前在线人数为：{}",websocketCount);
        //10秒检查本次的连接是否有登录操作，如没有登录则断开连接或重连
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                for (WebSocketClient client:unknowClients){
                    if (client == WebSocketClient.this){
//                    log.warn(LOGOUTTIME + "秒未登录，超时关闭。");
//                    client.session.close();
                        //重连
                        log.warn(LOGOUTTIME + "秒未登录，重连");
                        --websocketCount;
                        client.onOpen(clientInfo,session);
                    }
                }
            }
        },LOGOUTTIME);
    }

    @OnError
    public void onError(Session session,Throwable error){
        log.warn("websocket onerroe userid={}",userId,error);
    }

    /**
     * 收到客户端消息后调用的方法
     * @param message
     * @param session
     */
    @OnMessage
    public void onMessage(String message,Session session){
        log.info("onMessage收到消息：{}，userId:{}",message,userId);
        this.lastHeartbeatTime = System.currentTimeMillis();
        WebSocketCmd cmd = JSON.parseObject(message,WebSocketCmd.class);
        cmd.setTime(System.currentTimeMillis());
        //根据不同指令做不同的操作
        switch (cmd.getCmd()){
            //用户操作
            case WebSocketCmd.CmdUserCall:
                webSocketMessage.onUserCall(cmd,this);
                break;
            case WebSocketCmd.CmdPong:
                break;
             //客户端ping：发返回信息至客户端，把上次心跳时间设置为当前时间
            case WebSocketCmd.CmdPing:
                WebSocketCmd webSocketCmdPong = new WebSocketCmd();
                webSocketCmdPong.setCmd(WebSocketCmd.CmdPong);
                webSocketCmdPong.setUnionId(userId);
                webSocketCmdPong.setTime(System.currentTimeMillis());
                sendMessage(JSON.toJSONString(webSocketCmdPong));
                break;
             default:
                 log.info("未找到对应指令：{}",cmd.getCmd());
                 break;
        }
    }

    /**
     * 从redis取出指令，看是否能在本服务器处理，处理成功返回true
     * @param cmd
     * @return
     */
    public static boolean onRedisCmd(WebSocketCmd cmd){
        //根据不同指令处理相应业务
        switch (cmd.getCmd()){
            case WebSocketCmd.CmdInvalidParams:
                WebSocketClient webSocketClient = getUserWebSocket(cmd.getUnionId());
                if (webSocketClient == null){
                    log.warn("当前服务器找不到连接，unionId={}",cmd.getUnionId());
                    return false;
                }
                log.info("当前服务器找到连接，unionId={}",cmd.getUnionId());
                return webSocketClient.sendMessage(JSON.toJSONString(cmd));
             //其他指令
            default:
                log.info("未找到对应指令：{}",JSON.toJSONString(cmd));
                return false;
        }
    }

    /**
     * 发送消息
     * @param message
     * @return
     */
    public boolean sendMessage(String message){
        if (this.session == null){
            return false;
        }
        if (!this.session.isOpen()){
            log.error("sendMessage session is closed,message={}",message);
            close();
            return false;
        }
        try {
            this.session.getBasicRemote().sendText(message);
        }catch (Exception e){
            log.error("sendMessage failed {}",e);
            return false;
        }
        log.info("sendMessage发送消息：{}",message);
        return true;
    }

    protected void close(){
        if (this.session == null){
            return;
        }
        try {
            this.session.close();
        }catch (IOException e){
            log.error("WebSocketClient close {}",e);
        }
    }

    /**
     * 发送错误消息
     * @param cmdString
     * @param message
     * @param unionId
     */
    public void sendErrorCmd(String cmdString,String message,String unionId){
        WebSocketCmd reLoginCmd = new WebSocketCmd();
        reLoginCmd.setCmd(cmdString);
        reLoginCmd.setMsg(message);
        reLoginCmd.setTime(System.currentTimeMillis());
        reLoginCmd.setUnionId(unionId);
        sendMessage(JSON.toJSONString(reLoginCmd));
    }

    /**
     * 根据用户id获取连接
     * @param unionId
     * @return
     */
    public static WebSocketClient getUserWebSocket(String unionId){
        if (StringUtils.isEmpty(unionId)){
            return null;
        }
        WebSocketClient webSocketClient = loginedClientsMap.get(unionId);
        if (null != webSocketClient){
            return webSocketClient;
        }else {
            return null;
        }
    }

    protected synchronized boolean onlogin(){
        JedisCluster jedisCluster = applicationContext.getBean(JedisCluster.class);
        //判断当前是否登录，如果存在则移除本次连接，使用之前的
        WebSocketClient oldWebSocketClient = getUserWebSocket(this.userId);
        if (oldWebSocketClient != null){
            if (oldWebSocketClient.session.isOpen()){
                //原连接未关闭，使用之前的，将现在的置空
                log.warn("onlogin,already exist websocket,close new websocket,userId:{}",this.userId);
                this.userId = null;
                return false;
            }else {
                log.warn("onlogin,already exist websocket,but it's closed,remove it,userId:{}",this.userId);
                //原连接已关闭，移除并使用现在连接
                unknowClients.remove(oldWebSocketClient);
                loginedClientsMap.remove(oldWebSocketClient.getUserId());
                loginedClientsMap.put(userId,this);
                unknowClients.remove(this);
                oldWebSocketClient.close();
            }
        }else {
            //判断该id是否在其他服务器已登录，如登录移除本次连接
            Boolean longinFlag = jedisCluster.hexists(LOGINEDUSERS,this.userId);
            if (longinFlag){
                log.warn("userId:{},已在其他服务器登录",this.userId);
                this.userId = null;
                return false;
            }else {
                loginedClientsMap.put(userId,this);
                unknowClients.remove(this);
            }
        }
        try {
            //在redis中标识该userId已有websocket连接
            String ads = InetAddress.getLocalHost().getHostAddress();
            jedisCluster.hset(LOGINEDUSERS,this.userId,ads);
            jedisCluster.expire(LOGINEDUSERS,60*60*24);
        }catch (Exception e){
            log.error("onlogin hset error {}",e);
        }
        return true;
    }


}
