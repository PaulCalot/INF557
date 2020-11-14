import java.util.Timer;
import java.util.TimerTask;

public class ConnectedHandler extends Handler {
  
  private static boolean DEBUG = false;
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
  // to be completed

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
    // to be completed
  }

  @Override public void send(final String payload) { 
  // to be completed
    if(payload.equals(HELLO)){
      String hello_payload, ack_payload, message;
      int len, temp1, temp2, localId1, dstId = -1, packet_Number=0, i=0;

      hello_payload = localId+";"+dstId+";"+packet_Number+";"+HELLO;
      Ticker ticker = new Ticker(this.downside, hello_payload, this.destination, DELAY);
      while(i<MAX_REPEAT){
        message = "zzz"; Handler.this.queue.take();
        temp1 = message.indexOf(";");
        temp2 = message.indexOf(";", temp1);
        dstId = Integer.parseInt(message.substring(0, temp1));
        localId1 = Integer.parseInt(message.substring(temp1, temp2));
        if(localId == localId1) break;
        i++;
      }
      if (localId == localId1){
        ticker.close();
        ack_payload = localId+";"+dstId+";"+(++packet_Number)+";"+ACK;
        this.downside.send(ack_payload, destination);
      }
    }
  }

  @Override
  public void send(String payload, String destinationAddress) {
    no_send();
  }

  @Override
  public void close() {
    // to be completed
    super.close();
  }

}
