/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package monitor3.common;

/**
 *
 * @author Federico
 */
public class RPI_IO_DATA {
    
    private static int input_port;
    private static int output_port;
    private static int[] analog_port = new int[8];
    private static double[] analog_values = new double[8];
    
    public RPI_IO_DATA(){
        input_port=0;
        output_port=0;
        
        for(int i=0; i<8 ; i++){
            analog_port[i]=0;
            analog_values[i]=0.0;
        }
    }

    public synchronized int getInput_port() {
        return input_port;
    }

    public synchronized void setInput_port(int value) {
        input_port = value;
    }

    public synchronized int getOutput_port() {
        return output_port;
    }

    public synchronized void setOutput_port(int value) {
        output_port = value;
    }

    public synchronized int[] getAnalog_port() {
        return analog_port;
    }

    public synchronized void setAnalog(int i, int value) {
        analog_port[i]=value;
    }
    
    public synchronized int getAnalog(int i){
        return analog_port[i];
    }

    public synchronized double[] getAnalog_values() {
        return analog_values;
    }

    public synchronized void setAnalog(int i, double value) {
        analog_values[i] = value;
    }
    
    public synchronized double getAnalogValue(int i){
        return analog_values[i];
    }
    
    
}
