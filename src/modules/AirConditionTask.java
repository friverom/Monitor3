/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modules;

import Comunicatioms.EmailMessage;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 *
 * @author Federico
 */
public class AirConditionTask implements Runnable{
    
    AirCondition data=null;
    int[] inputList=null;   //List of inputs
    int[] outputList=new int[3]; // List of outputs
    static int timer=0; //internal count for 1 minute
    static int state=0; //Air Condition state
    static int runState=0; //Auto/Man state
    static Calendar nextDate;
    static boolean alarm_flag=false;
    static boolean schedule_flag=false;
    EmailMessage email=new EmailMessage();
    
    public AirConditionTask(AirCondition data){
        this.data=data;
        this.data.temp.add(getTemp());
        this.inputList=new int[this.data.inputCount];
        this.setInputList(inputList);
        this.setOutputList(outputList);
        nextDate=this.data.schedule.calcScheduleTime();
        System.out.println("Air Conditioning variables set. OK");
    }

    @Override
    public void run() {
        
        try {
            email.setFrom(new InternetAddress("svmi.radar@gmail.com"));
            email.setTo(InternetAddress.parse("federico.rivero.m@gmail.com"));
            email.setSubject("SVMI. Radar Station Air Condition");
        } catch (AddressException ex) {
            Logger.getLogger(IntrusionTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    /*    data.email.setFrom("svmi.radar@gmail.com");
        data.email.setTo("federico.rivero.m@gmail.com");
        data.email.setSubject("SVMI. Radar Station Air Condition");*/
        while (true) {
            processTemp();
            alarm_flag=checkTempAlarm();
            data.alarm_flag=this.alarm_flag;
            schedule_flag=checkDateChange();
            
            if(data.rpio.getInput(inputList[0])){
                if(state==0){
                    setAirCondition(1);
                }
                check_state(); //if Auto run schedule
                data.state=this.state;
                data.auto=true;
            } else {
                state=0;
                setAirCondition(0); //if Man turn off AC's
                data.auto=false;
            }
            try {
                Thread.sleep(1000); //Sample Temp every second
            } catch (InterruptedException ex) {
                Logger.getLogger(AirConditionTask.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void check_state(){
    
        switch(state){
        
            //State 0. System in auto an AC#1 running. No alarm
            case 0:
                if (schedule_flag) {
                    setAirCondition(2);
                    schedule_flag = false;
                    state = 1; //Switch to state 1 if schedule signal
                    String message=email.getActualDate();
                    message=message+"\nAC#2 running by schedule.\nActual Room Temp is ";
                    String temp=String.format("%.2f%n", data.avg_temp.getData(0));
                    System.out.println(message+temp);
                    data.email.sendEmail(email,message+temp);
                } else if (alarm_flag) {
                    setAirCondition(2);
                    data.rpio.setRly(outputList[2]);
                    state = 2; //AC #1 in alarm switch to state 2
                    String message=email.getActualDate();
                    message=message+"\nAlarm AC#1. Switched to AC#2.\nActual Room Temp is ";
                    String temp=String.format("%.2f%n", data.avg_temp.getData(0));
                    System.out.println(message+temp);
                    data.email.sendEmail(email,message+temp);
                }
                break;
            //State 1. System in Auto an AC#2 running. No alarm    
            case 1:
                if(schedule_flag){
                    setAirCondition(1);
                    schedule_flag=false;
                    state=0; //Switch to state 0 if schedula signal
                    String message=email.getActualDate();
                    message=message+"\nAC#1 running by schedule.\nActual Room Temp is ";
                    String temp=String.format("%.2f%n", data.avg_temp.getData(0));
                    System.out.println(message+temp);
                    data.email.sendEmail(email,message+temp);
                }else if(alarm_flag){
                    setAirCondition(1);
                    data.rpio.setRly(outputList[2]);
                    state=3; // AC #2 in alarm, switch to state 3
                    String message=email.getActualDate();
                    message=message+"\nAlarm AC#2. Switched to AC#1.\nActual Room Temp is ";
                    String temp=String.format("%.2f%n", data.avg_temp.getData(0));
                    System.out.println(message+temp);
                    data.email.sendEmail(email,message+temp);
                }
                break;
            //AC #1 in alarm. AC #2 running. No reset signal    
            case 2:
                if(data.rpio.getInput(inputList[1]) && !alarm_flag){
                    setAirCondition(1);
                    data.rpio.resetRly(outputList[2]);
                    state=0; // Switch to state 0 if reset and no alarm present
                    String message=email.getActualDate();
                    message=message+"\nAC system Reset. AC#1 Running.\nActual Room Temp is ";
                    String temp=String.format("%.2f%n", data.avg_temp.getData(0));
                    System.out.println(message+temp);
                    data.email.sendEmail(email,message+temp);
                }
                break;
                
            case 3:
                if (data.rpio.getInput(inputList[1]) && !alarm_flag) {
                    setAirCondition(2);
                    data.rpio.resetRly(outputList[2]);
                    state = 1; // Switch to state 1 if reset and no alarm present
                    String message=email.getActualDate();
                    message=message+"\nAC system Reset. AC#2 Running.\nActual Room Temp is ";
                    String temp=String.format("%.2f%n", data.avg_temp.getData(0));
                    System.out.println(message+temp);
                    data.email.sendEmail(email,message+temp);
                }
                break;
            default:
        }
    }
    
    private void setAirCondition(int i){
        
        switch(i){
            case 0:
                data.rpio.resetRly(outputList[0]);
                data.rpio.resetRly(outputList[1]);
                break;
            case 1:
                data.rpio.setRly(outputList[0]);
                data.rpio.resetRly(outputList[1]);
                break;
            case 2:
                data.rpio.resetRly(outputList[0]);
                data.rpio.setRly(outputList[1]);
                break;
            default:
                
        }
    }
    private boolean checkDateChange(){
        
        Calendar date=Calendar.getInstance();
        if(date.getTimeInMillis()>nextDate.getTimeInMillis()){
            nextDate=data.schedule.calcScheduleTime();
            data.nextDate=this.nextDate;
         /*   System.out.println("Actual Time "+date.getTime()+" Mills "+date.getTimeInMillis());
            System.out.println("Next Change "+nextDate.getTime()+" Mills "+nextDate.getTimeInMillis());*/
           // System.out.format("Temp %.2f%n",data.temp.average(60));
            return true;
        } else {
            return false;
        }
        
    }
    private void processTemp(){
    
            data.temp.add(filter(getTemp()));
            timer = timer + 1;
            if (timer > 60) {
                data.avg_temp.add(data.temp.average(60));
                timer = 0;
            }
    }
    private boolean checkTempAlarm(){
        
        double roomTemp=data.temp.average(60);
        
        if(roomTemp>data.alarm){
         //   data.rpio.setRly(outputList[2]);
            if (!alarm_flag) {
                String message=email.getActualDate();
                message = message+"\nTEMP ALARM. \nActual Room Temp is ";
                String temp = String.format("%.2f%n", data.temp.average(60));
                System.out.println(message+temp);
                data.email.sendEmail(email,message+temp);
            }
            return true;
        }
        else if(roomTemp<25.0){
        //    data.rpio.resetRly(outputList[2]);
            if (alarm_flag) {
                String message=email.getActualDate();
                message = message+"\nTEMP NORMAL. \nActual Room Temp is ";
                String temp = String.format("%.2f%n", data.temp.average(60));
                System.out.println(message+temp);
                data.email.sendEmail(email,message+temp);
            }
            return false;
        } else {
            return alarm_flag;
        }
    }
    
   private double getTemp(){
        
        int value=data.rpio.getChannel(1);
        
        //double analog=(double)value/4096*5;
        //double temp=18.752*analog-36.616;
        double analog=(double)value/4096*25.0;
        double temp=4.6888*analog-54.877;
       // System.out.format("Temp %.2f%n", temp);
        return temp;
    }
    
    private double filter(double t){
        
        double temp=t*data.alfa+data.temp.getData(0)*(1-data.alfa);
        return temp;
    }
    
    /**
     * This method initializes the Input array list
     * @param inputs 
     */
    private void setInputList(int[] inputs){
        
        int rly=this.data.input;
        
        for(int i=0;i<inputList.length;i++){
            inputList[i]=rly++;
        }
    }
    
    /**
     * Initialize Output relay list
     * @param outputList 
     */
    private void setOutputList(int[] outputList){
        outputList[0]=data.outputRly;
        outputList[1]=data.outputRly+1;
        outputList[2]=data.outputRly+2;
    }
    
}
