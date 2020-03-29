package com.xudasong.test.client;

public class WebSocketCmd {

    /**
     * 重复登录
     */
    public static final String CmdRepeatLogin = "repeat_login";
    /**
     * 用户操作
     */
    public static final String CmdUserCall = "call";

    /**
     * 参数不合法
     */
    public static final String CmdInvalidParams = "invalid_params";

    //消息类型
    private String cmd;

    private long time;
    private String unionId;
    private String msg;
    private Integer code;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    //心跳检测
    public static final String CmdPing = "ping";
    public static final String CmdPong = "pong";

    public String getUnionId() {
        return unionId;
    }

    public void setUnionId(String unionId) {
        this.unionId = unionId;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
