# scheduleTask
延迟任务学习记录

### java.util.Timer

##### 特性：

1. 后台线程执行计划任务，只有一个线程，可以执行一次或者定期重复执行。
2. Timer执行的任务尽量比较短暂，因为只能有一个Task执行，如果一个任务占用时间过长，会造成其他任务在相对集中的时间执行。
3. 默认情况下，线程是非守护线程进行执行任务，防止应用被终止。可以通过cancel方法终止任务。
4. Timer是线程安全的。
5. 不一定准时的执行，因为使用了Object.wait(long)方法。
6. 可以使用Java5.0 java.util.concurrent 下的ScheduledThreadPoolExecutor是基于线程池模式的任务。
7. 一个任务报错所有的任务都不能在执行

##### Timer内部实现

1. 关键属性

``` java
// 任务队列，线程安全基于锁定queue实现,内部是TimerTask数组
private final TaskQueue queue = new TaskQueue();
```

 ```java
 // 执行任务队列的线程
 private final TimerThread thread = new TimerThread(queue); 
 ```

2. 关键方法

```java
// 构造方法
// name:决定Timer名称
// isDaemon:是否是守护线程
// thread.start():启动线程执行TimerTask
public Timer(String name, boolean isDaemon) {
    thread.setName(name);
    thread.setDaemon(isDaemon);
    thread.start();
}
```

 ```java
 // schedule和scheduleAtFixedRate最终都会调用sched方法，只是schedule的period变为负数
 // schedule计算下次执行时间时，根据当前任务的当前时间计算
 // 以下2种情况可能2种方法效果不同
 // 1:设置的执行时间早于当前时间，而且间隔要大于周期period
 // 2:如果有其他任务执行时间较长导致周期性任务未能及时执行，那么下次执行时间会有区别
 public void schedule(TimerTask task, long delay, long period) {
     if (delay < 0)
         throw new IllegalArgumentException("Negative delay.");
     if (period <= 0)
         throw new IllegalArgumentException("Non-positive period.");
     sched(task, System.currentTimeMillis()+delay, -period);
 }
 public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
     if (delay < 0)
         throw new IllegalArgumentException("Negative delay.");
     if (period <= 0)
         throw new IllegalArgumentException("Non-positive period.");
     sched(task, System.currentTimeMillis()+delay, period);
 }
 ```



   ```java
   //指定时间,指定周期执行TimerTask
   //task：要执行的任务
   //time: 延迟时间，单位毫秒
   //period: 周期，单位毫秒
   private void sched(TimerTask task, long time, long period) {
           if (time < 0)
               throw new IllegalArgumentException("Illegal execution time.");
   	// Constrain value of period sufficiently to prevent numeric
       // overflow while still being effectively infinitely large.
       if (Math.abs(period) > (Long.MAX_VALUE >> 1))
           period >>= 1;
   	//加锁
       synchronized(queue) {
           if (!thread.newTasksMayBeScheduled)
               throw new IllegalStateException("Timer already cancelled.");
   		//对task加锁
           synchronized(task.lock) {
               if (task.state != TimerTask.VIRGIN)
                   throw new IllegalStateException(
                       "Task already scheduled or cancelled");
               task.nextExecutionTime = time;
               task.period = period;
               task.state = TimerTask.SCHEDULED;
           }
   		//加入task
           queue.add(task);
           //加入之后最小的线程等于当前线程则唤醒
           if (queue.getMin() == task)
               queue.notify();
       }
   }
   ```

##### TimerThread 内部实现

1. 关键属性

```java
// 如果是flase则表示没有存活的task需要执行
// 设置为flase可以优雅的终止执行
boolean newTasksMayBeScheduled = true;
```

2. 关键方法

```java
private void mainLoop() {
    while (true) {
        try {
            // 最近需要执行的任务
            TimerTask task;
            // 判断task是否需要执行
            boolean taskFired;
            // 加锁
            synchronized(queue) {
                // Wait for queue to become non-empty
                // 等待queue不为空
                while (queue.isEmpty() && newTasksMayBeScheduled)
                    queue.wait();
                // 说明queue不为空或者newTasksMayBeScheduled为flase
                // 如果是空则不再while(true)循环，说明newTasksMayBeScheduled为flase
                if (queue.isEmpty())
                    break; // Queue is empty and will forever remain; die

                // Queue nonempty; look at first evt and do the right thing
                // 当前时间，执行时间
                long currentTime, executionTime;
                // 取出队列中最小的task
                task = queue.getMin();
                synchronized(task.lock) {
                    // 如果任务已经取消则
                    if (task.state == TimerTask.CANCELLED) {
                        queue.removeMin();
                        continue;  // No action required, poll queue again
                    }
                    // 获取当前时间
                    currentTime = System.currentTimeMillis();
                    // 任务的执行时间
                    executionTime = task.nextExecutionTime;
                    // 是否执行
                    if (taskFired = (executionTime<=currentTime)) {
                        // 执行，判断是否具有周期，如果没有周期，则从队列中移除，任务状态为执行中
                        if (task.period == 0) { // Non-repeating, remove
                            queue.removeMin();
                            task.state = TimerTask.EXECUTED;
                        } else { // Repeating task, reschedule
                            // 重复任务，重新为Min中task制定计划
                            // task.period<0 ? currentTime   - task.period
                            //    : executionTime + task.period
                            // schedule执行的任务是<0,scheduleAtFixedRate是>0
                            queue.rescheduleMin(
                                task.period<0 ? currentTime   - task.period
                                : executionTime + task.period);
                        }
                    }
                }
                // 如果Min的task未到执行时间，等待执行时间和当前时间间隔的毫秒数
                if (!taskFired) // Task hasn't yet fired; wait
                    queue.wait(executionTime - currentTime);
            }
            // 当前任务可以执行
            if (taskFired)  // Task fired; run it, holding no locks
                task.run();
        } catch(InterruptedException e) {
        }
    }
}
```

