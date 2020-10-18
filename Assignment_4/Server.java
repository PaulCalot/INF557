import java.net.ServerSocket;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.net.SocketTimeoutException;
import java.io.InputStreamReader;
import java.lang.System;
import java.util.regex.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketException;

class Xserver {
    ServerSocket serverSocket;
    Socket clientSocket;
    int m_port;
    int m_nb_con = 10;
    int delay=5000;
    String m_routeDir;

    boolean DEBUG=true;

    String bad_request = "HTTP/1.1 400 Bad request\nContent-Length:-1\n";
    String welcome = "HTTP/1.1 200 OK\nContent-Length:10\n\nWelcome\n";
    String not_found = "HTTP/1.1 404 Not Found\nContent-Length: 10\n";

    public Xserver(int port, String routeDir){
        m_port = port;
        m_routeDir = routeDir;
        try{
            serverSocket = new ServerSocket(m_port, m_nb_con);
        }
        catch(IOException e){}

        try{
            serverSocket.setSoTimeout(delay);
        }
        catch(SocketException e){}

        while(true){
            try{
                clientSocket = serverSocket.accept();
            }
            catch(SocketTimeoutException e){
                System.err.println(e.getMessage());
                System.exit(0);
            }
            catch(IOException e){}
            try{
                if(DEBUG) System.out.println("Before handle_connexion");
                handleConnexion(clientSocket);
            }
            catch(IOException e){
                System.err.println(e.getMessage());
            }
        }
    }
        
    void handleConnexion(Socket clientSocket) throws IOException {
        int delay = 20*1000;            // Here, we wait for 20 seconds between two requests from the same client
        long old_time = System.nanoTime();

        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        String GET;
        while(true)
        {
            while(!in.ready()){
                if((System.nanoTime()-old_time)/1000000 > delay){
                    clientSocket.close();
                    return;
                }
            }
            
            GET = in.readLine();
            if(DEBUG) System.out.println(GET);
            if (GET!=null){
                if(DEBUG) System.out.println("Search for GET line in handle_connexion");
                String GET_pattern ="GET (.*?) HTTP/1.1";
                boolean b = Pattern.matches(GET_pattern, GET);
                if(!b) {
                    out.print(bad_request);
                    clientSocket.close();
                    return;
                }
                else{
                    String HOST = in.readLine();
                    if(DEBUG) System.out.println("Search for Host line in handle_connexion");
                    String HOST_pattern = "Host: (.*?)";
                    b = Pattern.matches(HOST_pattern, HOST);
                    if(!b) {
                        out.print(bad_request);
                        clientSocket.close();
                        return;
                    }
                    else{
                        if(DEBUG) System.out.println("read the remaining header lines of handle_connexion");
                        String line = in.readLine();
                        if(DEBUG) System.out.println("line: "+ line);

                        while(line != null && line.length() != 0){
                            line = in.readLine();
                            if(DEBUG) System.out.println(line);
                        }
                        if(DEBUG) System.out.println(GET);
                        String PATH = GET.substring(4, GET.length()-9);
                        if(DEBUG) System.out.println(PATH);
                        if(PATH.equals("/")){
                            out.print(welcome);
                        }
                        else{
                            if(DEBUG) System.out.println("search for a file in the server");
                            try{
                            if(DEBUG) System.out.println(m_routeDir+PATH);
                                BufferedReader fileReader = new BufferedReader(new FileReader(m_routeDir+PATH));
                                out.print("HTTP/1.1 200 OK\n");
                                String data = "\n";
                                do {
                                    line =  fileReader.readLine();
                                    data = data + line + "\n";
                                } while (line != null);
                                out.print("Content-Length: " + Integer.toString(data.length()) + "\n");
                                out.print(data);
                                fileReader.close();
                            }
                            catch(FileNotFoundException e){
                                out.print(not_found);
                            }
                        }
                    }
                }
                out.flush();
                old_time = System.nanoTime();
            }
        }
    }

    public static void main(String[] args){
        Xserver server = new Xserver(Integer.parseInt(args[0]), args[1]);
    }
}

public class Server {
}
