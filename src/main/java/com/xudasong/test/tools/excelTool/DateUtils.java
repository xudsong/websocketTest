package com.xudasong.test.tools.excelTool;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtils {

    public static final String DAY_PATTERN = "yyyy-MM-dd";
    public static final String HOUR_PATTERN = "HH:mm:ss";

    public static LocalDateTime toLocalDateTime(Date date){
        Instant instant = Instant.ofEpochMilli(date.getTime());
        ZoneId zone = ZoneId.systemDefault();
        return LocalDateTime.ofInstant(instant,zone);
    }

    public static Date toDate(LocalDateTime localDateTime){
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date toDate(LocalDate localDate){
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    public static String getCurrentDayStr(){
        return toStr(new Date(),DAY_PATTERN);
    }

    public static String toStr(Date date,String pattern){
        return DateFormatUtils.format(date,pattern);
    }

    public static Date getCurrentDayStart(){
        return toDate(LocalDate.now().atStartOfDay());
    }

    public static Date getCurrentDayEnd(){
        return toDate(LocalDate.now().atTime(23,59,59));
    }

    public static LocalTime getLocalTime(String str){
        return LocalTime.parse(str, DateTimeFormatter.ofPattern(HOUR_PATTERN));
    }

    public static Date getDateStart(Date startDate){
        return toDate(toLocalDateTime(startDate).toLocalDate());
    }

    public LocalDateTime getDateStart(LocalDateTime localDateTime){
        return localDateTime.toLocalDate().atStartOfDay();
    }

    public static Date getDateEnd(Date endDate){
        LocalDateTime localDateTime = toLocalDateTime(endDate).toLocalDate().atTime(23,59,59);
        return toDate(localDateTime);
    }

    public LocalDateTime getDateEnd(LocalDateTime localDateTime){
        return localDateTime.toLocalDate().atTime(23,59,59);
    }

    public static Date strToDate(String str){
        Date date;
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            date = dateFormat.parse(str);
        }catch (Exception e){
            return null;
        }
        return date;
    }

}
