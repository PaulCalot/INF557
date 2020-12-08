// Paul CALOT && Philippe SAGBO
import java.util.concurrent.ArrayBlockingQueue;
import java.util.HashMap;

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
    // to be completed
    // message format : remote id, local id, port number, format
    String[] split = message.payload.split(";");
    if(split.length != 4){
      if(DEBUG) System.err.println("Message not corresponding to expected pattern ... " + message.toString());
    }
    else{
      int rId = Integer.parseInt(split[0]);
      int lId = Integer.parseInt(split[1]);
      int pN = Integer.parseInt(split[2]);
      String rpayload = split[3].trim();

      if (rpayload.equals("--ACK--")){
        ID_addr.put(message.sourceAddress, rId);
      }
      else if (rpayload.equals(HELLO) && !upsideHandlers.containsKey(rId) && !queue.contains(new ConnectionParameters(rId, message.sourceAddress))){ // && !ID_addr.containsKey(message.sourceAddress) && !queue.contains(new ConnectionParameters(rId, message.sourceAddress))){
        try{
          queue.add(new ConnectionParameters(rId, message.sourceAddress));
          ID_addr.put(message.sourceAddress, rId);
        }
        catch(IllegalStateException e){
          if(DEBUG) System.err.println("Queue is full");
        }
      }
    }
    try{
      if(ID_addr.get(message.sourceAddress) != null) upsideHandlers.get(ID_addr.get(message.sourceAddress)).receive(message);
    }
    catch(NullPointerException e){
      if(DEBUG) System.err.println("Source address not registred.");
    }
  }
}
