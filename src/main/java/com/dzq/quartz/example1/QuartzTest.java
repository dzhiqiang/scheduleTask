package com.dzq.quartz.example1;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Date;

public class QuartzTest {


    public static void main(String[] args) throws SchedulerException, InterruptedException {

        SchedulerFactory sf = new StdSchedulerFactory();
        Scheduler sched = sf.getScheduler();

        JobDetail job = JobBuilder.newJob(HelloJob.class)
                .withIdentity("job1", "group1")
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger1", "group1")
                .startAt(new Date())
                .build();

        sched.scheduleJob(job, trigger);

        sched.start();

        Thread.sleep(5L * 1000L);

        sched.shutdown(true);

    }


}
