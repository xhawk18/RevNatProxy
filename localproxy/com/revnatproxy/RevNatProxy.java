package com.revnatproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class RevNatProxy{
    static final int HTTP_TIMEOUT_REMOTE_MS = 20000;
    static final int HTTP_TIMEOUT_LOCAL_MS = 10000;
    static Integer unique_id = null;
    
    public static void resetUid(){
        unique_id = (Integer)(int)Math.floor(Math.random() * Integer.MAX_VALUE);
    }
    
    private class FeedBack{
        Integer serialNo_;
        ProxyResponse proxyResponse_;
    }
    
    public ProxyResponse doURL(String method, String address, Bin postBinData, int timeoutMilliseconds) {
        ProxyResponse proxyResponse = null;
        URL url = null;
        HttpURLConnection conn = null;
        try {
            //android.util.Log.d("RevNatProxy", String.format("send %s: %s", method, address));
            
            if(postBinData != null){
                util.log("POST data");
            }

            method = method.toUpperCase();
            boolean isPost = method.equals("POST");
            
            url = new URL(address);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(timeoutMilliseconds);
            conn.setReadTimeout(timeoutMilliseconds);
            conn.setRequestMethod(method);
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            
            if(isPost){
                conn.setRequestProperty("Content-Length", 
                        (postBinData == null ? "0" : String.valueOf(postBinData.size())));  
                //set to send POST request
                conn.setDoOutput(true);
                conn.setDoInput(true);
            }
            conn.connect();

            if(isPost){
                OutputStream out = conn.getOutputStream();
                for(int i = 0; postBinData != null && i < postBinData.getBlockCount(); ++i){
                    out.write(postBinData.binData_.get(i), 0, postBinData.getBlockSize(i));
                }
                out.flush();
            }
            
            proxyResponse = new ProxyResponse();
            proxyResponse.code_ = conn.getResponseCode();
            proxyResponse.headers_ = conn.getHeaderFields();
            //android.util.Log.d("RevNatProxy", String.format("get %s with code %d", address, proxyResponse.code_));
            //if(proxyResponse.code_ == 200)
            {
                InputStream body = null;
                try{
                    body = conn.getInputStream();
                }catch(IOException ex){
                    body = conn.getErrorStream();
                }
                BufferedInputStream in = new BufferedInputStream(body);
                byte[] buffer = new byte[4096];
                int size;
                while ((size = in.read(buffer)) > 0){
                    proxyResponse.binData_.addBinData(buffer, 0, size);
                }
            }
        } catch (IOException e) {
            util.log("postURL: IO IOException");
            proxyResponse = new ProxyResponse();
            proxyResponse.code_ = 503;
            proxyResponse.binData_.setStrData(e.getMessage());
        } catch (Exception e){
            util.log("postURL: IOException");
            proxyResponse = new ProxyResponse();
            proxyResponse.code_ = 503;
            proxyResponse.binData_.setStrData(e.getMessage());            
        } finally {
            util.log("postURL: final");
            if (conn != null){
                conn.disconnect();
            }
            util.log("postURL: after final");
        }
        return proxyResponse;
    }
    
    FeedBack getEvent(String address, String client, Integer serial, ProxyResponse proxyResponse){
        String query = "uid=" + String.valueOf(unique_id) + "&client=" + util.urlEncode(client);
        if(serial != null && proxyResponse != null)
            query = query + "&serial=" + serial;
        util.log("getEvent: post: " + query);
        
        //Set POST binData
        //Set additional header
        Bin binData = null;
        
        if(proxyResponse != null){
            query += "&code=" + String.valueOf(proxyResponse.code_);
            binData = new Bin();
            String headers = "";
            for (Map.Entry<String, List<String>> entry : proxyResponse.headers_.entrySet()) {
                String name = entry.getKey();
                for(String value: entry.getValue()){
                    if(name != null){
                        headers += name;
                        headers += ": ";
                    }
                    headers += value;
                    headers += "\n";
                }
            }
            binData.addStrData(util.urlEncode(headers));
            binData.addStrData("\n");
            binData.addBinData(proxyResponse.binData_);
        }
        
        ProxyResponse param = doURL("POST", address + "?" + query, binData, HTTP_TIMEOUT_REMOTE_MS);
        if(param != null && param.code_ == 200)
            return sendRequest(client, param.binData_.toString());
        else
            return null;
    }

    FeedBack sendRequest(String client, String param){
        Integer serialNo = null;
        String method = "GET";
        //String server = null;
        String path = null;
        String query = "";

        String[] params = param.split("&");
        for(int i = 0; i < params.length; ++i){
            String[] a = params[i].split("=", 2);

            if(a[0].equals("serial")) try{
                serialNo = Integer.parseInt(a[1], 10);
            }catch(NumberFormatException ex){serialNo = null;}
            //else if(a[0].equals("server")) server = util.urlDecode(a[1]);
            else if(a[0].equals("path")) path = util.urlDecode(a[1]);
            else if(a[0].equals("method")) method = util.urlDecode(a[1]).toUpperCase();
            else if(a[0].equals("query")) query = util.urlDecode(a[1]);
        }

        if(path == null || serialNo == null){
            return null;
        }

        util.log("sendRequest: post: " + query);
        
        //if(server == null)
        //    server = "http://127.0.0.1";
        //if(server.endsWith("/")) server = server.substring(0, server.length() - 1);
        //String address = (path.startsWith("/") ? server + path : server + "/" + path);
        if(path.startsWith("//"))
            path = "http:" + path;
        else if(path.startsWith(":"))
            path = "http://127.0.0.1" + path;
        else if(path.startsWith("/"))
            path = "http://127.0.0.1" + path;
        else if(path.matches("^[^:/.]+:.*"))
            ;   //like http://192.168.1.100 ...
        else if(path.matches("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+(:[0-9]+)?/.*")){
            //like 192.168.1.100:8080/... 
            //or   192.168.1.100/...
            path = "http://" + path;
        }
        else
            path = "http://127.0.0.1/" + path;
        String address = path;

        Bin binData = null;
        if(method.equals("POST")){
            binData = new Bin();
            binData.setStrData(query);
        }
        else{
            if(query.length() > 0)
            address += "?" + query;
        }
        
        FeedBack feedback = new FeedBack();
        feedback.proxyResponse_ = doURL(method, address, binData, HTTP_TIMEOUT_LOCAL_MS);
        feedback.serialNo_ = serialNo;
        return feedback;
    }

    Thread thread_ = null;
    public synchronized void startThread(final String serverUrl, final String clientName){
        if(thread_ != null)
            return;
        
        thread_ = new Thread(){
            @Override
            public void run(){
                Integer serialNo = null;
                ProxyResponse proxyResponse = null;
                while(thread_ != null){
                    FeedBack feedBack = getEvent(serverUrl, clientName, serialNo, proxyResponse);
                    if(feedBack != null){
                        serialNo = feedBack.serialNo_;
                        proxyResponse = feedBack.proxyResponse_;
                    }
                }
            }
        };
        thread_.start();
    }
    
    synchronized void set_thread_null(boolean interrupt){
        if(thread_ != null){
            if(interrupt)
                thread_.interrupt();
            thread_ = null;
        }
    }

    public void stopThread(){
        set_thread_null(true);
    }
    
    public static void main(String args[]){
        if(args.length != 2 && args.length != 3){
            String jarName = "localproxy.jar";
            try{
                jarName = new java.io.File(RevNatProxy.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getName();
            }catch(Exception ex){}
            System.err.println(String.format("Usage: java -jar %s <proxy_address> <client_name> [count]", jarName));
            System.err.println(" proxy_address - URL address of the proxy server.");
            System.err.println(" client_name   - name of this client.");
            System.err.println(" count         - Number of threads used, default is 1.");
            return;
        }

        String serverUrl = null;
        String clientName = null;
        int count = 1;
        if(args.length >= 2){
            serverUrl = args[0];
            clientName = args[1];
        }
        if(args.length >= 3){
            count = Integer.parseInt(args[2], 10);
        }
        
        System.out.println(clientName + " connect to " + serverUrl);
        RevNatProxy[] revnatProxy = new RevNatProxy[count];
        
        RevNatProxy.resetUid();
        for(int i = 0; i < count; ++i){
            revnatProxy[i] = new RevNatProxy();
            revnatProxy[i].startThread(serverUrl, clientName);
        }
        
        System.out.println("Press enter key to stop RevNat Proxy client ...");
        try{ System.in.read(); } catch(IOException ex){}
        
        for(int i = 0; i < count; ++i)
            revnatProxy[i].stopThread();
        System.exit(0);
    }
}
