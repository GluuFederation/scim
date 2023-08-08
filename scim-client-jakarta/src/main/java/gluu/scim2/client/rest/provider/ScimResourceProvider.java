package gluu.scim2.client.rest.provider;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

import static org.gluu.oxtrust.model.scim2.Constants.MEDIA_TYPE_SCIM_JSON;

@Provider
@Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
@Produces({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
public class ScimResourceProvider extends JacksonJsonProvider { }
