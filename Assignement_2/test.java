// link for more info : https://dzone.com/articles/java-thread-tutorial-creating-threads-and-multithr

import java.lang.Thread; // implicitely imported so useless

class test implements Runnable {
    Thread t_1;
    Thread t_2;
    String name;
    int table;
    int count;

    public test(Thread other_thread_1,Thread other_thread_2, String name_of_the_thread, int table_, int count_){
        this.t_1 = other_thread_1;
        this.t_2 = other_thread_2;
        this.name = name_of_the_thread;
        this.table = table_;
        this.count = count_;
    }
    public void run(){
        // function to edit because we implement the interface Runnable
        for(int k = 1; k< this.count; k++){
            System.out.print(this.name + "  :  ");
            System.out.println(this.table*k);
        }
        try {
            if(t_2 != null){
                t_2.join();
            }
        }
        catch(Exception e){
            System.err.println(e.getMessage());
        }
        
        try{
            if(t_1 != null){
                t_1.join();
            }
        }
        catch(Exception e){
            System.err.println(e.getMessage());
        }

        System.out.println(this.name + " [OK]");
    }
}


class multithread {
    public static void main(String args[]){
        Thread t_1 = new Thread(new test(null, null, "first", 13, 10));
        Thread t_2 = new Thread(new test(t_1, null, "second", 17, 6));
        Thread t_3 = new Thread(new test(t_1, t_2, "third", 23, 3));
        t_3.start();
        t_2.start();
        t_1.start();

    }
}

