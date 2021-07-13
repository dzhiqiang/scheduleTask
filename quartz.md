### Quartz

1. 可以立即或者延迟执行任务
2. 可以周期执行定时任务,周期可以按照年，月，周，日，时，分，秒指定，比较灵活

[官方的例子](http://www.quartz-scheduler.org/documentation/quartz-2.2.2/examples/)

##### 第一个例子举例

[例子地址](http://www.quartz-scheduler.org/documentation/quartz-2.2.2/examples/Example1.html)

1. ###### 任务创建

   > 定时任务必须继承Job接口

```java
public class HelloJob implements Job {
    final Logger logger = LoggerFactory.getLogger(HelloJob.class);
    // 必须重写public void execute(JobExecutionContext context) throws JobExecutionException方法
    // JobExecutionContext:任务执行上下文，能得到Scheduler,Trigger等执行环境，JobExecutionContext会详细介绍作用
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("定时任务开始执行");
    }
}
```

2. ScheduleFactory首先创建ScheduleFactory

```java
// 创建Scheduler工厂
SchedulerFactory sf = new StdSchedulerFactory();
```

```java
SchedulerFactory接口
// 获取默认的Scheduler引用
Scheduler getScheduler() throws SchedulerException;
// 根据schedName名称获取Scheduler引用
Scheduler getScheduler(String schedName) throws SchedulerException;
// 返回所有已经的Scheduler集合
Collection<Scheduler> getAllSchedulers() throws SchedulerException;
```