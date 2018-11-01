/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modules;

import Comunicatioms.EmailMessage;
import Comunicatioms.Gmail;
import Comunicatioms.RD3mail;
import Comunicatioms.WhatsappSender;
import RPI_IO_Lib.RPI_IO;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
    
    RD3mail rd3email = new RD3mail("svmi.radar@adr3group.com", "$radar.2018*");
    EmailMessage email=new EmailMessage();
    boolean email_flag = false;
    
    WhatsappSender whatsup=new WhatsappSender();
    
    private int mainState=0;
    private int genState=0;
    private int surgeState=0;
    
    private Date date_period=null;
    private long mains_start_time=0;
    private long mains_down_time=0;
    private int mains_loss_counter=0;
    
    private long gen_start_time=0;
    private long gen_alive_time=0;
    private int gen_start_counter=0;
   /**
    * Class Constructor
    * @param rpio 
    */ 
    public Energy(RPI_IO rpio){
        this.rpio=rpio;
        this.mains_start_time=System.currentTimeMillis();
        date_period=new Date();
        
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
    
    public String reset_power() {
        date_period=new Date();
        mains_start_time=System.currentTimeMillis();
        mains_down_time=0;
        mains_loss_counter=0;
        gen_alive_time=0;
        gen_start_counter=0;
        
        String resp="Reset command executed.";
        return resp;
    }
    
    public String getReport(){
        String report="";
        SimpleDateFormat ft = 
        new SimpleDateFormat ("dd/MM/yyyy  HH:mm");
        Date date=new Date();
        
        if(mainState==0)
            report=report+"Main Power: ON\n";
        else
            report=report+"Main Power: OFF\n";
              
        if(genState==0)
            report=report+"Generator: STOP\n";
        else
            report=report+"Generator: RUNNING\n";
        
        if(surgeState==0)
            report=report+"Surge Protection: ACTIVE\n\n";
        else
            report=report+"Surge Protection: DISCONNECTED\n\n";
        
        String mains=String.format("%.1f", mains_down_time/(1000.0*3600.0));
        String gen=String.format("%.1f",gen_alive_time/(1000.0*3600.0));
        
        report=report+"Period: "+ft.format(date_period)+" to "+ft.format(date);
        report=report+"\nMain Power OFF Time: "+mains+" hrs\n";
        report=report+"Main Power Interrupt counter: "+mains_loss_counter+"\n\n";
        report=report+"Generator RUNNING time: "+gen+" hrs\n";
        report=report+"Generator starts counter: "+gen_start_counter+"\n\n";
        
        return report;
    }
    
    private void logEvent(String event) throws IOException{
        
        //Check if file exists
        File f=new File("/home/pi/NetBeansProjects/Monitor3/power_log.txt");
        if(!f.exists()){
            f.createNewFile(); //Create file
        } 
        
        try (FileWriter fw = new FileWriter("/home/pi/NetBeansProjects/Monitor3/power_log.txt",true)) {
            PrintWriter pw=new PrintWriter(fw);
            pw.println(event);
            pw.close();
        }
        
    }
    public String getDate() {
        Date dNow = new Date();
        SimpleDateFormat ft
                = new SimpleDateFormat("dd/MM/yyyy 'at' HH:mm:ss");
        String actualDate=ft.format(dNow);
        return actualDate;
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
    
    public String getLog() throws FileNotFoundException, UnsupportedEncodingException{
        String attach ="/home/pi/NetBeansProjects/Monitor3/power_log.txt";
        
        return attach;
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
            email.setFrom(new InternetAddress("svmi.radar@adr3group.com"));
            email.setTo(InternetAddress.parse("federico.rivero.m@gmail.com"));
            email.setSubject("SVMI. Radar Station Power Supply");
        } catch (AddressException ex) {
            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
        }
        while(true){
            
               try {
                   checkMains();
               } catch (IOException ex) {
                   Logger.getLogger(Energy.class.getName()).log(Level.SEVERE, null, ex);
               }
               try {
                   checkGen();
               } catch (IOException ex) {
                   Logger.getLogger(Energy.class.getName()).log(Level.SEVERE, null, ex);
               }
            checkSurge();
            try {
                Thread.sleep(timer);
            } catch (InterruptedException ex) {
                Logger.getLogger(modules.Energy.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        }
    
    }
    
     private void checkMains() throws IOException{
        
        switch(mainState){
            case 0:
                if(!rpio.getInput(inputList[0])){
                    mainState=1;
                    mains_start_time=System.currentTimeMillis();
                    mains_loss_counter++;
                    String message=getDate();
                    message=message+" Mains Power OFF";
                    logEvent(message);
                    System.out.println(message);
                    if(this.email_flag){
                      //  rd3email.sendEmail(email, message);
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
                    String message=getDate();
                    message=message+" Mains Power ON";
                    logEvent(message);
                    System.out.println(message);
                    if(this.email_flag){
                     //   rd3email.sendEmail(email, message);
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI","Main Power ON");
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                mains_down_time=mains_down_time+(System.currentTimeMillis()-mains_start_time);
                mains_start_time=System.currentTimeMillis();
                break;
            default:
        }
    
    }
    
    private void checkGen() throws IOException {
        switch (genState) {
            case 0:
                if (rpio.getInput(inputList[1])) {
                    genState = 1;
                    gen_start_time=System.currentTimeMillis();
                    gen_start_counter++;
                    String message = getDate();
                    message = message + " Generator ON";
                    logEvent(message);
                    rpio.setRly(1);
                    System.out.println(message);
                    if(this.email_flag){
                     //   rd3email.sendEmail(email, message);
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
                    
                    String message = getDate();
                    message = message + " Generator OFF";
                    System.out.println(message);
                    logEvent(message);
                    rpio.resetRly(1);
                    if(this.email_flag){
                     //   rd3email.sendEmail(email, message);
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI","Generator STOP");
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                gen_alive_time=gen_alive_time+(System.currentTimeMillis()-gen_start_time);
                gen_start_time=System.currentTimeMillis();
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
                     //   rd3email.sendEmail(email, message);
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
                      //  rd3email.sendEmail(email, message);
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
