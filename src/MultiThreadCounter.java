import java.io.File;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

class MultiThreadCounter {

    private final ReentrantLock fileLock = new ReentrantLock();
    private final LongAdder pdfCount = new LongAdder();
    private final SynchronousQueue<String> queue;

    MultiThreadCounter(SynchronousQueue<String> q) { 
        this.queue = q; 
    }

    long count(String rootPath) throws InterruptedException {
        File root = new File(rootPath);
        File[] top = root.listFiles();
        if (top == null || top.length == 0) return 0L;

        int chunk = (int) Math.ceil(top.length / 4.0);
        Thread[] workers = new Thread[4];

        for (int t = 0; t < 4; t++) {
            final int start = t * chunk;
            final int end   = Math.min(top.length, (t + 1) * chunk);

            workers[t] = new Thread(() -> {
                try {
                    for (int i = start; i < end; i++) scan(top[i]);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }, "Worker-"+t);
            workers[t].start();
        }

        for (Thread worker : workers) worker.join();
        return pdfCount.sum();
    }

    private void scan(File f) throws InterruptedException {
        if (f == null) return;

        if (f.isFile()) {
            fileLock.lock();
            try {
                if (f.getName().toLowerCase().endsWith(".pdf")) pdfCount.increment();
            } finally {
                fileLock.unlock();
            }
            if (f.getName().toLowerCase().endsWith(".pdf")) queue.put("[Thread Pool] Found " + pdfCount + " PDFs - " + f.getAbsolutePath());

        } else if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null)
                for (File c : kids) scan(c);
        }
    }
}
