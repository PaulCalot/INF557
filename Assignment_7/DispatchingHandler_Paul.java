// Paul CALOT && Philippe SAGBO
import java.util.concurrent.ArrayBlockingQueue;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
//import java.util.concurrent.ConcurrentSkipListMap;
public class DispatchingHandler extends Handler {

  /** one will need it */
  private static final String HELLO = "--HELLO--";

  /** An arbitrary base value for the numbering of handlers. **/
  private static int counter = 35000;

  /** the queue for pending connections */
  private final ArrayBlockingQueue<ConnectionParameters> queue;


  // to be completed
  private HashMap<String, Integer> ID_addr;

  private static final boolean DEBUG = false;

  private final HashSet<Integer> seen_addr;
  private final LinkedList<Message> hellos_waiting_to_be_sent;

  /**
   * Initializes a new dispatching handler with the specified parameters
   * 
   * @param _under
   *                         the {@link Handler} on which the new handler will
   *                         be stacked
   * @param _queueCapacity
   *                         the capacity of the queue of pending connections
   */
  public DispatchingHandler(final Handler _under, int _queueCapacity) {
    super(_under, ++counter, false);
    this.queue = new ArrayBlockingQueue<ConnectionParameters>(_queueCapacity);
    // add other initializations if needed
    this.ID_addr = new HashMap<String, Integer>();
    this.seen_addr = new HashSet<Integer>();
    this.hellos_waiting_to_be_sent = new LinkedList<Message>();
  }

  /**
   * Retrieves and removes the head of the queue of pending connections, waiting
   * if no elements are present on this queue.
   *
   * @return the connection parameters record at the head of the queue
   * @throws InterruptedException
   *                                if the calling thread is interrupted while
   *                                waiting
   */
  public ConnectionParameters accept() throws InterruptedException {
    return this.queue.take();
  }

  @Override
  public void send(String payload) {
    no_send();
  }

  @Override
  protected void send(String payload, String destinationAddress) {
    this.downside.send(payload, destinationAddress);
  }

  @Override
  public void handle(Message message) {
    if(DEBUG) System.out.println("");
    if(DEBUG) System.out.println("START handle");


    // -------- test ------------

    if(DEBUG) System.out.println("Sending saved hellos");
    for(int k = 0; k<hellos_waiting_to_be_sent.size(); k++){
      Message local_message = hellos_waiting_to_be_sent.pop();
      try{
        if(local_message.sourceAddress!=null && ID_addr.containsKey(local_message.sourceAddress)){
          if(DEBUG) System.out.println("Sending saved hellos to the correct address.");
          upsideHandlers.get(ID_addr.get(local_message.sourceAddress)).receive(local_message);
        }
        else{
          hellos_waiting_to_be_sent.addLast(local_message);
        }
      }
      catch(NullPointerException e){
        if(DEBUG) System.err.println("Source address not registered.");
      }
    } 
    
    // to be completed
    // message format : remote id, local id, port number, format
    if(DEBUG) System.err.println("Recieved message " + message.payload + ":" + message.sourceAddress);
    String[] split = message.payload.split(";");
    if(split.length != 4){
      if(DEBUG) System.err.println("Message not corresponding to expected pattern ... " + message.toString());
    }
    else{
      int rId = Integer.parseInt(split[0]);
      int lId = Integer.parseInt(split[1]);
      //int pN = Integer.parseInt(split[2]);
      String rpayload = split[3].trim();

      if (rpayload.equals("--ACK--")){
        if(DEBUG) System.err.println("Recieved ACK");
        ID_addr.put(message.sourceAddress, lId); // this is how we should do it - saving the unique local ID we could not get before ...
        try{
            upsideHandlers.get(ID_addr.get(message.sourceAddress)).receive(message);
          }
        catch(NullPointerException e){
          if(DEBUG) System.err.println("Source address not registered.");
        }
        //seen_addr.remove(rId);
      }
      else if(rpayload.equals(HELLO)){
        if(!seen_addr.contains(rId)){
          try{
            queue.add(new ConnectionParameters(rId, message.sourceAddress)); // queue already synchronized
            seen_addr.add(rId);
            if(DEBUG) System.err.println("ADDING TO THE QUEUE");
          }
          catch(IllegalStateException e){
            if(DEBUG) System.err.println("Queue is full");
          }
        }
        else{
          if(DEBUG) System.err.println("Save 'hello' for later");

          hellos_waiting_to_be_sent.addLast(message);;
        }
        ID_addr.put(message.sourceAddress, lId); // this is how we should do it - saving the unique local ID we could not get before ...
      }
      try{
        if(message.sourceAddress!=null && ID_addr.containsKey(message.sourceAddress)){
          if(DEBUG) System.out.println("Sending message to the correct address.");
          upsideHandlers.get(ID_addr.get(message.sourceAddress)).receive(message);
        }
      }
      catch(NullPointerException e){
        if(DEBUG) System.err.println("Source address not registered.");
      }
    }
    if(DEBUG) System.out.println("END handle");
  }
}

