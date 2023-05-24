package cn.zflzqy.mysqldatatoes.thread;


import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ：zfl
 * @description：线程池创建工具
 * @date ：2022/2/24 21:58
 */
public class ThreadPoolFactory {
    private static final AtomicInteger count = new AtomicInteger(1);

    public ThreadPoolFactory() {
    }
    public static ThreadPoolExecutor build(){
        // cpu核心数据
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("mysql-data-to-es" + "-" + count.getAndIncrement());
                return thread;
            }
        };
        ThreadPoolExecutor threadPoolExecutor =
                new ThreadPoolExecutor(1,1,30L, TimeUnit.SECONDS,new LinkedBlockingQueue<>(),threadFactory);
        // 启动核心线程
        threadPoolExecutor.prestartAllCoreThreads();
        return threadPoolExecutor;
    }
}
