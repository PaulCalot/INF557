// Paul Calot && Philippe SAGBO
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;

public class GroundLayer {

  /**
   * This {@code Charset} is used to convert between our Java native String
   * encoding and a chosen encoding for the effective payloads that fly over the
   * network.
   */
  private static final Charset CONVERTER = StandardCharsets.UTF_8;

  /**
   * This value is used as the probability that {@code send} really sends a
   * datagram. This allows to simulate the loss of packets in the network.
   */
  public static double RELIABILITY = 1.0;

  private static DatagramSocket localSocket = null;
  private static Thread receiver = null;
  private static Handler handler = null;

  public static void start(int _localPort, Handler _handler)
      throws SocketException {
    if (handler != null)
      throw new IllegalStateException("GroundLayer is already started");
    handler = _handler;
    // TO BE COMPLETED
    receiver = new Thread(new Runnable() {
      public void run() {
        try{
          localSocket = new DatagramSocket(_localPort);
        }
        catch(SocketException e){
          System.err.println(e.getMessage());
          Thread.currentThread().interrupt();
          }
        while(!Thread.currentThread().isInterrupted()){
          try{

            byte[] recvBuffer = new byte[1024];
            DatagramPacket UDPPacket = new DatagramPacket(recvBuffer, recvBuffer.length);
            localSocket.receive(UDPPacket);
            String recv_data = new String(UDPPacket.getData());
          
            System.out.println("payload: " + recv_data.trim().length());
            handler.handle(new Message(CONVERTER.decode(ByteBuffer.wrap(recv_data.trim().getBytes())).toString(), UDPPacket.getSocketAddress().toString()));
          }
          catch(SocketException e){
            System.err.println(e.getMessage());
            Thread.currentThread().interrupt();
          }
          catch(IOException e){
            System.err.println(e.getMessage());
            Thread.currentThread().interrupt();
          }
        }
      }
    });
    receiver.setDaemon(true);
    receiver.start();
  }

  public static void send(String payload, SocketAddress destinationAddress) {
    if (Math.random() <= RELIABILITY) {
      // MUST SEND
      try{
        byte[] sendBuffer = CONVERTER.encode(payload).array();

        DatagramPacket UDPPacket = new DatagramPacket(sendBuffer, sendBuffer.length, destinationAddress);
        localSocket.send(UDPPacket);
      }
      catch(SocketException e){
        System.err.println(e.getMessage());
      }
      catch(IOException e){
        System.err.println(e.getMessage());
      }

    }
  }

  public static void close() {
    // TO BE COMPLETED
    localSocket.close();
    receiver.interrupt();
    handler = null;
    System.err.println("GroundLayer closed");
  }

}
