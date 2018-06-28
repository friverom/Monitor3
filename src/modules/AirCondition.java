/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modules;

import Common.DataArray;
import Comunicatioms.EmailMessage;
import Comunicatioms.Gmail;
import Comunicatioms.RD3mail;
import Comunicatioms.WhatsappSender;
import RPI_IO_Lib.RPI_IO;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;


/**
 *
 * @author Federico
 */
public class AirCondition {
    
    RPI_IO rpio = null;
    DataArray temperature = new DataArray(60);
    DataArray avg_temp = new DataArray(1440);
    AirConditionScheduler schedule = new AirConditionScheduler();
   
    Calendar nextDate;
    boolean auto;
    double alarm = 0; //Alarm Temp
    double alfa = 0; // dT/(dT+RC) for filter
    boolean alarm_flag = false;
    
    int input = 0;    //first RPI board input port. Auto/Manual
    int inputCount = 0;    //How many inputs
    int[] inputList=null;   //List of inputs
    int outputRly = 0; //first RPI board output port. Toggle and alarm
    int[] outputList=new int[3]; // List of outputs
    int outputCount = 0;
    
    static int timer=0; //internal count for 1 minute for average
    static int state=0; //Air Condition state
    static int runState=0; //Auto/Man state
    static boolean schedule_flag=false;
    
    RD3mail rd3email = new RD3mail("svmi.radar@adr3group.com", "$radar.2018*");
    boolean email_flag = false;
    EmailMessage email=new EmailMessage();
    
    WhatsappSender whatsup=new WhatsappSender();
   
    public AirCondition(RPI_IO rpio){
        this.rpio=rpio;
        nextDate=schedule.calcScheduleTime();
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
        inputList=new int[inputCount];
        this.setInputList(inputList);
        
    }

    public void setInputCount(int inputCount) {
        this.inputCount = inputCount;
    }

    public void setOutputRly(int outputRly) {
        this.outputRly = outputRly;
        this.setOutputList(outputList);
    }

    public void setOutputCount(int outputCount) {
        this.outputCount = outputCount;
    }
    
    public void start(){
        
        Thread task=new Thread(new AirConditionTask(),"Air Condition Task");
        task.start();
}
    public String getTemperature(){
        String temp=String.format("%.2f", temperature.average(60));
        return temp;
    }
    public String getReport(){
        String report;
        String temp = String.format("%.2f", temperature.average(60));
        SimpleDateFormat format1 = new SimpleDateFormat("dd/MM/yyyy 'at' HH:mm:ss zzz");

         if(auto){
                    report="AC System mode: AUTOMATIC\n";
                }
                else{
                    report="AC System mode: MANUAL\n";
                }
         
        switch(state){
            case 0:
                report=report+"AC #1: RUNNING\n";
                report=report+"Actual room temperature: "+temp+"\n";
                report=report+"AC switchover on: "+format1.format(nextDate.getTime());
                break;
                
            case 1:
                report=report+"AC #2: RUNNING\n";
                report=report+"Actual room temperature: "+temp+"\n";
                report=report+"AC switchover on: "+format1.format(nextDate.getTime());
                break;
                
            case 2:
                report=report+"AC #1: FAIL\n";
                report=report+"AC #2: RUNNING\n";
                report=report+"Actual room temperature: "+temp+"\n";
                break;
                
            case 3:
                report=report+"AC #2: FAIL\n";
                report=report+"AC #1: RUNNING\n";
                report=report+"Actual room temperature: "+temp+"\n";
                break;
        }
        
        return report;
        
    }
    
