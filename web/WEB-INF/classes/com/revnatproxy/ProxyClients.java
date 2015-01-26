package com.revnatproxy;

import java.util.Map;
import java.util.HashMap;
import javax.servlet.ServletContext;

public class ProxyClients{
    private HashMap<String, ProxyClient> clients_ = new HashMap<String, ProxyClient>();
    private int serialNO_ = 0;

    static public ProxyClients getInstance(ServletContext application){
        ProxyClients proxyClients = (ProxyClients)application.getAttribute("proxyClients");
        if(proxyClients == null){
            synchronized(application){
                proxyClients = (ProxyClients)application.getAttribute("proxyClients");
                if(proxyClients == null){
                    proxyClients = new ProxyClients();
                    application.setAttribute("proxyClients", proxyClients);
                }
            }
        }
        return proxyClients;
    }
    
    private int getSerialNo(){
        int serialNO;
        synchronized(this){
            serialNO = serialNO_;
            ++serialNO_;
        }
        return serialNO;
    }
    
    public ProxyClient getClient(String clientName){
        ProxyClient client;
        synchronized(clients_){
            client = clients_.get(clientName);
            if(client == null){
                client = new ProxyClient();
                clients_.put(clientName, client);
            }
        }
        return client;
    }
    
    public ProxyResponse sendRequest(String clientName, String method, String client, String path, String query, long milliseconds){
        int serialNo = getSerialNo();
        ProxyClient proxyClient = getClient(clientName);
        return proxyClient.sendRequest(serialNo, method, client, path, query, milliseconds);
    }
    
    
}
