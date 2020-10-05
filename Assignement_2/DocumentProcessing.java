
import java.util.regex.*;

//import jdk.nashorn.internal.runtime.regexp.joni.Regex;

public class DocumentProcessing {
  private static boolean debug = false;

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
    String line_terminators = "\\R"; //"\\n|\\r\\n|\\r|\\u0085|\\u2028|\\u2029";
    String anything_plus_line_terminators = "(([^>]|" + line_terminators + ")*?)";
    String between_A_and_href = "((\\s" + anything_plus_line_terminators + "\\s)|\\s)";
    String single_or_double_quotes_lookahead = "(\"(?=([^'\"]*?)\")|'(?=([^'\"]*?)'))";
    //String single_or_double_quotes_behind = "((?<=\"([^']*?))\"|(?<='([^\"]*?))')"; // requires a fixed lenght here so it does not work
    String raw_url_pattern = single_or_double_quotes_lookahead+"(.*?)"+"(\"|')"; // basically handles the "url" or 'url'
    String href_and_spaces = "href(\\s*?)=(\\s*?)";
    //Pattern p = Pattern.compile("<A((.|\\n)*?)href(\\s*?)=(\\s*?)\"(.*?)\"(.*?)>|<A((.|\\n)*?)href(\\s*?)=(\\s*?)\\'(.*?)\\'(.*?)>", Pattern.CASE_INSENSITIVE); // reluctant quantifiers
    Pattern p = Pattern.compile("<A" + between_A_and_href + href_and_spaces + raw_url_pattern + anything_plus_line_terminators + ">", Pattern.CASE_INSENSITIVE); // reluctant quantifiers

    Matcher m = p.matcher(data);
    
    while(m.find(start)){  // looking from index start (no useful)
      start = m.end();
      String possible_match = m.group();
      
      // Debug
      if(debug){
        System.out.println(" ");
        System.out.println("Possible match in position " + Integer.toString(start) + " : " + possible_match);
      }
      Pattern p_ = Pattern.compile(href_and_spaces+raw_url_pattern, Pattern.CASE_INSENSITIVE);
      Matcher m_ = p_.matcher(possible_match);
      if(m_.find()){ // in theory, our previous pattern made sure that what we are doing here actually works 
        // and that there is no need for other securities on the presence of certain patterns.
        String possible_url = m_.group();
        // debug
        if(debug){  
          System.out.println("Possible url in position : " + possible_url);
        }
        possible_url = possible_url.split("(?i)"+href_and_spaces+single_or_double_quotes_lookahead, 2)[1]; // not working properlyu - maybe add a condition on the second " / ' after the url
        possible_url = possible_url.substring(0,possible_url.length()-1);
        try {
          MyURL my_url = new MyURL(possible_url);
          if(my_url.getProtocol().equals("http")){
            handler.takeUrl(possible_url);
          }
          else{
            if(debug){
              System.out.println(possible_url + " : bad protocol.");
            }
          } 
        }
        catch(IllegalArgumentException e){
          if(debug){
            System.err.println(e.getMessage());
         }
        } 
      }

      }
  }

  public static void main(String[] args) {
    //String data = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"><html><head><title>test page 1</title></head><body> <hr><A aoeihaod \n href=\"http://www.enseignement.polytechnique.fre/Philippe.Chassignet/test/p2.html\">page 2</A>--<a target=\"blank_\" HREF  = 'http://www.enseignement.polytechnique.fr/profs/informatique/Philippe.Chassignet/test/p4.html' /><a href = \"http://www.enseignement.polytechnique.fr/profs/informatique/Philippe.Chassignet/test/p1.html\" id='1'>page 1 again</a><A href=\"https://www.enseignement.polytechnique.fr/profs/informatique/Philippe.Chassignet/test/p5.html\">page 5</A><hr></body></html>";
    if(debug){
      String data = "<a href='http://host/file.ext1'><a href='http://host/file.ext2'>blah</a>";
      DocumentProcessing.parseBuffer(data);
    }
  }
}


