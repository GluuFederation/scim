package gluu.scim2.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import gluu.scim2.client.factory.ScimClientFactory;
import gluu.scim2.client.rest.ClientSideService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gluu.oxtrust.model.scim2.user.UserResource;
import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import java.nio.file.Paths;
import java.util.Map;

/**
 * Created by jgomer on 2017-06-09.
 */
public class BaseTest {

    protected static ClientSideService client=null;
    protected Logger logger = LogManager.getLogger(getClass());
    protected ObjectMapper mapper=new ObjectMapper();

    @BeforeSuite
    public void initTestSuite(ITestContext context) throws Exception {

        logger.info("Invoked initTestSuite of '{}'", context.getCurrentXmlTest().getName());
        if (client==null) {
            setupClient(context.getSuite().getXmlSuite().getParameters());
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }

    }

    @AfterSuite
    public void finalize() {
        client.close();
    }

    private void setupClient(Map<String, String> params) throws Exception{

        logger.info("Initializing client...");
        boolean testMode=Boolean.parseBoolean(System.getProperty("testmode"));

        /*
         To get a simpler client (not one that supports all possible operations as in this case), you can use as class
         parameter any other interface from gluu.scim2.client.rest or org.gluu.oxtrust.ws.rs.scim2 packages. Find an
         example at test method gluu.scim2.client.SampleTest#smallerClient
         */
        if (testMode)
            client=ScimClientFactory.getTestClient(ClientSideService.class, params.get("domainURL"), params.get("OIDCMetadataUrl"));
            //client=ScimClientFactory.getTestClient(ClientSideService.class, params.get("domainURL"), params.get("OIDCMetadataUrl"), "clientId", "clientSecret");
        else
            client=ScimClientFactory.getClient(
                    ClientSideService.class,
                    params.get("domainURL"),
                    params.get("umaAatClientId"),
                    params.get("umaAatClientJksPath"),
                    params.get("umaAatClientJksPassword"),
                    params.get("umaAatClientKeyId"));
    }

    public UserResource getDeepCloneUsr(UserResource bean) throws Exception{
        return mapper.readValue(mapper.writeValueAsString(bean), UserResource.class);
    }

}