##### TimerTask 内部实现

1. 属性

```java
// 记录状态
int state = VIRGIN;
// 下次执行时间
long nextExecutionTime;
// 周期
long period = 0;
```

2. 方法

```java
// 执行，不会新起线程，直接调用run方法
public abstract void run();
// 计算最近一次应该执行的时间
public long scheduledExecutionTime() {
    synchronized(lock) {
        return (period < 0 ? nextExecutionTime + period
                : nextExecutionTime - period);
    }
}
```

##### TaskQueue 内部实现,TaskQueue是优先队列

1. 属性

```java
// TimerTask数组，默认长度128
// 优先队列，size从1开始的，根节点是queue[1]，queue保证节点优先于子节点，并不保证左右子节点是优先级
// 优先级根据TimerTask的nextExecutionTime决定
// 当节点为k，那么父节点=k >> 1,左子节点 k << 1,右子节点 k << 1 + 1
private TimerTask[] queue = new TimerTask[128];
// TimerTask个数
private int size = 0;
```

2. 方法

```java
// java.util.Timer#sched时添加任务
void add(TimerTask task) {
    // 当存储不够时增加2倍
    if (size + 1 == queue.length)
        queue = Arrays.copyOf(queue, 2*queue.length);
	// 在size+1的位置保存新的task
    queue[++size] = task;
    // 向上重建堆
    fixUp(size);
}
// 从底向上修复堆，顶是queue[1]
private void fixUp(int k) {
    while (k > 1) {
        int j = k >> 1;
        if (queue[j].nextExecutionTime <= queue[k].nextExecutionTime)
            break;
        TimerTask tmp = queue[j];  queue[j] = queue[k]; queue[k] = tmp;
        k = j;
    }
}
// 修复k节点以下的堆
private void fixDown(int k) {
    int j;
    while ((j = k << 1) <= size && j > 0) {
        if (j < size &&
            queue[j].nextExecutionTime > queue[j+1].nextExecutionTime)
            j++; // j indexes smallest kid
        if (queue[k].nextExecutionTime <= queue[j].nextExecutionTime)
            break;
        TimerTask tmp = queue[j];  queue[j] = queue[k]; queue[k] = tmp;
        k = j;
    }
}

```

### ScheduledExecutorService 延迟任务

##### 特性：

1. 不仅可以延迟，周期性执行任务，而且继承ThreadPoolExecutor，具有线程池的灵活性和扩展性
2. 能够多工作线程执行任务

##### ScheduledExecutorService构造方法

> 构造实现类ScheduledThreadPoolExecutor，因为继承了ThreadPoolExecutor，具有线程池能力，最终调用的是线程池构造方法

```java
// 使用的是ThreadPoolExecutor构造方法
// corePoolSize核心线程数
// maximumPoolSize线程总个数
// keepAliveTime 线程存活时间
// unit keepAliveTime的时间单位
// workQueue 任务队列
// threadFactory 创建线程的工厂
// handler 线程满时的拒绝策略
public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler)
```

1. 关键属性

> 因为底层使用的线程池，这次主讲延迟任务，只需要关注BlockingQueue<Runnable> workQueue属性

```java
public ScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS,
              new DelayedWorkQueue());// 构造方法中workQueue为DelayedWorkQueue延迟队列，后面作为关键类介绍
}
```

2. 关键方法

```java
// 延迟执行任务
// command 需要执行的任务
// delay 延迟时间
// unit 单位
// triggerTime 计算延迟时间，方法简单不在单独介绍
// decorateTask 封装Task任务，返回RunnableScheduledFuture，实现类ScheduledFutureTask继承FutureTask实现RunnableScheduledFuture，因为delayedExecute参数是RunnableScheduledFuture接口先搞清这个接口
// delayedExecute 执行任务
public ScheduledFuture<?> schedule(Runnable command,
                                    long delay,
                                    TimeUnit unit) {
    if (command == null || unit == null)
        throw new NullPointerException();
    // 
    RunnableScheduledFuture<?> t = decorateTask(command,
        new ScheduledFutureTask<Void>(command, null,
                                      triggerTime(delay, unit)));
    delayedExecute(t);
    return t;
}
```

##### RunnableScheduledFuture讲解

![RunnableScheduledFuture](https://raw.githubusercontent.com/dzhiqiang/PicGo-gallery/main/RunnableScheduledFuture.png)

