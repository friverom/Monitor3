/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modules;

import Common.SunsetCalculator;
import Comunicatioms.EmailMessage;
import Comunicatioms.Gmail;
import Comunicatioms.RD3mail;
import Comunicatioms.WhatsappSender;
import RPI_IO_Lib.RPI_IO;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;


/**
 *
 * @author Federico
 */
public class ExteriorLights {
    RPI_IO rpio=null;
    double latitud=0;
    double longitud=0;
    int input=0;    //first RPI board input port
    int[] inputList=null;   //List of inputs
    int count=0;    //How many inputs
    int[] outputList=new int[2]; // List of outputs
    int outputRly=0; //first RPI board output port
    int timer=0; // timer to turn off lights in minutes
    RD3mail rd3email = new RD3mail("svmi.radar@adr3group.com","$radar.2018*");
    boolean email_flag=false;
    
    WhatsappSender whatsup=new WhatsappSender();
    
    static int plat_state=0; //PLatform Lights running state
    static int obst_state=0; //Obstruction light state
    static boolean sunrise_flag=false;
    static boolean sunset_flag=false;
    
    EmailMessage email=new EmailMessage();
    private SunsetCalculator calc=new SunsetCalculator();
    
    public ExteriorLights(RPI_IO rpio) {
        this.rpio=rpio;
        System.out.println("Exterior Lights variables set. OK");
    }

    public void setEmailFlag(boolean t){
        this.email_flag=t;
    }
    
