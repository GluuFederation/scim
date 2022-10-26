package gluu.scim2.client.factory;

import gluu.scim2.client.OAuthScimClient;
import gluu.scim2.client.UmaScimClient;
import gluu.scim2.client.TestModeScimClient;
import gluu.scim2.client.rest.ClientSideService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Path;

import org.gluu.util.security.SecurityProviderUtility;

/**
 * A factory class to obtain "client" objects that allow interaction with the SCIM service.
 */
public class ScimClientFactory {

    private static Class<ClientSideService> defaultInterface;

    static {
        SecurityProviderUtility.installBCProvider();
        defaultInterface = ClientSideService.class;
    }

    /**
     * Constructs an object that allows direct interaction with the SCIM API assuming it is protected by UMA.
     * This method hides the complexity of authorization steps required at both the resource and authorization server in an
     * UMA setting. The parameters needed as well as examples can be found at the
     * <a href="https://www.gluu.org/docs/ce/user-management/scim2/">SCIM 2.0 docs page</a>.
     * @param interfaceClass The Class to which the object returned will belong to. Normally it will be an interface inside
     *                       package {@link gluu.scim2.client.rest gluu.scim2.client.rest} or {@link org.gluu.oxtrust.ws.rs.scim2 org.gluu.oxtrust.ws.rs.scim2}
     * @param domain The root URL of the SCIM service. Usually in the form {@code https://your.gluu-server.com/identity/restv1}
     * @param umaAatClientId Requesting party Client Id
     * @param umaAatClientJksPath Path to requesting party jks file
     * @param umaAatClientJksPassword Keystore password
     * @param umaAatClientKeyId Key Id in the keystore. Pass an empty string to use the first key in keystore
     * @param <T> The type the object returned will belong to.
     * @return An object that allows to invoke service methods
     */
    public static <T> T getClient(Class <T> interfaceClass, String domain, String umaAatClientId,
            String umaAatClientJksPath, String umaAatClientJksPassword, String umaAatClientKeyId) {
        InvocationHandler handler = new UmaScimClient<>(interfaceClass, domain, umaAatClientId,
                umaAatClientJksPath, umaAatClientJksPassword, umaAatClientKeyId);
        return typedProxy(interfaceClass, handler);
    }

    /**
     * See {@link #getClient(java.lang.Class, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String) }
     * @param domain The root URL of the SCIM service. Usually in the form {@code https://your.gluu-server.com/identity/restv1}
     * @param umaAatClientId Requesting party Client Id
     * @param umaAatClientJksPath Path to requesting party jks file in local filesystem
     * @param umaAatClientJksPassword Keystore password
     * @param umaAatClientKeyId Key Id in the keystore. Pass an empty string to use the first key in keystore
     * @return An object that allows calling User, Group, and FidoDevices operations. It also has some support to
     * call service provider configuration endpoints (see section 4 of RFC 7644)
     */
    public static ClientSideService getClient(String domain, String umaAatClientId,
            String umaAatClientJksPath, String umaAatClientJksPassword, String umaAatClientKeyId) {
        return getClient(defaultInterface, domain, umaAatClientId, umaAatClientJksPath,
                umaAatClientJksPassword, umaAatClientKeyId);
    }

    /**
     * Constructs an object that allows direct interaction with the SCIM API assuming it is protected by test mode.
     * This method hides the complexity of steps required at the authorization server in a test-mode setting.
     * Usage examples of this type of client can be found at the
     * <a href="https://www.gluu.org/docs/ce/user-management/scim2/">SCIM 2.0 docs page</a>.
     * @param interfaceClass The Class to which the object returned will belong to. Normally it will be an interface inside
     *                       package {@link gluu.scim2.client.rest gluu.scim2.client.rest} or {@link org.gluu.oxtrust.ws.rs.scim2 org.gluu.oxtrust.ws.rs.scim2}
     * @param domain The root URL of the SCIM service. Usually in the form {@code https://your.gluu-server.com/identity/restv1}
     * @param OIDCMetadataUrl URL of authorization servers' metadata document. Usually in the form {@code https://your.gluu-server.com/.well-known/openid-configuration}
     * @param <T> The type the object returned will belong to.
     * @return An object that allows to invoke service methods
     * @throws Exception If there is initialization problem
     */
    public static <T> T getTestClient(Class <T> interfaceClass, String domain, String OIDCMetadataUrl) throws Exception {
        InvocationHandler handler = new TestModeScimClient<>(interfaceClass, domain, OIDCMetadataUrl);
        return typedProxy(interfaceClass, handler);
    }
    
