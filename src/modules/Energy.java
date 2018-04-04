/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modules;

import Comunicatioms.Gmail;
import RPI_IO_Lib.RPI_IO;

/**
 *
 * @author Federico
 */
public class Energy {
    
    RPI_IO rpio=null;
    int input = 0;    //first RPI board input port. Auto/Manual
    int inputCount = 0;    //How many inputs
    
    Gmail email = new Gmail("svmi.radar@gmail.com", "svmi1234");
    boolean email_flag = false;
    
    int mainState=0;
    int genState=0;
    int surgeState=0;
    
    public Energy(RPI_IO rpio){
        this.rpio=rpio;
    }

    public void setInput(int input) {
        this.input = input;
    }

    public void setInputCount(int inputCount) {
        this.inputCount = inputCount;
    }

    public boolean isEmail_flag() {
        return email_flag;
    }

    public void setEmail_flag(boolean email_flag) {
        this.email_flag = email_flag;
    }

    public int getMainState() {
        return mainState;
    }

    public int getGenState() {
        return genState;
    }

    public int getSurgeState() {
        return surgeState;
    }
    
    public void start(){
        
        Thread task=new Thread(new EnergyTask(this));
        task.start();
}
}
