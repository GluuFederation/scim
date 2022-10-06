package org.gluu.oxtrust.service.scim2;

import java.util.Optional;
import java.util.HashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;

import org.gluu.config.oxtrust.ScimMode;
import org.gluu.oxtrust.model.scim2.SearchRequest;
import org.gluu.oxtrust.service.external.ExternalScimService;
import org.gluu.oxtrust.service.external.OperationContext;
import org.gluu.oxtrust.service.external.TokenDetails;
import org.gluu.oxtrust.service.JsonConfigurationService;
import org.gluu.persist.model.base.Entry;
import org.gluu.persist.PersistenceEntryManager;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class ExternalConstraintsService {
    
    private static final String TOKENS_DN = "ou=tokens,o=gluu";

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    private JsonConfigurationService jsonConfigurationService;

    @Inject
    ExternalScimService externalScimService;

    public Response applyEntityCheck(Entry entity, Object payload, HttpHeaders httpHeaders,
            UriInfo uriInfo, String httpMethod, String resourceType) throws Exception {
        
        Response response = null;
        if (externalScimService.isEnabled()) {
            OperationContext ctx = makeContext(httpHeaders, uriInfo, httpMethod, resourceType);
            response = externalScimService.executeManageResourceOperation(entity, payload, ctx);
        }
        return response;
        
    }

    public Response applySearchCheck(SearchRequest searchReq, HttpHeaders httpHeaders, 
            UriInfo uriInfo, String httpMethod, String resourceType) throws Exception {
        
        Response response = null;        
        if (externalScimService.isEnabled()) {

            OperationContext ctx = makeContext(httpHeaders, uriInfo, httpMethod, resourceType);
            response = externalScimService.executeManageSearchOperation(searchReq, ctx);
            
            if (response == null) {
                String filterPrepend = ctx.getFilterPrepend();

                if (!StringUtils.isEmpty(filterPrepend)) {
                    if (StringUtils.isEmpty(searchReq.getFilter())) {
                        searchReq.setFilter(filterPrepend);
                    } else {
                        searchReq.setFilter(String.format("%s and (%s)", filterPrepend, searchReq.getFilter()));
                    }
                }
            }
        }
        return response;

    }
      
    private OperationContext makeContext(HttpHeaders httpHeaders, UriInfo uriInfo,
            String httpMethod, String resourceType) {

        OperationContext ctx = new OperationContext();
        ctx.setBaseUri(uriInfo.getBaseUri());
        ctx.setMethod(httpMethod);
        ctx.setResourceType(resourceType);
        ctx.setPath(uriInfo.getPath());
        ctx.setQueryParams(uriInfo.getQueryParameters());
        ctx.setRequestHeaders(httpHeaders.getRequestHeaders());
        
        ScimMode mode = jsonConfigurationService.getOxTrustappConfiguration().getScimProperties()
                .getProtectionMode();

        if (!ScimMode.BYPASS.equals(mode)) {        
            String token = Optional.ofNullable(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION))
                    .map(authz -> authz.replaceFirst("Bearer\\s+", "")).orElse(null);

            TokenDetails details = getDatabaseToken(token, mode);
            if (details == null) {
                log.warn("Unable to get token details");
                details = new TokenDetails();
            }

            details.setValue(token);
            ctx.setTokenDetails(details);
        }
        return ctx;

    }
    
    private TokenDetails getDatabaseToken(String token, ScimMode mode) {
        
        String hashedToken = token.startsWith("{sha256Hex}") ? token : DigestUtils.sha256Hex(token);
        try {
            String dn = ScimMode.UMA.equals(mode) ? "ou=uma_rpt," : "";
            dn = String.format("tknCde=%s,%s", hashedToken, dn + TOKENS_DN);

            return entryManager.find(TokenDetails.class, dn);
        } catch (Exception e) {
            log.warn(e.getMessage());
            return null;
        }
        
    }
    
}
