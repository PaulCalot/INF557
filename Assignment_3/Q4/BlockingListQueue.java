// Paul Calot && Philippe SAGBO

import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * Basic implementation with a LinkedList.
 */
public class BlockingListQueue implements URLQueue {
  private boolean debug = true;
  private final LinkedList<String> queue;

  public BlockingListQueue() {
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
    if(debug){
      System.out.println(Thread.currentThread().toString()+" has added an url to the queue.");
      System.out.println("Queue size is now " +  Integer.toString(queue.size()));
    }
    notifyAll();
  }

  @Override
  public synchronized String dequeue() {
    try{
      while(this.queue.isEmpty()){
        if(debug){
          System.out.println(Thread.currentThread().toString()+" is (still) waiting.");
        }
        wait();
      }
      if(debug){
        System.out.println(Thread.currentThread().toString()+" is dequeing.");
        System.out.println("Queue size is now " +  Integer.toString(queue.size()-1));
      }
      String head = this.queue.remove();
      if(head.equals("**STOP**")){
        if(debug){
          System.err.println(Thread.currentThread().toString()+" has been interrupted (in-band).");
        }
        Thread.currentThread().interrupt();
      }
      return head; // unlimited capacity - no need for notify
     }
    catch(InterruptedException e){
      if(debug){
        System.err.println(Thread.currentThread().toString()+" has been interrupted (out-of-band).");
      }
      Thread.currentThread().interrupt();
      return "**STOP**";
    }
  }

  public synchronized long getSize(){
    return this.queue.size();
  }
}
