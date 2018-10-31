/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modules;

import Common.PulseOutput;
import Comunicatioms.EmailMessage;
import Comunicatioms.Gmail;
import Comunicatioms.RD3mail;
import Comunicatioms.WhatsappSender;
import RPI_IO_Lib.RPI_IO;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;


/**
 * Intrusion setup class. This class serves as an interface between the main 
 * program and the intrusion task. The class holds all tha variables to set up
 * the running task.
 * @author Federico
 */
public class IntrusionAlarm {
    
    RPI_IO rpio=null;
    int input=0;    //first RPI board input port
    int count=0;    //How many inputs
    int[] inputList=null;   //List of inputs
    int outputRly=0; //first RPI board output port
    int[] outputList=new int[2]; // List of outputs
    int timer=0; // timer to turn off lights in minutes
    RD3mail rd3email = new RD3mail("svmi.radar@adr3group.com","$radar.2018*");
    boolean email_flag=false;
    
    WhatsappSender whatsup = new WhatsappSender();
    
    static boolean flag=false; //intrusion flag
    static int state=0;
    EmailMessage email=new EmailMessage();
    
    /**
     * Class constructor
     * @param rpio 
     */
    public IntrusionAlarm(RPI_IO rpio){
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
        inputList=new int[count];
        this.setInputList(inputList);
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
        this.setOutputList(outputList);
    }
    
    /**
     * Initialize timer in minutes
     * @param timer minutes
     */
    public void setTimer(int timer){
        this.timer=timer;
    }
    
    public String getReport(){
        String report;
        
        if(!rpio.getInput(inputList[0])){
            report="Radar Room Door: OPEN\n";
        }
        else{
            report="Radar Room Door: CLOSE\n";
        }
        
        if(!rpio.getInput(inputList[1])){
            report=report+"Generator Room Door: OPEN\n";
        }
        else{
            report=report+"Generator Room Door: CLOSE\n";
        }
        
        if(email_flag){
            report=report+"Alarm ENABLE\n";
        } else {
            report=report+"Alarm DISABLE\n";
        }
        
        return report;
    }
    
    public String resetAlarm(){
        String resp="";
        int mainDoor=inputList[0];
        int generatorDoor=inputList[1];
        int intrusionAlarm=outputList[0];
        
        if(rpio.getInput(mainDoor) && rpio.getInput(generatorDoor) && state!=0 ){
            rpio.resetRly(intrusionAlarm);
            state=0;
            try {
                if(email_flag == true){
                whatsup.sendGroupMessage("+584241184923", "Radar SVMI","Intrusion Alarm RESET");
                }
                state=0;
                resp="Intrusion Alarm RESET";
            } catch (Exception ex) {
                Logger.getLogger(IntrusionAlarm.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (!rpio.getInput(mainDoor)){
            resp="Cannot RESET Intrusion Alarm\n";
            resp=resp+"Radar room Door is OPEN\n";
            if(!rpio.getInput(generatorDoor)){
                resp=resp+"Generator room Door is OPEN";
            }
        } else if(!rpio.getInput(generatorDoor)){
            resp="Cannot RESET Intrusion Alarm\n";
            resp=resp+"Generator room Door is OPEN\n";
        }
                
        return resp;
    }
    /**
     * Starts intrusion task
     */
    public void start(){
        Thread intrusionTask=new Thread(new IntrusionTask(),"Intrusion Task");
     //   intrusionTask.setPriority(Thread.NORM_PRIORITY+1);
        intrusionTask.start();
        
    }
    
    public class IntrusionTask implements Runnable{

        @Override
        public void run() {
        System.out.println("Intrusion task started. OK");

        try {
            email.setFrom(new InternetAddress("svmi.radar@adr3group.com"));
            email.setTo(InternetAddress.parse("federico.rivero.m@gmail.com"));
            email.setSubject("SVMI. Radar Station Intrusion");
        } catch (AddressException ex) {
            Logger.getLogger(modules.IntrusionAlarm.class.getName()).log(Level.SEVERE, null, ex);
        }
        
     /*   vars.email.setFrom("svmi.radar@gmail.com");
        vars.email.setTo("federico.rivero.m@gmail.com");
        vars.email.setSubject("SVMI. Radar Station Intrusion");*/
        while (true) {
            checkMainDoor();
            checkIntrusion();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(modules.IntrusionAlarm.class.getName()).log(Level.SEVERE, null, ex);
            }
        }    
        }
    
    }
    
    /**
     * Check if main door is open to turn on lights
     */
    private void checkMainDoor(){
        
        if(!rpio.getInput(inputList[0])){
            rpio.setRly(outputList[1]);
            flag=true;
            
        }else if (flag == true) {
            Thread setTimer = new Thread(new PulseOutput(rpio, outputList[1], timer));
            setTimer.start();
            flag=false;
        }
        
    }
    
    /**
     * Checks if any door is open to generate alarm
     */
    private void checkIntrusion(){
       
        boolean result=false;
        int mainDoor=inputList[0];
        int generatorDoor=inputList[1];
        int intrusionAlarm=outputList[0];
        Calendar date=Calendar.getInstance();
        
        switch(state){
            
            case 0:
                if (!rpio.getInput(mainDoor)) {
                    rpio.setRly(intrusionAlarm);
                    if (email_flag == true) {
                        // vars.email.setMessage("Radar Room door opened.");
                       // rd3email.sendEmail(email,email.getActualDate()+"\nRadar Room door opened.");
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI","Radar Room Door OPEN");
                        } catch (Exception ex) {
                            Logger.getLogger(IntrusionAlarm.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    System.out.println("Radar Room door opened at " + date.getTime());
                    state = 1;
                } else if (!rpio.getInput(generatorDoor)) {
                    rpio.setRly(intrusionAlarm);
                    if (email_flag == true) {
                     //   vars.email.setMessage("Generator Room door opened.");
                       // rd3email.sendEmail(email,email.getActualDate()+"\nGenerator Room door opened.");
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI","Generator Room Door OPEN");
                        } catch (Exception ex) {
                            Logger.getLogger(IntrusionAlarm.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    System.out.println("Generator Room door opened at " + date.getTime());
                    state = 3;
                }
               break;
               
            case 2:
                break;
                
            case 1:
                if(!rpio.getInput(generatorDoor)){
                   rpio.setRly(intrusionAlarm);
                   if (email_flag == true) {
                     //  vars.email.setMessage("Generator Room door opened.");
                     //  rd3email.sendEmail(email,email.getActualDate()+"\nGenerator Room door opened.");
                       try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI","Generator Room Door OPEN");
                        } catch (Exception ex) {
                            Logger.getLogger(IntrusionAlarm.class.getName()).log(Level.SEVERE, null, ex);
                        }
                   }
                   System.out.println("Generator Room door opened at "+date.getTime());
                   state=2;
               }
               break;
               
            case 3:
                if(!rpio.getInput(mainDoor)){
                   rpio.setRly(intrusionAlarm);
                   if (email_flag == true) {
                     //  vars.email.setMessage("Generator Room door opened.");
                     //  rd3email.sendEmail(email,email.getActualDate()+"\nGenerator Room door opened.");
                       try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI","Radar Room Door OPEN");
                        } catch (Exception ex) {
                            Logger.getLogger(IntrusionAlarm.class.getName()).log(Level.SEVERE, null, ex);
                        }
                   }
                   System.out.println("Radar Room door opened at "+date.getTime());
                   state=2;
               }
            default:
        }
        
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
    }
    
}
