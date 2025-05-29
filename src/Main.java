import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final SynchronousQueue<String> syncQ = new SynchronousQueue<>();
    private static final EventBuffer              evtQ  = new EventBuffer();   // NEW

    public static void main(String[] args) throws Exception {

        String dir = DirectoryPathHandler.getValidPath();

        Thread printer = new Thread(() -> {
            boolean syncDone = false, poolDone = false;
            try {
                while (!(syncDone && poolDone)) {

                    if (!poolDone) {
                        String m = evtQ.take();           
                        if ("POOL_DONE".equals(m)) poolDone = true;
                        else System.out.println(m);
                    }

                    String s = syncQ.poll(50, TimeUnit.MILLISECONDS);
                    if (s != null) {
                        if ("SYNC_DONE".equals(s)) syncDone = true;
                        else System.out.println(s);
                    }
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }, "Printer");
        printer.start();

        Thread single = new Thread(() -> { 
            try {
                long t = new SingleThreadCounter(syncQ).count(dir);
                syncQ.put("Final: [Single Thread] Found " + t + " PDFs");
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        single.start();

        Thread four = new Thread(() -> { 
            try {
                long t = new MultiThreadCounter(syncQ).count(dir);
                syncQ.put("Final: [4 Threads] Found " + t + " PDFs");
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        four.start();

        Thread pool = new Thread(() -> {
            try {
                long t = new ThreadPoolCounter(evtQ).count(dir);
                evtQ.put("Final: [Thread Pool] Found " + t + " PDFs");
                evtQ.put("POOL_DONE");                
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        pool.start();

        single.join();
        four.join();
        syncQ.put("SYNC_DONE");

        pool.join();
        printer.join();
    }
}
