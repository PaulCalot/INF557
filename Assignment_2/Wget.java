// Paul Calot && Philippe SAGBO
import java.util.HashSet;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
public class Wget {

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
    // to be completed later
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
    iterativeDownload(args[0]);
  }

}
