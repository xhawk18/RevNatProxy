package com.revnatproxy;

import java.net.URLEncoder;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;

public class util{
    public static void log(String s){
        //System.out.print(s);
        //System.out.flush();
    }
    
    public static String urlEncode(String s){
        try{
            String result = URLEncoder.encode(s, "UTF-8");
            return result;
        }catch(UnsupportedEncodingException ex){
            return "";
        }
    }
    public static String urlDecode(String s){
        try{
            String result = URLDecoder.decode(s, "UTF-8");
            return result;
        }catch(UnsupportedEncodingException ex){
            return "";
        }
    }
    
    public static String getParameter(String query, String name){
        name += '=';
        int pos = query.indexOf(name);
        if(pos == 0 || (pos > 0 && query.charAt(pos - 1) == '&')){
            pos += name.length();
            int end = query.indexOf('&', pos);
            String value = (end == -1 ? query.substring(pos) : query.substring(pos, end));
            return urlDecode(value);
        }
        else
            return null;
    }
}
