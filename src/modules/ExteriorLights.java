/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modules;

import Comunicatioms.Gmail;
import RPI_IO_Lib.RPI_IO;
import java.util.Calendar;

/**
 *
 * @author Federico
 */
public class ExteriorLights {
    RPI_IO rpio=null;
    double latitud=0;
    double longitud=0;
    int input=0;    //first RPI board input port
    int count=0;    //How many inputs
    int outputRly=0; //first RPI board output port
    int timer=0; // timer to turn off lights in minutes
    Gmail email = new Gmail("svmi.radar@gmail.com","svmi1234");
    boolean email_flag=false;
    
    public ExteriorLights(RPI_IO rpio) {
        this.rpio=rpio;
        System.out.println("Setting up Exterior lights task");
    }

    public void setEmailFlag(boolean t){
        this.email_flag=t;
    }
    
    public boolean getEmailFlag(){
        return this.getEmailFlag();
    }
    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    public void setInput(int input) {
        this.input = input;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setOutputRly(int outputRly) {
        this.outputRly = outputRly;
    }

    public void setTimer(int timer) {
        this.timer = timer;
    }
    
    public void start(){
        Thread task=new Thread(new ExteriorLightsTask(this));
        task.start();
    }
    

    
    
    
}
