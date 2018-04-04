/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package monitor3.common;

import Common.DataArray;
import RPI_IO_Lib.RPI_IO;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import modules.Intrusion;
import Common.SunsetCalculator;
import modules.AirCondition;
import modules.AirConditionScheduler;
import modules.Energy;
import modules.ExteriorLights;

/**
 *
 * @author Federico
 */
public class Monitor3 {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        RPI_IO rpio = new RPI_IO();
        RPI_IO_DATA data = new RPI_IO_DATA();
        Intrusion intrusion=new Intrusion(rpio);
        ExteriorLights lights=new ExteriorLights(rpio);
        AirCondition aircondition=new AirCondition(rpio);
        Energy energy=new Energy(rpio);
    
        //Intrusion module sett up
        intrusion.setInputNumber(2); //Input doors to be monitor
        intrusion.setInputPort(1);  //RPI Board Input port
        intrusion.setOutputRly(2);  //RPI first output port
        intrusion.setTimer(5);  //Timer for internal lights
        intrusion.start();  //start task
        intrusion.setEmail_flag(true);
        
        //Exterior Lights module set up
        lights.setLatitud(10.599594);
        lights.setLongitud(-66.997908);
        lights.setCount(1);
        lights.setInput(3);
        lights.setOutputRly(4);
        lights.start();
        lights.setEmailFlag(true);
        
        //Air Conditioning task
        aircondition.setInput(4); //Input port
        aircondition.setInputCount(2); //Number of input
        aircondition.setOutputRly(6); //firstOutput relay
        aircondition.setOutputCount(3);
        aircondition.setRC_const(0.61);
        aircondition.setAlarm(26.0);
        aircondition.setSchedule(AirConditionScheduler.HOUR, 12);
        aircondition.start();
        
        //Energy Task
        energy.setInput(6); //First Input port
        energy.setInputCount(3);
        energy.start();
        

    }
    
    

}
