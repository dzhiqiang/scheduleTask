package com.dzq.quartz.example1;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class TaskJob implements Job {
    private Runnable runnable;
    public TaskJob(Runnable runnable) {
        this.runnable = runnable;
    }
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        this.runnable.run();
    }
}
