package cn.lunadeer.essentialsd.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Time {
   public static Integer getCurrent() {
      return Integer.parseInt((new SimpleDateFormat("yyyyMMdd")).format(new Date()));
   }

   public static Integer getFromTimestamp(Long timestamp_ms) {
      return Integer.parseInt((new SimpleDateFormat("yyyyMMdd")).format(new Date(timestamp_ms)));
   }
}
