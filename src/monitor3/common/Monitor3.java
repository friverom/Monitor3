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
import java.io.IOException;
import java.util.ArrayList;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import modules.AirCondition;
import modules.AirConditionScheduler;
import modules.Energy;
import modules.ExteriorLights;

/**
 *
 * @author Federico
 */
public class Monitor3 {

    static RPI_IO rpio = null;
    static RPI_IO_DATA data = null;
    static Intrusion intrusion = null;
    static ExteriorLights lights = null;
    static AirCondition aircondition = null;
    static Energy energy = null;
    static ResetPi reset = null;
    static RD3mail rd3email = new RD3mail("svmi.radar@adr3group.com", "$radar.2018*");
                
    static EmailMessage message = new EmailMessage();
    static ArrayList<EmailMessage> emailList = new ArrayList<EmailMessage>();
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {

        rpio = new RPI_IO();
        data = new RPI_IO_DATA();
        intrusion = new Intrusion(rpio);
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
        intrusion.setEmail_flag(true);
        //    System.out.println("Report:\n"+intrusion.getReport());

        //Exterior Lights module set up
        lights.setLatitud(10.599594);
        lights.setLongitud(-66.997908);
        lights.setCount(1);
        lights.setInput(3);
        lights.setOutputRly(4);
        lights.start();
        lights.setEmailFlag(true);
        //   System.out.println("Report:\n"+lights.getReport());

        //Air Conditioning task
        aircondition.setInputCount(2); //Number of input
        aircondition.setInput(4); //Input port
        aircondition.setOutputRly(6); //firstOutput relay
        aircondition.setOutputCount(3);
        aircondition.setRC_const(0.61);
        aircondition.setAlarm(26.0);
        aircondition.setSchedule(AirConditionScheduler.DAY, 1);
        aircondition.setEmailFlag(true);
        aircondition.start();

        //Energy Task
        energy.setInput(6); //First Input port
        energy.setInputCount(3);
        energy.setEmailFlag(true);
        energy.start(1000);
        //   System.out.println("Report:\n"+energy.getReport());
        
        //Reset Monitor
        reset.setInput1(4);
        reset.setInput2(5);
        reset.start(1000);
        
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
                    rd3email.sendEmail(message, request);
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
    
    public static String getRequest(String subject){
        
        String request=message.getActualDate()+"\n\n";
        subject=subject.toLowerCase();
        
        switch(subject){
            case "status":
            case "estado":
                request=request+intrusion.getReport()+"\n";
                request=request+lights.getReport()+"\n";
                request=request+energy.getReport()+"\n";
                request=request+aircondition.getReport()+"\n";
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
                break;
                
            case "lights off":
                lights.lightOFF();
                break;
                
            case "system reset":
                reset.resetCommand();
                break;
                
            case "system shutdown":
                reset.shutDownCommand();
                break;
            default:
                request="Comando invalido";
                
            
        }
        return request;
    }

}
