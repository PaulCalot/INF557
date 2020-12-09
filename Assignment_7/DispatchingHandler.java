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
  private HashMap<Integer, Integer> ID_addr; // correspondance : rId -> lI

  private static final boolean DEBUG = false;

  private final HashSet<Integer> seen_addr;

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
    this.ID_addr = new HashMap<Integer, Integer>();
    this.seen_addr = new HashSet<Integer>();
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
    // -------- test ------------
    // message format : remote id, local id, port number, format
    String[] split = message.payload.split(";");
    if(split.length != 4){
      if(DEBUG) System.err.println("Message not corresponding to expected pattern ... " + message.toString());
    }

    else{
      int rId = Integer.parseInt(split[0]);
      int lId = Integer.parseInt(split[1]);
      String rpayload = split[3].trim();

      if(rpayload.equals("--ACK--")){ // handling --ACK--
        boolean valid_ack = true;
        try{
          upsideHandlers.get(lId).receive(message);   // if this does not work, then we should add it.
        }
        catch(NullPointerException e){
          valid_ack = false;
        }
        if(valid_ack) ID_addr.put(rId, lId); 
      }

      else if(rpayload.equals(HELLO)){ // handing --HELLO--
        if(!seen_addr.contains(rId)){ // first hello
          boolean queue_full = false;
          try{
            queue.add(new ConnectionParameters(rId, message.sourceAddress)); // queue already synchronized
          }
          catch(IllegalStateException e){
            queue_full=true;
          }
          if(!queue_full){
            seen_addr.add(rId);
          }
        }
        else{ // not first hello
          try{
            upsideHandlers.get(lId).receive(message); 
          }
          catch(NullPointerException e){
            try{
              upsideHandlers.get(ID_addr.get(rId)).receive(message); 
            }
            catch(NullPointerException ee){
                // we are dropping it
            }
          }
          
        }
      }
      else{ // handling other messages
        try{
          upsideHandlers.get(lId).receive(message);
        }
        catch(NullPointerException e){
          if(DEBUG) System.err.println("Source address not registered.");
        }
      }
    }
  }
}
