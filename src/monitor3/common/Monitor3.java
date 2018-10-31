/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package monitor3.common;

import Common.DataArray;
import Common.ResetPi;
import RPI_IO_Lib.RPI_IO;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import modules.Intrusion;
import Common.SunsetCalculator;
import Comunicatioms.EmailMessage;
import Comunicatioms.Gmail;
import Comunicatioms.RD3mail;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import modules.AirCondition;
import modules.AirConditionScheduler;
import modules.Energy;
import modules.ExteriorLights;
import modules.IntrusionAlarm;

/**
 * Version 1.11
 * - Added mains power timer and interruptions counter and email command to reset
 * variables.
 * - Remove sending email messages on each event.
 * - Added AC running hours counter
 * @author Federico
 */
public class Monitor3 {

    static RPI_IO rpio = null;
    static RPI_IO_DATA data = null;
    static IntrusionAlarm intrusion = null;
    static ExteriorLights lights = null;
    static AirCondition aircondition = null;
    static Energy energy = null;
    static ResetPi reset = null;
    //static RD3mail rd3email = new RD3mail("svmi.radar@adr3group.com", "$radar.2018*");
    static RD3mail rd3email = new RD3mail("test.adr3@adr3group.com", "$test.2018*");            
    static EmailMessage message = new EmailMessage();
    static ArrayList<EmailMessage> emailList = new ArrayList<EmailMessage>();
    static boolean messageFlag=false;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException, FileNotFoundException, UnsupportedEncodingException {

        rpio = new RPI_IO();
        data = new RPI_IO_DATA();
        intrusion = new IntrusionAlarm(rpio);
        lights = new ExteriorLights(rpio);
        aircondition = new AirCondition(rpio);
        energy = new Energy(rpio);
        reset = new ResetPi(rpio);

        rpio.out_off();
        //Intrusion module sett up
        intrusion.setInputNumber(2); //Input doors to be monitor
        intrusion.setInputPort(1);  //RPI Board Input port
        intrusion.setOutputRly(2);  //RPI first output port
        intrusion.setTimer(5);  //Timer for internal lights
        intrusion.start();  //start task
        intrusion.setEmail_flag(messageFlag);
        //    System.out.println("Report:\n"+intrusion.getReport());

        //Exterior Lights module set up
        lights.setLatitud(10.599594);
        lights.setLongitud(-66.997908);
        lights.setCount(1);
        lights.setInput(3);
        lights.setOutputRly(4);
        lights.start();
        lights.setEmailFlag(messageFlag);
        //   System.out.println("Report:\n"+lights.getReport());

        //Air Conditioning task
        aircondition.setInputCount(2); //Number of input
        aircondition.setInput(4); //Input port
        aircondition.setOutputRly(6); //firstOutput relay
        aircondition.setOutputCount(3);
        aircondition.setRC_const(0.61);
        aircondition.setAlarm(26.0);
        aircondition.setSchedule(AirConditionScheduler.DAY, 1);
      //  aircondition.setSchedule(AirConditionScheduler.HOUR, 1);
        aircondition.setEmailFlag(messageFlag);
        aircondition.start();

        //Energy Task
        energy.setInput(6); //First Input port
        energy.setInputCount(3);
        energy.setEmailFlag(messageFlag);
        energy.start(1000);
        //   System.out.println("Report:\n"+energy.getReport());
        
        //Reset Monitor
        reset.setInput1(4);
        reset.setInput2(5);
        reset.start(1000);
        
        
        //Loop every 5 seconds to check for email commands.
         int connect_status=0;
                 
        while (true) {

            rpio.out_on();
            try {
                connect_status = rd3email.checkEmail(emailList);
            } catch (NoSuchProviderException ex) {
                Logger.getLogger(Monitor3.class.getName()).log(Level.SEVERE, null, ex);
                rpio.blink_1Hz();
            }

            //   System.out.println("Email count "+emailList.size());
            String subject;
            String request;
            if (connect_status == 0) {
                for (int i = 0; i < emailList.size(); i++) {
                    message = (EmailMessage) emailList.get(i);
                    /*    System.out.println("Email "+i);
                System.out.println("From: "+message.getFrom());
                System.out.println("To: "+message.getTo()[0]);*/
                    subject = message.getSubject();
                    request = getRequest(subject);
                    message.setReply();
                    if(request.equalsIgnoreCase("comando invalido")){
                        rd3email.sendEmail(message, request);
                    }else {
                    String[] parts = request.split("/");
                    if (parts[1].equalsIgnoreCase("home")) {
                        String text=message.getActualDate()+"\n";
                        text=text+subject+" File attached";
                        rd3email.sendEmailAttach(message, text, request);
                    } else {
                        rd3email.sendEmail(message, request);
                    }
                }
                    emailList.clear();

                }
                rpio.out_off();
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Monitor3.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static String getRequest(String subject) throws FileNotFoundException, UnsupportedEncodingException{
        
        String request=message.getActualDate()+"\n";
        request=request+"Monitor3 Version 1.31\n\n";
        subject=subject.toLowerCase();
        
        switch(subject){
            case "status":
            case "estado":
                request=request+intrusion.getReport()+"\n";
                request=request+lights.getReport()+"\n";
                request=request+energy.getReport()+"\n";
                request=request+aircondition.getReport()+"\n";
                request=request+aircondition.runReport()+"\n";
                break;
                
            case "temperature":
            case "temp":
                request=request+"Actual room temperature:\n"+aircondition.getTemperature();
                break;
                
            case "reset ac":
                request=request+aircondition.resetAC();
                break;
                
            case "sun data":
                request=lights.getSunData();
                break;
                
            case "lights on":
                lights.lightON();
                request=request+"Platform Lights turned ON";
                break;
                
            case "lights off":
                lights.lightOFF();
                request=request+"Platform Lights turned OFF";
                break;
                
            case "system reset":
                reset.resetCommand();
                break;
                
            case "system shutdown":
                reset.shutDownCommand();
                break;
                
            case "reset intrusion alarm": 
                request=request+intrusion.resetAlarm();
                break;
                                       
            case "version":
                request="Monitor3 version 1.31";
                break;
            
            case "reset power":
                request=request+energy.reset_power();
                break;
                
            case "temp log 1":
                request=aircondition.getLog1();
                break;
                
            case "temp log 5":
                request=aircondition.getLog5();
                break;
                
            case "temp log 60":
                request=aircondition.getLog60();
                break;
                
            case "ack ac alarm":
                request=aircondition.alarmAck();
                break;
                
            case "energy log":
                request=energy.getLog();
                break;
                
            default:
                request="Comando invalido";
                
            
        }
        return request;
    }

}
