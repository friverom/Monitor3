package Common;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.util.Calendar;

/**
 *
 * @author frive
 */
public class SunsetCalculator {

    private double longitud;
    private double latitud;
    private final double zenith=90.83333;
    
    private Calendar date;
    private int year;
    private int month;
    private int day;
    private int localOffset=0;
    
    public void setDate(Calendar date){
        this.date=date;
        year=date.get(Calendar.YEAR);
        month=date.get(Calendar.MONTH)+1;
        day=date.get(Calendar.DATE);
        localOffset=date.get(Calendar.ZONE_OFFSET)/(1000*3600);
    }
    public void setDate(int y, int m, int d, int l){
        year=y;
        month=m;
        day=d;
        localOffset=l;
    }
    
    public void setLocation(double lat, double lon){
        longitud=lon;
        latitud=lat;
    }

    public Calendar getSunset(Calendar d){
        
        this.setDate(d);
        //1.- Calculate day of year
        double N1=Math.floor(275*month/9);
        double N2=Math.floor((month+9)/12);
        double N3=(1+Math.floor((year-4*Math.floor(year/4)+2)/3));
        double N=N1-(N2*N3)+day-30;
        
        //2.- Convert Longitud to hour value and calculate aprox time
        double lngHour=longitud/15;
        double t=N+((18-lngHour)/24);
        
        //3.-Calculate Sun mean anomally
        double M=(0.9856*t)-3.289;
        
        //4.-Calculate Sun true longitud
        double L=M+(1.916*Math.sin(Math.toRadians(M)))+(0.020*Math.sin(Math.toRadians(2*M)))+282.634;
        if(L>360)
            L=L-360;
        else if(L<0)
            L=L+360;
        
        //5a.-Calculate Sun Ascension
        double RA=Math.atan(0.91764*Math.tan(Math.toRadians(L)));
        RA=Math.toDegrees(RA);
        if(RA>360)
            RA=RA-360;
        else if(RA<0)
            RA=RA+360;
        
        //5b.-Right ascension must be in the same quadrant as L
        double Lquadrant=Math.floor(L/90)*90;
        double RAquadrant=Math.floor(RA/90)*90;
        RA=RA+(Lquadrant-RAquadrant);
        
        //5c.- Right ascension value must be converted to hours
        RA=RA/15;
        
        //6.-Calculate Sun declination
        double sinDec=0.39782*Math.sin(Math.toRadians(L));
        double cosDec=Math.cos(Math.asin(sinDec));
        
        //7a.-Calculate Sun hour angle
        double cosH=(Math.cos(Math.toRadians(zenith))-(sinDec*Math.sin(Math.toRadians(latitud))));
        cosH=cosH/(cosDec*Math.cos(Math.toRadians(latitud)));
       /* if(Math.abs(cosH)>1)
            return "-1"; //Sun never sets or rise in this location*/
        
        //7b.-Finish calculating H and convert into hours
        double H=Math.toDegrees(Math.acos(cosH));
        H=H/15;
        
        //8 Calculate local mean time of setting
        double T=H+RA-.06571*t-6.622;
        
        //9 adjust back to UTC
        double UT=T-lngHour;
        if(UT>24)
            UT=UT-24;
        else if(UT<0)
            UT=UT+24;
        
        //10.-Convert UT to local Time
        double localT=UT+localOffset;
        if(localT>24)
            localT=localT-24;
        else if(localT<0)
            localT=localT+24;
        
        
      //  return date;
     //   System.out.println("Sunset: "+localT);
        //System.out.println("Sunset "+localT);
        date=this.convertDate(localT);
       /* System.out.println("Date from class "+date.getTime()+" Time "+localT);
        String s;
        s = localtoString(localT);*/
        return date; 
    }
    
    public Calendar getSunrise(Calendar d){
        this.setDate(d);
        //1.- Calculate day of year
        double N1=Math.floor(275*month/9);
        double N2=Math.floor((month+9)/12);
        double N3=(1+Math.floor((year-4*Math.floor(year/4)+2)/3));
        double N=N1-(N2*N3)+day-30;
        
        //2.- Convert Longitud to hour value and calculate aprox time
        double lngHour=longitud/15;
        double t=N+((6-lngHour)/24);
        
        //3.-Calculate Sun mean anomally
        double M=(0.9856*t)-3.289;
        
        //4.-Calculate Sun true longitud
        double L=M+(1.916*Math.sin(Math.toRadians(M)))+(0.020*Math.sin(Math.toRadians(2*M)))+282.634;
        if(L>360)
            L=L-360;
        else if(L<0)
            L=L+360;
        
        //5a.-Calculate Sun Ascension
        double RA=Math.atan(0.91764*Math.tan(Math.toRadians(L)));
        RA=Math.toDegrees(RA);
        if(RA>360)
            RA=RA-360;
        else if(RA<0)
            RA=RA+360;
        
        //5b.-Right ascension must be in the same quadrant as L
        double Lquadrant=Math.floor(L/90)*90;
        double RAquadrant=Math.floor(RA/90)*90;
        RA=RA+(Lquadrant-RAquadrant);
        
        //5c.- Right ascension value must be converted to hours
        RA=RA/15;
        
        //6.-Calculate Sun declination
        double sinDec=0.39782*Math.sin(Math.toRadians(L));
        double cosDec=Math.cos(Math.asin(sinDec));
        
        //7a.-Calculate Sun hour angle
        double cosH=(Math.cos(Math.toRadians(zenith))-(sinDec*Math.sin(Math.toRadians(latitud))));
        cosH=cosH/(cosDec*Math.cos(Math.toRadians(latitud)));
     /*   if(Math.abs(cosH)>1)
            return "-1"; //Sun never sets or rise in this location*/
        
        //7b.-Finish calculating H and convert into hours
        double H=360-Math.toDegrees(Math.acos(cosH));
        H=H/15;
        
        //8 Calculate local mean time of setting
        double T=H+RA-.06571*t-6.622;
        
        //9 adjust back to UTC
        double UT=T-lngHour;
        if(UT>24)
            UT=UT-24;
        else if(UT<0)
            UT=UT+24;
        
        //10.-Convert UT to local Time
       double localT=UT+localOffset;
        
       if(localT>24)
            localT=localT-24;
        else if(localT<0)
            localT=localT+24;
        
      //  System.out.println("Sunrise: "+localT);
      
        date=this.convertDate(localT);
        /* String s;
        s=localtoString(localT);*/
        return date;
        
    }
    
    private Calendar convertDate(double time){
        
        double t=time-0;
        double hour=Math.floor(t);
        double dMin=(time-hour)*60;
        double min=Math.floor(dMin);
        double dSec=dMin-min;
        double secs=Math.floor(dSec*60);
        
        date.set(Calendar.HOUR_OF_DAY, (int) hour);
        date.set(Calendar.MINUTE, (int) min);
        date.set(Calendar.SECOND,(int) secs);
        
        return date;
    }
    public String localtoString(double ltime){
        
        double h=Math.floor(ltime);
        double m=Math.floor((ltime-h)*60);
        
        String s;
        if(h<10)
            s="0"+(int)h;
        else
            s=""+(int)h;
        
        if(m<10)
            s=s+":0"+(int)m;
        else
            s=s+":"+(int)m;
        
        return s;
    }
        
    
}
