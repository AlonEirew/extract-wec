package wec.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class ExecutorServiceFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorServiceFactory.class);

    public static ExecutorService getExecutorService(int poolSize) {
        LOGGER.info("Creating new ExecutorService...");
        return new ThreadPoolExecutor(
                poolSize,
                poolSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(poolSize * 2),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public static void closeService(ExecutorService elasticSearchPool) {
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
        }
    }
}
