/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Comunicatioms;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import static java.lang.ProcessBuilder.Redirect.to;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;

/**
 *
 * @author Federico
 */
public class RD3mail {

 //   private String smtpHost = "smtp.gmail.com";
 //   private String pop3Host = "pop.gmail.com";
    private String smtpHost = "mail.adr3group.com";
    private String pop3Host = "mail.adr3group.com";
    private String storeType="pop3";
    private String user = null;
    private String pass = null;
    private boolean sessionDebug = false;

    public RD3mail(){
    
    }
    public RD3mail(String user, String pass){
        this.user=user;
        this.pass=pass;
    
    }
    
    public void setUser(String user) {
        this.user = user;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }
 
    public synchronized void sendEmailAttach(EmailMessage message, String messageText, String attachName){
        try {
            Properties props = System.getProperties();
            props.put("mail.pop3.host", pop3Host);
            props.put("mail.pop3.port", "995");
            props.put("mail.pop3.ssl.enable","true");
            //   props.put("mail.pop3.starttls.enable", "true");
            
            //   props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", "465");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.timeout", 5000);
            props.put("mail.smtp.connectiontimeout", 5000);
            //  props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.enable","true");
            //   props.put("mail.user",user);
            //   props.put("mail.password",pass);
            
            java.security.Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            
            Session mailSession = Session.getDefaultInstance(props, null);
            mailSession.setDebug(sessionDebug);
            Message msg = new MimeMessage(mailSession);
            msg.setRecipients(Message.RecipientType.TO, message.getTo());
            msg.setFrom(message.getFrom());
            msg.setSubject(message.getSubject());
            
            BodyPart messageBodyPart=new MimeBodyPart();
            String textMessage = message.getActualDate()+"/n";
            textMessage=textMessage+"Attached is the temperature Log File";
            messageBodyPart.setText(messageText);
            
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            
            messageBodyPart= new MimeBodyPart();
            DataSource source = new FileDataSource(attachName);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName("TempLogFile.txt");
            multipart.addBodyPart(messageBodyPart);
            msg.setContent(multipart);
            
             
            Transport transport = mailSession.getTransport("smtp");
            transport.connect(smtpHost, user, pass);
            transport.sendMessage(msg, msg.getAllRecipients());
            transport.close();
        } catch (MessagingException ex) {
            Logger.getLogger(RD3mail.class.getName()).log(Level.SEVERE, null, ex);
        }
            
    }
    public synchronized void sendEmail(EmailMessage message, String messageText) {
        try {

            Properties props = System.getProperties();
            props.put("mail.pop3.host", pop3Host);
            props.put("mail.pop3.port", "995");
            props.put("mail.pop3.ssl.enable","true"); 
         //   props.put("mail.pop3.starttls.enable", "true");

         //   props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", "465");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.timeout", 5000);    
            props.put("mail.smtp.connectiontimeout", 5000);    
          //  props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.enable","true");
         //   props.put("mail.user",user);
         //   props.put("mail.password",pass);

            java.security.Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

            Session mailSession = Session.getDefaultInstance(props, null);
            mailSession.setDebug(sessionDebug);
            Message msg = new MimeMessage(mailSession);
            msg.setRecipients(Message.RecipientType.TO, message.getTo());
            msg.setFrom(message.getFrom());
            msg.setSubject(message.getSubject());
            msg.setSentDate(new Date());
            msg.setText(messageText);
            
            Transport transport = mailSession.getTransport("smtp");
            transport.connect(smtpHost, user, pass);
            transport.sendMessage(msg, msg.getAllRecipients());
            transport.close();
         //   System.out.println("Message sent succesfully");

        } catch (Exception ex) {
            System.out.println(ex);

        }
    }
    
