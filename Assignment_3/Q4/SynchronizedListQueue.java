// Paul Calot && Philippe SAGBO

import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * Basic implementation with a LinkedList.
 */
public class SynchronizedListQueue implements URLQueue {
  
  private final LinkedList<String> queue;

  public SynchronizedListQueue() {
    this.queue = new LinkedList<String>();
  }

  @Override
  public synchronized boolean isEmpty() {
    return this.queue.size() == 0;
  }
  @Override
  public synchronized boolean isFull() {
    return false;
  }

  @Override
  public synchronized void enqueue(String url) {
    this.queue.add(url);
  }

  @Override
  public synchronized String dequeue() throws NoSuchElementException {
    return this.queue.remove(); // unlimited capacity - no need for notify
  }

  public synchronized long getSize(){
    return this.queue.size();
  }
}
