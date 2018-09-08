/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Comunicatioms;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

/**
 *
 * @author Federico
 */
public class EmailMessage {
    
    private Address[] to = null;
    private Address from = null;
    private String subject = null;
    private String text = null;
    
    public EmailMessage(){
        
    }
    
    public EmailMessage(Address[] to, Address from, String subject, String text){
       
            this.to=to;
            this.from=from;
            this.subject=subject;
            this.text=text;
                  
    }
    
    public void setReply(){
        
        Address address = new InternetAddress();
        address=this.from;
        this.from=this.to[0];
        this.to[0]=address;
        
    }
    
    public void setText(String text){
        this.text=text;
    }

    public void setTo(Address[] to) {
        this.to = to;
    }

    public void setFrom(Address from) {
        this.from = from;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Address[] getTo() {
        return to;
    }

    public Address getFrom() {
        return from;
    }

    public String getSubject() {
        return subject;
    }

    public String getText() {
        return text;
    }
    
    public String getActualDate() {
        Date dNow = new Date();
        SimpleDateFormat ft
                = new SimpleDateFormat("dd/MM/yyyy 'at' HH:mm zzz");
        String actualDate=ft.format(dNow);
        return actualDate;
    }
    
    
    
}
