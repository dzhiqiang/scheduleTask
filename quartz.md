### Quartz

[源码地址](https://github.com/quartz-scheduler/quartz.git)

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

2. ScheduleFactory

```java
// 创建Scheduler工厂，StdSchedulerFactory()无参构造方法，里面没有任何代码执行
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

ScheduleFactory有2个实现类

1. StdSchedulerFactory：根据配置文件读取属性，配置文件的位置在org/quartz/quartz.properties
   也可以定义其他文件，需要配置系统属性org.quartz.properties
2. DirectSchedulerFactory：SchedulerFactory所需要的配置参数，直接在createScheduler方法中传入，要对DirectSchedulerFactory所依赖的属性比较了解

```java
// 范例
DirectSchedulerFactory.getInstance().createScheduler("My Quartz Scheduler", "My Instance", threadPool, jobStore, "localhost", 1099);
```

3. 从org.quartz.impl.StdSchedulerFactory#getScheduler()入手看SchedulerFactory到底做了什么

   > 注意：Scheduler类在示例的后面的代码中需要执行定时任务，执行org.quartz.impl.StdScheduler#scheduleJob(org.quartz.JobDetail, org.quartz.Trigger)方法，所以在org.quartz.impl.StdSchedulerFactory#getScheduler()为构造Scheduler示例还是做了不少事情的，下面具体分析

##### 关键方法

> 在获取Schedule过程中比较重要的是：第9步ThreadPool启动执行QuartzSchedulerThread提交的Trigger和第12步QuartzSchedulerThread启动，QuartzSchedulerThread线程是查询可执行的trigger放入到线程池执行

```java
// 获取默认的getScheduler
public Scheduler getScheduler() throws SchedulerException {
    // cfg：PropertiesParser对配置文件的封装
    // 如果为空说明没初始化，没有加载数据，则开始初始化
    if (cfg == null) {
        // 方法比较简单，过长，就不展示代码了
        // 1. 查找配置文件，把配置文件数据加载到properties中
        // 2. 把系统属性加载到properties中，如果有相同的key也会进行覆盖
        // 3. 把properties封装到PropertiesParser中
        // 加载配置文件或者配置的属性策略可以学习
        initialize();
    }
    // 饱汉式单例的SchedulerRepository，里面封装Map<String, Scheduler>
    SchedulerRepository schedRep = SchedulerRepository.getInstance();
    // 根据schedule名称得到Schedule，从PropertiesParser中获取属性为org.quartz.scheduler.instanceName: DefaultQuartzScheduler
    Scheduler sched = schedRep.lookup(getSchedulerName());
    if (sched != null) {
        // 如果已经关闭则移除，否则返回
        if (sched.isShutdown()) {
            schedRep.remove(getSchedulerName());
        } else {
            return sched;
        }
    }
    // 如果为空则进行初始化
    // 1. 通过配置找到schedule名称,线程名称,schedInstId的生成规则
    // 2. 生成类加载辅助类：主要是加载类
    // 3. 判断是否使用jmx客户端，调用方式参数配置
    // 4. 判断是否使用rmi调用方式
    // 5. 创建调用时使用的类，通过load加载，加载后通过propertie文件对类进行赋值
    //    5.1 JobFactory 用于生成Job，用户定义的需要执行的任务
    //    5.2 InstanceIdGenerator 用于生成schedInstId,其他类是通过schedName和schedInstId关联到Schedule
    //    5.3 设置线程池ThreadPool
    //    5.4 生成JobStore,并设置schedName, schedInstId，如果是JobStoreSupport，生成Semaphore示例放入到JobStoreSupport中
    //    5.5 生成数据链接提供者可以有多个，如果没有配置数据源，还可以通过JNDI的方式获取cp，没有JNDI可以选择poolingProvider,根据数据连接提供名称存储到DBConnectionManager中
    //    5.6 生成SchedulerPlugin
    //    5.7 生成JobListener
    //    5.8 生成TriggerListener
    //    5.9 生成ThreadExecutor
    //    5.10 启动所有fire everything up
    //    5.11 创建JobRunShellFactory
    // 6. 创建QuartzSchedulerResources，对属性赋值，一些属性暂时未知什么作用
    //        rsrcs.setName(schedName);
    //        rsrcs.setThreadName(threadName);
    //        rsrcs.setInstanceId(schedInstId);
    //        rsrcs.setJobRunShellFactory(jrsf);
    //        rsrcs.setMakeSchedulerThreadDaemon(makeSchedulerThreadDaemon);
    //        rsrcs.setThreadsInheritInitializersClassLoadContext(threadsInheritInitalizersClassLoader);
    //        rsrcs.setBatchTimeWindow(batchTimeWindow);
    //        rsrcs.setMaxBatchSize(maxBatchSize);
    //        rsrcs.setInterruptJobsOnShutdown(interruptJobsOnShutdown);
    //        rsrcs.setInterruptJobsOnShutdownWithWait(interruptJobsOnShutdownWithWait);
    //        rsrcs.setJMXExport(jmxExport);
    //        rsrcs.setJMXObjectName(jmxObjectName);
    //        if (managementRESTServiceEnabled) {
    //            ManagementRESTServiceConfiguration managementRESTServiceConfiguration = new ManagementRESTServiceConfiguration();
    //            managementRESTServiceConfiguration.setBind(managementRESTServiceHostAndPort);
    //            managementRESTServiceConfiguration.setEnabled(managementRESTServiceEnabled);
    //            rsrcs.setManagementRESTServiceConfiguration(managementRESTServiceConfiguration);
    //        }
    //        if (rmiExport) {
    //            rsrcs.setRMIRegistryHost(rmiHost);
    //            rsrcs.setRMIRegistryPort(rmiPort);
    //            rsrcs.setRMIServerPort(rmiServerPort);
    //            rsrcs.setRMICreateRegistryStrategy(rmiCreateRegistry);
    //            rsrcs.setRMIBindName(rmiBindName);
    //        }
    // 7. 对ThreadPool设置schedName, schedInstId
    // 8. rsrcs.setThreadExecutor(threadExecutor);
    // 9. rsrcs.setThreadPool(tp); 并tp初始化
    //    tp.initialize();
    // 10. rsrcs.setJobStore(js);
    // 11. add plugins
    // 12. 创建qs QuartzScheduler(rsrcs,...) 关联上rsrcs,创建属性QuartzSchedulerThread并执行，创建SchedulerSignalerImpl
    // 13. 创建scheduler StdScheduler(qs) 关联上qs
    // 14. 插件初始化
    // 15. add listeners:jobListeners,triggerListeners
    // 16. scheduler上下文属性context放入数据，数据来源于properties配置
    // 17. job store启动关联schedInstId，schedName
    // 18. jrsf初始化
    // 19. qs初始化，绑定Scheduler
    // 20. SchedulerRepository绑定scheduler,返回scheduler
    sched = instantiate();
    return sched;
}

```

##### ![quartz类图](https://raw.githubusercontent.com/dzhiqiang/PicGo-gallery/main/quartz%E7%B1%BB%E5%9B%BE.png)

3. 线程池ThreadPool,SimpleThreadPool

   > 在创建时，放入到QuartzSchedulerResources中，然后调用initialize初始化

   1. 重要属性

      ```java
      // 所有线程
      private List<WorkerThread> workers;
      // 可用线程
      private LinkedList<WorkerThread> availWorkers = new LinkedList<WorkerThread>();
      // 工作中线程
      private LinkedList<WorkerThread> busyWorkers = new LinkedList<WorkerThread>();
      ```
      
   2. 重要方法
   
      ```java
      // 初始化线程池，根据线程池个数创建线程，分配到可用线程
      public void initialize() throws SchedulerConfigException {
      		// 已经初始化
              if(workers != null && workers.size() > 0) // already initialized...
                  return;
          	// 校验已经忽略
          	// 决定线程组
              if(isThreadsInheritGroupOfInitializingThread()) {
                  threadGroup = Thread.currentThread().getThreadGroup();
              } else {
                  // follow the threadGroup tree to the root thread group.
                  threadGroup = Thread.currentThread().getThreadGroup();
                  ThreadGroup parent = threadGroup;
                  while ( !parent.getName().equals("main") ) {
                      threadGroup = parent;
                      parent = threadGroup.getParent();
                  }
                  threadGroup = new ThreadGroup(parent, schedulerInstanceName + "-SimpleThreadPool");
                  if (isMakeThreadsDaemons()) {
                      threadGroup.setDaemon(true);
                  }
              }
      
      
              if (isThreadsInheritContextClassLoaderOfInitializingThread()) {
                  getLog().info(
                          "Job execution threads will use class loader of thread: "
                                  + Thread.currentThread().getName());
              }
      
              // 创建线程，放入到属性workers
              Iterator<WorkerThread> workerThreads = createWorkerThreads(count).iterator();
          	// 启动线程，并加入availWorkers
              while(workerThreads.hasNext()) {
                  WorkerThread wt = workerThreads.next();
                  wt.start();
                  availWorkers.add(wt);
              }
          }
      ```
   
      ```java
      // 执行任务
      public boolean runInThread(Runnable runnable) {
          if (runnable == null) {
              return false;
          }
          synchronized (nextRunnableLock) {
              handoffPending = true;
              // Wait until a worker thread is available
              while ((availWorkers.size() < 1) && !isShutdown) {
                  try {
                      nextRunnableLock.wait(500);
                  } catch (InterruptedException ignore) {
                  }
              }
              // 执行任务
              if (!isShutdown) {
                  WorkerThread wt = (WorkerThread)availWorkers.removeFirst();
                  busyWorkers.add(wt);
                  wt.run(runnable);
              } else {
                  // If the thread pool is going down, execute the Runnable
                  // within a new additional worker thread (no thread from the pool).
                  WorkerThread wt = new WorkerThread(this, threadGroup,
                                                     "WorkerThread-LastJob", prio, isMakeThreadsDaemons(), runnable);
                  busyWorkers.add(wt);
                  workers.add(wt);
                  wt.start();
              }
              nextRunnableLock.notifyAll();
              handoffPending = false;
          }
      
          return true;
      }
      ```
   
      
   
4. WorkerThread

   ```java
   // 判断是否在执行中
   private AtomicBoolean run = new AtomicBoolean(true);
   // 所属线程池
   private SimpleThreadPool tp;
   // 运行的任务
   private Runnable runnable = null;
   ```

   ```java
   @Override
   public void run() {
       boolean ran = false;
       while (run.get()) {
           try {
               synchronized(lock) {
               	// 如果为空且在运行中则暂停
                   while (runnable == null && run.get()) {
                       lock.wait(500);
                   }
   				// 不为空则执行
                   if (runnable != null) {
                       ran = true;
                       runnable.run();
                   }
               }
           } catch (InterruptedException unblock) {
               // 异常处理,只是打印日志，不会抛出异常
           } finally {
               synchronized(lock) {
                   runnable = null;
               }
               // repair the thread in case the runnable mucked it up...
               if(getPriority() != tp.getThreadPriority()) {
                   setPriority(tp.getThreadPriority());
               }
   			if (runOnce) {
               	run.set(false);
   				clearFromBusyWorkersList(this);
   			} else if(ran) {
   				ran = false;
   				makeAvailable(this);
   			}
   		}
   	}
   }
   ```

5. StdScheduler，最终调用QuartzScheduler的scheduleJob

   ```java
   // 执行JobDetail，执行时间Trigger，这2个类可以看下面分析
   public Date scheduleJob(JobDetail jobDetail, Trigger trigger)
           throws SchedulerException {
       // 跳转到QuartzScheduler执行
       return sched.scheduleJob(jobDetail, trigger);
   }
   ```

6. QuartzScheduler，最终向JobStore保存JobDetail和Trigger,被QuartzSchedulerThread获取，然后再线程池执行

   ```java
   public Date scheduleJob(JobDetail jobDetail,
                           Trigger trigger) throws SchedulerException {
       // 校验
       OperableTrigger trig = (OperableTrigger)trigger;
   	// trigger和job关联
       if (trigger.getJobKey() == null) {
           trig.setJobKey(jobDetail.getKey());
       } else if (!trigger.getJobKey().equals(jobDetail.getKey())) {
           throw new SchedulerException(
               "Trigger does not reference given job!");
       }
   	// 校验
       trig.validate();
       // 第一次运行时间
       Calendar cal = null;
       if (trigger.getCalendarName() != null) {
           cal = resources.getJobStore().retrieveCalendar(trigger.getCalendarName());
       }
       Date ft = trig.computeFirstFireTime(cal);
   
       if (ft == null) {
           throw new SchedulerException(
               "Based on configured schedule, the given trigger '" + trigger.getKey() + "' will never fire.");
       }
   	// 保存jobDetail和trigger
       // RAMJobStore.java举例，Job存放在JobStore的group:HashMap<String, HashMap<JobKey, JobWrapper>>,HashMap<JobKey, JobWrapper> jobsByKey
       // RAMJobStore.java举例，Trigger存放在Map<JobKey, List<TriggerWrapper>> triggersByJob,HashMap<String, HashMap<TriggerKey, TriggerWrapper>> triggersByGroup,HashMap<TriggerKey, TriggerWrapper> triggersByKey,TreeSet<TriggerWrapper> timeTriggers
       resources.getJobStore().storeJobAndTrigger(jobDetail, trig);
       notifySchedulerListenersJobAdded(jobDetail);
       // 提示SchedulerThread已经有新的trigger新增了
       notifySchedulerThread(trigger.getNextFireTime().getTime());
       notifySchedulerListenersSchduled(trigger);
       return ft;
   }
   ```

   

7. QuartzSchedulerThread

   ```java
   public void run() {
       int acquiresFailed = 0;
       while (!halted.get()) {
           try {
               // 校验暂停，暂停则等待，比较经典的生产者消费者模型写法
               synchronized (sigLock) {
                   while (paused && !halted.get()) {
                       try {
                           // wait until togglePause(false) is called...
                           sigLock.wait(1000L);
                       } catch (InterruptedException ignore) {
                       }
                       acquiresFailed = 0;
                   }
                   if (halted.get()) {
                       break;
                   }
               }
               // 如果有失败的情况，则等待一会
               // 比如存储trigger用数据库的方式，数据库崩溃，不会直接退出，循环时等待
               if (acquiresFailed > 1) {
                   try {
                       long delay = computeDelayForRepeatedErrors(qsRsrcs.getJobStore(), acquiresFailed);
                       Thread.sleep(delay);
                   } catch (Exception ignore) {
                   }
               }
   			// 可用线程，如果无可用线程则会阻塞，所以availThreadCount > 0一直会是true
               int availThreadCount = qsRsrcs.getThreadPool().blockForAvailableThreads();
               if(availThreadCount > 0) {
                   List<OperableTrigger> triggers;
                   // 当前时间
                   long now = System.currentTimeMillis();
   				
                   clearSignaledSchedulingChange();
                   try {
                       // 获取最近时间执行的trigger idleWaitTime:30秒，availThreadCount：可用线程10，BatchTimeWindow：0
                       triggers = qsRsrcs.getJobStore().acquireNextTriggers(
                           now + idleWaitTime, Math.min(availThreadCount, qsRsrcs.getMaxBatchSize()), qsRsrcs.getBatchTimeWindow());
                       acquiresFailed = 0;
                       if (log.isDebugEnabled())
                           log.debug("batch acquisition of " + (triggers == null ? 0 : triggers.size()) + " triggers");
                   } catch (XXXException jpe) {
                       // 失败的话
                       acquiresFailed++;
                       continue;
                   }
   				// 不为空
                   if (triggers != null && !triggers.isEmpty()) {
   					
                       now = System.currentTimeMillis();
                       // 拿到最近执行的第一个
                       long triggerTime = triggers.get(0).getNextFireTime().getTime();
                       long timeUntilTrigger = triggerTime - now;
                       // 还未到执行时间
                       while(timeUntilTrigger > 2) { // 2：从判断可以执行到执行可能需要2ms
                           synchronized (sigLock) {
                               if (halted.get()) {// 循环时一直判断是否被关闭
                                   break;
                               }
                               // 判断是否有新的trigger加入，没有的话，则wait与当前时间的毫秒差
                               // 与QuartzScheduler方法scheduleJob中notifySchedulerThread(trigger.getNextFireTime().getTime());对应，当新增trigger时Scheduler会记录新增的下次执行时间并notifyAll，与已经获取的最早的trigger执行时间做比较，如果新增加的在则为flase，然后wait，否则if不成立，向下执行
                               if (!isCandidateNewTimeEarlierWithinReason(triggerTime, false)) {
                                   try {
                                       now = System.currentTimeMillis();
                                       timeUntilTrigger = triggerTime - now;
                                       if(timeUntilTrigger >= 1)
                                           sigLock.wait(timeUntilTrigger);
                                   } catch (InterruptedException ignore) {
                                   }
                               }
                           }
                           // 判断是否有新增的trigger，如果时间靠前则清空triggers，跳转到while (!halted.get())
                           if(releaseIfScheduleChangedSignificantly(triggers, triggerTime)) {
                               break;
                           }
                           // 到点唤醒，再次计算执行时间与当前时间的时间差
                           now = System.currentTimeMillis();
                           timeUntilTrigger = triggerTime - now;
                       }
   
                       // 如果为空则说明releaseIfScheduleChangedSignificantly为true，被清空，有新trigger加入
                       if(triggers.isEmpty())
                           continue;
   
                       // set triggers to 'executing'
                       List<TriggerFiredResult> bndles = new ArrayList<TriggerFiredResult>();
   
                       boolean goAhead = true;
                       synchronized(sigLock) {
                           goAhead = !halted.get();
                       }
                       if(goAhead) {
                           try {
                               // 根据triggers 得到TriggerFiredResult列表，并计算所有trigger的下次运行时间，如果下次运行时间不为空，继续添加到timeTriggers中
                               // TriggerFiredResult引用TriggerFiredBundle
                               // public TriggerFiredBundle(JobDetail job, OperableTrigger trigger, Calendar cal,
                                         // boolean jobIsRecovering, Date fireTime, Date scheduledFireTime,
                                         // Date prevFireTime, Date nextFireTime) {
                               //     this.job = job; //定时任务
                               //     this.trigger = trigger; //触发器
                               //     this.cal = cal;         //时间，null，未知含义
                               //     this.jobIsRecovering = jobIsRecovering;// 恢复，未知含义
                               //     this.fireTime = fireTime;              // 真正的执行时间
                               //     this.scheduledFireTime = scheduledFireTime; // 计划执行的时间
                               //     this.prevFireTime = prevFireTime;           // 上次执行时间
                               //     this.nextFireTime = nextFireTime;           // 下次执行时间
                               // }
                               List<TriggerFiredResult> res = qsRsrcs.getJobStore().triggersFired(triggers);
                               if(res != null)
                                   bndles = res;
                           } catch (SchedulerException se) {
                               qs.notifySchedulerListenersError(
                                   "An error occurred while firing triggers '"
                                   + triggers + "'", se);
                               //QTZ-179 : a problem occurred interacting with the triggers from the db
                               //we release them and loop again
                               for (int i = 0; i < triggers.size(); i++) {
                                   qsRsrcs.getJobStore().releaseAcquiredTrigger(triggers.get(i));
                               }
                               continue;
                           }
                       }
                       for (int i = 0; i < bndles.size(); i++) {
                           TriggerFiredResult result =  bndles.get(i);
                           TriggerFiredBundle bndle =  result.getTriggerFiredBundle();
                           Exception exception = result.getException();
   
                           if (exception instanceof RuntimeException) {
                               getLog().error("RuntimeException while firing trigger " + triggers.get(i), exception);
                               qsRsrcs.getJobStore().releaseAcquiredTrigger(triggers.get(i));
                               continue;
                           }
   
                           // it's possible to get 'null' if the triggers was paused,
                           // blocked, or other similar occurrences that prevent it being
                           // fired at this time...  or if the scheduler was shutdown (halted)
                           if (bndle == null) {
                               qsRsrcs.getJobStore().releaseAcquiredTrigger(triggers.get(i));
                               continue;
                           }
   
                           JobRunShell shell = null;
                           try {
                               // 创建执行脚本
                               shell = qsRsrcs.getJobRunShellFactory().createJobRunShell(bndle);
                               // 初始化
                               shell.initialize(qs);
                           } catch (SchedulerException se) {
                               qsRsrcs.getJobStore().triggeredJobComplete(triggers.get(i), bndle.getJobDetail(), CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR);
                               continue;
                           }
   						// 在线程池执行
                           if (qsRsrcs.getThreadPool().runInThread(shell) == false) {
                               // this case should never happen, as it is indicative of the
                               // scheduler being shutdown or a bug in the thread pool or
                               // a thread pool being used concurrently - which the docs
                               // say not to do...
                               getLog().error("ThreadPool.runInThread() return false!");
                               qsRsrcs.getJobStore().triggeredJobComplete(triggers.get(i), bndle.getJobDetail(), CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR);
                           }
   
                       }
   
                       continue; // while (!halted)
                   }
               } else { // if(availThreadCount > 0)
                   // should never happen, if threadPool.blockForAvailableThreads() follows contract
                   continue; // while (!halted)
               }
   			// triggers为空了，暂停一段时间或者添加任务时被唤醒，然后再次去查询
               long now = System.currentTimeMillis();
               long waitTime = now + getRandomizedIdleWaitTime();
               long timeUntilContinue = waitTime - now;
               synchronized(sigLock) {
                   try {
                       if(!halted.get()) {
                           // QTZ-336 A job might have been completed in the mean time and we might have
                           // missed the scheduled changed signal by not waiting for the notify() yet
                           // Check that before waiting for too long in case this very job needs to be
                           // scheduled very soon
                           if (!isScheduleChanged()) {
                               sigLock.wait(timeUntilContinue);
                           }
                       }
                   } catch (InterruptedException ignore) {
                   }
               }
   
           } catch(RuntimeException re) {
               getLog().error("Runtime error occurred in main trigger firing loop.", re);
           }
       } // while (!halted)
   
       // drop references to scheduler stuff to aid garbage collection...
       qs = null;
       qsRsrcs = null;
   }
   ```

##### quartz设计的思考

还是延迟或者周期性执行任务，执行时间不在是long类型和单位，而是一个计算执行时间的Trigger,然后Trigger与Task（在quartz叫Job）对应。

执行任务同样被拆分，1）谁执行任务2）任务执行，这次和jdk的任务不同的是，执行任务的时候有<u>**依赖**</u>,那么则携带参数Context

1. 如何把jdk中的task转为Job,适配器模式

```java
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
```

2. 异常处理加强印象：自己能处理的自己处理，自己没有处理方式的抛出异常
   
   1. quartz任务执行失败只打印日志，不影响其他执行的任务。
   
3. 设计模式

   1. 工厂模式：StdSchedulerFactory，创建StdScheduler的工厂，无参，所有的属性都是从classpath中的配置文件中获取，特定的工厂创建特定的类。一般无参数。

      ```
      // 无参数，就能够创建出来
      SchedulerFactory sf = new StdSchedulerFactory();
      ```

   2. 构造器模式：需要一些必须的参数，并不是所有的参数都是必须的，都具有一些行为意义，比如TriggerBuilder.startAt(),并不是属性，而是具有一定的实际意义。在什么时候开始执行，虽然内部的属性是startTime. quartz还可以创建不同的Trigger.Builder里面还有一个builder，创建真正的Trigger.那个跟工厂模式类似。和工厂的区别是具有不同的参数，是跟创建这个类特定的的参数

      ```java
      // 能够创建Trigger,但是不同的Trigger里面的构造方法的参数还不一样
      // 比如Corn模式下就需要表达式，那么在TriggerBuilder有真正的创建Trigger的Builder.这些build有不同的参数，不同的类也是一种builder模式
      Trigger trigger = TriggerBuilder.newTrigger()
              .withIdentity("trigger1", "group1")
              .startAt(new Date())
              .build();
      // TriggerBuilder里面重要的参数，默认情况下是SimpleScheduleBuilder类
      scheduleBuilder = SimpleScheduleBuilder.simpleSchedule();
      // scheduleBuilder属性同时也可以通过withSchedule的方式赋值不同的Builder，下面的实例中创建了CronScheduleBuilder。创建的Trigger需要表达式。
      TriggerBuilder.newTrigger().withSchedule(CronScheduleBuilder.cronSchedule("* * * * * * *"))
      ```

      与Executors.newScheduledThreadPool(1);做对比，都是具体的核心属性，Builder方式没什么实际意义，而且参数比较多。

   3. 单例模式：

      ```java
      public class SchedulerRepository {
          private HashMap<String, Scheduler> schedulers;
          private static SchedulerRepository inst;
          // 私有的构造方法。
          private SchedulerRepository() {
              schedulers = new HashMap<String, Scheduler>();
          }
          // 静态单例模式，饱汉式
          public static synchronized SchedulerRepository getInstance() {
              if (inst == null) {
                  inst = new SchedulerRepository();
              }
      
              return inst;
          }
      ```

