package com.xudasong.test.tools.excelTool;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TableHeader {

    private String key;
    private String name;
    private ValueHandler handler;

    public static TableHeader simpleHeader(String key,String name){
        return new TableHeader(key,name,null);
    }

}
