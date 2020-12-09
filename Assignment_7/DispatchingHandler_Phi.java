import java.util.concurrent.ArrayBlockingQueue;
import java.util.HashMap;
import java.util.HashSet;

public class DispatchingHandler extends Handler {

  /** one will need it */
  private static final String HELLO = "--HELLO--";

  /** An arbitrary base value for the numbering of handlers. **/
  private static int counter = 35000;

  /** the queue for pending connections */
  private final ArrayBlockingQueue<ConnectionParameters> queue;

  // to be completed
  private HashSet<String> HELLOS;
  private HashSet<String> HELLOS_DEFINITIVE;
  private HashSet<String> ACKS;

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
    HELLOS = new HashSet<String>();
    HELLOS_DEFINITIVE = new HashSet<String>();
    ACKS = new HashSet<String>();
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
    String[] split = message.payload.split(";");
    if(split.length != 4){
      if(DEBUG) System.err.println("Message not corresponding to expected pattern ... " + message.toString());
    }
    else{
      int ignore = 0;
      int rId = Integer.parseInt(split[0]);
      int lId = Integer.parseInt(split[1]);
      int pN = Integer.parseInt(split[2]);
      String rpayload = split[3];

      if (rpayload.equals("--HELLO--")){
        if (HELLOS_DEFINITIVE.contains(message.payload)){
          /* Just send it upwards, don't ask any more questions */
          try{
            upsideHandlers.get(lId).receive(message);
          }
          catch(NullPointerException e){
            if(DEBUG) System.err.println("Source address not registred.");
          }
        }
        else{
          if(!HELLOS.contains(message.payload)){
            try{
              queue.add(new ConnectionParameters(rId, message.sourceAddress));
            }
            catch(IllegalStateException e){
              if(DEBUG) System.err.println("Queue is full");
              ignore=1;
            }
          }
          if(ignore==0){
            try{
              upsideHandlers.get(lId).receive(message);
            }
            catch(NullPointerException e){
              if(DEBUG) System.err.println("Source address not registred.");
            }
            HELLOS.add(message.payload);
          }
        }
      }

      else if (rpayload.equals("--ACK--")){
        /* Due to this error message: [__test__.Failure:_some_incoming_redundant_HELLO_after_already_received_ACK_must_be_pushed_upwards] */
        try{
          upsideHandlers.get(lId).receive(message);
          ACKS.add(message.payload);
        }
        catch(NullPointerException e){
          if(DEBUG) System.err.println("Source address not registred.");
        }
        // We can the hashset to find the corresponding HELLO and then we transfer it from HELLOS to HELLOS_DEFINITIVE */
        if(pN==0){
          for(String hello : HELLOS){
            String[] split_hello = hello.split(";");
            int rId2 = Integer.parseInt(split_hello[0]);
            int lId2 = Integer.parseInt(split_hello[1]);
            int pN2 = Integer.parseInt(split_hello[2]);
            if(pN2==pN && rId2 == lId){
              HELLOS.remove(hello);
              HELLOS_DEFINITIVE.add(hello);
            }
          }
        }
      }
      else {
        try{
          upsideHandlers.get(lId).receive(message);
        }
        catch(NullPointerException e){
          if(DEBUG) System.err.println("Source address not registred.");
        }
      }
    }
  }
}
