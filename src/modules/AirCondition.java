/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modules;

import Common.DataArray;
import Comunicatioms.Gmail;
import RPI_IO_Lib.RPI_IO;
import java.util.Calendar;

/**
 *
 * @author Federico
 */
public class AirCondition {
    
    RPI_IO rpio = null;
    DataArray temp = new DataArray(60);
    DataArray avg_temp = new DataArray(1440);
    AirConditionScheduler schedule = new AirConditionScheduler();
    Gmail email = new Gmail("svmi.radar@gmail.com", "svmi1234");
    boolean email_flag = false;

    Calendar nextDate;
    int state=0;
    boolean auto;
    double alarm = 0; //Alarm Temp
    double alfa = 0; // dT/(dT+RC) for filter
    boolean alarm_flag = false;
    
    int input = 0;    //first RPI board input port. Auto/Manual
    int inputCount = 0;    //How many inputs
    int outputRly = 0; //first RPI board output port. Toggle and alarm
    int outputCount = 0;
   
    public AirCondition(RPI_IO rpio){
        this.rpio=rpio;
    }

    public void setSchedule(int timer, int times){
        schedule.setSchedule(timer, times);
        schedule.calcScheduleTime();
    }
    
    public void setEmailFlag(boolean flag){
        this.email_flag=flag;
    }
    public void setAlarm(double alarm) {
        this.alarm = alarm;
    }
    
    public void setRC_const(double rc){
        this.alfa=rc;
    }

    public void setInput(int input) {
        this.input = input;
    }

    public void setInputCount(int inputCount) {
        this.inputCount = inputCount;
    }

    public void setOutputRly(int outputRly) {
        this.outputRly = outputRly;
    }

    public void setOutputCount(int outputCount) {
        this.outputCount = outputCount;
    }
    
    public void start(){
        
        Thread task=new Thread(new AirConditionTask(this));
        task.start();
}
}
