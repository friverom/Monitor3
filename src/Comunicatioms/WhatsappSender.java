/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Comunicatioms;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WhatsappSender {
    
    private static final String INSTANCE_ID = "15";
    private static final String CLIENT_ID = "friverom@gmail.com";
    private static final String CLIENT_SECRET = "5987e0a07c6d443aa5b3a62d1ed2fb9d";
    private static final String WA_GATEWAY_URL1 = "http://api.whatsmate.net/v3/whatsapp/single/text/message/" + INSTANCE_ID;
    private static final String WA_GATEWAY_URL2 = "http://api.whatsmate.net/v3/whatsapp/group/text/message/" + INSTANCE_ID;
    
   
    /**
     *
     * Sends out a WhatsApp message via WhatsMate WA Gateway.
     *
     */
    public void sendMessage(String number, String message) throws Exception {
        
        String jsonPayload = new StringBuilder()
                .append("{")
                .append("\"number\":\"")
                .append(number)
                .append("\",")
                .append("\"message\":\"")
                .append(message)
                .append("\"")
                .append("}")
                .toString();

        URL url = new URL(WA_GATEWAY_URL1);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("X-WM-CLIENT-ID", CLIENT_ID);
        conn.setRequestProperty("X-WM-CLIENT-SECRET", CLIENT_SECRET);
        conn.setRequestProperty("Content-Type", "application/json");

        OutputStream os = conn.getOutputStream();
        os.write(jsonPayload.getBytes());
        os.flush();
        os.close();

        int statusCode = conn.getResponseCode();
      /*  System.out.println("Response from WA Gateway: \n");
        System.out.println("Status Code: " + statusCode); */
        BufferedReader br = new BufferedReader(new InputStreamReader(
                (statusCode == 200) ? conn.getInputStream() : conn.getErrorStream()
        ));

        String output;
        while ((output = br.readLine()) != null) {
          //  System.out.println(output);
          output=output;
        }
        conn.disconnect();
    }
    
     /**

   * Sends out a WhatsApp message to a group

   */

  public void sendGroupMessage(String groupAdmin, String groupName, String message) throws Exception {

    // TODO: Should have used a 3rd party library to make a JSON string from an object

    String jsonPayload = new StringBuilder()
      .append("{")
      .append("\"group_admin\":\"")
      .append(groupAdmin)
      .append("\",")
      .append("\"group_name\":\"")
      .append(groupName)
      .append("\",")
      .append("\"message\":\"")
      .append(message)
      .append("\"")
      .append("}")
      .toString();

    URL url = new URL(WA_GATEWAY_URL2);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setDoOutput(true);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("X-WM-CLIENT-ID", CLIENT_ID);
    conn.setRequestProperty("X-WM-CLIENT-SECRET", CLIENT_SECRET);
    conn.setRequestProperty("Content-Type", "application/json");

    OutputStream os = conn.getOutputStream();
    os.write(jsonPayload.getBytes());
    os.flush();
    os.close();

    int statusCode = conn.getResponseCode();

   /* System.out.println("Response from WA Gateway: \n");
    System.out.println("Status Code: " + statusCode);*/

    BufferedReader br = new BufferedReader(new InputStreamReader(
        (statusCode == 200) ? conn.getInputStream() : conn.getErrorStream()
      ));
    String output;
    while ((output = br.readLine()) != null) {
      //  System.out.println(output);
      output=output;
    }
    conn.disconnect();
  }
}
