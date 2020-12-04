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
    if (message.payload.equals("--ACK--")){
      String[] split = message.payload.split(";");
        ID_addr.put(message.sourceAddress, Integer.parseInt(split[0]));
    }
    if (message.payload.equals(HELLO)){
      String[] split = message.payload.split(";");
      try{
        queue.add(new ConnectionParameters(Integer.parseInt(split[0]), message.sourceAddress));
        ID_addr.put(message.sourceAddress, Integer.parseInt(split[0]));
      }
      catch(IllegalStateException e){
        if(DEBUG) System.err.println("Queue is full");
      }
    }
    if(ID_addr.get(message.sourceAddress) != null) upsideHandlers.get(ID_addr.get(message.sourceAddress)).receive(message);
  }
}
