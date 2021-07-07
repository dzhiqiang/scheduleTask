package com.dzq.timer;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;

public class TimerTest {

    private static final long ONE_SECOND = 1000;
    /**
     * 验证一个执行时间稍长定时任务导致其他任务推迟
      */
    @Test
    public void test_01() {
        Timer timer = new Timer();

        TimerUtil.addScheduleTimerTask(5, 2 * ONE_SECOND, timer, ONE_SECOND);
    }

    /**
     * 验证schedule和scheduleAtFixedRate区别
     * schedule
     * 在10秒之前和当前时间只执行1次
     */
    @Test
    public void test_02() throws InterruptedException {

        Timer timer = new Timer();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -10);
        // schedule开始时间为当前时间前5秒
        TimerUtil.addScheduleTimerTask(1, 0, timer, calendar.getTime(), 3 * ONE_SECOND);

        Thread.sleep(20 * ONE_SECOND);

    }
    /**
     * 验证schedule和scheduleAtFixedRate区别
     * scheduleAtFixedRate
     * 在10秒之前和当前时间执行多次
     */
    @Test
    public void test_03() throws InterruptedException {

        Timer timer = new Timer();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -10);
        // schedule开始时间为当前时间前5秒
        TimerUtil.addScheduleAtFixedRateTimerTask(1, 0, timer, calendar.getTime(), 3 * ONE_SECOND);

        Thread.sleep(20 * ONE_SECOND);

    }

}
