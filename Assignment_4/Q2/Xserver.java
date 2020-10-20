// Paul Calot && Philippe SAGBO

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.net.Socket;
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
import java.nio.charset.Charset;

/**
 * Basic implementation with a LinkedList.
 */

public class Xserver {
    ServerSocket serverSocket;
    Socket clientSocket;
    int m_port;
    int m_nb_con = 5;
    int delay=20*1000;
    String m_routeDir;
    int poolSize;

    boolean DEBUG=false;

    String bad_request = "HTTP/1.1 400 Bad Request\nContent-Length: 211\n\n<!DOCTYPE html>\n<html lang=en>\n<head><title>Error response</title></head>\n<body>\n<h1>Error response</h1>\n<p>Error code 400.\n<p>Message: Bad request.\n<p>Error code explanation: 400 = Bad request.\n</body>\n</html>\n";
    String welcome = "HTTP/1.1 200 OK\nContent-Type: text/html; charset=UTF-8\nContent-Length: 8\n\nWelcome\n";
    String not_found = "HTTP/1.1 404 Not Found\nContent-Length: 212\n\n<!DOCTYPE html>\n<html lang=en>\n<head><title>Error response</title></head>\n<body>\n<h1>Error response</h1>\n<p>Error code 404.\n<p>Message: Not found.\n<p>Error code explanation: 404 = File not found.\n</body>\n</html>\n";
    String OK_status = "HTTP/1.1 200 OK\nContent-Type: text/html; charset=UTF-8\n";
// String OK_status = "HTTP/1.1 200 OK\n";
    public Xserver(int port, String routeDir){
        m_port = port;
        m_routeDir = routeDir;
        try{
            serverSocket = new ServerSocket(m_port, m_nb_con);
        }
        catch(IOException e){
            System.err.println(e.getMessage());
            System.exit(0);
        }
        try{
            serverSocket.setSoTimeout(delay);
        }
        catch(SocketException e){
            System.err.println(e.getMessage());
            System.exit(0);
        }

        while(true){
            try{
               clientSocket = serverSocket.accept();
            }
            catch(SocketTimeoutException e){
               System.err.println(e.getMessage());
               break;
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
        try{
                serverSocket.close();
        }
        catch(IOException e){}
    }
    public Xserver(int port, String routeDir, int nb_threads){
        m_port = port;
        m_routeDir = routeDir;
        poolSize = nb_threads;

        final long initialThreadNumber = Thread.activeCount(); 
        final BlockingListQueue queue = new BlockingListQueue();
        Thread[] threadsList = new Thread[poolSize];

        try{
            serverSocket = new ServerSocket(m_port, m_nb_con);
            serverSocket.setSoTimeout(delay);
        }
        catch(IOException e){
            System.err.println(e.getMessage());
            System.exit(0);
        }
        if(poolSize <= 0) return;
        for(int k=0;k<poolSize;k++){
          threadsList[k] = new Thread(new ClientPool(queue, this), String.valueOf(k));
          threadsList[k].start();
        }

        while(true){
            try{
                clientSocket = serverSocket.accept();
                queue.enqueue(clientSocket);
                if((getWaitingThreadsNb(threadsList)==poolSize) && queue.isEmpty()){
                    for(int k=0;k<poolSize;k++){
                        threadsList[k].interrupt();
                        try{
                          threadsList[k].join();
                        }
                        catch (InterruptedException e1){}
                    }
                    break;
                }
            }
            catch(SocketTimeoutException e){
               System.err.println(e.getMessage());
               break;
            }
            catch(IOException e){
               System.err.println(e.getMessage());
               break;
            }
        }
        try{
            serverSocket.close();
        }
        catch(IOException e){}
     }

    public static int getWaitingThreadsNb(Thread[] threadsList){
        int waiting_nb = 0;
        for(int k=0; k<threadsList.length;k++)
            if(threadsList[k].getState()==Thread.State.WAITING) waiting_nb+=1;
    return waiting_nb;
    }

    void handleConnexion(Socket clientSocket) throws IOException {
        int delay = 200*1000;            // Here, we wait for 200 seconds between two requests from the same client
        long old_time = System.nanoTime();

        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        String GET;
        while(true)
        {
/*            while(!in.ready()){
                if((System.nanoTime()-old_time)/1000000 > delay){
                    in.close();
                    out.close();
                    clientSocket.close();
                    return;
                }
            }*/
            
            GET = in.readLine();
            if(DEBUG) System.out.println(GET);
            if (GET!=null){
                if(DEBUG) System.out.println("Search for GET line in handle_connexion");
                String GET_pattern ="GET (.*?) HTTP/1.1";
                boolean b = Pattern.matches(GET_pattern, GET);
                if(!b)
                {
                    out.print(bad_request);
                    out.flush();
                    in.close();
                    out.close();
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
                        out.flush();
                        in.close();
                        out.close();
                        clientSocket.close();
                        return;
                    }
                    else{
                        if(DEBUG) System.out.println("read the remaining header lines of handle_connexion");
                        String line = in.readLine();
                        if(DEBUG) System.out.println("line: "+ line);

                        while(line.length() != 0){
                            line = in.readLine();
                            if(DEBUG) System.out.println(line);
                        }
                        if(DEBUG) System.out.println(GET);
                        String PATH = GET.substring(4, GET.length()-9);
                        if(DEBUG) System.out.println(PATH);
                        if(PATH.equals("/")){
                            out.print(welcome);
                            out.flush();
                        }
                        else{
                            if(DEBUG) System.out.println("search for a file in the server");
                            int trailing_part = PATH.indexOf("#");
                            int querry_parameter = PATH.indexOf("?");                         
                            if(trailing_part != -1 && querry_parameter != -1){
                                int min = (trailing_part < querry_parameter) ? trailing_part : querry_parameter;
                                PATH = PATH.substring(0, min);
                            }
                            else if(trailing_part == -1 && querry_parameter != -1) PATH = PATH.substring(0, querry_parameter);
                            else if(trailing_part != -1 && querry_parameter == -1) PATH = PATH.substring(0, trailing_part);

                            // Now, we verify if PATH is valid
                            PATH = PATH.trim();
                            if(PATH.length()==0 || PATH.charAt(0) !=  '/' || PATH.charAt(1)==' '){
                                out.print(bad_request);
                                out.flush();
                                in.close();
                                out.close();
                                clientSocket.close();
                                return;
                            }
                            try{
                            if(DEBUG) System.out.println(m_routeDir+PATH);
                                BufferedReader fileReader = new BufferedReader(new FileReader(m_routeDir+PATH));
                                int taille_data = 0;
                                String data = "";
                                line = fileReader.readLine();
                                int png = 0;
                                while(line!=null){
                                    if(Charset.forName("utf-8").newEncoder().canEncode(line)==false){
                                        if(DEBUG) System.out.println("non-ascii is here");
                                        out.print(not_found);
                                        out.flush();
                                        png = 1;
                                        break;
                                    }
                                    taille_data += line.length()+1;
                                    data = data + line + "\n";
                                    line = fileReader.readLine();
                                }
                                if(png==0){
                                    out.print(OK_status);
                                    out.print("Content-Length: " + Integer.toString(taille_data) + "\n\n");
                                    out.print(data);
                                    out.flush();
                                }
                                fileReader.close();
                            }
                            catch(FileNotFoundException e){
                                out.print(not_found);
                                out.flush();
                            }
                        }
                    }
                }
//                out.flush();
                old_time = System.nanoTime();
            }
            else {
                in.close();
                out.close();
                return;
            }
        }
    }


    public static void main(String[] args){
        Xserver server;
        if(args.length<2)
            System.out.println("<Usage> javac Xserver port rootDir [nb_threads]");
        else if (args.length==2)
            server = new Xserver(Integer.parseInt(args[0]), args[1]);
        else server = new Xserver(Integer.parseInt(args[0]), args[1], Integer.parseInt(args[2]));
    }
}


class BlockingListQueue {
    private boolean debug = false;
    private final LinkedList<Socket> queue;

