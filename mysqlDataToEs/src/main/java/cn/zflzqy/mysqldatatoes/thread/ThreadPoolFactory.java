package cn.zflzqy.mysqldatatoes.thread;


import java.util.concurrent.*;

/**
 * @author ：zfl
 * @description：线程池创建工具
 * @date ：2022/2/24 21:58
 */
public class ThreadPoolFactory {

    public ThreadPoolFactory() {
    }
    public static ThreadPoolExecutor build(){
        // cpu核心数据
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        Thread  thread = new Thread("mysql-data-to-es");
        threadFactory.newThread(thread);

        ThreadPoolExecutor threadPoolExecutor =
                new ThreadPoolExecutor(1,1,30L, TimeUnit.SECONDS,new LinkedBlockingQueue<>(),threadFactory);
        // 启动核心线程
        threadPoolExecutor.prestartAllCoreThreads();
        return threadPoolExecutor;
    }
}
