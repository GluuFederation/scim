package gluu.scim2.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.gluu.oxauth.client.*;
import org.gluu.oxauth.model.common.*;
import org.gluu.oxauth.model.crypto.OxAuthCryptoProvider;
import org.gluu.oxauth.model.register.ApplicationType;
import org.gluu.util.StringHelper;
import gluu.scim2.client.exception.ScimInitializationException;

import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;

/**
 * Instances of this class contain the necessary logic to handle the authorization process required by a client of SCIM
 * service.
 * <p><b>Note:</b> Do not instantiate this class in your code. To interact with the service, call the corresponding method in
 * class {@link gluu.scim2.client.factory.ScimClientFactory ScimClientFactory} that returns a proxy object wrapping this client
 * @param <T> Type parameter of superclass
 */
public class OAuthScimClient<T> extends AbstractScimClient<T> {

    private static final long serialVersionUID = 3141592672017122134L;

    private static final String SCOPES = Stream.of(
    	"https://gluu.org/scim/users.read",
		"https://gluu.org/scim/users.write",
		"https://gluu.org/scim/groups.read",
		"https://gluu.org/scim/groups.write",
		"https://gluu.org/scim/fido.read",
		"https://gluu.org/scim/fido.write",
		"https://gluu.org/scim/fido2.read",
		"https://gluu.org/scim/fido2.write",
		"https://gluu.org/scim/all-resources.search",
		"https://gluu.org/scim/bulk")
	        .collect(Collectors.joining(" "));

    private Logger logger = LogManager.getLogger(getClass());

    private String access_token;
    private String tokenEndpoint;   //Url of authorization's server token endpoint i.e. https://<host:port>/jans-auth/restv1/token

    private String clientId;
    private String password;
    private AuthenticationMethod tokenEndpointAuthnMethod;
    private String keyId;
    
    private ObjectMapper mapper = new ObjectMapper();
    private OxAuthCryptoProvider cryptoProvider;

	public OAuthScimClient(Class<T> serviceClass, String serviceUrl, String OIDCMetadataUrl, 
		String id, String secret, boolean secretPostAuthnMethod) throws Exception {
	
        super(serviceUrl, serviceClass);
        checkRequiredness(id, secret, OIDCMetadataUrl);
        
		clientId = id;
		password = secret;
		tokenEndpoint = getTokenEndpoint(OIDCMetadataUrl);
		tokenEndpointAuthnMethod = secretPostAuthnMethod 
			? AuthenticationMethod.CLIENT_SECRET_POST : AuthenticationMethod.CLIENT_SECRET_BASIC;
		updateTokens();

    }
    
    public OAuthScimClient(Class<T> serviceClass, String serviceUrl, String OIDCMetadataUrl, 
    	String id, Path keyStorePath, String keyStorePassword, String keyId) throws Exception {

        super(serviceUrl, serviceClass);
        checkRequiredness(id, keyStorePassword, OIDCMetadataUrl);
        
		try {
			cryptoProvider = new OxAuthCryptoProvider(keyStorePath.toString(), keyStorePassword, null);
		} catch (Exception ex) {
			throw new ScimInitializationException("Failed to initialize crypto provider");
		}
        if (StringHelper.isEmpty(keyId)) {
			// Get first key
			List<String> aliases = cryptoProvider.getKeys();
			if (aliases.size() > 0) {
				keyId = aliases.get(0);
			} else {
                throw new ScimInitializationException("No keys found in keystore");
			}
		}
		clientId = id;
		tokenEndpoint = getTokenEndpoint(OIDCMetadataUrl);
        tokenEndpointAuthnMethod = AuthenticationMethod.PRIVATE_KEY_JWT;
        this.keyId = keyId;
		updateTokens();
        
    }
    
    private void checkRequiredness(String ...args) throws ScimInitializationException {
    	if (Stream.of(args).anyMatch(StringHelper::isEmpty))
    	    throw new ScimInitializationException("One or more required values are missing");
    }
    
    private String getTokenEndpoint(String metadataUrl) throws Exception {
        //Extract token endpoint from metadata URL
        JsonNode tree = mapper.readTree(new URL(metadataUrl));
        return tree.get("token_endpoint").asText();
    }
    
    private void updateTokens() throws Exception {
		access_token = getTokens().getAccessToken();
		logger.debug("Got token: " + access_token);
    }

    private TokenResponse getTokens() throws Exception {

        TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setAuthenticationMethod(tokenEndpointAuthnMethod);
        tokenRequest.setScope(SCOPES);
        tokenRequest.setAuthUsername(clientId);
        
        if (keyId == null) {
            tokenRequest.setAuthPassword(password);
        } else {
            tokenRequest.setCryptoProvider(cryptoProvider);
            tokenRequest.setAlgorithm(cryptoProvider.getSignatureAlgorithm(keyId));
            tokenRequest.setKeyId(keyId);
            tokenRequest.setAudience(tokenEndpoint);
        }

        TokenClient tokenClient = new TokenClient(tokenEndpoint);
        tokenClient.setRequest(tokenRequest);
        return tokenClient.exec();

    }

    /**
     * Builds a string suitable for being passed as an authorization header. It does so by
     * prefixing the current access token this object has with the word "Bearer "
     *
     * @return String built
     */
    @Override
    String getAuthenticationHeader() {
        return "Bearer " + access_token;
    }

    /**
     * Gets a new access token from the authorization server
     *
     * @param response The type of clients represented by this class ignore this param 
     * @return A boolean value indicating the operation was successful
     */
    @Override
    boolean authorize(Response response) {
        /*
        This method is called if the attempt to use the service returned unauthorized (status = 401), so here we
        ask for another token, or else leave it that way (forbidden)
         */
        try {
            updateTokens();
            return access_token != null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;   //do not make an additional attempt, e.g. getAuthenticationHeader is not called once more
        }

    }

}
