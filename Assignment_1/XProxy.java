import java.net.*;
import java.io.*;
import javax.net.ssl.SSLSocketFactory;
import javax.net.SocketFactory;
import java.util.regex.*;

public class Xurl extends MyURL {
    Xurl(String url, String proxy_name, int port) {
        super(url);
        String true_proxy = proxy_verification(proxy_name, port);
        String new_url = proxy_road_to_200_or_400(url); 
        proxy_download(new_url);
    } 
    
    public String proxy_verification(String proxy_name, int port) {
        Pattern p;
        Matcher m;
        String proxy = "http://";

//        if(port == -1) port = 80;
        
        if(proxy_name.indexOf("/")!=-1 || proxy_name.indexOf(":") != -1 or proxy_name){
            System.out.println("Invalid proxy!!");
            return null;
        }

        if(port != -1){
            proxy.concat(":");
            proxy.concat(Integer.toString(port));
        }
        proxy.concat("/");

        Socket s;
        PrintStream out;
        BufferedReader in;

        String line;
        int status_code;

        MyURL url_temp;

        try{
            while(true) // Follow_redirection
            {
                url_temp = new MyURL(proxy);

                if(url_temp.getProtocol().equals("https")) // Creation of a SSLSocket
                {
                    SocketFactory sslsockf = SSLSocketFactory.getDefault();
                    if(url_temp.getPort()==-1) // We use the default port for https: 443
                    {
                        s = sslsockf.createSocket(url_temp.getHost(), 443);
                    }
                    else  
                        s = sslsockf.createSocket(url_temp.getHost(), url_temp.getPort()); 
                }
                else
                {
                    if(url_temp.getPort() == -1)
                        s = new Socket(url_temp.getHost(), 80);
                    else
                        s = new Socket(url_temp.getHost(), url_temp.getPort());
                }
            
                out = new PrintStream(s.getOutputStream());
                in = new BufferedReader(new InputStreamReader(s.getInputStream()));
             
                // Follow the HTTP protocol of GET <path> HTTP/1.0 followed by an empty line
                out.print( "GET " + url_temp.getPath() + " HTTP/1.1\r\n" );
                out.print( "Host: " + url_temp.getHost() + "\r\n" );
                out.print("\r\n");

                // status_code retrieval
                line = in.readLine();
                p = Pattern.compile("\\d{3}");
                m = p.matcher(line);
                m.find();
                status_code = Integer.parseInt(line.substring(m.start(), m.end()));

                if(status_code == 301 || status_code == 302) {
                    // redirection following
                    p = Pattern.compile("Location: ");
                    m = p.matcher(line);
                    while(!p.matcher(line).find()) line = in.readLine();
                    m = p.matcher(line);
                    m.find();

                    // In case of the path of our new url is an empty string
                    if(line.charAt(line.length()-1) != '/' && line.split("/").length==3)
                        redirect_url = line.substring(m.end())+"/";
                    else
                        redirect_url = line.substring(m.end());
                }
                else {
                    in.close();
                    out.close();
                    s.close();
                    break;
                }
            }
        }
        catch(Exception e){}
        return redirect_url;
    }