    public String resetAC(){
        
        String reset="";
        String message=email.getActualDate();
        String temp="";
        
        if(!alarm_flag){
            switch(state){
                
                case 2:
                    setAirCondition(1);
                    rpio.resetRly(outputList[2]);
                    state=0; // Switch to state 0 if reset and no alarm present
                    message=message+"\nAC system Reset. AC#1 Running.\nActual Room Temp is ";
                    temp=String.format("%.2f%n", temperature.average(60));
                    System.out.println(message+temp);
                    if(email_flag){
                        rd3email.sendEmail(email,message+temp);
                    try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "AC system RESET");
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "AC#1 RUNNING");
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "Actual Room Temp is "+String.format("%.2f", temperature.average(60)));
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    reset=message+temp;
                    break;
                    
                case 3:
                    setAirCondition(2);
                    rpio.resetRly(outputList[2]);
                    state = 1; // Switch to state 1 if reset and no alarm present
                    message=message+"\nAC system Reset. AC#2 Running.\nActual Room Temp is ";
                    temp=String.format("%.2f%n", temperature.average(60));
                    System.out.println(message+temp);
                    if(email_flag){
                        rd3email.sendEmail(email,message+temp);
                    try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "AC system RESET");
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "AC#2 RUNNING");
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "Actual Room Temp is "+String.format("%.2f", temperature.average(60)));
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    reset=message+temp;
                    break;
                    
                default:
                    reset="\nAC System running normally. NO need to reset\n";
            }
        }
        return reset;
    }
    
     /**
     * This method initializes the Input array list
     * @param inputs 
     */
    private void setInputList(int[] inputs){
        
        int rly=this.input;
        
        for(int i=0;i<inputList.length;i++){
            inputList[i]=rly++;
        }
    }
    
    /**
     * Initialize Output relay list
     * @param outputList 
     */
    private void setOutputList(int[] outputList){
        outputList[0]=outputRly;
        outputList[1]=outputRly+1;
        outputList[2]=outputRly+2;
    }
    
    public class AirConditionTask implements Runnable{

        @Override
        public void run() {
             try {
            email.setFrom(new InternetAddress("svmi.radar@adr3group.com"));
            email.setTo(InternetAddress.parse("federico.rivero.m@gmail.com"));
            email.setSubject("SVMI. Radar Station Air Condition");
        } catch (AddressException ex) {
            Logger.getLogger(modules.AirCondition.class.getName()).log(Level.SEVERE, null, ex);
        }
    /*    data.email.setFrom("svmi.radar@gmail.com");
        data.email.setTo("federico.rivero.m@gmail.com");
        data.email.setSubject("SVMI. Radar Station Air Condition");*/
        nextDate=schedule.calcScheduleTime();
        while (true) {
            processTemp();
            alarm_flag=checkTempAlarm();
            schedule_flag=checkDateChange();
            
            if(rpio.getInput(inputList[0])){
                if(state==0){
                    setAirCondition(1);
                }
                check_state(); //if Auto run schedule
                auto=true;
            } else {
                state=0;
                setAirCondition(0); //if Man turn off AC's
                auto=false;
            }
            try {
                Thread.sleep(1000); //Sample Temp every second
            } catch (InterruptedException ex) {
                Logger.getLogger(modules.AirCondition.class.getName()).log(Level.SEVERE, null, ex);
            }
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
                    String temp=String.format("%.2f%n", temperature.average(60));
                    System.out.println(message+temp);
                    if(email_flag){
                        rd3email.sendEmail(email,message+temp);
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "AC#2 running by schedule.");
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "Actual Room Temp is "+String.format("%.2f", temperature.average(60)));
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } else if (alarm_flag) {
                    setAirCondition(2);
                    rpio.setRly(outputList[2]);
                    state = 2; //AC #1 in alarm switch to state 2
                    String message=email.getActualDate();
                    message=message+"\nAC#1 ALARM. Switched to AC#2.\nActual Room Temp is ";
                    String temp=String.format("%.2f%n", temperature.average(60));
                    System.out.println(message+temp);
                    if(email_flag){
                        rd3email.sendEmail(email,message+temp);
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "AC#1 ALARM. Switched to AC#2");
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "Actual Room Temp is "+String.format("%.2f", temperature.average(60)));
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
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
                    String temp=String.format("%.2f%n", temperature.average(60));
                    System.out.println(message+temp);
                    if(email_flag){
                        rd3email.sendEmail(email,message+temp);
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "AC#1 running by schedule");
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "Actual Room Temp is "+String.format("%.2f", temperature.average(60)));
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }else if(alarm_flag){
                    setAirCondition(1);
                    rpio.setRly(outputList[2]);
                    state=3; // AC #2 in alarm, switch to state 3
                    String message=email.getActualDate();
                    message=message+"\nAlarm AC#2. Switched to AC#1.\nActual Room Temp is ";
                    String temp=String.format("%.2f%n", temperature.average(60));
                    System.out.println(message+temp);
                    if(email_flag){
                        rd3email.sendEmail(email,message+temp);
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI","AC#2 ALARM. Switched to AC#1" );
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "Actual Room Temp is "+String.format("%.2f", temperature.average(60)));
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                break;
            //AC #1 in alarm. AC #2 running. No reset signal    
            case 2:
                if(rpio.getInput(inputList[1]) && !alarm_flag){
                    setAirCondition(1);
                    rpio.resetRly(outputList[2]);
                    state=0; // Switch to state 0 if reset and no alarm present
                    String message=email.getActualDate();
                    message=message+"\nAC system Reset. AC#1 Running.\nActual Room Temp is ";
                    String temp=String.format("%.2f%n", temperature.average(60));
                    System.out.println(message+temp);
                    if(email_flag){
                        rd3email.sendEmail(email,message+temp);
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "AC System ALARM RESET. AC#1 Running");
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "Actual Room Temp is "+String.format("%.2f", temperature.average(60)));
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                break;
                
            case 3:
                if (rpio.getInput(inputList[1]) && !alarm_flag) {
                    setAirCondition(2);
                    rpio.resetRly(outputList[2]);
                    state = 1; // Switch to state 1 if reset and no alarm present
                    String message=email.getActualDate();
                    message=message+"\nAC system Reset. AC#2 Running.\nActual Room Temp is ";
                    String temp=String.format("%.2f%n", temperature.average(60));
                    System.out.println(message+temp);
                    if(email_flag){
                        rd3email.sendEmail(email,message+temp);
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "AC System ALARM RESET. AC#2 Running");
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "Actual Room Temp is "+String.format("%.2f", temperature.average(60)));
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                break;
            default:
        }
    }
    
    private void setAirCondition(int i){
        
        switch(i){
            case 0:
                rpio.resetRly(outputList[0]);
                rpio.resetRly(outputList[1]);
                break;
            case 1:
                rpio.setRly(outputList[0]);
                rpio.resetRly(outputList[1]);
                break;
            case 2:
                rpio.resetRly(outputList[0]);
                rpio.setRly(outputList[1]);
                break;
            default:
                
        }
    }
    private boolean checkDateChange(){
        
        Calendar date=Calendar.getInstance();
        if(date.getTimeInMillis()>nextDate.getTimeInMillis()){
            nextDate=schedule.calcScheduleTime();
            
         /*   System.out.println("Actual Time "+date.getTime()+" Mills "+date.getTimeInMillis());
            System.out.println("Next Change "+nextDate.getTime()+" Mills "+nextDate.getTimeInMillis());*/
           // System.out.format("Temp %.2f%n",data.temp.average(60));
            return true;
        } else {
            return false;
        }
        
    }
    private void processTemp(){
    
            temperature.add(filter(getTemp()));
            timer = timer + 1;
            if (timer > 60) {
                avg_temp.add(temperature.average(60));
                timer = 0;
            }
    }
    private boolean checkTempAlarm(){
        
        double roomTemp=temperature.average(60);
        
        if(roomTemp>alarm){
         //   data.rpio.setRly(outputList[2]);
            if (!alarm_flag) {
                String message=email.getActualDate();
                message = message+"\nTEMP ALARM. \nActual Room Temp is ";
                String temp = String.format("%.2f%n", temperature.average(60));
                System.out.println(message+temp);
                if(email_flag){
                    rd3email.sendEmail(email,message+temp);
                    try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "Radar Room Temperature ALARM");
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "Actual Room Temp is "+String.format("%.2f", temperature.average(60)));
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                }
            }
            return true;
        }
        else if(roomTemp<25.0){
        //    data.rpio.resetRly(outputList[2]);
            if (alarm_flag) {
                String message=email.getActualDate();
                message = message+"\nTEMP NORMAL. \nActual Room Temp is ";
                String temp = String.format("%.2f%n",temperature.average(60));
                System.out.println(message+temp);
                if(email_flag){
                    rd3email.sendEmail(email,message+temp);
                    try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "Radar Room Temperature NORMAL");
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "Actual Room Temp is "+String.format("%.2f", temperature.average(60)));
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                }
            }
            return false;
        } else {
            return alarm_flag;
        }
    }
    
   private double getTemp(){
        
        int value=rpio.getChannel(1);
        
        //double analog=(double)value/4096*5;
        //double temp=18.752*analog-36.616;
        double analog=(double)value/4096*4.096;
        double temp=22.965*analog-41.427;
       // System.out.format("Temp %.2f%n", temp);
        return temp;
    }
    
    private double filter(double t){
        
        double temp=t*alfa+temperature.getData(0)*(1-alfa);
        return temp;
    }
}
