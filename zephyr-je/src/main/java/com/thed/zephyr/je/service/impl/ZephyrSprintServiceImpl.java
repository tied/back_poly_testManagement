package com.thed.zephyr.je.service.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.greenhopper.service.sprint.Sprint;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.base.Optional;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.thed.zephyr.je.service.ZephyrSprintService;
import com.thed.zephyr.je.vo.SprintBean;
import com.thed.zephyr.util.ZephyrComponentAccessor;
import com.thed.zephyr.util.optionalservice.GHSprintService;
import com.thed.zephyr.util.optionalservice.ServiceAccessor;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * Created by smangal on 9/29/15.
 */
public class ZephyrSprintServiceImpl extends BaseManagerImpl  implements ZephyrSprintService {
    private static final Logger log = Logger.getLogger(ZephyrSprintServiceImpl.class);
    private final ServiceAccessor optionalServiceAccessor;

    public ZephyrSprintServiceImpl(ActiveObjects ao, ServiceAccessor optionalServiceAccessor) {
        super(ao);
        this.optionalServiceAccessor = optionalServiceAccessor;
    }

    @Override
    public Optional<SprintBean> getSprint(Long sprintId) {
//        return fetchSprintBean(sprintId);
        return fetchSprintBeanLocally(sprintId);

    }
    
    private Optional<SprintBean> fetchSprintBeanLocally(Long springId) {
        try {
            GHSprintService sprintService = optionalServiceAccessor.getSprintService();
            if(sprintService != null){
                ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
                Optional<Sprint> sprintOption = sprintService.getSprint(user, springId);
                if(sprintOption.isPresent()) {
                    Sprint sprint = sprintOption.get();
                    SprintBean sb = new SprintBean();
                    sb.setId(sprint.getId());
                    sb.setName(sprint.getName());
                    sb.setCompleteDate(sprint.getCompleteDate());
                    sb.setStartDate(sprint.getStartDate());
                    sb.setEndDate(sprint.getEndDate());
                    sb.setState(sprint.getState().name());
                    return Optional.of(sb);
                }
            }
        } catch(Exception e) {
            log.error("Error retrieving sprint information.", e);
        }
        return Optional.absent();
    }

    private void initSSL() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){
            public X509Certificate[] getAcceptedIssuers(){return null;}
            public void checkClientTrusted(X509Certificate[] certs, String authType){}
            public void checkServerTrusted(X509Certificate[] certs, String authType){}
        }};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {}

    }
}
