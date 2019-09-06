package utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;

public class ExecutorServiceFactory {
    private final static Logger LOGGER = LogManager.getLogger(ExecutorServiceFactory.class);
    private final ExecutorService elasticSearchPool;

    public ExecutorServiceFactory() {
        this(Runtime.getRuntime().availableProcessors());

    }

    public ExecutorServiceFactory(int capacity) {
        elasticSearchPool = new ThreadPoolExecutor(capacity, capacity,
                0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(100),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public Future<?> submit(Runnable runnable) {
        return elasticSearchPool.submit(runnable);
    }

    public <T> Future<T> submit(Callable<T> callable) {
        return elasticSearchPool.submit(callable);
    }

    public void closeService() {
        if(elasticSearchPool != null) {
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
        }
    }
}
