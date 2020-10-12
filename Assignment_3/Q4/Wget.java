// Paul Calot && Philippe SAGBO
import java.util.HashSet;
import java.util.NoSuchElementException;

import java.lang.InterruptedException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Wget {
  static final boolean debug = false;

  public static void iterativeDownload(String initialURL) {
    final URLQueue queue = new ListQueue();
    final HashSet<String> seen = new HashSet<String>();
    // defines a new URLhandler
    DocumentProcessing.handler = new DocumentProcessing.URLhandler() {
      // this method will be called for each matched url
      @Override
      public void takeUrl(String url) {
        // to be completed at exercise 2
	if(seen.add(url)){
		queue.enqueue(url);
	
		MyURL my_url = new MyURL(url);
		Xurl.download(url);
		String filename;
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

		DocumentProcessing.parseBuffer(data);
		queue.dequeue();
	}
      }
    };
    // to start, we push the initial url into the queue
    DocumentProcessing.handler.takeUrl(initialURL);
    while (!queue.isEmpty()) {
      String url = queue.dequeue();
      Xurl.download(url); // don't change this line
    }
   
}

  @SuppressWarnings("unused")
  public static void multiThreadedDownload(String initialURL) {
    final long initialThreadNumber = Thread.activeCount();
    final SynchronizedListQueue queue = new SynchronizedListQueue();
    final MyHashSet<String> seen = new MyHashSet<String>();
    final long delay = 1000;
    // defines a new URLhandler
    DocumentProcessing.handler = new DocumentProcessing.URLhandler() {
      // this method will be called for each matched url
      @Override
      public void takeUrl(String url) {
        // to be completed at exercise 2
        if(seen.add(url)){
          queue.enqueue(url);
        }
      }
    };
    queue.enqueue(initialURL);
    seen.add(initialURL);
    do{
      try {
        Thread t = new Thread(new Producer(queue.dequeue()));
        t.start();
      }
    catch(NoSuchElementException e1){
        if(debug)System.err.println(e1.getMessage());
        try {
          Thread.sleep(delay);
        }
        catch(InterruptedException e2){
          if(debug)System.err.println(e2.getMessage());
        }
    }
    }while((Thread.activeCount()>initialThreadNumber) || !queue.isEmpty());
  }


  @SuppressWarnings("unused")
  public static void threadPoolDownload(int poolSize, String initialURL) {
    final long initialThreadNumber = Thread.activeCount(); //+Integer.toLong(poolSize);
    final BlockingListQueue queue = new BlockingListQueue();
    final MyHashSet<String> seen = new MyHashSet<>();
    final long delay = 1000;
    Thread[] threadsList = new Thread[poolSize];
    // defines a new URLhandler
    DocumentProcessing.handler = new DocumentProcessing.URLhandler() {
      // this method will be called for each matched url
      @Override
      public void takeUrl(String url) {
        // to be completed at exercise 2
        if(seen.add(url)){
          queue.enqueue(url);
        }
      }
    };
    if(debug){
      System.out.println("Adding initial URL");
    }
    queue.enqueue(initialURL);
    seen.add(initialURL);
    for(int k=0;k<poolSize;k++){
      threadsList[k] = new Thread(new ProducerPool(queue));
      threadsList[k].start();
    }
    boolean b = true;
    while(b){
      // is there a way to make sure that all this method is run at once and not interrupted by other threads 
      // (along with queue.isEmpty())
      if((getWaitingThreadsNb(threadsList)==poolSize) && queue.isEmpty()){
        try{
          Thread.currentThread().sleep(delay);
          if(debug){
            System.out.println("Main thread is sleeping.");
          }
        }
        
        catch(InterruptedException e2){
          if(debug)System.err.println(e2.getMessage());
          //Thread.currentThread().interrupt();
        }
        // TODO : make sure we have the right terminaison condition
        if((getWaitingThreadsNb(threadsList)==poolSize) && queue.isEmpty()){
          if(debug){
            System.out.println("Interrupting all threads...");
          }
          for(int k=0;k<poolSize;k++){
            threadsList[k].interrupt();
            try{
              threadsList[k].join();
            }catch (InterruptedException e1){}
          }
          b = false;
        }
      }
    }
  }

  public static int getWaitingThreadsNb(Thread[] threadsList){
    int waiting_nb = 0;
    for(int k=0; k<threadsList.length;k++){
        if(debug){
          //System.out.println(Thread.currentThread().toString()+" is "+threadsList[k].getState());
        }
        if(threadsList[k].getState()==Thread.State.WAITING){
          /*if(debug){
            System.out.println(Thread.currentThread().toString()+" is waiting.");
          }*/
          waiting_nb+=1;
        }
    }
    return waiting_nb;
  }
  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: java Wget url");
      System.exit(-1);
    }
    //iterativeDownload(args[0]);
    //multiThreadedDownload(args[0]);
    threadPoolDownload(Integer.parseInt(args[0]), args[1]);
  }

}


class Producer implements Runnable {
  // private SynchronizedListQueue m_queue; // takeURL alrady has it
  private String m_url;
  private boolean debug = true;
  public Producer(String url){ //SynchronizedListQueue queue, 
    //m_queue = queue;
    m_url = url;
  }

  public void run(){
    if(debug){
      System.out.println(Thread.currentThread() + " was here.");
    }
    Xurl.download(this.m_url);
    String filename;
    MyURL my_url = new MyURL(this.m_url);
		if(my_url.getPath().equals("/"))
			filename = "index";
		else{
			String[] names = my_url.getPath().split("/");
			filename = names[names.length - 1];
    }
    // TODO ? Make sure we are not replacing an existing file
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
}


class ProducerPool implements Runnable {
  private BlockingListQueue m_queue; // takeURL alrady has it
  private boolean debug = false;

  public ProducerPool(BlockingListQueue queue){
    m_queue = queue;
  }

  public void run(){
    while(!Thread.currentThread().isInterrupted()){
      if(debug){  
        System.out.println(Thread.currentThread() + " was here.");
      }
      
      String url = m_queue.dequeue(); // the thread may be force to wait in the given dequeue function
      /*if(url.equals("**STOP**")){ 
        Thread.currentThread().interrupt();
        break;
      }*/
      if(Thread.currentThread().isInterrupted()){
        Thread.currentThread().interrupt();
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
  }
}

class MyHashSet<E> extends HashSet<E> {
  @Override public synchronized boolean add(Object o) {
    return super.add((E) o);
  }
}



