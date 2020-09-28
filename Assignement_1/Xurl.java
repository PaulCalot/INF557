
//import java.lang.Object;
import java.net.Socket; // https://docs.oracle.com/javase/7/docs/api/java/net/Socket.html
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress; // https://docs.oracle.com/javase/7/docs/api/java/net/InetAddress.html
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.io.OutputStream;
import java.io.PrintStream;
//import java.io.PrintWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.*;

public class Xurl {

    MyURL my_url;
    
    public Xurl(String url){
        my_url = new MyURL(url);
    }
    
    public void Download() {
        
        // while we don't have something to download we keep on going 

        if(my_url.getProtocol().equals("http")){
            try {
                InetAddress ip = InetAddress.getByName(my_url.getHost());
                int port = (my_url.getPort() == -1) ? 80 : my_url.getPort();
                Socket socket = new Socket();
                
                socket.connect(new InetSocketAddress(ip, port)); // timeout value in ms
            
                PrintStream writer  = new PrintStream(socket.getOutputStream()); // https://docs.oracle.com/javase/7/docs/api/java/io/PrintStream.html
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); // https://docs.oracle.com/javase/7/docs/api/java/io/InputStreamReader.html

                // writing request
                //System.out.println("GET " + this.my_url.getPath() + " HTTP/1.1\r\nHost: " + this.my_url.getHost()+":"+port+"\r\n\r\n");
                writer.print("GET " + this.my_url.getPath() + " HTTP/1.1\r\nHost: " + this.my_url.getHost()+":"+port+"\r\n\r\n");

                String name[] = this.my_url.getPath().split("/");
                String name_file = (name.length == 0) ? "index" : name[name.length-1];

                // reading response :
                String line = reader.readLine();
                
                String reply[] = line.split("\\s+");
                assert(reply.length == 3);
                
                if(reply[1].equals("200")){
                    System.out.println(line);
                }
                else{
                    System.err.println("Can not download ; server reply was : " + line);
                    reader.close();
                    System.exit(1);
                }
                // reading header
                boolean b2 = false;
                while(!(line = reader.readLine()).equals("")){
                    System.out.println(line);
                    /*String split[] = line.split(":");
                    if(split.length > 1 && split[0].equals("Content-Length") && split[1] != "0"){
                        b2 = true;
                    }*/
                }
                //System.out.println("Downloading now ... ");
                // reading and saving file if there is one
                /*while((line = reader.readLine()) != null){
                    writer_to_file.write(line+"\n");
                    System.out.println(line);
                }*/
                if(reader.ready()){
                    BufferedWriter writer_to_file =  new BufferedWriter(new FileWriter(name_file));

                    while(reader.ready()){
                        line = reader.readLine();
                        writer_to_file.write(line+"\r\n");
                        //System.out.println(line);
                    }
                    writer_to_file.close();
                }
                reader.close();
            }
            catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        Xurl my_Xurl = new Xurl(args[0]);
        my_Xurl.Download();
    }
}   
 