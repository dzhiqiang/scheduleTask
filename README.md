# scheduleTask
延迟任务学习记录

- [ ] 补充内存图
- [ ] 时间轮算法

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
>
> DelayedWorkQueue类特性
>
> 1. 阻塞队列
> 2. 根据延迟时间优先队列

```java
public ScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS,
              new DelayedWorkQueue());// 构造方法中workQueue为DelayedWorkQueue延迟队列
}

```

2. 关键方法

```java
// 延迟执行任务
// command 需要执行的任务
// delay 延迟时间
// unit 单位
// triggerTime 计算延迟时间，方法简单不在单独介绍
// decorateTask 封装Task任务，返回RunnableScheduledFuture，实现类ScheduledFutureTask继承FutureTask实现RunnableScheduledFuture，因为delayedExecute参数是RunnableScheduledFuture接口先搞清这个接口，ScheduledFutureTask继承Runnable
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
// 延迟任务执行
private void delayedExecute(RunnableScheduledFuture<?> task) {
    // 线程池关闭 拒绝任务
    if (isShutdown())
        reject(task);
    else {
        // 在延迟队列中增加任务，后面介绍
        super.getQueue().add(task);
        // 如果已经关闭，判断是否在关闭的情况下还要执行，不在执行的话移除task
        if (isShutdown() &&
            !canRunInCurrentRunState(task.isPeriodic()) &&
            remove(task))
            task.cancel(false);
        else
            // 线程池开始执行，线程池内容不做过多介绍
            // 当启动的线程数小于核心线程数时直接增加线程执行：分析这一种情况就可以了
            ensurePrestart();
    }
}
// 增加执行线程worker
// firstTask此时为null
// core 为true：启动的线程数小于核心线程数
private boolean addWorker(Runnable firstTask, boolean core) {
    retry:
    // 判断状态 和 线程数量
    // 线程池实现逻辑跟延迟队列关系不大，默认执行成功
    for (;;) {
        int c = ctl.get();
        int rs = runStateOf(c);

        // Check if queue empty only if necessary.
        if (rs >= SHUTDOWN &&
            ! (rs == SHUTDOWN &&
               firstTask == null &&
               ! workQueue.isEmpty()))
            return false;

        for (;;) {
            int wc = workerCountOf(c);
            if (wc >= CAPACITY ||
                wc >= (core ? corePoolSize : maximumPoolSize))
                return false;
            if (compareAndIncrementWorkerCount(c))
                break retry;
            c = ctl.get();  // Re-read ctl
            if (runStateOf(c) != rs)
                continue retry;
            // else CAS failed due to workerCount change; retry inner loop
        }
    }
    // 新加的worker初始状态
    boolean workerStarted = false;
    boolean workerAdded = false;
    Worker w = null;
    try {
        // 创建一个执行线程
        w = new Worker(firstTask);
        final Thread t = w.thread;
        if (t != null) {
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                // Recheck while holding lock.
                // Back out on ThreadFactory failure or if
                // shut down before lock acquired.
                int rs = runStateOf(ctl.get());
				// 状态正确，添加到线程池workers
                if (rs < SHUTDOWN ||
                    (rs == SHUTDOWN && firstTask == null)) {
                    if (t.isAlive()) // precheck that t is startable
                        throw new IllegalThreadStateException();
                    workers.add(w);
                    int s = workers.size();
                    if (s > largestPoolSize)
                        largestPoolSize = s;
                    workerAdded = true;
                }
            } finally {
                mainLock.unlock();
            }
            // 添加成功，开始执行Worker实现Runnable接口，启动线程，切换到Worker的run方法
            if (workerAdded) {
                t.start();
                workerStarted = true;
            }
        }
    } finally {
        if (! workerStarted)
            addWorkerFailed(w);
    }
    return workerStarted;
}
// 执行自己
public void run() {
    runWorker(this);
}
final void runWorker(Worker w) {
    Thread wt = Thread.currentThread();
    Runnable task = w.firstTask;
    w.firstTask = null;
    w.unlock(); // allow interrupts
    boolean completedAbruptly = true;
    try {
        // 延迟队列执行把任务放入到阻塞队列中task为空，直接从队列中获取，队列为空，getTask()阻塞
        // 因为是延迟队列，当任务有延迟，也会获取不到任务，阻塞
        while (task != null || (task = getTask()) != null) {
            w.lock();
            // If pool is stopping, ensure thread is interrupted;
            // if not, ensure thread is not interrupted.  This
            // requires a recheck in second case to deal with
            // shutdownNow race while clearing interrupt
            if ((runStateAtLeast(ctl.get(), STOP) ||
                 (Thread.interrupted() &&
                  runStateAtLeast(ctl.get(), STOP))) &&
                !wt.isInterrupted())
                wt.interrupt();
            try {
                beforeExecute(wt, task);
                Throwable thrown = null;
                try {
                    // 得到任务执行
                    task.run();
                } catch (RuntimeException x) {
                    thrown = x; throw x;
                } catch (Error x) {
                    thrown = x; throw x;
                } catch (Throwable x) {
                    thrown = x; throw new Error(x);
                } finally {
                    afterExecute(task, thrown);
                }
            } finally {
                task = null;
                w.completedTasks++;
                w.unlock();
            }
        }
        completedAbruptly = false;
    } finally {
        processWorkerExit(w, completedAbruptly);
    }
}
// 从队列中获取任务
private Runnable getTask() {
    boolean timedOut = false; // Did the last poll() time out?
	// 有大量的校验工作，暂时先不管
    for (;;) {
        int c = ctl.get();
        int rs = runStateOf(c);

        // Check if queue empty only if necessary.
        if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
            decrementWorkerCount();
            return null;
        }

        int wc = workerCountOf(c);

        // Are workers subject to culling?
        boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

        if ((wc > maximumPoolSize || (timed && timedOut))
            && (wc > 1 || workQueue.isEmpty())) {
            if (compareAndDecrementWorkerCount(c))
                return null;
            continue;
        }

        try {
            // 判断Worker是否淘汰，选择是否阻塞
            // poll不阻塞或者阻塞固定时间，如果时间耗尽则返回null
            // take如果为空阻塞
            Runnable r = timed ?
                workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
            workQueue.take();
            // 返回任务
            if (r != null)
                return r;
            // r为null则timedOut为true 导致上面代码if ((wc > maximumPoolSize || (timed && timedOut)) 中的(timed && timedOut)为true，返回null，结束worker执行
            timedOut = true;
        } catch (InterruptedException retry) {
            timedOut = false;
        }
    }
}
```

##### RunnableScheduledFuture讲解

> 实现RunnableFuture接口，具有Future能力：获取执行结果和Runnable能力，run：执行能力
>
> 实现ScheduleFuture接口，具有得到延期时间能力和具有Future能力：获取执行结果,因为继承Futurn所以可以作为返回值

![RunnableScheduledFuture](https://raw.githubusercontent.com/dzhiqiang/PicGo-gallery/main/RunnableScheduledFuture.png)

### Quartz

> 内容比较多，[点击链接查看](https://github.com/dzhiqiang/scheduleTask/blob/main/quartz.md)

