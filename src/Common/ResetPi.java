
package Common;

import RPI_IO_Lib.RPI_IO;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to handle reboot and shutdown commands for the Raspberry Pi.
 * Hardware reboot can be performed by selecting two digital inputs for
 * a period longer than 7 seconds. Two methods are provided for selecting 
 * each digital inputs.
 *
 * @author Federico
 */
public class ResetPi {
    
    private RPI_IO rpio=null;
    private int in1=0;
    private int in2=0;
    private int tim=0;
    
    /**
     * Class constructor
     * @param rpio 
     */
    public ResetPi(RPI_IO rpio){
        this.rpio=rpio;
    }
    
    /**
     * Select digital input #1 
     * @param i int between 1..8
     */
    public void setInput1(int i){
        in1=i;
    }
    /**
     * Select digital input #2
     * @param i int between 1..8
     */
    public void setInput2(int i){
        in2=i;
    }
    
    /**
     * Starts Reset object and runs every t seconds
     * @param t int time in milliseconds
     */
    public void start(int t){
        this.tim=t;
        Thread task=new Thread(new reset(),"Monitor Reset");
        task.start();
    }
    
    /**
     * Reboot method
     */
    public void resetCommand() {
        Process p;
        try {
            System.out.println("RESET COMMAND");
            p = Runtime.getRuntime().exec("sudo reboot now");
            p.waitFor();
        } catch (IOException ex) {
            Logger.getLogger(ResetPi.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(ResetPi.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    /**
     * Shutdown method
     */
    public void shutDownCommand() {
        Process p;
        try {
            System.out.println("RESET COMMAND");
            p = Runtime.getRuntime().exec("sudo shutdown now");
            p.waitFor();
        } catch (IOException ex) {
            Logger.getLogger(ResetPi.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(ResetPi.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    /** 
     * Reset Inner class.
     * Checks if in1 and in2 are set for more than 7 seconds and invokes
     * the reboot method.
     * Runs every "tim" seconds
     */
    private class reset implements Runnable{

        @Override
        public void run() {
            int timer=0;
            
            while(true){
               if(!rpio.getInput(in1) && rpio.getInput(in2)){
                   timer=timer+1;
                   System.out.println("Reset Timer: "+timer);
               }else{
                   timer=0;
               }
               
               if(timer>=7){
                   resetCommand();
                   timer=0;
               }
                try {  
                    Thread.sleep(tim);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ResetPi.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
           
        }
        
    }
    
}
