/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modules;

import Comunicatioms.EmailMessage;
import Comunicatioms.Gmail;
import Comunicatioms.WhatsappSender;
import RPI_IO_Lib.RPI_IO;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 *
 * @author Federico
 */
public class Energy {
    
    RPI_IO rpio=null;
    int input = 0;    //first RPI board input port. Auto/Manual
    int inputCount = 0;    //How many inputs
    int[] inputList=null;   //List of inputs
    int timer = 0; //Sample period in millis.
    
    Gmail gmail = new Gmail("svmi.radar@gmail.com", "svmi1234");
    EmailMessage email=new EmailMessage();
    boolean email_flag = false;
    
    WhatsappSender whatsup=new WhatsappSender();
    
    private int mainState=0;
    private int genState=0;
    private int surgeState=0;
    
   /**
    * Class Constructor
    * @param rpio 
    */ 
    public Energy(RPI_IO rpio){
        this.rpio=rpio;
    }

    public void setInput(int input) {
        this.input = input;
    }

    public void setInputCount(int inputCount) {
        this.inputCount = inputCount;
        this.inputList=new int[inputCount];
        this.setInputList(inputList);
    }

    public boolean isEmail_flag() {
        return email_flag;
    }

    public void setEmailFlag(boolean email_flag) {
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

    public void setTimer(int timer) {
        this.timer = timer;
    }
    
    public String getReport(){
        String report="";
        
        if(mainState==0)
            report=report+"Main Power: ON\n";
        else
            report=report+"Main Power: OFF\n";
        
        if(genState==0)
            report=report+"Generator: STOP\n";
        else
            report=report+"Generator: RUNNING\n";
        
        if(surgeState==0)
            report=report+"Surge Protection: ACTIVE\n";
        else
            report=report+"Surge Protection: DISCONNECTED\n";
             
        return report;
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
    
    public void start(int timer){
        
        this.timer=timer;
        Thread task=new Thread(new EnergyTask(),"Energy Task");
        task.start();
}
    
    public class EnergyTask implements Runnable{

        @Override
        public void run() {
           try {
            email.setFrom(new InternetAddress("svmi.radar@gmail.com"));
            email.setTo(InternetAddress.parse("federico.rivero.m@gmail.com"));
            email.setSubject("SVMI. Radar Station Power Supply");
        } catch (AddressException ex) {
            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
        }
        while(true){
            
            checkMains();
            checkGen();
            checkSurge();
            try {
                Thread.sleep(timer);
            } catch (InterruptedException ex) {
                Logger.getLogger(modules.Energy.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        }
    
    }
    
     private void checkMains(){
        
        switch(mainState){
            case 0:
                if(!rpio.getInput(inputList[0])){
                    mainState=1;
                    String message=email.getActualDate();
                    message=message+"\nMain Power Supply OFF";
                    System.out.println(message);
                    if(this.email_flag){
                        gmail.sendEmail(email, message);
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI","Main Power OFF");
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                break;
                
            case 1:
                if(rpio.getInput(inputList[0])){
                    mainState=0;
                    String message=email.getActualDate();
                    message=message+"\nMain Power Supply ON";
                    System.out.println(message);
                    if(this.email_flag){
                        gmail.sendEmail(email, message);
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI","Main Power ON");
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                break;
            default:
        }
    
    }
    
    private void checkGen() {
        switch (genState) {
            case 0:
                if (rpio.getInput(inputList[1])) {
                    genState = 1;
                    String message = email.getActualDate();
                    message = message + "\nGenerator Supply ON";
                    System.out.println(message);
                    if(this.email_flag){
                        gmail.sendEmail(email, message);
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI","Generator RUNNING");
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                break;

            case 1:
                if (!rpio.getInput(inputList[1])) {
                    genState = 0;
                    String message = email.getActualDate();
                    message = message + "\nGenerator Supply OFF";
                    System.out.println(message);
                    if(this.email_flag){
                        gmail.sendEmail(email, message);
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI","Generator STOP");
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                break;
            default:
        }
    }
    
    private void checkSurge(){
    switch (surgeState) {
            case 0:
                if (rpio.getInput(inputList[2])) {
                    surgeState = 1;
                    String message = email.getActualDate();
                    message = message + "\nSurge Protector FAIL.\nSurge Protection INOPERATIVE";
                    System.out.println(message);
                    if(this.email_flag){
                        gmail.sendEmail(email, message);
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI","Surge Protection INOPERATIVE");
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                break;

            case 1:
                if (!rpio.getInput(inputList[2])) {
                    surgeState = 0;
                    String message = email.getActualDate();
                    message = message + "\nSurge Protection ACTIVE";
                    System.out.println(message);
                    if(this.email_flag){
                        gmail.sendEmail(email, message);
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI","Surge Protection ACTIVE");
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                break;
            default:
        }
    }
    
    
}
