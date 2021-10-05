package org.gluu.scim.ws.rs;

import java.util.Arrays;
import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gluu.scim.ScimConfiguration;
import org.gluu.oxtrust.ws.rs.scim2.BulkWebService;
import org.gluu.oxtrust.ws.rs.scim2.FidoDeviceWebService;
import org.gluu.oxtrust.ws.rs.scim2.Fido2DeviceWebService;
import org.gluu.oxtrust.ws.rs.scim2.GroupWebService;
import org.gluu.oxtrust.ws.rs.scim2.ResourceTypeWS;
import org.gluu.oxtrust.ws.rs.scim2.SchemaWebService;
import org.gluu.oxtrust.ws.rs.scim2.ServiceProviderConfigWS;
import org.gluu.oxtrust.ws.rs.scim2.UserWebService;
import org.gluu.service.JsonService;
import org.slf4j.Logger;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * This class implements the endpoint at which the requester can obtain SCIM metadata configuration. Similar to the SCIM
 * /ServiceProviderConfig endpoint
 */
@ApplicationScoped
@Path("/scim/scim-configuration")
@Api(value = "/.well-known/scim-configuration", description = "The SCIM server endpoint that provides configuration data. ")
public class ScimConfigurationWS {

    @Inject
    private Logger log;

    @Inject
    private JsonService jsonService;

    @Inject
    private UserWebService userService;

    @Inject
    private GroupWebService groupService;

    @Inject
    private FidoDeviceWebService fidoService;

    @Inject
    private Fido2DeviceWebService fido2Service;

    @Inject
    private BulkWebService bulkService;

    @Inject
    private ServiceProviderConfigWS serviceProviderService;

    @Inject
    private ResourceTypeWS resourceTypeService;

    @Inject
    private SchemaWebService schemaService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Provides metadata as json document. It contains options and endpoints supported by the SCIM server.",
            response = ScimConfiguration.class
    )
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Failed to build SCIM configuration json object.") })
    public Response getConfiguration() {

        try {
            final ScimConfiguration c2 = new ScimConfiguration();
            c2.setVersion("2.0");
            c2.setAuthorizationSupported(Arrays.asList("uma", "oauth2"));
            c2.setUserEndpoint(userService.getEndpointUrl());
            c2.setGroupEndpoint(groupService.getEndpointUrl());
            c2.setFidoDevicesEndpoint(fidoService.getEndpointUrl());
            c2.setFido2DevicesEndpoint(fido2Service.getEndpointUrl());
            c2.setBulkEndpoint(bulkService.getEndpointUrl());
            c2.setServiceProviderEndpoint(serviceProviderService.getEndpointUrl());
            c2.setResourceTypesEndpoint(resourceTypeService.getEndpointUrl());
            c2.setSchemasEndpoint(schemaService.getEndpointUrl());

            // Convert manually to avoid possible conflicts between resteasy providers, e.g. jettison, jackson
            final String entity = jsonService.objectToPerttyJson(Collections.singletonList(c2));
            log.info("SCIM configuration: {}", entity);

            return Response.ok(entity).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to generate SCIM configuration").build());
        }
        
    }

}
