package com.thed.jira.zauth.filter;

import com.atlassian.jira.bc.security.login.LoginService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.util.CookieUtils;
import com.atlassian.seraph.auth.DefaultAuthenticator;
import com.google.common.collect.Iterables;
import com.thed.jira.zauth.MultiReadHttpServletRequest;
import com.thed.jira.zauth.utils.JIRAUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ZAuthenticationFilter implements Filter {

    private static final Logger log = Logger.getLogger(ZAuthenticationFilter.class);
    private FilterConfig config;

    @Override
    public void init(FilterConfig config) throws ServletException {
        this.config = config;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if ((!(request instanceof HttpServletRequest)) || (!(response instanceof HttpServletResponse))) {
            if (log.isInfoEnabled())
                log.info("Ignoring non-HTTP requests. Sorry dont know what to do with such requests.");
            chain.doFilter(request, response);
            return;
        }
        String remoteAddr = getClientIpAddr((HttpServletRequest) request);
        if (log.isInfoEnabled())
            log.info(remoteAddr);
        if (!Iterables.contains(JIRAUtil.getWhiteList(), remoteAddr)) {
            log.warn("Requesting server [" + remoteAddr + "] is not in white list, ignoring it. This is NOT normal");
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest req = new MultiReadHttpServletRequest((HttpServletRequest) request);
        String url = req.getRequestURI();

        if (StringUtils.contains(url, "/rest/auth/latest/session")) {
            String loginBody = getBody(req);
            AuthParams creds = new ObjectMapper().readValue(loginBody, AuthParams.class);
            if (creds.username != null) {
                ApplicationUser user = ComponentAccessor.getUserManager().getUserByName(creds.username);
                if (user != null) {
                    ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(user);
                    ((HttpServletRequest)request).getSession().setAttribute(DefaultAuthenticator.LOGGED_IN_KEY, user);

                    final SessionInfo sessionInfo = new SessionInfo(CookieUtils.JSESSIONID, req.getSession().getId());
                    LoginService loginService = ComponentAccessor.getComponentOfType(LoginService.class);
                    final LoginInfo loginInfo = new LoginInfo(loginService.getLoginInfo(creds.username));
                    final AuthSuccess authSuccess = new AuthSuccess(sessionInfo, loginInfo);

                    response.setContentType(MediaType.APPLICATION_JSON);
                    ((HttpServletResponse) response).setStatus(Response.Status.OK.getStatusCode());
                    response.getOutputStream().write(new ObjectMapper().writeValueAsBytes(authSuccess));
                    return;
                }
            }
        } else {
//            InputStream inStream = req.getInputStream();
//            try {
//                SOAPEnvelope soap = new SOAPEnvelope(inStream);
//                RPCElement rpcElement = (RPCElement) (soap.getBodyElements().get(0));
//                String operation = rpcElement.getName();
//                String loginNameOrToken = ((MessageElement) rpcElement.getChildren().get(0)).getValue();
//
//                if (StringUtils.equalsIgnoreCase("login", operation)) {
//                    String res = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><soapenv:Body><ns1:loginResponse soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:ns1=\"http://soap.rpc.jira.atlassian.com\"><loginReturn xsi:type=\"xsd:string\">"
//                            + loginNameOrToken
//                            + "</loginReturn></ns1:loginResponse></soapenv:Body></soapenv:Envelope>\n";
//                    response.getOutputStream().write(res.getBytes(Charset.forName("UTF-8")));
//                    return;
//                }
//
//                ApplicationUser user = ComponentAccessor.getUserManager().getUserByName(loginNameOrToken);
//                ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(user);
//                log.info(operation + loginNameOrToken);
//            } catch (SAXException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
        }

        if (url != null) {
            chain.doFilter(req, response);
            return;
        }
    }

    private String getBody(HttpServletRequest request) throws IOException {

        String body = null;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            } else {
                stringBuilder.append("");
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }

        body = stringBuilder.toString();
        return body;
    }

    /**
     * @param request
     * @return
     */
    public String getClientIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    @Override
    public void destroy() {
        this.config = null;
    }


    /**
     *
     <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
     <soapenv:Body>
     <ns1:loginResponse soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" xmlns:ns1="http://soap.rpc.jira.atlassian.com">
     <loginReturn xsi:type="xsd:string">21f45cff3fa9981c944802f710d6ed2ca736d793</loginReturn>
     </ns1:loginResponse>
     </soapenv:Body>
     </soapenv:Envelope>

     HTTP/1.1 200 OK

     Server: Apache-Coyote/1.1

     X-AREQUESTID: 666x692x1

     Set-Cookie: atlassian.xsrf.token=B0Q8-ZMV7-4JWN-VC5R|36c7309174cb54145aa476ba5c10e104205f6c63|lout; Path=/jira

     X-AUSERNAME: anonymous

     Set-Cookie: JSESSIONID=2E21A04246D29BBF5F1697302F7777C1; Path=/

     X-Content-Type-Options: nosniff

     X-ASESSIONID: 1b0uvmh

     Content-Encoding: gzip

     Vary: User-Agent

     Content-Type: text/xml;charset=utf-8

     Content-Length: 261

     Date: Sat, 24 Aug 2013 15:06:46 GMT



     <?xml version="1.0" encoding="UTF-8"?><soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><soapenv:Body><ns1:loginResponse soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" xmlns:ns1="http://soap.rpc.jira.atlassian.com"><loginReturn xsi:type="xsd:string">trustedappstoken</loginReturn></ns1:loginResponse></soapenv:Body></soapenv:Envelope>

     */
}
