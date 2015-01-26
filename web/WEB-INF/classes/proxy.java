/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.ArrayList;
import com.revnatproxy.ProxyClients;
import com.revnatproxy.ProxyResponse;
import com.revnatproxy.Bin;
import com.revnatproxy.util;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletContext;

/**
 * Example servlet showing request information.
 *
 * @author James Duncan Davidson <duncan@eng.sun.com>
 */

public class proxy extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        ServletContext application = getServletConfig().getServletContext();
        String contextPath = request.getContextPath();
        String servletPath = request.getServletPath();
        String uri = request.getRequestURI();
        String fullPath = uri.substring(contextPath.length() + servletPath.length());
        
        if(fullPath.startsWith("/"))
            fullPath = fullPath.substring(1);
        
        String client = null;
        String path = null;

        int index = fullPath.indexOf("/");
        if(index != -1){
            client = fullPath.substring(0, index);
            path = fullPath.substring(index + 1);
            path = util.urlDecode(path);
        }
        
        if(client == null || path == null){
            response.addHeader("Content-Type", "text/plain;charset=UTF-8");
            response.getOutputStream().write(("Usage: " + contextPath + servletPath + "/<client>/<url>").getBytes());
            return;
        }

        String method = request.getMethod();
        String query = null;
        if(method.toUpperCase().equals("POST")){
            Map<String, String[]> requestParameters = request.getParameterMap();
            if(requestParameters != null){
                for(String name: requestParameters.keySet()){
                    String[] values = requestParameters.get(name);
                    for(int i = 0; i < values.length; ++i){
                        String s = util.urlEncode(name) + "=" + util.urlEncode(values[i]);
                        if(query == null) query = s;
                        else query += "&" + s;
                    }
                }
            }
        }
        else
            query = request.getQueryString();

        ProxyClients proxyClients = ProxyClients.getInstance(application);
        ProxyResponse result = proxyClients.sendRequest(client, method, client, path, query, 15000);        

        if(result == null){
            response.addHeader("Content-Type", "text/plain;charset=UTF-8");
            return;
        }

        response.setStatus(result.code_);
        
        ArrayList<String> headers = new ArrayList<String>();
        byte[] buffer = new byte[8192]; //Large enough for header
        int bufferSize = 0;

        int headerSize = 0;
        int headerEndI = 0;
        int headerEndJ = 0;
        boolean foundBody = false;
        for(int i = 0; !foundBody && i < result.binData_.getBlockCount(); ++i){
            byte[] bytes = result.binData_.getBlock(i);
            int size = result.binData_.getBlockSize(i);
            for(int j = 0; j < size; ++j){
                if(bytes[j] == '\n'){
                    headerEndI = i;
                    headerEndJ = j;
                    foundBody = true;
                    break;
                }
            }
            
            if(foundBody) headerSize += headerEndJ + 1;
            else headerEndJ += size;
            
            int copySize = size;
            if(foundBody) copySize = headerEndJ;
            if(copySize > buffer.length - bufferSize)
                copySize = buffer.length - bufferSize;
            System.arraycopy(bytes, 0, buffer, bufferSize, copySize);
            bufferSize += copySize;
        }
        
        //Fill headers
        boolean foundContextType = false;
        boolean foundContextLength = false;
        boolean foundContextDisposition = false;
        
        String header = new String(buffer, 0, bufferSize, "UTF-8");
        header = util.urlDecode(header);
        String lines[] = header.split("\n");
        
        for(int i = 0; i < lines.length; ++i){
            String[] values = lines[i].split(": ", 2);
            if(values.length == 2){
                String name = values[0].toUpperCase();
                if(name.equals("CONTENT-LENGTH")){
                    //foundContextLength = true;
                    continue;
                }
                else if(name.equals("TRANSFER-ENCODING")){
                    continue;
                }
                else if(name.equals("CONTENT-TYPE"))
                    foundContextType = true;
                else if(name.equals("CONTENT-DISPOSITION"))
                    foundContextDisposition = true;
                response.addHeader(values[0], values[1]);
            }
        }

        if(!foundContextType)
            response.addHeader("Content-Type", "text/plain;charset=UTF-8");
        if(!foundContextLength){
            int contentLength = result.binData_.size() - headerSize;
            response.setContentLength(contentLength);
        }
        if(!foundContextDisposition){
            if(path != null){
                index = path.lastIndexOf("/");
                String fileName = path.substring(index + 1);
                response.addHeader("Content-Disposition", "inline; filename=" + fileName);
            }
        }

        ServletOutputStream os = response.getOutputStream();
        for(int i = headerEndI; i < result.binData_.getBlockCount(); ++i){
            byte[] bytes = result.binData_.getBlock(i);
            int size = result.binData_.getBlockSize(i);
            int j = (i == headerEndI ? headerEndJ + 1 : 0);
            int copySize = (j < size ? size - j : 0);
            os.write(bytes, j, copySize);
        }

        //os.write(header.getBytes());
        //result.binData_.addStrData(String.format("\n%d,%d", headerSize, result.binData_.size()));        
    }
    
    @Override
    public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        doGet(request, response);
    }
}

