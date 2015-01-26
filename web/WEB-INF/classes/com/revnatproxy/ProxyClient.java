package com.revnatproxy;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class ProxyClient{
    public String uid_ = "";
    private HashMap<Integer, ProxyCommand> pendCommands_ = new HashMap<Integer, ProxyCommand>();
    private HashMap<Integer, ProxyCommand> sentCommands_ = new HashMap<Integer, ProxyCommand>();

    public ProxyResponse sendRequest(int serialNo, String method, String client, String path, String query, long milliseconds){
        ProxyCommand proxyCommand = new ProxyCommand();
        proxyCommand.serialNo_ = serialNo;
        proxyCommand.method_ = method;
        proxyCommand.client_ = client;
        proxyCommand.path_ = path;
        proxyCommand.query_ = query;
        proxyCommand.result_ = null;

        synchronized(pendCommands_){
            pendCommands_.put(serialNo, proxyCommand);
            pendCommands_.notifyAll();
        }

        synchronized(proxyCommand){
            //Wait response from client
            if(proxyCommand.result_ == null){
                try{
                    //util.log("ProxyClient: before wait\n");
                    //util.log(String.format("pendCommands_.size() = %d %d\n", pendCommands_.size(), pendCommands_.hashCode()));
                    proxyCommand.wait(milliseconds);
                    //util.log(String.format("2pendCommands_.size() = %d %d\n", pendCommands_.size(), pendCommands_.hashCode()));
                }catch(InterruptedException ex){
                    proxyCommand.result_ = new ProxyResponse();
                    proxyCommand.result_.binData_.setStrData("\nInternal Server Error");
                    proxyCommand.result_.code_ = 500;
                }
            }
            
            if(proxyCommand.result_ == null){
                proxyCommand.result_ = new ProxyResponse();
                proxyCommand.result_.binData_.setStrData(String.format("\nRequest Timeout (%d milliseconds)", milliseconds));
                proxyCommand.result_.code_ = 408;
            }

            //util.log(String.format("3pendCommands_.size() = %d %d\n", pendCommands_.size(), pendCommands_.hashCode()));
        }
        
        //If the command is not accepted by any client, remove it anyway.
        synchronized(pendCommands_){
            //util.log(String.format("pendCommands_.remove\n")); 
            pendCommands_.remove(serialNo);
        }
        synchronized(sentCommands_){
            sentCommands_.remove(serialNo);
        }
        return proxyCommand.result_;
    }
    
    public void feedbackRequest(int serialNo, ProxyResponse result){
        ProxyCommand proxyCommand;
        synchronized(sentCommands_){
            proxyCommand = (ProxyCommand)sentCommands_.get(serialNo);
            if(proxyCommand != null)
                sentCommands_.remove(serialNo);
        }

        if(proxyCommand != null){
            synchronized(proxyCommand){
                proxyCommand.result_ = result;
                proxyCommand.notify();
            }
        }
    }
    
    public ProxyCommand waitPopRequest(String uid, long milliseconds){
        boolean restorePendings = false;
        synchronized(uid_){
            if(uid == null) return null;
            restorePendings = !uid_.equals(uid);            
            uid_ = uid;
        }

        ProxyCommand proxyCommand;
        synchronized(pendCommands_){
            if(restorePendings){
                util.log("Restore pending command due to client uid changed\n");
                synchronized(sentCommands_){
                    //Move all sentCommands_ to pendCommands_
                    for(Integer serialNo: sentCommands_.keySet()){
                        pendCommands_.put(serialNo, sentCommands_.get(serialNo));
                    }
                    sentCommands_.clear();
                }
            }
            
            //util.log(String.format("4pendCommands_.size() = %d %d, uid = %s\n", pendCommands_.size(), pendCommands_.hashCode(), uid));
            if(pendCommands_.size() == 0){
                try{
                    //util.log("waitPopRequest: before wait\n");
                    pendCommands_.wait(milliseconds);
                    //util.log(String.format("5pendCommands_.size() = %d %d, uid = %s\n", pendCommands_.size(), pendCommands_.hashCode(), uid));
                }catch(InterruptedException ex){}
            }
            if(pendCommands_.size() == 0)
                return null;
            synchronized(uid_){
                if(!uid_.equals(uid)){
                    util.log(String.format("drop uid = %s, new uid = %s\n", uid, uid_));
                    return null;
                }
            }

            Set<Integer> keys = pendCommands_.keySet();
            Integer serialNo = keys.iterator().next();
            proxyCommand = pendCommands_.get(serialNo);
            pendCommands_.remove(serialNo);
            //util.log(String.format("3pendCommands_.remove, proxyCommand = "+(proxyCommand == null ? "null" : "ok")+"\n")); 
        }
        
        synchronized(sentCommands_){
            sentCommands_.put(proxyCommand.serialNo_, proxyCommand);
        }
        return proxyCommand;
    }

}
