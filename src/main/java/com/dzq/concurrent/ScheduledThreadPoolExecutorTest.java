package com.dzq.concurrent;

import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledThreadPoolExecutorTest {
    // 增加定时任务
    @Test
    public void test_01() throws InterruptedException {

        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);
        System.out.println("加载任务"+ System.currentTimeMillis());
        scheduledThreadPool.schedule(new Runnable() {
            public void run() {
                System.out.println("开始执行1---" + System.currentTimeMillis());
                try {
                    Thread.sleep(2000);
                    System.out.println("执行结束1---"+ System.currentTimeMillis());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 1000, TimeUnit.MILLISECONDS);
        scheduledThreadPool.schedule(new Runnable() {
            public void run() {
                System.out.println("开始执行2---" + System.currentTimeMillis());
                try {
                    Thread.sleep(2000);
                    System.out.println("执行结束2---"+ System.currentTimeMillis());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 100, TimeUnit.MILLISECONDS);
        System.out.println("加载完成"+ System.currentTimeMillis());

        Thread.currentThread().join();
    }

}
