/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modules;

import Comunicatioms.Gmail;
import RPI_IO_Lib.RPI_IO;

/**
 * Intrusion setup class. This class serves as an interface between the main 
 * program and the intrusion task. The class holds all tha variables to set up
 * the running task.
 * @author Federico
 */
public class Intrusion {
    
    RPI_IO rpio=null;
    int input=0;    //first RPI board input port
    int count=0;    //How many inputs
    int outputRly=0; //first RPI board output port
    int timer=0; // timer to turn off lights in minutes
    Gmail email = new Gmail("svmi.radar@gmail.com","svmi1234");
    boolean email_flag=false;
    
    /**
     * Class constructor
     * @param rpio 
     */
    public Intrusion(RPI_IO rpio){
        this.rpio=rpio;
        System.out.println("Setting up Intrusion task");
    }

    public boolean isEmail_flag() {
        return email_flag;
    }

    public void setEmail_flag(boolean email_flag) {
        this.email_flag = email_flag;
    }
    
    
    /**
     * Initialize first input port
     *
     * @param input from 1..8
     */
    public void setInputPort(int input) {
        this.input = input;
    }
    
    /**
     * Initialize how many input to monitor
     * @param count 
     */
    public void setInputNumber(int count){
        this.count=count;
    }
    
    /**
     * Initialize RPI board first relay output
     * @param relay 1..8
     */
    public void setOutputRly(int relay){
        this.outputRly=relay;
    }
    
    /**
     * Initialize timer in minutes
     * @param timer minutes
     */
    public void setTimer(int timer){
        this.timer=timer;
    }
    
    /**
     * Starts intrusion task
     */
    public void start(){
        Thread intrusionTask=new Thread(new IntrusionTask(this));
     //   intrusionTask.setPriority(Thread.NORM_PRIORITY+1);
        intrusionTask.start();
        
    }
    
}