    /**
     * Constructs an object that allows direct interaction with the SCIM API assuming it is protected by test mode.
     * Usage examples of this type of client can be found at the
     * <a href="https://www.gluu.org/docs/ce/user-management/scim2/">SCIM 2.0 docs page</a>.
     * @param interfaceClass The Class to which the object returned will belong to. Normally it will be an interface inside
     *                       package {@link gluu.scim2.client.rest gluu.scim2.client.rest} or {@link org.gluu.oxtrust.ws.rs.scim2 org.gluu.oxtrust.ws.rs.scim2}
     * @param domain The root URL of the SCIM service. Usually in the form {@code https://your.gluu-server.com/identity/restv1}
     * @param OIDCMetadataUrl URL of authorization servers' metadata document. Usually in the form {@code https://your.gluu-server.com/.well-known/openid-configuration}
     * @param clientId ID of an already registered OIDC client in the Gluu Server
     * @param clientSecret Secret of the corresponding client (see clientID parameter)
     * @param <T> The type the object returned will belong to.
     * @return An object that allows to invoke service methods
     * @throws Exception If there is initialization problem
     */
    public static <T> T getTestClient(Class <T> interfaceClass, String domain, String OIDCMetadataUrl,
            String clientId, String clientSecret) throws Exception {
        InvocationHandler handler = new TestModeScimClient<>(interfaceClass, domain,
                OIDCMetadataUrl, clientId, clientSecret);
        return typedProxy(interfaceClass, handler);
    }
    
    /**
     * See {@link #getTestClient(java.lang.Class, java.lang.String, java.lang.String) }
     * @param domain The root URL of the SCIM service. Usually in the form {@code https://your.gluu-server.com/identity/restv1}
     * @param OIDCMetadataUrl URL of authorization servers' metadata document. Usually in the form {@code https://your.gluu-server.com/.well-known/openid-configuration}
     * @return An object that allows calling User, Group, and FidoDevices operations. It also has some support to
     * call service provider configuration endpoints (see section 4 of RFC 7644)
     * @throws Exception If there is initialization problem
     */
    public static ClientSideService getTestClient(String domain, String OIDCMetadataUrl) throws Exception {
        return getTestClient(defaultInterface, domain, OIDCMetadataUrl);
    }

    /**
     * See {@link #getTestClient(java.lang.Class, java.lang.String, java.lang.String, java.lang.String, java.lang.String) }
     * @param domain The root URL of the SCIM service. Usually in the form {@code https://your.gluu-server.com/identity/restv1}
     * @param OIDCMetadataUrl URL of authorization servers' metadata document. Usually in the form {@code https://your.gluu-server.com/.well-known/openid-configuration}
     * @param clientId ID of an already registered OIDC client in the Gluu Server
     * @param clientSecret Secret of the corresponding client (see clientID parameter)
     * @return An object that allows calling User, Group, and FidoDevices operations. It also has some support to
     * call service provider configuration endpoints (see section 4 of RFC 7644)
     * @throws Exception If there is initialization problem
     */
    public static ClientSideService getTestClient(String domain, String OIDCMetadataUrl,
            String clientId, String clientSecret) throws Exception {
        return getTestClient(defaultInterface, domain, OIDCMetadataUrl, clientId, clientSecret);
    }
    
