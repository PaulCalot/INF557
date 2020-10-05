public class MyURL {
    String protocol, hostname, path;
    int port = -1;

    MyURL(String url){
        String temp;
        String[] tab = url.split("/");
        
        if(tab.length==1){
            throw new IllegalArgumentException("This string cannot be parsed. \nA valid URL is of the following form: <protocol>://<hostname>[:<port>]/<path>");
        }
        
        if(url.indexOf("://")==-1){
            throw new IllegalArgumentException("This string doesn't contain any protocol. \nA valid URL is of the following form: <protocol>://<hostname>[:<port>]/<path>");
        }
        protocol = tab[0].split(":")[0];

        temp = url.substring(protocol.length()+3); // Remove the protocol and the "://" from the url

        if(temp.indexOf("/")==-1){
            throw new IllegalArgumentException("This string doesn't contain any path. \nA valid URL is of the following form: <protocol>://<hostname>[:<port>]/<path>");
        }

        if((temp.indexOf(":") != -1) && (temp.indexOf(":") < temp.indexOf("/"))){
            // If there is a ":" character and it is located before a "/"
            // we verify that we have a valid number (an positive integer)
            try {
                port = Integer.parseInt(temp.substring(temp.indexOf(":")+1).split("/")[0]);
                hostname = temp.split(":")[0];
            }
            catch (NumberFormatException e){
                throw new IllegalArgumentException("This port of your string is not a decimal number. \nA valid URL is of the following form: <protocol>://<hostname>[:<port>]/<path>");
            }
        }
        else 
            hostname = temp.split("/")[0];

        path = temp.substring(temp.indexOf("/")); // Remove the protocol and the "://" from the url
    }
    
    public int getPort(){
        return port;
    }

    public String getHost(){
        return hostname;
    }

    public String getProtocol(){
        return protocol;
    }

    public String getPath(){
        return path;
    }

    /*
    public static void main(String[] args){
        MyURL url = new MyURL(args[0]);
        System.out.println("Protocol: " + url.getProtocol());
        System.out.println("Hostname: " + url.getHost());
        System.out.println("Port: " + url.getPort());
        System.out.println("Path: " + url.getPath());

    }*/
}
