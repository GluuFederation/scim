package org.gluu.oxtrust.ws.rs.scim2;

import static org.gluu.oxtrust.model.scim2.Constants.MEDIA_TYPE_SCIM_JSON;
import static org.gluu.oxtrust.model.scim2.Constants.UTF8_CHARSET_FRAGMENT;

import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.gluu.oxtrust.model.scim2.Meta;
import org.gluu.oxtrust.model.scim2.provider.config.AuthenticationScheme;
import org.gluu.oxtrust.model.scim2.provider.config.ServiceProviderConfig;
import org.gluu.oxtrust.model.scim2.util.ScimResourceUtil;
import org.gluu.oxtrust.service.scim2.interceptor.RejectFilterParam;

/**
 * @author Rahat Ali Date: 05.08.2015
 * Updated by jgomer2001 on 2017-09-23
 */
@Named("serviceProviderConfig")
@Path("/scim/v2/ServiceProviderConfig")
public class ServiceProviderConfigWS extends BaseScimWebService {

    @GET
    @Produces(MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT)
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @RejectFilterParam
    public Response serve(){

        try {
            ServiceProviderConfig serviceProviderConfig = new ServiceProviderConfig();
            serviceProviderConfig.getFilter().setMaxResults(appConfiguration.getScimProperties().getMaxCount());

            Meta meta = new Meta();
            meta.setLocation(endpointUrl);
            meta.setResourceType(ScimResourceUtil.getType(serviceProviderConfig.getClass()));
            serviceProviderConfig.setMeta(meta);

            boolean onTestMode = appConfiguration.isScimTestMode();
            serviceProviderConfig.setAuthenticationSchemes(Arrays.asList(
                    AuthenticationScheme.createOAuth2(onTestMode), AuthenticationScheme.createUma(!onTestMode)));

            return Response.ok(resourceSerializer.serialize(serviceProviderConfig)).build();
        }
        catch (Exception e){
            log.error(e.getMessage(), e);
            return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }

    }

    @PostConstruct
    public void setup(){
        //Do not use getClass() here...
        init(ServiceProviderConfigWS.class);
    }

}