     public synchronized int checkEmail(ArrayList<EmailMessage> emailList) throws NoSuchProviderException
    {
        
        try {

            //create properties field
            Properties properties = new Properties();
            
            properties.put("mail.pop3.host", pop3Host);
            properties.put("mail.pop3.port", "995");
            properties.put("mail.pop3.ssl.enable","true");
            properties.put("mail.pop3.timeout",5000);
            properties.put("mail.pop3.connectiontimeout", 5000);
         //   properties.put("mail.pop3.starttls.enable", "true");

         //   props.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.host", smtpHost);
            properties.put("mail.smtp.port", "465");
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.timeout", 5000);    
            properties.put("mail.smtp.connectiontimeout", 5000);    
          //  properties.put("mail.smtp.starttls.required", "true");
            properties.put("mail.smtp.ssl.enable","true");
         //   props.put("mail.user",user);
         //   props.put("mail.password",pass);
            Session emailSession = Session.getDefaultInstance(properties);
            emailSession.setDebug(sessionDebug);
            //create the POP3 store object and connect with the pop server
            Store store = emailSession.getStore("pop3s");
            
            try {
                store.connect(pop3Host, user, pass);
            } catch (MessagingException ex) {
                Logger.getLogger(RD3mail.class.getName()).log(Level.SEVERE, null, ex);
                store.close();
                System.out.println("POP3 connection Time out");
                return -1;
            }
            
            //create the folder object and open it
            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_WRITE);
            
            // retrieve the messages from the folder in an array and print it
            Message[] messages = emailFolder.getMessages();
            
            //System.out.println("messages.length---" + messages.length);
            
            for (int i = 0, n = messages.length; i < n; i++) {
                Message message = messages[i];
                EmailMessage email = new EmailMessage();
                email.setFrom(message.getFrom()[0]);
                email.setTo(message.getRecipients(Message.RecipientType.TO));
                email.setSubject(message.getSubject());
                emailList.add(email);
                message.setFlag(Flags.Flag.DELETED, true);
            }
            
            //close the store and folder objects
            emailFolder.close(true);
            store.close();
            
            
        } catch (MessagingException ex) {
            Logger.getLogger(RD3mail.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Connection Error");
            return -2;
        }
     
   return 0;
   }
/*
public void fetch() {
      try {
         // create properties field
         Properties properties = new Properties();
         properties.put("mail.store.protocol", "pop3");
         properties.put("mail.pop3.host", pop3Host);
         properties.put("mail.pop3.port", "995");
         properties.put("mail.pop3.starttls.enable", "true");
         Session emailSession = Session.getDefaultInstance(properties);
         // emailSession.setDebug(true);

         // create the POP3 store object and connect with the pop server
         Store store = emailSession.getStore("pop3s");

         store.connect(pop3Host, user, pass);

         // create the folder object and open it
         Folder emailFolder = store.getFolder("INBOX");
         emailFolder.open(Folder.READ_WRITE);

         BufferedReader reader = new BufferedReader(new InputStreamReader(
	      System.in));

         // retrieve the messages from the folder in an array and print it
         Message[] messages = emailFolder.getMessages();
         System.out.println("messages.length---" + messages.length);

         for (int i = 0; i < messages.length; i++) {
            Message message = messages[i];
            System.out.println("---------------------------------");
            writePart(message);
            message.setFlag(Flags.Flag.DELETED, true);
            String line = reader.readLine();
            if ("YES".equals(line)) {
               message.writeTo(System.out);
            } else if ("QUIT".equals(line)) {
               break;
            }
         }

         // close the store and folder objects
         emailFolder.close(true);
         store.close();

      } catch (NoSuchProviderException e) {
         e.printStackTrace();
      } catch (MessagingException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

public void writePart(Part p) throws Exception {
      if (p instanceof Message)
         //Call methos writeEnvelope
         writeEnvelope((Message) p);

      System.out.println("----------------------------1");
      System.out.println("CONTENT-TYPE: " + p.getContentType());

      //check if the content is plain text
      if (p.isMimeType("text/plain")) {
         System.out.println("This is plain text");
         System.out.println("---------------------------2");
         System.out.println((String) p.getContent());
      } 
      //check if the content has attachment
      else if (p.isMimeType("multipart/*")) {
         System.out.println("This is a Multipart");
         System.out.println("---------------------------3");
         Multipart mp = (Multipart) p.getContent();
         int count = mp.getCount();
         System.out.println("Message Body Parts "+count);
         this.recvMessage=(String)mp.getBodyPart(0).getContent();
         for (int i = 0; i < count; i++)
            writePart(mp.getBodyPart(i));
      } 
      //check if the content is a nested message
      else if (p.isMimeType("message/rfc822")) {
         System.out.println("This is a Nested Message");
         System.out.println("---------------------------4");
         writePart((Part) p.getContent());
      } 
      
      else {
         Object o = p.getContent();
         if (o instanceof String) {
            System.out.println("This is a string");
            System.out.println("---------------------------5");
            System.out.println((String) o);
         } 
         else if (o instanceof InputStream) {
            System.out.println("This is just an input stream");
            System.out.println("---------------------------6");
            InputStream is = (InputStream) o;
            is = (InputStream) o;
            int c;
            while ((c = is.read()) != -1)
               System.out.write(c);
         } 
         else {
            System.out.println("This is an unknown type");
            System.out.println("---------------------------7");
            System.out.println(o.toString());
         }
      }

   }
public void writeEnvelope(Message m) throws Exception {
      System.out.println("This is the message envelope");
      System.out.println("---------------------------");
      Address[] a;

      // FROM
      if ((a = m.getFrom()) != null) {
         for (int j = 0; j < a.length; j++){
             this.setFrom(a[j].toString());
         System.out.println("FROM: " + a[j].toString());
         }
      }

      // TO
      if ((a = m.getRecipients(Message.RecipientType.TO)) != null) {
         for (int j = 0; j < a.length; j++){
             this.setTo(a[j].toString());
         System.out.println("TO: " + a[j].toString());
         }
      }

      // SUBJECT
      if (m.getSubject() != null){
          this.setRecvSubject(m.getSubject());
          System.out.println("SUBJECT: " + m.getSubject());
      }

   }*/

}
