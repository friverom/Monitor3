package Comunicatioms;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Federico
 */
public class MonitorLogger {
    
    public final static Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    public MonitorLogger(){
        
    }
    public void setupLogger(){
        LogManager.getLogManager().reset(); //Reset all handlers
        log.setLevel(Level.ALL);
        
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.SEVERE);
        log.addHandler(ch);
        
        try {
            FileHandler fh = new FileHandler("Monitor3Log.log");
            fh.setLevel(Level.FINE);
            fh.setFormatter(new SimpleFormatter());
            log.addHandler(fh);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error opening log file");
        } catch (SecurityException ex) {
            log.log(Level.SEVERE, "Security Exception in logger class");
        }
        
        
    }
    
    public void log(Level l, String message){
        
        log.log(l, message);
    }
 
}
