import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
public class ConnectedHandler extends Handler {

  /**
   * @return an integer identifier, supposed to be unique.
   */
  public static int getUniqueID() {
    return (int) (Math.random() * Integer.MAX_VALUE);
  }

  // don't change the two following definitions

  private static final String HELLO = "--HELLO--";
  private static final String ACK = "--ACK--";

  /**
   * the two following parameters are suitable for manual experimentation and
   * automatic validation
   */

  /** delay before retransmitting a non acked message */
  private static final int DELAY = 300;

  /** number of times a non acked message is sent before timeout */
  private static final int MAX_REPEAT = 10;

  /** A single Timer for all usages. Don't cancel it. **/
  private static final Timer TIMER = new Timer("ConnectedHandler's Timer",
      true);

  private final int localId;
  private final String destination;
  private Handler aboveHandler;
  private int remoteId;
  private int packetNumber;
  private Handler under;
  /**
   * Initializes a new connected handler with the specified parameters
   * 
   * @param _under
   *          the {@link Handler} on which the new handler will be stacked
   * @param _localId
   *          the connection Id used to identify this connected handler
   * @param _destination
   *          a {@code String} identifying the destination
   */
  public ConnectedHandler(final Handler _under, int _localId,
      String _destination) {
    super(_under, _localId, true);
    this.localId = _localId;
    this.destination = _destination;
    this.remoteId = -1;
    this.packetNumber = 0;
    this.under = _under;
    send(HELLO);
  }

  // don't change this definition
  @Override
  public void bind(Handler above) {
    if (!this.upsideHandlers.isEmpty())
      throw new IllegalArgumentException(
          "cannot bind a second handler onto this "
              + this.getClass().getName());
    this.aboveHandler = above;
    super.bind(above);
  }

  @Override
  public void handle(Message message) {
    // message is of the form : 
    // 1. ACK : destinationId;senderId;packetNumber;--ACK--
    // 2. PACKET : destinationId;senderId;packetNumber;payload
    // 3. HELLO : destinationId;-1;packetNumber;--HELLO--

    String payload = message.payload;
    String sourceAddress = message.sourceAddress;

    String[] split = payload.split(";",4);

    // HELLO message
    if(this.remoteId == -1 && split[1].equals("-1") && split[2].equals(Integer.toString(this.packetNumber)) && split[3].equals(HELLO)){
      this.remoteId = Integer.parseInt(split[0]);
      // in this order !!
      this.send(ACK);
      this.packetNumber = (this.packetNumber+1)%2;
      // no need to notify
    }
    else if(this.remoteId == Integer.parseInt(split[0]) && Integer.parseInt(split[0]) == this.localId){
      // in this case, we check if it's an acknoledgement
      int packetNumber_ = Integer.parseInt(split[2]);
      String payload_ = split[3];
      if(packetNumber_==this.packetNumber){
        // then we are recieving an expected ACK most likely
        if(payload_.equals(ACK)){
          this.packetNumber = (this.packetNumber+1)%2;
          notify();
        }
        else{
          System.out.println("Expected ACK : ");
          System.out.println("Recieved message : " + message.toString());
          System.out.println("Current state : "+Integer.toString(this.packetNumber));
        }
      }
    }
  }


  @Override
  public synchronized void send(final String payload) {    
    String tmp_payload = "";
    if(payload.equals(HELLO)){
      tmp_payload = Integer.toString(this.localId)+";"+"-1;0"+payload;
    }
    else{
      tmp_payload = Integer.toString(this.localId)+";"+Integer.toString(this.remoteId)+";"+Integer.toString(this.packetNumber)+";"+payload;
    }
    String formatted_payload = tmp_payload;
    TimerTask task;
    Handler handler_ = this.under;
    String destination_ = this.destination;
    if(payload.equals(ACK)){
      // send acklodegment only once
      task = new TimerTask(){
        @Override
        public void run() {
          handler_.send(formatted_payload, destination_);          
        }
      };
      TIMER.schedule(task, new Date());
    }
    else{    // waiting for acknoledgement
      task = new TimerTask(){
        int count = 0;
        @Override
        public void run() {
          count +=1;
          handler_.send(formatted_payload, destination_);          
          if(count>MAX_REPEAT){
            this.cancel();
            notify(); // to break WAITING mode of send
          }
        }
      };
      TIMER.schedule(task, new Date(), DELAY);
    }
    try {
      wait();
    }
    catch(InterruptedException e){
      System.err.println(e.getMessage());
    }
    task.cancel();
    TIMER.purge();
  }

  @Override
  public void send(String payload, String destinationAddress) {
    no_send();
  }

  @Override
  public void close() {
    // TO BE COMPLETED
    TIMER.cancel();
    System.err.println("ConnectedHandler closed");
    super.close();
  }
}
