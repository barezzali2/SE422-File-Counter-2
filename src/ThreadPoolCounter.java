import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

class ThreadPoolCounter {

    private volatile long pdfCount = 0;                 
    private final ReentrantLock fileLock = new ReentrantLock();
    private final EventBuffer events;               
    /* -------------------------------- */

    ThreadPoolCounter(EventBuffer events) {
        this.events = events;
    }

    long count(String rootPath) throws InterruptedException {

        int numThreads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

        CountDownLatch startLatch = new CountDownLatch(1);     
        AtomicInteger taskCounter = new AtomicInteger();
        Object doneLock = new Object();
        

        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
                              

        taskCounter.incrementAndGet();
       pool.submit(() -> {
            try {
                startLatch.await(); // Wait for the latch before starting
                scan(new File(rootPath), pool, taskCounter, doneLock);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                if (taskCounter.decrementAndGet() == 0) {
                    synchronized (doneLock) {
                        doneLock.notifyAll();
                    }
                }
            }
        });

        startLatch.countDown();


        // Wait for all tasks to finish
        synchronized (doneLock) {
            while (taskCounter.get() > 0) {
                doneLock.wait();
            }
        }
       

        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        return pdfCount;
    }


    private void submitScan(File f, ExecutorService pool, AtomicInteger taskCounter, Object doneLock) {
        taskCounter.incrementAndGet(); // Increment BEFORE submitting
        pool.submit(() -> {
            try {
                scan(f, pool, taskCounter, doneLock);
            } finally {
                if (taskCounter.decrementAndGet() == 0) {
                    synchronized (doneLock) {
                        doneLock.notifyAll();
                    }
                }
            }
        });
    }

    private void scan(File file, ExecutorService pool, AtomicInteger taskCounter, Object doneLock) {
        if (file == null) return;

        if (file.isFile()) {
            fileLock.lock();
            try {
                if (file.getName().toLowerCase().endsWith(".pdf")) pdfCount++;
            } finally {
                fileLock.unlock();
            }
            if (file.getName().toLowerCase().endsWith(".pdf")) {
                events.put("[Thread Pool] found " + pdfCount + " PDFs - " + file.getAbsolutePath());
            }
        } else if (file.isDirectory()) {
            File[] kids = file.listFiles();
            if (kids != null) {
                for (File c : kids) submitScan(c, pool, taskCounter, doneLock);
            }
        }
    }
}
