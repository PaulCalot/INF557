// Paul Calot && Philippe SAGBO
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.lang.InterruptedException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Wget {

  @SuppressWarnings("unused")
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

  public static void multiThreadedDownload(String initialURL) {
    final long initialThreadNumber = Thread.activeCount();
    final SynchronizedListQueue queue = new SynchronizedListQueue();
    final MyHashSet seen = new MyHashSet();
    final long delay = 0;
    final boolean debug = false;
    // defines a new URLhandler
    DocumentProcessing.handler = new DocumentProcessing.URLhandler() {
      // this method will be called for each matched url
      @Override
      public void takeUrl(String url) {
        if(seen.add(url)){
          queue.enqueue(url);
        }
      }
    };
    DocumentProcessing.handler.takeUrl(initialURL);
    do{
      try {
        String uurl=queue.dequeue();
        Thread t = new Thread(new Producer(uurl, seen), uurl);
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
        if((Thread.activeCount()==initialThreadNumber) && queue.isEmpty()) break;
      }
    }while(true);
  }


  @SuppressWarnings("unused")
  public static void threadPoolDownload(int poolSize, String initialURL) {
    // to be completed later
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: java Wget url");
      System.exit(-1);
    }
    multiThreadedDownload(args[0]);
  }

}


class Producer implements Runnable {
  // private SynchronizedListQueue m_queue; // takeURL alrady has it
  private String m_url;
  private MyHashSet s;
  private boolean debug = true;
  public Producer(String url, MyHashSet seen){ 
    m_url = url;
    s = seen;
  }

  public void run(){
    if(debug){
        System.out.println(Thread.currentThread() + " was here [+].");
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
        if(debug)System.err.println(e.getMessage());
    }
    DocumentProcessing.parseBuffer(data); // seek new urls and call takeURL on them
    if(debug){
      System.out.println(Thread.currentThread() + " was here [-].");
    }
  }
}

class MyHashSet extends HashSet<String> {
  @Override public synchronized boolean add(String s) {
    return super.add(s);
  }
}



