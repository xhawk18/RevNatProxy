# RevNat Proxy

##What is RevNat Proxy?

RevNat Proxy is a online reversed http proxy. Suppose that we have two websites --
 1. A local website behind NAT,
  * http://192.168.1.100:8080/myweb.html
 2. A public website on internat
  * http://my.public.address.com/jsp

With RevNat Proxy, users can visit the local website through online proxy address
  * http://my.public.address.com/jsp/clientname/http://192.168.1.100:8080/myweb.html

##How to setup?

 1. Deploy the files in folder "web" to tomcat server on the internat.
 
 If you've deployed the web to http://my.public.address.com/jsp, check this address
  * http://my.public.address.com/jsp/get_event.jsp

 and you'll see "error=parameters" if successful.

 2. Run the application localproxy.jar on a computer in local network.
 
 Please be sure that the computer that runs localproxy.jar can connect to local web site "http://192.168.1.100:8080/myweb.html".
 Then run this command to connect to public server.
  * java -jar localproxy.jar http://my.public.address.com/jsp/get_event.jsp clientname
 
 That's all, now you can visit the local web site behind NAT through public internet address - 
  * http://my.public.address.com/jsp/clientname/http://192.168.1.100:8080/myweb.html
