/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modules;

import Comunicatioms.EmailMessage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 *
 * @author Federico
 */
public class EnergyTask implements Runnable{
    
    Energy data=null;
     int[] inputList=null;   //List of inputs
    EmailMessage email=new EmailMessage();
    
    public EnergyTask(Energy data){
        this.data=data;
        this.inputList=new int[this.data.inputCount];
        this.setInputList(inputList);
        System.out.println("Energy variables set. OK");
    }

    @Override
    public void run() {
        try {
            email.setFrom(new InternetAddress("svmi.radar@gmail.com"));
            email.setTo(InternetAddress.parse("federico.rivero.m@gmail.com"));
            email.setSubject("SVMI. Radar Station Power Supply");
        } catch (AddressException ex) {
            Logger.getLogger(IntrusionTask.class.getName()).log(Level.SEVERE, null, ex);
        }
        while(true){
            
            checkMains();
            checkGen();
            checkSurge();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(EnergyTask.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void checkMains(){
        
        switch(data.mainState){
            case 0:
                if(!data.rpio.getInput(inputList[0])){
                    data.mainState=1;
                    String message=email.getActualDate();
                    message=message+"\nMain Power Supply OFF";
                    System.out.println(message);
                    data.email.sendEmail(email, message);
                }
                break;
                
            case 1:
                if(data.rpio.getInput(inputList[0])){
                    data.mainState=0;
                    String message=email.getActualDate();
                    message=message+"\nMain Power Supply ON";
                    System.out.println(message);
                    data.email.sendEmail(email, message);
                }
                break;
            default:
        }
    
    }
    
    private void checkGen() {
        switch (data.genState) {
            case 0:
                if (data.rpio.getInput(inputList[1])) {
                    data.genState = 1;
                    String message = email.getActualDate();
                    message = message + "\nGenerator Supply ON";
                    System.out.println(message);
                    data.email.sendEmail(email, message);
                }
                break;

            case 1:
                if (!data.rpio.getInput(inputList[1])) {
                    data.genState = 0;
                    String message = email.getActualDate();
                    message = message + "\nGenerator Supply OFF";
                    System.out.println(message);
                    data.email.sendEmail(email, message);
                }
                break;
            default:
        }
    }
    
    private void checkSurge(){
    switch (data.surgeState) {
            case 0:
                if (data.rpio.getInput(inputList[2])) {
                    data.surgeState = 1;
                    String message = email.getActualDate();
                    message = message + "\nSurge Protector FAIL.\nSurge Protection INOPERATIVE";
                    System.out.println(message);
                    data.email.sendEmail(email, message);
                }
                break;

            case 1:
                if (!data.rpio.getInput(inputList[2])) {
                    data.surgeState = 0;
                    String message = email.getActualDate();
                    message = message + "\nSurge Protection ACTIVE";
                    System.out.println(message);
                    data.email.sendEmail(email, message);
                }
                break;
            default:
        }
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
    
}
