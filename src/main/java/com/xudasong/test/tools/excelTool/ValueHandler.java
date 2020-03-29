package com.xudasong.test.tools.excelTool;

import com.alibaba.fastjson.JSONObject;

public interface ValueHandler {
    String handle(Object value, JSONObject jsonRowData);
}
