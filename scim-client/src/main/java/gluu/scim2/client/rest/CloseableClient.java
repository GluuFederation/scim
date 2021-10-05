package gluu.scim2.client.rest;

import javax.ws.rs.core.MultivaluedMap;

@Deprecated
public interface CloseableClient {

    void close();

    void setCustomHeaders(MultivaluedMap<String, String> headers);

}
