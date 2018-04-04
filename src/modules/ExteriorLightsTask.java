/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modules;

import Common.SunsetCalculator;
import Comunicatioms.EmailMessage;
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
public class ExteriorLightsTask implements Runnable{
    
    private ExteriorLights data=null;
    int[] inputList=null;   //List of inputs
    int[] outputList=new int[2]; // List of outputs
    static int state=0; //PLatform Lights running state
    static int obst_state=0; //Obstruction light state
    static boolean sunrise_flag=false;
    static boolean sunset_flag=false;
    
    EmailMessage email=new EmailMessage();
    private SunsetCalculator calc=new SunsetCalculator();

    public ExteriorLightsTask(ExteriorLights data) {
        this.data=data;
        this.inputList=new int[this.data.count];
        this.setInputList(inputList);
        this.setOutputList(outputList);
        System.out.println("Exterior Lights variables set. OK");
        calc.setLocation(data.latitud, data.longitud);
    }
    
    @Override
    public void run() {
        
        System.out.println("Exterior lights task started. OK");
         try {
            email.setFrom(new InternetAddress("svmi.radar@gmail.com"));
            email.setTo(InternetAddress.parse("federico.rivero.m@gmail.com"));
            email.setSubject("SVMI. Radar Station Lights");
        } catch (AddressException ex) {
            Logger.getLogger(IntrusionTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    /*    data.email.setFrom("svmi.radar@gmail.com");
        data.email.setTo("federico.rivero.m@gmail.com");
        data.email.setSubject("SVMI. Radar Station Lights");*/
        
        while(true){
            
            platformLights();
            obstructionLights();
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(ExteriorLightsTask.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }    
    
    private void obstructionLights(){
        
        TimeZone tz1 = TimeZone.getTimeZone("GMT-4");
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
                    data.rpio.setRly(outputList[1]);
                    if (data.email_flag == true) {
                       // data.email.setMessage("Obstruction lights ON.");
                        data.email.sendEmail(email,email.getActualDate()+"\nObstruction lights ON.");
                    }
                    System.out.println("Obstruction lights ON at "+date.getTime());
                    obst_state = 1;
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
                    data.rpio.resetRly(outputList[0]);
                    data.rpio.resetRly(outputList[1]);
                    obst_state = 0;
                    if (data.email_flag == true) {
                     //   data.email.setMessage("Obstruction lights and Platform lights OFF.");
                        data.email.sendEmail(email,email.getActualDate()+"\nObstruction lights and Platform lights OFF.");
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
        
        switch(state){
                
                case 0:
                    if(data.rpio.getInput(inputList[0])){
                        state=1;
                        data.rpio.setRly(outputList[0]);
                        if (data.email_flag == true) {
                         //   data.email.setMessage("Platform lights ON.");
                            data.email.sendEmail(email,email.getActualDate()+"\nPlatform lights ON.");
                        }
                        System.out.println("Platform lights ON at "+date.getTime());
                    }
                    break;
                
                case 1:
                    if(!data.rpio.getInput(inputList[0])){
                     state=2;   
                    }
                    break;
                
                case 2:
                    if(data.rpio.getInput(inputList[0])){
                        state=3;
                        data.rpio.resetRly(outputList[0]);
                        if (data.email_flag == true) {
                          //  data.email.setMessage("Platform lights OFF.");
                            data.email.sendEmail(email,email.getActualDate()+"\nPlatform lights OFF.");
                        }
                        System.out.println("Platform lights OFF at "+date.getTime());
                    }
                    break;
                    
                case 3:
                    if(!data.rpio.getInput(inputList[0])){
                        state=0;
                    }
                    break;
                
                default:
            }
    }
  /*  private Calendar calcSunset(Calendar date){
        
        calc.setLocation(data.latitud,data.longitud);
        calc.setDate(date);
        Calendar sunset=calc.getSunset();
        int hour=getHour(sunset);
        int minutes=getMinutes(sunset);
        date.set(Calendar.HOUR_OF_DAY, hour);
        date.set(Calendar.MINUTE,minutes);
        
        return date;
    }
    private Calendar calcSunrise(Calendar date){
        
        calc.setLocation(data.latitud,data.longitud);
        calc.setDate(date);
        Calendar sunrise=calc.getSunrise();
        int hour=getHour(sunrise);
        int minutes=getMinutes(sunrise);
        date.set(Calendar.HOUR_OF_DAY, hour);
        date.set(Calendar.MINUTE,minutes);
        
        return date;
    }*/
    
    
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
    }

    
}
