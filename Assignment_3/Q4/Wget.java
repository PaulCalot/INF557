// Paul Calot && Philippe SAGBO
import java.util.HashSet;
import java.util.NoSuchElementException;

import java.lang.InterruptedException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Wget {
  static final boolean debug = true;

  public static void threadPoolDownload(int poolSize, String initialURL) {
    final long initialThreadNumber = Thread.activeCount(); //+Integer.toLong(poolSize);
    final BlockingListQueue queue = new BlockingListQueue();
    final MyHashSet seen = new MyHashSet();
    final long delay = 1000;
    Thread[] threadsList = new Thread[poolSize];
    DocumentProcessing.handler = new DocumentProcessing.URLhandler() {
      @Override
      public void takeUrl(String url) {
        if(seen.add(url)){
          queue.enqueue(url);
        }
      }
    };
    if(poolSize <= 0) return;
    if(debug){
      System.out.println("Adding initial URL");
    }
    DocumentProcessing.handler.takeUrl(initialURL);
    for(int k=0;k<poolSize;k++){
      threadsList[k] = new Thread(new ProducerPool(queue, seen), String.valueOf(k));
      threadsList[k].start();
    }
    while(true){
      // is there a way to make sure that all this method is run at once and not interrupted by other threads 
      // (along with queue.isEmpty())
/*      if((getWaitingThreadsNb(threadsList)==poolSize) && queue.isEmpty()){
        try{
          Thread.sleep(delay);
          if(debug){
            System.out.println("Main thread is sleeping.");
          }
        }
        
        catch(InterruptedException e2){
          if(debug)System.err.println(e2.getMessage());
          //Thread.currentThread().interrupt();
        }*/
        // TODO : make sure we have the right terminaison condition
        if((getWaitingThreadsNb(threadsList)==poolSize) && queue.isEmpty()){
          if(debug){
            System.out.println("Interrupting all threads...");
          }
          for(int k=0;k<poolSize;k++){
            threadsList[k].interrupt();
            try{
              threadsList[k].join();
            }
            catch (InterruptedException e1){}
          }
          break;
        }
//      }
    }
  }

  public static int getWaitingThreadsNb(Thread[] threadsList){
    int waiting_nb = 0;
    for(int k=0; k<threadsList.length;k++){
        if(threadsList[k].getState()==Thread.State.WAITING) waiting_nb+=1;
    }
    return waiting_nb;
  }
  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: java Wget nbr url");
      System.exit(-1);
    }
    threadPoolDownload(Integer.parseInt(args[0]), args[1]);
  }

}




class ProducerPool implements Runnable {
  private BlockingListQueue m_queue; // takeURL alrady has it
  private MyHashSet s; // takeURL alrady has it
  private boolean debug = true;

  public ProducerPool(BlockingListQueue queue, MyHashSet seen){
    s = seen;
    m_queue = queue;
  }

  public void run(){
    if(debug) System.out.println(Thread.currentThread() + " was here [+].");
    while(!Thread.currentThread().isInterrupted()){
      
      String url = m_queue.dequeue(); // the thread may be force to wait in the given dequeue function
      if(url==null || url.equals("**STOP**")){ 
        Thread.currentThread().interrupt();
        if(debug) System.out.println(Thread.currentThread() + " was here [---].");
        break;
      }
      if(Thread.currentThread().isInterrupted()){
        Thread.currentThread().interrupt();
        if(debug) System.out.println(Thread.currentThread() + " was here [---].");
        break;
      }
      Xurl.download(url);
      String filename;
      MyURL my_url = new MyURL(url);
      if(my_url.getPath().equals("/"))
        filename = "index";
      else{
        String[] names = my_url.getPath().split("/");
        filename = names[names.length - 1];
      }
      String data = "";
      try {
        BufferedReader reader = new BufferedReader(new FileReader(filename)); 
        String line=reader.readLine();
        while(line!=null){
          data = data.concat(line);
          line = reader.readLine();
        }
        reader.close();
      } 
      catch(IOException e){
        System.err.println(e.getMessage());
      }
      DocumentProcessing.parseBuffer(data); // seek new urls and call takeURL on them
    }
    if(debug) System.out.println(Thread.currentThread() + " was here [-].");
  }
}

class MyHashSet extends HashSet<String> {
  @Override public synchronized boolean add(String str) {
    return super.add(str);
  }
}