    /**
     * Constructs an object that allows direct interaction with the SCIM API assuming it is protected by oauth mode.
     * Usage examples of this type of client can be found at the
     * <a href="https://www.gluu.org/docs/ce/user-management/scim2/">SCIM 2.0 docs page</a>.
     * @param interfaceClass The Class to which the object returned will belong to. Normally it will be an interface inside
     *                       package {@link gluu.scim2.client.rest gluu.scim2.client.rest} or {@link org.gluu.oxtrust.ws.rs.scim2 org.gluu.oxtrust.ws.rs.scim2}
     * @param domain The root URL of the SCIM service. Usually in the form {@code https://your.gluu-server.com/identity/restv1}
     * @param OIDCMetadataUrl URL of authorization servers' metadata document. Usually in the form {@code https://your.gluu-server.com/.well-known/openid-configuration}
     * @param clientId ID of an already registered OIDC client in the Gluu Server
     * @param clientSecret Secret of the corresponding client (see clientID parameter)
     * @param secretPostAuthnMethod Whether the client uses client_secret_post (true) or client_secret_basic (false) to 
     *                              authenticate to the token endpoint
     * @param <T> The type the object returned will belong to.
     * @return An object that allows to invoke service methods
     * @throws Exception If there is initialization problem
     */
    public static <T> T getOAuthClient(Class<T> interfaceClass, String domain, String OIDCMetadataUrl,
            String clientId, String clientSecret, boolean secretPostAuthnMethod) throws Exception {    
        InvocationHandler handler = new OAuthScimClient(interfaceClass, domain, OIDCMetadataUrl, 
        	clientId, clientSecret, secretPostAuthnMethod);
        return typedProxy(interfaceClass, handler);
    }
    
    public static <T> T getOAuthClient(Class<T> interfaceClass, String domain, String OIDCMetadataUrl,
            String clientId, Path keyStorePath, String keyStorePassword, String keyId) throws Exception {
        InvocationHandler handler = new OAuthScimClient(interfaceClass, domain, OIDCMetadataUrl, 
                clientId, keyStorePath, keyStorePassword, keyId);
        return typedProxy(interfaceClass, handler);
    }

    
    /**
     * See {@link #getOAuthClient(java.lang.Class, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean) }
     * @param domain The root URL of the SCIM service. Usually in the form {@code https://your.gluu-server.com/identity/restv1}
     * @param OIDCMetadataUrl URL of authorization servers' metadata document. Usually in the form {@code https://your.gluu-server.com/.well-known/openid-configuration}
     * @param clientId ID of an already registered OIDC client in the Gluu Server
     * @param clientSecret Secret of the corresponding client (see clientID parameter)
     * @param secretPostAuthnMethod Whether the client uses client_secret_post (true) or client_secret_basic (false) to 
     *                              authenticate to the token endpoint
     * @return An object that allows calling User, Group, and FidoDevices operations. It also has some support to
     * call service provider configuration endpoints (see section 4 of RFC 7644)
     * @throws Exception If there is initialization problem
     */
    public static ClientSideService getOAuthClient(String domain, String OIDCMetadataUrl,
            String clientId, String clientSecret, boolean secretPostAuthnMethod) throws Exception {    
    	return getOAuthClient(defaultInterface, domain, OIDCMetadataUrl, clientId,
                clientSecret, secretPostAuthnMethod);
    }
    
    public static ClientSideService getOAuthClient(String domain, String OIDCMetadataUrl,
            String clientId, Path keyStorePath, String keyStorePassword, String keyId) throws Exception {
        return getOAuthClient(defaultInterface, domain, OIDCMetadataUrl, clientId,
                keyStorePath, keyStorePassword, keyId);
    }
	
    private static <T> T typedProxy(Class <T> interfaceClass, InvocationHandler handler){
        return interfaceClass.cast(Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass}, handler));
    }

}
