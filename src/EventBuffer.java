import java.util.LinkedList;

class EventBuffer {

    private final LinkedList<String> q = new LinkedList<>();

    synchronized void put(String msg) {
        q.addLast(msg);
        notify();                      
    }

    synchronized String take() throws InterruptedException {
        while (q.isEmpty()) wait();
        return q.removeFirst();
    }
}
