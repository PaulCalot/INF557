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
  private static boolean DEBUG = false;

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

            String payload;
            payload = new String(UDPPacket.getData(), 0, UDPPacket.getLength(), CONVERTER);
            /*
            payload = new String(UDPPacket.getData(), "utf-8");
            payload = payload.substring(0, UDPPacket.getLength());
          
            if(UDPPacket.getLength()>=2){
              if(payload.charAt(UDPPacket.getLength()-1) == '\0' && payload.charAt(UDPPacket.getLength()-3) == '\0'){
                if((UDPPacket.getData()[0]==-1) && (UDPPacket.getData()[1]==-2)){
                  if(DEBUG) System.out.println("utf-16");
                  payload = new String(UDPPacket.getData(), "utf-16");
                  payload = payload.substring(0, UDPPacket.getLength()/2);
                }
                else{
                  if(DEBUG) System.out.println("utf-16le");
                  payload = new String(UDPPacket.getData(), "utf-16le");
                  payload = payload.substring(0, UDPPacket.getLength()/2);
                }
              }
              else if(payload.charAt(UDPPacket.getLength()-2) == '\0' && payload.charAt(UDPPacket.getLength()-4) == '\0'){
                if(DEBUG) System.out.println("utf-16be");
                payload = new String(UDPPacket.getData(), "utf-16be");
                payload = payload.substring(0, UDPPacket.getLength()/2);
              }
            }*/


            if(DEBUG) System.out.print("payload: " + payload + " " + payload.length() + " ");
            
            handler.receive(new Message(payload, UDPPacket.getSocketAddress().toString()));
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
      System.out.println("Coucou_rela \n");
      try{
        System.out.println("Coucou\n");
//        byte[] sendBuffer = CONVERTER.encode(payload).array();
        byte[] sendBuffer = payload.getBytes(CONVERTER);

        DatagramPacket UDPPacket = new DatagramPacket(sendBuffer, sendBuffer.length, destinationAddress);
        if(DEBUG) System.out.println(sendBuffer.length);
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
