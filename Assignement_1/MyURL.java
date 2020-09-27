// https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html (pour les Pattern)
import java.util.regex.*;

public class MyURL { 

    private String m_url,  m_protocol, m_host, m_path;
    private int m_port;

    public MyURL(String url){
        this.m_url = url;
        
        boolean b1 = Pattern.matches("([a-zA-Z])*://([a-zA-Z_0-9\\.])*:([0-9])*/([a-zA-Z_0-9/\\.])*", url);
        boolean b2 = Pattern.matches("([a-zA-Z])*://([a-zA-Z_0-9\\.])*/([a-zA-Z_0-9/\\.])*", url);
        
        if(!b1 && !b2){
            throw new IllegalArgumentException(url + " is not in the right format ! It should be : <protocol>://<hostname>[:<port>]/<path>");
        }
                 
        String split[] = url.split("://",2);
       
        this.m_protocol = split[0];
        split = split[1].split("/", 2);

        this.m_path = "/" + split[1];

        split = split[0].split(":",2);

        this.m_host = split[0];
        
        if(split.length == 1){
            this.m_port = -1;
        }
        else{
            this.m_port = Integer.parseInt(split[1]);
        }
    }

    public String getProtocol(){
        return this.m_protocol;
    }
    public String getHost(){
        return this.m_host;
    }
    public int getPort(){
        return this.m_port;
    }
    public String getPath(){
        return this.m_path;
    }

    public String getURL(){
        return this.m_url;
    }

    public static void main(String[] args) {

        MyURL url = new MyURL("http://www.google.com/");

        System.out.print("Starting test on " + url.getURL() + "     ");

        String url_protocol = url.getProtocol();
        String url_Host = url.getHost();
        int url_Port = url.getPort();
        String url_getPath = url.getPath();

        if(!url_protocol.equals("http")){
            System.out.println("Wrong protocol, got '" + url_protocol + "'.");
            return;
        }

        if(!url_Host.equals("www.google.com")){
            System.out.println("Wrong host : "+url_Host);
            return;
        }

        if(url_Port != -1){
            System.out.println("Wrong port : " + Integer.toString(url_Port));
            return;
        }

        if(!url_getPath.equals("/")){
            System.out.println("Wrong path : " + url_getPath);
            return;
        }
        System.out.println("[OK]");

        url = new MyURL("http://localhost:8888/tree/");

        System.out.print("Starting test on " + url.getURL() + "     ");

        url_protocol = url.getProtocol();
        url_Host = url.getHost();
        url_Port = url.getPort();
        url_getPath = url.getPath();

        if(!url_protocol.equals("http")){
            System.out.println("Wrong protocol, got '" + url_protocol + "'.");
            return;
        }

        if(!url_Host.equals("localhost")){
            System.out.println("Wrong host : "+url_Host);
            return;
        }

        if(url_Port != 8888){
            System.out.println("Wrong port : " + Integer.toString(url_Port));
            return;
        }

        if(!url_getPath.equals("/tree/")){
            System.out.println("Wrong path : " + url_getPath);
            return;
        }
        System.out.println("[OK]");
    }
}