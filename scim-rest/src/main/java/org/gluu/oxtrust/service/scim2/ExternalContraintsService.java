package org.gluu.oxtrust.service.scim2;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.gluu.persist.model.base.Entry;
import org.gluu.oxtrust.service.external.ExternalScimService;
import org.gluu.oxtrust.service.external.OperationContext;
import org.gluu.oxtrust.ws.rs.scim2.BaseScimWebService;

@ApplicationScoped
public class ExternalContraintsService {

    @Inject
    ExternalScimService externalScimService;

    public Response applyEntityCheck(Entry entity, HttpHeaders httpHeaders, UriInfo uriInfo,
            String httpMethod, String resourceType) throws Exception {
        
        Response response = null;
        if (externalScimService.isEnabled()) {
            
            OperationContext ctx = new OperationContext();
            ctx.setBaseUri(uriInfo.getBaseUri());
            ctx.setMethod(httpMethod);
            ctx.setPath(uriInfo.getPath());
            ctx.setQueryParams(uriInfo.getQueryParameters());
            ctx.setRequestHeaders(httpHeaders.getRequestHeaders());
            
            if (!externalScimService.executeAllowResourceOperation(entity, ctx)) {
                String error = externalScimService.executeRejectedResourceOperationResponse(entity, ctx);
                response = BaseScimWebService.getErrorResponse(Status.FORBIDDEN, error);
            }
        }
        return response;
        
    }
    
}
