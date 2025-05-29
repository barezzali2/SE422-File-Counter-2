import java.io.File;
import java.util.concurrent.SynchronousQueue;

class SingleThreadCounter {

    private final SynchronousQueue<String> queue;
    private long pdfCount = 0;

    SingleThreadCounter(SynchronousQueue<String> q) { 
        this.queue = q; 
    }

    long count(String rootPath) throws InterruptedException {
        scan(new File(rootPath));
        return pdfCount;
    }

    private void scan(File file) throws InterruptedException {
        if (file == null) return;

        if (file.isFile()) {
            if (file.getName().toLowerCase().endsWith(".pdf")) {
                pdfCount++;
                queue.put("[Single Thread] found " + pdfCount + " PDFs - " + file.getAbsolutePath());
            }
        } else if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null)
                for (File c : children) scan(c);
        }
    }
}
