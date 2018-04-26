/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Common;

import RPI_IO_Lib.RPI_IO;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Federico
 */
public class ResetPi {
    
    private RPI_IO rpio=null;
    private int in1=0;
    private int in2=0;
    private int tim=0;
    
    
    public ResetPi(RPI_IO rpio){
        this.rpio=rpio;
    }
    
    public void setInput1(int i){
        in1=i;
    }
    
    public void setInput2(int i){
        in2=i;
    }
    
    public void start(int t){
        this.tim=t;
        Thread task=new Thread(new reset(),"Monitor Reset");
        task.start();
    }
    
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
