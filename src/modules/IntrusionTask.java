/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modules;

import Common.PulseOutput;
import Comunicatioms.EmailMessage;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * Class to monitor Intrusion inputs
 * @author Federico
 */
public class IntrusionTask implements Runnable{
    
    Intrusion vars=null; //Class variables
    int[] inputList=null;   //List of inputs
    int[] outputList=new int[2]; // List of outputs
    static boolean flag=false; //intrusion flag
    static int state=0;
    EmailMessage email=new EmailMessage();
    
    /**
     * Class constructor
     * @param vars 
     */
    public IntrusionTask(Intrusion vars){
        this.vars=vars;
        this.inputList=new int[this.vars.count];
        this.setInputList(inputList);
        this.setOutputList(outputList);
        System.out.println("Intrusion variables set. OK");
    }
    
    /**
     * Running task
     */
    @Override
    public void run() {
        System.out.println("Intrusion task started. OK");

        try {
            email.setFrom(new InternetAddress("svmi.radar@gmail.com"));
            email.setTo(InternetAddress.parse("federico.rivero.m@gmail.com"));
            email.setSubject("SVMI. Radar Station Intrusion");
        } catch (AddressException ex) {
            Logger.getLogger(IntrusionTask.class.getName()).log(Level.SEVERE, null, ex);
        }
        
     /*   vars.email.setFrom("svmi.radar@gmail.com");
        vars.email.setTo("federico.rivero.m@gmail.com");
        vars.email.setSubject("SVMI. Radar Station Intrusion");*/
        while (true) {
            this.checkMainDoor();
            this.checkIntrusion();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(IntrusionTask.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Check if main door is open to turn on lights
     */
    private void checkMainDoor(){
        
        if(!vars.rpio.getInput(inputList[0])){
            vars.rpio.setRly(outputList[1]);
            flag=true;
            
        }else if (flag == true) {
            Thread setTimer = new Thread(new PulseOutput(vars.rpio, outputList[1], vars.timer));
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
                if (!vars.rpio.getInput(mainDoor)) {
                    vars.rpio.setRly(intrusionAlarm);
                    if (vars.email_flag == true) {
                        // vars.email.setMessage("Radar Room door opened.");
                        vars.email.sendEmail(email,email.getActualDate()+"\nRadar Room door opened.");
                    }
                    System.out.println("Radar Room door opened at " + date.getTime());
                    state = 1;
                } else if (!vars.rpio.getInput(generatorDoor)) {
                    vars.rpio.setRly(intrusionAlarm);
                    if (vars.email_flag == true) {
                     //   vars.email.setMessage("Generator Room door opened.");
                        vars.email.sendEmail(email,email.getActualDate()+"\nGenerator Room door opened.");
                    }
                    System.out.println("Generator Room door opened at " + date.getTime());
                    state = 2;
                }
               break;
               
            case 1:
               if(vars.rpio.getInput(mainDoor)){
                   vars.rpio.resetRly(intrusionAlarm);
                   if (vars.email_flag == true) {
                    //   vars.email.setMessage("Radar Room door closed.");
                       vars.email.sendEmail(email,email.getActualDate()+"\nRadar Room door closed.");
                   }
                   System.out.println("Radar Room door closed at "+date.getTime());
                   state=0;
               } else if(!vars.rpio.getInput(generatorDoor)){
                   vars.rpio.setRly(intrusionAlarm);
                   if (vars.email_flag == true) {
                     //  vars.email.setMessage("Generator Room door opened.");
                       vars.email.sendEmail(email,email.getActualDate()+"\nGenerator Room door opened.");
                   }
                   System.out.println("Generator Room door opened at "+date.getTime());
                   state=3;
               }
               break;
               
            case 2:
                if(vars.rpio.getInput(generatorDoor)){
                  vars.rpio.resetRly(intrusionAlarm);
                    if (vars.email_flag == true) {
                      //  vars.email.setMessage("Generator Room door closed.");
                        vars.email.sendEmail(email,email.getActualDate()+"\nGenerator Room door closed.");
                    }
                   System.out.println("Generator Room door closed at "+date.getTime());
                   state=0;  
                } else if(!vars.rpio.getInput(mainDoor)){
                    vars.rpio.setRly(intrusionAlarm);
                    if (vars.email_flag == true) {
                      //  vars.email.setMessage("Radar Room door opened.");
                        vars.email.sendEmail(email,email.getActualDate()+"\nRadar Room door opened.");
                    }
                   System.out.println("Radar Room door opened at "+date.getTime());
                   state=3; 
                }
                break;
                
            case 3:
                if(vars.rpio.getInput(mainDoor)){
                    if (vars.email_flag == true) {
                     //   vars.email.setMessage("Radar Room door closed.");
                        vars.email.sendEmail(email,email.getActualDate()+"\nRadar Room door closed.");
                    }
                   System.out.println("Radar Room door closed at "+date.getTime());
                   state=2; 
                } else if(vars.rpio.getInput(generatorDoor)){
                    if (vars.email_flag == true) {
                     //   vars.email.setMessage("Generator Room Door closed.");
                        vars.email.sendEmail(email,email.getActualDate()+"\nGenerator Room Door closed.");
                    }
                    System.out.println("Generator Door closed at "+date.getTime());
                    state=1;
                }
            default:
        }
        
    }
    
    /**
     * This method initializes the Input array list
     * @param inputs 
     */
    private void setInputList(int[] inputs){
        
        int rly=this.vars.input;
        
        for(int i=0;i<inputList.length;i++){
            inputList[i]=rly++;
        }
    }
    
    /**
     * Initialize Output relay list
     * @param outputList 
     */
    private void setOutputList(int[] outputList){
        outputList[0]=vars.outputRly;
        outputList[1]=vars.outputRly+1;
    }
    
}
