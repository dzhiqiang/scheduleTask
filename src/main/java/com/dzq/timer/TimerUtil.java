package com.dzq.timer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class TimerUtil {

    public static void addScheduleTimerTask(final int count, final long sleep, Timer timer, long delay) {

        for (int i = 0; i < count; i++) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("开始执行" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                    sleep(sleep);
                }
            }, delay);
        }
    }

    public static void addScheduleTimerTask(final int count, final long sleep, Timer timer, Date delay) {

        for (int i = 0; i < count; i++) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("开始执行" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                    sleep(sleep);
                }
            }, delay);
        }
    }

    public static void addScheduleTimerTask(final int count, final long sleep, Timer timer, long delay,long period) {

        for (int i = 0; i < count; i++) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("开始执行" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                    sleep(sleep);
                }
            }, delay, period);
        }
    }

    public static void addScheduleTimerTask(final int count, final long sleep, Timer timer, Date delay,long period) {

        for (int i = 0; i < count; i++) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("开始执行" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                    sleep(sleep);
                }
            }, delay,period);
        }
    }

    private static void sleep(long sleep) {
        if (sleep > 0) {
            try {
                Thread.sleep(sleep);
                System.out.println("结束执行" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            } catch (InterruptedException e) {
                //skip
            }
        }
    }

    public static void addScheduleAtFixedRateTimerTask(final int count, final long sleep, Timer timer, long delay,long period) {

        for (int i = 0; i < count; i++) {
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("开始执行" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                    sleep(sleep);
                }
            }, delay, period);
        }
    }

    public static void addScheduleAtFixedRateTimerTask(final int count, final long sleep, Timer timer, Date delay,long period) {

        for (int i = 0; i < count; i++) {
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("开始执行" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                    sleep(sleep);
                }
            }, delay, period);
        }
    }
}
