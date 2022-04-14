import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolManager {
    private static ExecutorService executorService;

    public static ExecutorService getThread() {
        executorService = Executors.newFixedThreadPool(12);
        return executorService;
    }
}
