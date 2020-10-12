
import java.util.Set;

public class ThreadStatus {
    public static void main(String args[]) throws Exception{
        for ( int i=0; i< 5; i++){
            Thread t = new Thread(new MyThread());
            t.setName("MyThread:"+i);
            t.start();
        }
        int threadCount = 0;
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for ( Thread t : threadSet){
            if ( t.getThreadGroup() == Thread.currentThread().getThreadGroup()){
                System.out.println("Thread :"+t+":"+"state:"+t.getState());
                ++threadCount;
            }
        }
        System.out.println("Thread count started by Main thread:"+threadCount+" : "+Integer.toString(Thread.activeCount()));
    }
}

class MyThread implements Runnable{
    public void run(){
        try{
            Thread.sleep(2000);
        }catch(Exception err){
            err.printStackTrace();
        }
    }}