    public void download(String new_url, MyURL url_temp) {
        try {
            Pattern p;
            Matcher m;
            Socket s;
            if(url_temp.getProtocol().equals("https")) // Creation of a SSLSocket
            {
                SocketFactory sslsockf = SSLSocketFactory.getDefault();
                if(url_temp.getPort()==-1) // We use the default port for https: 443
                {
                    s = sslsockf.createSocket(url_temp.getHost(), 443);
                }
                else  
                    s = sslsockf.createSocket(url_temp.getHost(), url_temp.getPort()); 
            }
            else
            {
                if(url_temp.getPort() == -1)
                    s = new Socket(url_temp.getHost(), 80);
                else
                    s = new Socket(url_temp.getHost(), url_temp.getPort());
            }
            
            PrintStream out;
            BufferedReader in;
            out = new PrintStream(s.getOutputStream());
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
             
            // Follow the HTTP protocol of GET <path> HTTP/1.0 followed by an empty line
            out.print( "GET " + url_temp.getPath() + " HTTP/1.1\r\n" );
            out.print( "Host: " + url_temp.getHost() + "\r\n" );
            out.print("\r\n");
            
            String line = in.readLine();
            int content = -1;
            Pattern content_length, chunked;
            content_length = Pattern.compile("Content-Length: ");
            chunked = Pattern.compile("Transfer-Encoding: chunked");

            while(line.length() != 0){
                if(content_length.matcher(line).find()){
                    m = content_length.matcher(line);
                    m.find();
                    content = Integer.parseInt(line.substring(m.end()));
                }
                else if(chunked.matcher(line).find()){
                    content = -2;
                }
                line = in.readLine();
            }

            if(content == 0){
                System.out.println("Error, your url is invalid. Please verify it.");
            }
            else{
                String filename;
                File f;
                if(url_temp.getPath().equals("/")) filename = "index";
                else {
                    String[] tab = url_temp.getPath().split("/");
                    filename = tab[tab.length-1];
                }
                f = new File(filename);
                f.createNewFile();
                FileOutputStream writer = new FileOutputStream(filename);
                BufferedInputStream input = new BufferedInputStream(s.getInputStream());
                byte[] str2bytes;
            
                if(content == -2){
                    line = in.readLine();
                    int chunck_size = Integer.parseInt(line, 16);
                    int i=0, count=0;
                    char[] c = new char[1];

                    while(chunck_size != 0){
                        count = 0;
                        while(count<chunck_size){
                            in.read(c,0,1);
                            writer.write(c[0]);
                            count++;
                        }
                        line = in.readLine();
                        line = in.readLine();
                        if(line==null) break;
                        chunck_size = Integer.parseInt(line, 16);
                    }
                }
                else if(content != -1){
                    int count=0;
                    char[] c = new char[1];
                    while(count<content){
                        in.read(c,0,1);
                        writer.write(c[0]);
                        count++;
                    }
                }
                else{
                    while(true){
                        line = in.readLine();
                        if(line != null){
                            str2bytes = line.getBytes();
                            if(line.indexOf("</html>") != -1){
                                writer.write(str2bytes);
                                break;
                            }
                            else 
                                writer.write(str2bytes);
                        }
                        writer.write(10);   // "\n"
                    }
                }
                writer.close();
                System.out.println("File saved sucessfully.");
            }
            s.close();
            in.close();
            out.close();
        }
        catch(Exception e){}
    }

    
    public String road_to_200_or_400(String website) {
        Pattern p;
        Matcher m;

        Socket s;
        PrintStream out;
        BufferedReader in;

        String line;
        int status_code;

        String redirect_url = website;
        MyURL url_temp;

        try{
            while(true) // Follow_redirection
            {
                url_temp = new MyURL(redirect_url);

                if(url_temp.getProtocol().equals("https")) // Creation of a SSLSocket
                {
                    SocketFactory sslsockf = SSLSocketFactory.getDefault();
                    if(url_temp.getPort()==-1) // We use the default port for https: 443
                    {
                        s = sslsockf.createSocket(url_temp.getHost(), 443);
                    }
                    else  
                        s = sslsockf.createSocket(url_temp.getHost(), url_temp.getPort()); 
                }
                else
                {
                    if(url_temp.getPort() == -1)
                        s = new Socket(url_temp.getHost(), 80);
                    else
                        s = new Socket(url_temp.getHost(), url_temp.getPort());
                }
            
                out = new PrintStream(s.getOutputStream());
                in = new BufferedReader(new InputStreamReader(s.getInputStream()));
             
                // Follow the HTTP protocol of GET <path> HTTP/1.0 followed by an empty line
                out.print( "GET " + url_temp.getPath() + " HTTP/1.1\r\n" );
                out.print( "Host: " + url_temp.getHost() + "\r\n" );
                out.print("\r\n");

                // status_code retrieval
                line = in.readLine();
                p = Pattern.compile("\\d{3}");
                m = p.matcher(line);
                m.find();
                status_code = Integer.parseInt(line.substring(m.start(), m.end()));

                if(status_code == 301 || status_code == 302) {
                    // redirection following
                    p = Pattern.compile("Location: ");
                    m = p.matcher(line);
                    while(!p.matcher(line).find()) line = in.readLine();
                    m = p.matcher(line);
                    m.find();

                    // In case of the path of our new url is an empty string
                    if(line.charAt(line.length()-1) != '/' && line.split("/").length==3)
                        redirect_url = line.substring(m.end())+"/";
                    else
                        redirect_url = line.substring(m.end());
                }
                else {
                    in.close();
                    out.close();
                    s.close();
                    break;
                }
            }
        }
        catch(Exception e){}
        return redirect_url;
    }


    public static void main(String[] args) throws Exception {
        Xurl wget = new Xurl(args[0]);
    }
}
