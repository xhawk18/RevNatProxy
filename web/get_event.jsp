<%@ page
    import="java.util.Vector"
    import="com.revnatproxy.util"
    import="com.revnatproxy.Bin"
    import="com.revnatproxy.ProxyCommand"
    import="com.revnatproxy.ProxyClient"
    import="com.revnatproxy.ProxyClients"
    import="com.revnatproxy.ProxyResponse"
    import="java.io.InputStream"
    import="java.io.BufferedInputStream"
    contentType="text/plain;charset=UTF-8" %><%

    ProxyResponse proxyResponse = new ProxyResponse();
    proxyResponse.code_ = 200;
    
    if(request.getMethod().toUpperCase().equals("POST")){
        InputStream body = request.getInputStream();
        BufferedInputStream in = new BufferedInputStream(body);
        
        byte[] buffer = new byte[Bin.BLOCK_SIZE];
        while(true){
            int size = in.read(buffer, 0, buffer.length);
            if(size <= 0) break;
            proxyResponse.binData_.addBinData(buffer, 0, size);
        }
    }

    String query = request.getQueryString();
    if(query == null) query = "";
    String clientName = util.getParameter(query, "client");
    String uid = util.getParameter(query, "uid");
    String serialNo = util.getParameter(query, "serial");
    String code = util.getParameter(query, "code");
    if(code != null){
        try{
            proxyResponse.code_ = Integer.parseInt(code, 10);
        }catch(NumberFormatException ex){}
    }
    
    if(clientName == null){
        %>error=parameters<%
        return;
    }

    ProxyClients proxyClients = ProxyClients.getInstance(application);
    ProxyClient proxyClient = proxyClients.getClient(clientName);
    
    //Send reponse data to proxy client
    if(serialNo != null){
        try{
            int iSerialNo = Integer.parseInt(serialNo, 10);
            proxyClient.feedbackRequest(iSerialNo, proxyResponse);
        }catch(NumberFormatException ex){}
    }
    
    //Wait new command
    ProxyCommand proxyCommand = proxyClient.waitPopRequest(uid, 15000);   //Wait at most 15 seconds 
    if(proxyCommand != null){
        String command = String.format("serial=%d&method=%s&path=%s",
            proxyCommand.serialNo_,
            proxyCommand.method_,
            util.urlEncode(proxyCommand.path_));
        if(proxyCommand.query_ != null)
            command += "&query=" + util.urlEncode(proxyCommand.query_);
        %><%= command %><%
    }
    else{
        %>error=timeout<%
    }
%>