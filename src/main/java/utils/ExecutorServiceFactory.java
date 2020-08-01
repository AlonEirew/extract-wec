package utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class ExecutorServiceFactory {
    private final static Logger LOGGER = LogManager.getLogger(ExecutorServiceFactory.class);

    private static ExecutorService elasticSearchPool;
    private static final ReentrantLock lock = new ReentrantLock();

    public static void initExecutorService() {
        initExecutorService(Runtime.getRuntime().availableProcessors());
    }

    public static void initExecutorService(int poolSize) {
        lock.lock();
        if (elasticSearchPool == null) {
            LOGGER.info("Starting new ExecutorService...");
            elasticSearchPool = new ThreadPoolExecutor(
                    poolSize,
                    poolSize,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(poolSize * 2),
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
        if(elasticSearchPool != null) {
            LOGGER.info("Closing the ExecutorService...");
            try {
                elasticSearchPool.shutdown();
                // Wait a while for existing tasks to terminate
                if (!elasticSearchPool.awaitTermination(5, TimeUnit.HOURS)) {
                    elasticSearchPool.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!elasticSearchPool.awaitTermination(60, TimeUnit.SECONDS))
                        LOGGER.error("Pool did not terminate");
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
}
