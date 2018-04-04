/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Common;

import RPI_IO_Lib.RPI_IO;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Federico
 */
public class PulseOutput implements Runnable {
    
    RPI_IO rpio=null;
    int relay=0;
    int timer=0;
    
    public PulseOutput(RPI_IO rpio, int relay, int timer){
        this.rpio=rpio;
        this.relay=relay;
        this.timer=timer;
    }

    @Override
    public void run() {
        
        rpio.setRly(relay);
        try {
            Thread.sleep(timer*60*1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(PulseOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
        rpio.resetRly(relay);
    }
    
}
