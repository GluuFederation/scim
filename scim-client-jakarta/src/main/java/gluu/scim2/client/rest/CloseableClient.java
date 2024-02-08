package gluu.scim2.client.rest;

import jakarta.ws.rs.core.MultivaluedMap;

@Deprecated
public interface CloseableClient {

    void close();

    void setCustomHeaders(MultivaluedMap<String, String> headers);

}
