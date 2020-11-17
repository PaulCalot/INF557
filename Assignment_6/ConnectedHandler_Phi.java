// Paul Calot && Philippe SAGBO
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
  private static final int DELAY = 3000;

  /** number of times a non acked message is sent before timeout */
  private static final int MAX_REPEAT = 10;

  /** A single Timer for all usages. Don't cancel it. **/
  private static final Timer TIMER = new Timer("ConnectedHandler's Timer",
      true);

  private final boolean debug = false;
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

    String[] split = payload.split(";");
    if(split.length != 4){
      if (this.debug) System.err.println("Message not corresponding to expected pattern ... " + message.toString());
    }
    else{
      // HELLO message
      if(this.remoteId == -1 && split[1].equals("-1") && split[2].equals(Integer.toString(this.packetNumber)) && split[3].equals(HELLO)){
        this.remoteId = Integer.parseInt(split[0]);
        synchronized(this) {
            notify();
        }
        this.send(ACK);
      }
      else if(Integer.parseInt(split[1]) == this.localId){
        this.remoteId = Integer.parseInt(split[0]);
        int packetNumber_ = Integer.parseInt(split[2]);
        String payload_ = split[3].substring(0, split[3].length()-1);
        if(packetNumber_==this.packetNumber){
            if(debug) System.out.println("Right packet_number");
          // then we are recieving an expected ACK most likely
            if(payload_.equals(ACK)){
                synchronized(this) {
                    notify();
                }
                if(debug) System.out.println("ACK received");
                this.packetNumber++;
          }
          else if(this.remoteId == Integer.parseInt(split[0]) && Integer.parseInt(split[1]) == this.localId && split[2].equals(Integer.toString(this.packetNumber))){
            this.send(ACK);
          }
      /*    else if(this.debug){
            System.out.println("Expected ACK : ");
            System.out.println("Recieved message : " + message.toString());
            System.out.println("Current state : "+Integer.toString(this.packetNumber));
          }*/
        }
        else{
            if(debug) System.out.println("Wrong packet_number");
        }
      }
    }
  }


  @Override
  public synchronized void send(final String payload) {
    if(!payload.equals(ACK)){
      String formatted_payload = Integer.toString(this.localId)+";"+Integer.toString(this.remoteId)+";"+Integer.toString(this.packetNumber)+";"+payload;

      Handler handler_ = this.under;
//      under.send(formatted_payload, this.destination);
      
      TimerTask task = new TimerTask(){
          int count = 0;
          @Override
          public void run() {
            count +=1;
            handler_.send(formatted_payload, destination);          
            //if(count>MAX_REPEAT){
            //  this.handle(Message( Integer.toString(this.localId)+";"+Integer.toString(this.remoteId)+";"+Integer.toString(this.packetNumber)+";"+ACK,));
            //}
          }
        };
        TIMER.schedule(task, new Date(), DELAY);
      try {
          wait();
      }
      catch(InterruptedException e){
        if(this.debug) System.err.println(e.getMessage());
      }
      task.cancel();
    }
    else if(payload.equals(ACK)){
      String ack_payload = Integer.toString(this.localId)+";"+Integer.toString(this.remoteId)+";"+Integer.toString(this.packetNumber)+";"+payload;
      this.under.send(ack_payload, destination);
      if(debug) System.out.println("ACK send");
      this.packetNumber++;
    }
      TIMER.purge();
  }

  @Override
  public void send(String payload, String destinationAddress) {
    no_send();
  }

  @Override
  public void close() {
    // TO BE COMPLETED
    //TIMER.cancel();
    if (this.debug) System.err.println("ConnectedHandler closed");
    super.close();
  }
}
