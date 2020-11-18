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
  private static final int DELAY = 300;

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
  private int hello_received=0;
  private int ack_received=0;
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

    String[] split = payload.split(";");
    if(split.length != 4){
      if (this.debug) System.err.println("Message not corresponding to expected pattern ... " + message.toString());
    }
    else{
        int rId = Integer.parseInt(split[0]);
        int lId = Integer.parseInt(split[1]);
        int pN = Integer.parseInt(split[2]);
        String rpayload = split[3].trim();

        if(this.localId==lId && rpayload.equals(ACK) && this.packetNumber==pN){
            if(!(this.remoteId != -1 && this.remoteId != rId)){
                if(this.remoteId==-1) this.remoteId = rId;
                if(debug) System.out.println("Message received: " + message.payload + "\n");
                synchronized(this){
                    ack_received=1;
                    notify();
                }
            }
        }

        else if(lId==-1 && pN==0 && rpayload.equals(HELLO)){
            if(debug) System.out.println("Message received: " + message.payload + "\n");
            if(this.remoteId==-1 || this.remoteId == rId){
                this.remoteId = rId;
                String ack_payload = this.localId+";"+this.remoteId+";0;--ACK--";
                this.downside.send(ack_payload, destination);
                if(debug) System.out.println("Message send " + ack_payload + "\n");
                synchronized(this){
                    hello_received=1;
                    notify();
                }
            }
            else{
                if(this.remoteId!= rId){
                  String other_ack = this.localId+";"+rId+";0;--ACK--";
                  this.downside.send(other_ack, destination);
                  if(debug) System.out.println("Message send " + other_ack + "\n");
                }
            }
        }
        
    }

  }


  @Override
  public void send(final String payload) {
      String formatted_payload = Integer.toString(this.localId)+";"+Integer.toString(this.remoteId)+";"+Integer.toString(this.packetNumber)+";"+payload;

      Handler handler_ = this.downside;
      
      TimerTask task = new TimerTask(){
          @Override
          public void run() {
            handler_.send(formatted_payload, destination);          
            if(debug) System.out.println("Message send " + formatted_payload + "\n");
          }
      };
      TIMER.schedule(task, new Date(), DELAY);
      synchronized(this){
          while(ack_received==0){
              try {
                      wait();
                  }
              catch(InterruptedException e){
                  if(this.debug) System.err.println(e.getMessage());
              }
          }
      }
      task.cancel();
      synchronized(this){
          while(hello_received==0 && payload.equals(HELLO)){
              try {
                  wait();
              }
              catch(InterruptedException e){
                  if(this.debug) System.err.println(e.getMessage());
              }
          }
      }
      TIMER.purge();
      this.packetNumber++;
  }

  @Override
  public void send(String payload, String destinationAddress) {
    no_send();
  }

  @Override
  public void close() {
    // TO BE COMPLETED
    //TIMER.cancel();
    if (this.debug) System.out.println("ConnectedHandler closed");
    super.close();
  }
}
