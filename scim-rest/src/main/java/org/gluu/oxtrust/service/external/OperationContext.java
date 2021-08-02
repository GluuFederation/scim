package org.gluu.oxtrust.service.external;

import java.net.URI;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;

public class OperationContext {

    private String path;
    private URI baseUri;
    private String method;
    private String resourceType;
    private MultivaluedMap<String, String> queryParams;
    private MultivaluedMap<String, String> requestHeaders;
    private Map<String, Object> passthroughMap;
    private String accessToken;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(URI baseUri) {
        this.baseUri = baseUri;
    }

    public MultivaluedMap<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(MultivaluedMap<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public MultivaluedMap<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(MultivaluedMap<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Map<String, Object> getPassthroughMap() {
        return passthroughMap;
    }

    public void setPassthroughMap(Map<String, Object> passthroughMap) {
        this.passthroughMap = passthroughMap;
    }

}
