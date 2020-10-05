// link for more info : https://dzone.com/articles/java-thread-tutorial-creating-threads-and-multithr
// java is passed by value - so when the value is a reference
// then we can modify it
import java.lang.Thread; // implicitely imported so useless

class test implements Runnable {
    Thread t_1;
    Thread t_2;
    String name;
    point x;
    int x_;
    public test(Thread other_thread_1,Thread other_thread_2, String name_of_the_thread, point x_, int x__){
        this.t_1 = other_thread_1;
        this.t_2 = other_thread_2;
        this.name = name_of_the_thread;
        this.x = x_;
        this.x_= x__; 
    }
    public void run(){
        // function to edit because we implement the interface Runnable
        System.out.println(this.name + " (before): " + Integer.toString(this.x.get()) + "  [OK]");
        
        this.x.set(this.x_);
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
        System.out.println(this.name + " (after): " + Integer.toString(this.x.get()) + "  [OK]");
    }
}

class point {

    private int x;

    public point(int x_){
        this.x = x_;
    }
    public synchronized int get(){
        return this.x;
    }
    public synchronized void set(int x_){
        this.x = x_;
    }
}

class multithread {
    public static void main(String args[]){
        point x = new point(3);
        Thread t_1 = new Thread(new test(null, null, "first", x, 10));
        Thread t_2 = new Thread(new test(t_1, null, "second", x, 6));
        Thread t_3 = new Thread(new test(t_1, t_2, "third", x, 2));
        t_3.start();
        t_2.start();
        t_1.start();
        // => j'avais raison
        // résultat non déterministe. Car on modifie avant toute chose mais on sait pas si on ocmmecne par t3, t2 ou t1.
        // pour être sûr qu'on a bien une valeur déterministe, il faudrait qu'on s'assure que les autres codes ont bien terminé
        // avant de modifier la valeur. (donc avec une instruction dans )
        // run qui est après les try-catch.

        // je modifie maintenant le code pour qu'il soit modifié seulement APRES que les autres threads aient bien terminés

    }
}

