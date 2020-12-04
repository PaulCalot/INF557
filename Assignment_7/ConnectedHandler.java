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

  private final boolean debug = true;
  
  private final int localId;
  private final String destination;
  private Handler aboveHandler;

  // to be completed
  private int remoteId;
  private int lpacketNumber;
  private int rpacketNumber;
  
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

    // to be completed
    this.remoteId = -1;
    this.lpacketNumber = 0;
    this.rpacketNumber = 0;
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
    String payload = message.payload;

    String[] split = payload.split(";");
    if(split.length != 4){
        if(this.debug) System.err.println("Message not corresponding to expected pattern ... " + message.toString());
    }
    else{
        int rId = Integer.parseInt(split[0]);
        int lId = Integer.parseInt(split[1]);
        int pN = Integer.parseInt(split[2]);
        String rpayload = split[3].trim();


        if(this.localId==lId && rpayload.equals(ACK) && this.lpacketNumber==pN && this.remoteId==rId){
          if(debug) System.out.println("Message received: " + message.payload + "\n");
          synchronized(this){
              notify();
          }
        }

        else if(lId==-1 && pN==0 && rpayload.equals(HELLO)){
          if(remoteId==-1) {
              rpacketNumber++;
              this.remoteId = rId;
          }
          if(debug) System.out.println("hello received: " + message.payload + "\n");
          
          String ack_payload = this.localId+";"+rId+";"+pN+";--ACK--";
          this.downside.send(ack_payload, destination);
          if(debug) System.out.println("Message sent " + ack_payload + "\n");
        }
        else{
            if(debug) System.out.println("Message dropped: " + message.payload + "\n");
        }

    }
  }


  @Override
  public void send(final String payload) {
    if(payload.equals(HELLO)){
      String formatted_payload = this.localId+";"+this.remoteId+";0;--HELLO--";

      Handler handler_ = this.downside;
      TimerTask task = new TimerTask(){
          @Override
          public void run() {
            handler_.send(formatted_payload, destination);          
            if(debug) System.out.println("Message sent " + formatted_payload + "\n");
          }
      };
      TIMER.schedule(task, new Date(), DELAY);
      
      while (rpacketNumber == 0) {  // Wait for an hello
        synchronized (this) {
          try {
            wait();
          } catch (InterruptedException e) {}
        }
      }
      lpacketNumber++;
    
      task.cancel();
      TIMER.purge(); 
    }
  }

  @Override
  public void send(String payload, String destinationAddress) {
    no_send();
  }

  @Override
  public void close() {
    // TO BE COMPLETED
    if (this.debug) System.out.println("ConnectedHandler closed");
    super.close();
  }
}
