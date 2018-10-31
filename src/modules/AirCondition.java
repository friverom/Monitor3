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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;
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
    long start_date;
    boolean auto;
    double alarm = 0; //Alarm Temp
    double alfa = 0; // dT/(dT+RC) for filter
    boolean alarm_flag = false;
    long ac1_timer=0;
    long ac2_timer=0;
    long ac_last=0;
       
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
   
    public AirCondition(RPI_IO rpio) throws FileNotFoundException{
        
        long[] log=new long[2];
        this.rpio=rpio;
        nextDate=schedule.calcScheduleTime();
        log=readAClog();
        ac1_timer=log[0];
        ac2_timer=log[1];
        start_date=log[2];
        ac_last=start_date;
       
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
        SimpleDateFormat format1 = new SimpleDateFormat("dd/MM/yyyy 'at' HH:mm zzz");

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
                report=report+"AC next switchover: "+format1.format(nextDate.getTime());
                break;
                
            case 1:
                report=report+"AC #2: RUNNING\n";
                report=report+"Actual room temperature: "+temp+"\n";
                report=report+"AC next switchover: "+format1.format(nextDate.getTime());
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
    
    public String runReport(){
        String resp="";
        SimpleDateFormat ft = new SimpleDateFormat("dd/MM/yyyy 'at' HH:mm");
        
            resp="Running hours since "+ft.format(start_date)+"\n";
            resp=resp+"AC #1: "+String.format("%.1f", ac1_timer/(1000.0*3600.0))+" hrs\n";
            resp=resp+"AC #2: "+String.format("%.1f", ac2_timer/(1000.0*3600.0))+" hrs\n";
            
        return resp;
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
                    //    rd3email.sendEmail(email,message+temp);
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
                    //    rd3email.sendEmail(email,message+temp);
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
    
    public String getLog1() throws FileNotFoundException, UnsupportedEncodingException{
        String attach ="/home/pi/NetBeansProjects/Monitor3/tempLog1.txt";
        createTempLog(attach,1);
        return attach;
    }
    
    public String getLog5() throws FileNotFoundException, UnsupportedEncodingException{
        String attach ="/home/pi/NetBeansProjects/Monitor3/tempLog5.txt";
        createTempLog(attach,5);
        return attach;
    }
    public String getLog60() throws FileNotFoundException, UnsupportedEncodingException{
        String attach ="/home/pi/NetBeansProjects/Monitor3/tempLog60.txt";
        createTempLog(attach,60);
        return attach;
    }
    
    public String alarmAck(){
        rpio.resetRly(outputList[2]);
        try {
            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "AC Alarm Acknowledge");
        } catch (Exception ex) {
            Logger.getLogger(AirCondition.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "AC alarm Acknowledged";
    }
     
    private void createTempLog(String filename, int sampletime) throws FileNotFoundException, UnsupportedEncodingException{
        
        PrintWriter writer = new PrintWriter(filename, "UTF-8");
        writer.format("%.2f,", avg_temp.getData(0));
        for(int i=sampletime; i<1440; i+=sampletime){
            writer.format("%.2f,", avg_temp.getData(i));
        }
        writer.close();
    }
    
    private void createAClog() throws FileNotFoundException, UnsupportedEncodingException{
        PrintWriter writer = new PrintWriter("ac_log.txt", "UTF-8");
        writer.println("Running hours,"+ac1_timer+","+ac2_timer+","+System.currentTimeMillis()+"\n");
        writer.close();
    }
    
    private long[] readAClog(){
        Scanner scanner;
        long[] log = new long[3];
        log[0]=0;
        log[1]=0;
        log[2]=0;
        
        try {
            scanner = new Scanner(new File("ac_log.txt"));
            String text = scanner.useDelimiter("\n").next();
            scanner.close(); // Put this call in a finally block

            String[] parts = text.split(",");
            
            log[0] = Long.parseLong(parts[1]);
            log[1] = Long.parseLong(parts[2]);
            log[2] = Long.parseLong(parts[3]);
            
        } catch (FileNotFoundException ex) {
            PrintWriter writer;
            try {
                writer = new PrintWriter("ac_log.txt", "UTF-8");
                writer.println("Running hours," + 0 + "," + 0 + ","+System.currentTimeMillis()+"\n");
                writer.close();
            } catch (FileNotFoundException ex1) {
                Logger.getLogger(AirCondition.class.getName()).log(Level.SEVERE, null, ex1);
            } catch (UnsupportedEncodingException ex1) {
                Logger.getLogger(AirCondition.class.getName()).log(Level.SEVERE, null, ex1);
            }
           
        }
       return log; 
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
                try {
                    check_state(); //if Auto run schedule
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(AirCondition.class.getName()).log(Level.SEVERE, null, ex);
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(AirCondition.class.getName()).log(Level.SEVERE, null, ex);
                }
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
    
    private void check_state() throws FileNotFoundException, UnsupportedEncodingException{
    
        switch(state){
        
            //State 0. System in auto an AC#1 running. No alarm
            case 0:
                if (schedule_flag) {
                    setAirCondition(2);
                    createAClog();
                    schedule_flag = false;
                    state = 1; //Switch to state 1 if schedule signal
                    String message=email.getActualDate();
                    message=message+"\nAC#2 running by schedule.\nActual Room Temp is ";
                    String temp=String.format("%.2f%n", temperature.average(60));
                    System.out.println(message+temp);
                    if(email_flag){
                     //   rd3email.sendEmail(email,message+temp);
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
                    createAClog();
                    state = 2; //AC #1 in alarm switch to state 2
                    String message=email.getActualDate();
                    message=message+"\nAC#1 ALARM. Switched to AC#2.\nActual Room Temp is ";
                    String temp=String.format("%.2f%n", temperature.average(60));
                    System.out.println(message+temp);
                    if(email_flag){
                    //    rd3email.sendEmail(email,message+temp);
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "AC#1 ALARM. Switched to AC#2");
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "Actual Room Temp is "+String.format("%.2f", temperature.average(60)));
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                ac1_timer=ac1_timer+System.currentTimeMillis()-ac_last;
                ac_last=System.currentTimeMillis();
                break;
            //State 1. System in Auto an AC#2 running. No alarm    
            case 1:
                if(schedule_flag){
                    setAirCondition(1);
                    schedule_flag=false;
                    createAClog();
                    state=0; //Switch to state 0 if schedula signal
                    String message=email.getActualDate();
                    message=message+"\nAC#1 running by schedule.\nActual Room Temp is ";
                    String temp=String.format("%.2f%n", temperature.average(60));
                    System.out.println(message+temp);
                    if(email_flag){
                     //   rd3email.sendEmail(email,message+temp);
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
                    createAClog();
                    state=3; // AC #2 in alarm, switch to state 3
                    String message=email.getActualDate();
                    message=message+"\nAlarm AC#2. Switched to AC#1.\nActual Room Temp is ";
                    String temp=String.format("%.2f%n", temperature.average(60));
                    System.out.println(message+temp);
                    if(email_flag){
                    //    rd3email.sendEmail(email,message+temp);
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI","AC#2 ALARM. Switched to AC#1" );
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "Actual Room Temp is "+String.format("%.2f", temperature.average(60)));
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                ac2_timer=ac2_timer+System.currentTimeMillis()-ac_last;
                ac_last=System.currentTimeMillis();
                break;
            //AC #1 in alarm. AC #2 running. No reset signal    
            case 2:
                if(rpio.getInput(inputList[1]) && !alarm_flag){
                    setAirCondition(1);
                    createAClog();
                    rpio.resetRly(outputList[2]);
                    state=0; // Switch to state 0 if reset and no alarm present
                    String message=email.getActualDate();
                    message=message+"\nAC system Reset. AC#1 Running.\nActual Room Temp is ";
                    String temp=String.format("%.2f%n", temperature.average(60));
                    System.out.println(message+temp);
                    if(email_flag){
                    //    rd3email.sendEmail(email,message+temp);
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "AC System ALARM RESET. AC#1 Running");
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "Actual Room Temp is "+String.format("%.2f", temperature.average(60)));
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                ac2_timer=ac2_timer+System.currentTimeMillis()-ac_last;
                ac_last=System.currentTimeMillis();
               
                break;
                
            case 3:
                if (rpio.getInput(inputList[1]) && !alarm_flag) {
                    setAirCondition(2);
                    createAClog();
                    rpio.resetRly(outputList[2]);
                    state = 1; // Switch to state 1 if reset and no alarm present
                    String message=email.getActualDate();
                    message=message+"\nAC system Reset. AC#2 Running.\nActual Room Temp is ";
                    String temp=String.format("%.2f%n", temperature.average(60));
                    System.out.println(message+temp);
                    if(email_flag){
                     //   rd3email.sendEmail(email,message+temp);
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "AC System ALARM RESET. AC#2 Running");
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "Actual Room Temp is "+String.format("%.2f", temperature.average(60)));
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                ac1_timer=ac1_timer+System.currentTimeMillis()-ac_last;
                ac_last=System.currentTimeMillis();
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
                //    rd3email.sendEmail(email,message+temp);
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
                 //   rd3email.sendEmail(email,message+temp);
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
