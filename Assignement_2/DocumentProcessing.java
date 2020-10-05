
import java.util.regex.*;

//import jdk.nashorn.internal.runtime.regexp.joni.Regex;

public class DocumentProcessing {

  public interface URLhandler {
    void takeUrl(String url);
  }

  public static URLhandler handler = new URLhandler() {
    @Override
    public void takeUrl(String url) {
      System.out.println(url);        // DON'T change anything here
    }
  };

  /**
   * Parse the given buffer to fetch embedded links and call the handler to
   * process these links.
   * 
   * @param data
   *          the buffer containing the html document
   */
  public static void parseBuffer(CharSequence data) {

    int start = 0;

    //Pattern p = Pattern.compile("<A((.|\\n)*?)href(\\s*?)=(\\s*?)\"(.*?)\"(.*?)>|<A((.|\\n)*?)href(\\s*?)=(\\s*?)\\'(.*?)\\'(.*?)>", Pattern.CASE_INSENSITIVE); // reluctant quantifiers
    Pattern p = Pattern.compile("<A((.|\\n)*?)href(\\s*?)=(\\s*?)(\"(?=([^']*?)\")|'(?=([^\"]*?)'))(.*?)(\"|')(.*?)>", Pattern.CASE_INSENSITIVE); // reluctant quantifiers

    Matcher m = p.matcher(data);
    
    while(m.find(start)){  // looking from index start (no useful)
      start = m.end();
      String possible_match = m.group();
      
      // Debug
      System.out.println(" ");
      System.out.println("Possible match in position " + Integer.toString(start) + " : " + possible_match);

      Pattern p_ = Pattern.compile("href(\\s*?)=(\\s*?)(\"(?=([^']*?)\")|'(?=([^\"]*?)'))(.*?)(\"|')", Pattern.CASE_INSENSITIVE);
      Matcher m_ = p_.matcher(possible_match);
      if(m_.find()){ // in theory, our previous pattern made sure that what we are doing here actually works 
        // and that there is no need for other securities on the presence of certain patterns.
        String possible_url = m_.group();
        // debug
        System.out.println("Possible url in position : " + possible_url);

        possible_url = possible_url.split("=(\\s)*?(\"(?=([^']*?)\")|'(?=([^\"]*?)'))", 2)[1];
        possible_url = possible_url.substring(0,possible_url.length()-1);
        try {
          MyURL my_url = new MyURL(possible_url);
          if(my_url.getProtocol().equals("http")){
            handler.takeUrl(possible_url);
          }
          else{
            System.out.println(possible_url + " : bad protocol.");
          }
        }
        catch(IllegalArgumentException e){
          System.err.println(e.getMessage());
        }
      }

      }
  }

  public static void main(String[] args) {
    String data = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"><html><head><title>test page 1</title></head><body> <hr><A aoeihaod \n href=\"http://www.enseignement.polytechnique.fre/Philippe.Chassignet/test/p2.html\">page 2</A>--<a target=\"blank_\" HREF  = \"http://www.enseignement.polytechnique.fr/profs/informatique/Philippe.Chassignet/test/p4.html' /><a href = \"http://www.enseignement.polytechnique.fr/profs/informatique/Philippe.Chassignet/test/p1.html\" id='1'>page 1 again</a><A href=\"https://www.enseignement.polytechnique.fr/profs/informatique/Philippe.Chassignet/test/p5.html\">page 5</A><hr></body></html>";

    DocumentProcessing.parseBuffer(data);

  }
}


