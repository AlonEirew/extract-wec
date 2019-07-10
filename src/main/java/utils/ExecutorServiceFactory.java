package utils;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class ExecutorServiceFactory {

    private static ExecutorService elasticSearchPool;
    private static ReentrantLock lock = new ReentrantLock();

    public static void initExecutorService(int poolSize) {
        lock.lock();
        if (elasticSearchPool == null) {
            elasticSearchPool = new ThreadPoolExecutor(
                    poolSize,
                    2 * poolSize,
                    20,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(2 * poolSize),
                    new ThreadPoolExecutor.CallerRunsPolicy());
        }
        lock.unlock();
    }

    public static Future<?> submit(Runnable runnable) {
        return elasticSearchPool.submit(runnable);
    }

    public static <T> Future<T> submit(Callable<T> callable) {
        return elasticSearchPool.submit(callable);
    }

    public static void closeService() {
        lock.lock();
        try {
            elasticSearchPool.shutdown();
            // Wait a while for existing tasks to terminate
            if (!elasticSearchPool.awaitTermination(5, TimeUnit.HOURS)) {
                elasticSearchPool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!elasticSearchPool.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            elasticSearchPool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }

        elasticSearchPool = null;
        lock.unlock();
    }
}
