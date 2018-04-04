/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package monitor3.common;

import monitor3.common.RPI_IO_DATA;
import RPI_IO_Lib.RPI_IO;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Federico
 */
public class RPI_Sampler implements Runnable{
    
    int period=0;
    RPI_IO rpio=null;
    RPI_IO_DATA data=new RPI_IO_DATA();
    
    public RPI_Sampler(RPI_IO rpio, int period){
        this.rpio=rpio;
        this.period=period;
    }

    @Override
    public void run() {
        
        while(true){
            data.setInput_port(rpio.getInputs());
            data.setOutput_port(rpio.getRlyStatus());
            
            for(int i=0;i<8;i++){
                data.setAnalog(i, rpio.getChannel(i+1));
            }
            try {
                Thread.sleep(period);
            } catch (InterruptedException ex) {
                Logger.getLogger(RPI_Sampler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        
    }
    
    
}