    public BlockingListQueue() {
        this.queue = new LinkedList<Socket>();
    }

    public synchronized boolean isEmpty() {
        return this.queue.size() == 0;
    }

    public synchronized void enqueue(Socket client_s) {
        this.queue.add(client_s);
        notifyAll();
    }

    public synchronized Socket dequeue() {
        try{
            while(this.queue.isEmpty()){
                if(debug) System.out.println(Thread.currentThread().toString()+" is (still) waiting.");
                wait();
            }
        return this.queue.remove();
        }
        catch(InterruptedException e){
            if(debug) System.err.println(Thread.currentThread().toString()+" has been interrupted.");
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    public synchronized long getSize(){
        return this.queue.size();
    }
}


class ClientPool implements Runnable {
    private BlockingListQueue m_queue; // takeURL alrady has it
    private boolean debug = true;
    Xserver m_server;

    public ClientPool(BlockingListQueue queue, Xserver server){
        m_queue = queue;
        m_server = server;
    }

    public void run(){
        if(debug) System.out.println(Thread.currentThread() + " was here [+].");
        while(!Thread.currentThread().isInterrupted()){
            Socket client_s = m_queue.dequeue(); // the thread may be force to wait in the given dequeue function
            if(client_s==null){ 
                Thread.currentThread().interrupt();
                if(debug) System.out.println(Thread.currentThread() + " was here [---].");
                break;
            }
            if(Thread.currentThread().isInterrupted()){
                Thread.currentThread().interrupt();
                if(debug) System.out.println(Thread.currentThread() + " was here [---].");
                break;
            }
            try{
                m_server.handleConnexion(client_s);
                client_s.close();
            }
            catch(IOException e){
                System.err.println(e.getMessage());
            }
        }
        if(debug) System.out.println(Thread.currentThread() + " was here [-].");
    }
}
