package com.revnatproxy;

import java.util.HashMap;
import java.util.ArrayList;
import java.lang.System;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

public class ProxyResponse{
    public Map<String, List<String>> headers_;
    public int code_;
    public Bin binData_;
    
    public ProxyResponse(){
        headers_ = new HashMap<String, List<String>>();
        binData_ = new Bin();
    }
}
