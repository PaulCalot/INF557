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
  private int localPN;
  private int remotePN;
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
    this.localPN = 0;
    this.remotePN = 0;
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
    if(debug) System.out.println("recieved : " + message.toString());
    String payload = message.payload;

    String[] split = payload.split(";");
    if(split.length != 4 && split.length !=3){
      if (this.debug) System.err.println("Message not corresponding to expected pattern ... " + message.toString());
    }
    else{
      // If we receive an ACK for the connexion setup
      int lId = Integer.parseInt(split[1]);
      int PN = Integer.parseInt(split[2]);
      int rId = Integer.parseInt(split[0]);
      String parsedPayload = "";
      if(split.length==4){
        parsedPayload = split[3];
      }
      if(parsedPayload.equals(HELLO)){
        if(lId == -1 && PN == 0 && rId >= 0){
          if(this.remoteId == -1){
            if(this.debug){
              System.out.println("First HELLO");
            }
            this.remoteId = rId;
            send(ACK);
            this.remotePN++;
          }
          else if (this.remoteId == rId){
            if(this.debug){
              System.out.println("Already received at least one HELLO");
            }
            // problem : we have to send an hello back
            // however for ACK, we use the rPN.
            // so we have to decrease the rPN, since it should be 1 by now
            // as we already received one HELLO at least
            this.remotePN--;
            send(ACK);
            this.remotePN++;
          }
        }
      }
      else if(parsedPayload.equals(ACK)){
        if(this.remoteId==rId && this.localId == lId){ // the ACK comes has the right Id
          // in addition we want to check if we get the right package number (ack to previous message)
          if(PN == this.localPN){
            if(this.debug) System.out.println("Received matched 'ACK'. Notifying all.");
            this.localPN++;
            synchronized(this){
              notifyAll();
            }
          }
        }
      }
      else{
        if(aboveHandler==null){
          return;
        }
        // checking if the message is correct
        if(this.remoteId==rId && this.localId == lId){
          if(PN == this.remotePN){
            this.aboveHandler.receive(new Message(parsedPayload, Integer.toString(this.localId)));
            this.send(ACK);
            this.remotePN++;
          }
          else if(PN == this.remotePN-1){
            // in this case, we already received the message
            // but our ack never was received.
            // thus we send it again, without passing the message to above handler.
            this.remotePN--;
            this.send(ACK);
            this.remotePN++;
          }
        }
      }
    }
  }

  @Override
  public void send(final String payload) {
    if(!payload.equals(ACK)){
      int local_save_lPN = this.localPN;
      String formatted_payload = Integer.toString(this.localId)+";"+Integer.toString(this.remoteId)+";"+Integer.toString(this.localPN)+";"+payload;

      Handler handler_ = this.under;
      
      TimerTask task = new TimerTask(){
      int count = 0;

      @Override
      public void run() {
        if(debug)System.out.println("Sending " + formatted_payload + " for the " + count + "th times.");
        count +=1;
        handler_.send(formatted_payload, destination);          
        //if(count>MAX_REPEAT){
        //  this.handle(Message( Integer.toString(this.localId)+";"+Integer.toString(this.remoteId)+";"+Integer.toString(this.packetNumber)+";"+ACK,));
        //}
        }
      };

      TIMER.schedule(task, new Date(), DELAY);

      // we should continue as long as we did not received the right ACK
      while(local_save_lPN==this.localPN){
        if(this.debug) System.out.println("Waking to the wrong ACK.");
        synchronized(this){
          try {
              wait();
          }
        catch(InterruptedException e){
          if(this.debug) System.err.println(e.getMessage());
          }
        }
        task.cancel();
      }
    }
    else if(payload.equals(ACK)){
      // we are carefull to use the right packet number for the remote Id.
      String ack_payload = Integer.toString(this.localId)+";"+Integer.toString(this.remoteId)+";"+Integer.toString(this.remotePN)+";"+payload;
      this.under.send(ack_payload, destination);
      if(debug) System.out.println("ACK send");
    }
      TIMER.purge();
  }

  @Override  public void send(String payload, String destinationAddress) {
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
