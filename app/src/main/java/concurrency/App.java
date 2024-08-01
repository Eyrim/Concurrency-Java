package concurrency;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class App {
    public static void main(String[] args) throws InterruptedException {

        ExecutorService pool = Executors.newFixedThreadPool(4);

        for (int i = 0; i < 10_000; i++) {
            Counter counterA = new Counter();

            CompletableFuture<Void> increment1 = CompletableFuture.runAsync(counterA::increment, pool);
            CompletableFuture<Void> increment2 = CompletableFuture.runAsync(counterA::increment, pool);

            CompletableFuture<Void> all = CompletableFuture.<Integer>allOf(increment1, increment2);
            all.thenApply((v) -> {
                if (counterA.get() != 2) {
                    System.out.println("Incorrect counter value: " + Integer.toString(counterA.get()));
                }

                return null;
            });
        }

        waitForThreadpoolShutdown(pool);
    }

    private static void waitForThreadpoolShutdown(ExecutorService pool) throws InterruptedException {
        pool.shutdownNow();
        if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
            System.err.println("Pool did not complete within 10 seconds");
            pool.shutdownNow();
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                System.err.println("Pool did not terminate");
            }
        }
    }

    public static class Counter {
        //private int val = 0;
        private AtomicInteger val = new AtomicInteger(0);

        //public synchronized void increment() {
        public void increment() {
            //val += 1;
            val.addAndGet(1);
        }

        public int get() {
            //return val;
            return val.get();
        }
    }
}

/*
 * Maps can't be used safely in multi threaded code because of their undlying implementation
 * Non-thread-safe maps use arrays and operate via moving elements around
 *
 * When these arrays are not implemented in a thread safe manner, the maps are faster to use
 * in syncronous code, but incredibly unsafe in multi threaded code.
 *
 * Java maps also have a number of stateful performance tricks they use, such as treeification.
 * The maps will convert each bucket into a tree when certain criteria are filled, when two threads
 * access the same map this process may happen and end in an inconsistent/unpredictable state.
 *
 * It is not safe to check if a value is in a map and then add it because these maps are not thread safe.
 * In syncronous code there is an inherent queue to operations, in multi threaded code this is unpredictable
 * and therefore one thread may decide a value is not in a map, when another thread has added it.
 */