    public boolean getEmailFlag(){
        return this.getEmailFlag();
    }
    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    public void setInput(int input) {
        this.input = input;
        inputList=new int[this.count];
        this.setInputList(inputList);
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setOutputRly(int outputRly) {
        this.outputRly = outputRly;
        this.setOutputList(outputList);
    }

    public void setTimer(int timer) {
        this.timer = timer;
    }
    
    public void start(){
        Thread task=new Thread(new ExteriorLightsTask(),"Lights Task");
        task.start();
    }
    
    public String getSunData() {
        SimpleDateFormat format1 = new SimpleDateFormat("HH:mm:ss zzz");
        
        Calendar date = Calendar.getInstance();
        Calendar set = calc.getSunset(date);
        
        date = Calendar.getInstance();
        Calendar rise = calc.getSunrise(date);
        String sunset = format1.format(set.getTime());
        String sunrise = format1.format(rise.getTime());
        
        String data="Sunrise: "+sunrise;
        data=data+"\nSunset: "+sunset+"\n";
        return data;
    }
    
    public String getReport(){
        String report;
       
        report=getSunData();
        
        if(plat_state==0){
            report=report+"Platform Lights: OFF\n";
        }
        else{
            report=report+"Platform Lights: ON\n";
        }
            
        if(obst_state==0){
            report=report+"Obstruction Lights: OFF\n";
        } 
        else{
            report=report+"Obstruction Lights: ON\n";
        }
        
        return report;
    }
    
    public void lightON() {
        Calendar date=Calendar.getInstance();
        plat_state = 2;
        rpio.setRly(outputList[0]);
        if (email_flag == true) {
            //   data.email.setMessage("Platform lights ON.");
            rd3email.sendEmail(email, email.getActualDate() + "\nPlatform lights ON.");
            try {
                whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "Platform lights ON");
            } catch (Exception ex) {
                Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("Platform lights ON at " + date.getTime());
    }

    public void lightOFF() {
        Calendar date = Calendar.getInstance();
        plat_state = 0;
        rpio.resetRly(outputList[0]);
        if (email_flag == true) {
            //   data.email.setMessage("Platform lights ON.");
            rd3email.sendEmail(email, email.getActualDate() + "\nPlatform lights OFF.");
            try {
                whatsup.sendGroupMessage("+584241184923", "Radar SVMI", "Platform lights OFF");
            } catch (Exception ex) {
                Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("Platform lights OFF at " + date.getTime());
    }

 public class ExteriorLightsTask implements Runnable{

        @Override
        public void run() {
            System.out.println("Exterior lights task started. OK");
         try {
            email.setFrom(new InternetAddress("svmi.radar@adr3group.com"));
            email.setTo(InternetAddress.parse("federico.rivero.m@gmail.com"));
            email.setSubject("SVMI. Radar Station Lights");
        } catch (AddressException ex) {
            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
        }
    /*    data.email.setFrom("svmi.radar@gmail.com");
        data.email.setTo("federico.rivero.m@gmail.com");
        data.email.setSubject("SVMI. Radar Station Lights");*/
        calc.setLocation(latitud, longitud);
        while(true){
            
            platformLights();
            obstructionLights();
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(modules.ExteriorLights.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        }
 
 }
 
 private void obstructionLights(){
        
      //  TimeZone tz1 = TimeZone.getTimeZone("GMT-4");
        Calendar date=Calendar.getInstance();
        Calendar sunrise=calc.getSunrise(date);
        
        date=Calendar.getInstance();
        Calendar sunset=calc.getSunset(date);
               
        date=Calendar.getInstance();
       
     /*   System.out.println("Actual Time: "+date.getTime()+" "+date.getTimeInMillis());
        System.out.println("Sunset "+sunset.getTime()+" "+sunset.getTimeInMillis());
        System.out.println("Sunrise "+sunrise.getTime()+" "+sunrise.getTimeInMillis());*/
        
       sunrise_flag=date.getTimeInMillis()<sunrise.getTimeInMillis();
       sunset_flag=date.getTimeInMillis()>sunset.getTimeInMillis();
       
       //state machine to handle obstruction light.
        switch (obst_state) {

            //Initial state. Obstruction light OFF. Change state to 1 if actual time
            //is greater than today's sunset.
            case 0:
                if (sunset_flag) {
                    rpio.setRly(outputList[1]);
                    obst_state = 1;
                    if (email_flag == true) {
                       // data.email.setMessage("Obstruction lights ON.");
                        rd3email.sendEmail(email,email.getActualDate()+"\nObstruction lights ON.");
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI","Obstruction Lights ON");
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    System.out.println("Obstruction lights ON at "+date.getTime());
                }
                break;
            // Obstruction light ON and wait for midnight to change to state2
            case 1:
                if (sunrise_flag) {
                    obst_state = 2;
                }
                break;
            //Obstruction light ON and wait's for sunrise to turn OFF obstruction
            //light and Platform lights.
            case 2:
                if (!sunrise_flag) {
                    rpio.resetRly(outputList[0]);
                    rpio.resetRly(outputList[1]);
                    obst_state = 0;
                    plat_state=0;
                    if (email_flag == true) {
                     //   data.email.setMessage("Obstruction lights and Platform lights OFF.");
                        rd3email.sendEmail(email,email.getActualDate()+"\nObstruction lights and Platform lights OFF.");
                        try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI","Obstrction and Platform lights OFF");
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    System.out.println("Obstruction lights OFF at "+date.getTime());
                    System.out.println("Platform lights OFF at "+date.getTime());
                }
            default:
        }
        
    }
    private void platformLights(){
        
       // TimeZone tz1 = TimeZone.getTimeZone("GMT-4");
        Calendar date=Calendar.getInstance();
        
        switch(plat_state){
                
                case 0:
                    if(rpio.getInput(inputList[0])){
                        plat_state=1;
                        rpio.setRly(outputList[0]);
                        if (email_flag == true) {
                         //   data.email.setMessage("Platform lights ON.");
                            rd3email.sendEmail(email,email.getActualDate()+"\nPlatform lights ON.");
                            try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI","Platform lights ON");
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        }
                        System.out.println("Platform lights ON at "+date.getTime());
                    }
                    break;
                
                case 1:
                    if(!rpio.getInput(inputList[0])){
                     plat_state=2;   
                    }
                    break;
                
                case 2:
                    if(rpio.getInput(inputList[0])){
                        plat_state=3;
                        rpio.resetRly(outputList[0]);
                        if (email_flag == true) {
                          //  data.email.setMessage("Platform lights OFF.");
                            rd3email.sendEmail(email,email.getActualDate()+"\nPlatform lights OFF.");
                            try {
                            whatsup.sendGroupMessage("+584241184923", "Radar SVMI","Platform lights OFF");
                        } catch (Exception ex) {
                            Logger.getLogger(Intrusion.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        }
                        System.out.println("Platform lights OFF at "+date.getTime());
                    }
                    break;
                    
                case 3:
                    if(!rpio.getInput(inputList[0])){
                        plat_state=0;
                    }
                    break;
                
                default:
            }
    }
    
    private int getHour(String s){
        String[] parts=s.split(":");
        return Integer.parseInt(parts[0]);
    }
    
    private int getMinutes(String s){
        String[] parts=s.split(":");
        return Integer.parseInt(parts[1]);
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
