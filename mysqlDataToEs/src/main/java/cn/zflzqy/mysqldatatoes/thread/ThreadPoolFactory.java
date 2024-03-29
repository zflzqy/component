package cn.zflzqy.mysqldatatoes.thread;



import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ：zfl
 * @description：线程池创建工具
 * @date ：2022/2/24 21:58
 */
public class ThreadPoolFactory {

    public ThreadPoolFactory() {
    }


    public static class ThreadPoolFactoryBuilderImpl {
        private String prefix;
        private int corePoolSize;
        private int maximumPoolSize;
        private Long keepAliveTime;

        public ThreadPoolFactoryBuilderImpl() {
        }

        public ThreadPoolFactoryBuilderImpl prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public ThreadPoolFactoryBuilderImpl corePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
            return this;
        }

        public ThreadPoolFactoryBuilderImpl maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public ThreadPoolFactoryBuilderImpl keepAliveTime(Long keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
            return this;
        }

        public ThreadPoolExecutor build() {
            final AtomicLong count = (null == prefix) ? null : new AtomicLong();
            ThreadFactory threadFactory = r -> {
                Thread thread = new Thread(r);
                if (count!=null) {
                    thread.setName(prefix + "-" + count.getAndIncrement());
                }else {
                    thread.setName(prefix);
                }
                return thread;
            };
            ThreadPoolExecutor threadPoolExecutor =
                    new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), threadFactory);
            // 启动核心线程
            threadPoolExecutor.prestartAllCoreThreads();
            return threadPoolExecutor;
        }
    }
}
