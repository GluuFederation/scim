package org.gluu.oxtrust.service.external;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.gluu.model.SimpleCustomProperty;
import org.gluu.model.custom.script.CustomScriptType;
import org.gluu.model.custom.script.conf.CustomScriptConfiguration;
import org.gluu.model.custom.script.type.scim.ScimType;
import org.gluu.persist.model.PagedResult;
import org.gluu.oxtrust.model.GluuGroup;
import org.gluu.oxtrust.model.scim.ScimCustomPerson;
import org.gluu.persist.model.base.Entry;
import org.gluu.service.custom.script.ExternalScriptService;

/**
 * @author Val Pecaoco
 * @author jgomer
 */
@ApplicationScoped
public class ExternalScimService extends ExternalScriptService {
	
	private static final long serialVersionUID = 1767751544454591666L;

    public ExternalScimService() {
        super(CustomScriptType.SCIM);
    }

    private CustomScriptConfiguration findConfigWithVersion(int version) {
        return customScriptConfigurations.stream()
                .filter(sc -> executeExternalGetApiVersion(sc) == version)
                .findFirst().orElse(null);    
    }
    
    private boolean executeScimCreateUserMethod(ScimCustomPerson user, CustomScriptConfiguration customScriptConfiguration) {

        try {
            log.debug("Executing python 'SCIM Create User' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.createUser(user, configurationAttributes);
            log.debug("executeScimCreateUserMethod result = " + result);
            return result;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(customScriptConfiguration.getCustomScript(), e);
        }
        return false;

    }

    private boolean executeScimPostCreateUserMethod(ScimCustomPerson user, CustomScriptConfiguration customScriptConfiguration) {

        try {
            if (executeExternalGetApiVersion(customScriptConfiguration) < 2)
                return true;

            log.debug("Executing python 'SCIM Post Create User' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.postCreateUser(user, configurationAttributes);
            log.debug("executeScimPostCreateUserMethod result = " + result);
            return result;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(customScriptConfiguration.getCustomScript(), e);
        }
        return false;

    }

    private boolean executeScimUpdateUserMethod(ScimCustomPerson user, CustomScriptConfiguration customScriptConfiguration) {

        try {
            log.debug("Executing python 'SCIM Update User' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.updateUser(user, configurationAttributes);
            log.debug("executeScimUpdateUserMethod result = " + result);
            return result;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(customScriptConfiguration.getCustomScript(), e);
        }
        return false;

    }

    private boolean executeScimPostUpdateUserMethod(ScimCustomPerson user, CustomScriptConfiguration customScriptConfiguration) {

        try {
            if (executeExternalGetApiVersion(customScriptConfiguration) < 2)
                return true;

            log.debug("Executing python 'SCIM Post Update User' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.postUpdateUser(user, configurationAttributes);
            log.debug("executeScimPostUpdateUserMethod result = " + result);
            return result;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(customScriptConfiguration.getCustomScript(), e);
        }
        return false;

    }

    private boolean executeScimDeleteUserMethod(ScimCustomPerson user, CustomScriptConfiguration customScriptConfiguration) {

        try {
            log.debug("Executing python 'SCIM Delete User' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.deleteUser(user, configurationAttributes);
            log.debug("executeScimDeleteUserMethod result = " + result);
            return result;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(customScriptConfiguration.getCustomScript(), e);
        }
        return false;

    }

    private boolean executeScimPostDeleteUserMethod(ScimCustomPerson user, CustomScriptConfiguration customScriptConfiguration) {

        try {
            if (executeExternalGetApiVersion(customScriptConfiguration) < 2)
                return true;

            log.debug("Executing python 'SCIM Post Delete User' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.postDeleteUser(user, configurationAttributes);
            log.debug("executeScimPostDeleteUserMethod result = " + result);
            return result;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(customScriptConfiguration.getCustomScript(), e);
        }
        return false;

    }

    private boolean executeScimGetUserMethod(ScimCustomPerson user, CustomScriptConfiguration customScriptConfiguration) {

        try {
            if (executeExternalGetApiVersion(customScriptConfiguration) < 3)
                return true;

            log.debug("Executing python 'SCIM Get User' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.getUser(user, configurationAttributes);
            log.debug("executeScimGetUserMethod result = " + result);
            return result;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(customScriptConfiguration.getCustomScript(), e);
        }
        return false;

    }

    private boolean executeScimCreateGroupMethod(GluuGroup group, CustomScriptConfiguration customScriptConfiguration) {

        try {
            log.debug("Executing python 'SCIM Create Group' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.createGroup(group, configurationAttributes);
            log.debug("executeScimCreateGroupMethod result = " + result);
            return result;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(customScriptConfiguration.getCustomScript(), e);
        }
        return false;

    }

    private boolean executeScimPostCreateGroupMethod(GluuGroup group, CustomScriptConfiguration customScriptConfiguration) {

        try {
            if (executeExternalGetApiVersion(customScriptConfiguration) < 2)
                return true;

            log.debug("Executing python 'SCIM Post Create Group' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.postCreateGroup(group, configurationAttributes);
            log.debug("executeScimPostCreateGroupMethod result = " + result);
            return result;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(customScriptConfiguration.getCustomScript(), e);
        }
        return false;

    }

    private boolean executeScimUpdateGroupMethod(GluuGroup group, CustomScriptConfiguration customScriptConfiguration) {

        try {
            log.debug("Executing python 'SCIM Update Group' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.updateGroup(group, configurationAttributes);
            log.debug("executeScimUpdateGroupMethod result = " + result);
            return  result;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(customScriptConfiguration.getCustomScript(), e);
        }
        return false;

    }

    private boolean executeScimPostUpdateGroupMethod(GluuGroup group, CustomScriptConfiguration customScriptConfiguration) {

        try {
            if (executeExternalGetApiVersion(customScriptConfiguration) < 2)
                return true;

            log.debug("Executing python 'SCIM Post Update Group' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.postUpdateGroup(group, configurationAttributes);
            log.debug("executeScimPostUpdateGroupMethod result = " + result);
            return  result;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(customScriptConfiguration.getCustomScript(), e);
        }
        return false;

    }

    private boolean executeScimDeleteGroupMethod(GluuGroup group, CustomScriptConfiguration customScriptConfiguration) {

        try {
            log.debug("Executing python 'SCIM Delete Group' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.deleteGroup(group, configurationAttributes);
            log.debug("executeScimDeleteGroupMethod result = " + result);
            return  result;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(customScriptConfiguration.getCustomScript(), e);
        }
        return false;

    }

    private boolean executeScimPostDeleteGroupMethod(GluuGroup group, CustomScriptConfiguration customScriptConfiguration) {

        try {
            if (executeExternalGetApiVersion(customScriptConfiguration) < 2)
                return true;

            log.debug("Executing python 'SCIM Post Delete Group' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.postDeleteGroup(group, configurationAttributes);
            log.debug("executeScimPostDeleteGroupMethod result = " + result);
            return  result;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(customScriptConfiguration.getCustomScript(), e);
        }
        return false;

    }

    private boolean executeScimGetGroupMethod(GluuGroup group, CustomScriptConfiguration customScriptConfiguration) {

        try {
            if (executeExternalGetApiVersion(customScriptConfiguration) < 3)
                return true;

            log.debug("Executing python 'SCIM Get Group' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();

            boolean result = externalType.getGroup(group, configurationAttributes);
            log.debug("executeScimGetGroupMethod result = " + result);
            return result;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(customScriptConfiguration.getCustomScript(), e);
        }
        return false;

    }

    private boolean executeScimPostSearchUsersMethod(PagedResult<ScimCustomPerson> pagedResult, CustomScriptConfiguration customScriptConfiguration) {

        try {
        	if (executeExternalGetApiVersion(customScriptConfiguration) < 4)
                return true;
            
            log.debug("Executing python 'SCIM Search Users' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            
            boolean result = externalType.postSearchUsers(pagedResult, configurationAttributes);
            log.debug("executeScimPostSearchUsersMethod result = " + result);
            return result;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(customScriptConfiguration.getCustomScript(), e);
        }
        return false;
        
    }
    
    private boolean executeScimPostSearchGroupsMethod(PagedResult<GluuGroup> pagedResult, CustomScriptConfiguration customScriptConfiguration) {

        try {
        	if (executeExternalGetApiVersion(customScriptConfiguration) < 4)
                return true;
            
            log.debug("Executing python 'SCIM Search Groups' method");
            ScimType externalType = (ScimType) customScriptConfiguration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = customScriptConfiguration.getConfigurationAttributes();
            
            boolean result = externalType.postSearchGroups(pagedResult, configurationAttributes);
            log.debug("executeScimPostSearchGroupsMethod result = " + result);
            return result;
            
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(customScriptConfiguration.getCustomScript(), e);
        }
        return false;
        
    }
    
    public boolean executeScimCreateUserMethods(ScimCustomPerson user) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimCreateUserMethod(user, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimPostCreateUserMethods(ScimCustomPerson user) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimPostCreateUserMethod(user, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimUpdateUserMethods(ScimCustomPerson user) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimUpdateUserMethod(user, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimPostUpdateUserMethods(ScimCustomPerson user) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimPostUpdateUserMethod(user, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimDeleteUserMethods(ScimCustomPerson user) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimDeleteUserMethod(user, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimPostDeleteUserMethods(ScimCustomPerson user) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimPostDeleteUserMethod(user, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimCreateGroupMethods(GluuGroup group) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimCreateGroupMethod(group, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimPostCreateGroupMethods(GluuGroup group) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimPostCreateGroupMethod(group, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimUpdateGroupMethods(GluuGroup group) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimUpdateGroupMethod(group, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimPostUpdateGroupMethods(GluuGroup group) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimPostUpdateGroupMethod(group, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimDeleteGroupMethods(GluuGroup group) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimDeleteGroupMethod(group, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimPostDeleteGroupMethods(GluuGroup group) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimPostDeleteGroupMethod(group, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimGetUserMethods(ScimCustomPerson user) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimGetUserMethod(user, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimGetGroupMethods(GluuGroup group) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimGetGroupMethod(group, customScriptConfiguration)) {
                return false;
            }
        }
        return true;

    }

    public boolean executeScimPostSearchUsersMethods(PagedResult<ScimCustomPerson> result) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimPostSearchUsersMethod(result, customScriptConfiguration)) {
                return false;
            }
        }
        return true;
    }
    
    public boolean executeScimPostSearchGroupsMethods(PagedResult<GluuGroup> result) {

        for (CustomScriptConfiguration customScriptConfiguration : this.customScriptConfigurations) {
            if (!executeScimPostSearchGroupsMethod(result, customScriptConfiguration)) {
                return false;
            }
        }
        return true;
    }

    public boolean executeAllowResourceOperation(Entry entity, OperationContext context) throws Exception {
        
        CustomScriptConfiguration configuration = findConfigWithVersion(5);
        
        if (configuration == null) {
            // All scim operation calls pass
            return true;
        }
        
        boolean result = false;
        try {
            log.debug("Executing python 'SCIM Allow Resource Operation' method");
            ScimType externalType = (ScimType) configuration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = configuration.getConfigurationAttributes();

            result = externalType.allowResourceOperation(context, entity, configurationAttributes);
            log.debug("executeAllowResourceOperation result = " + result);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(configuration.getCustomScript(), e);
            throw e;
        }        
        return result;

    }

    public String executeRejectedResourceOperationResponse(Entry entity, OperationContext context) throws Exception {
        
        CustomScriptConfiguration configuration = findConfigWithVersion(5);
        if (configuration == null) {
            // this is unexpected
            log.error("No suitable custom script found");
            throw new Exception("No script with API version 5 encountered");
        }

        String rejectionError = null;
        try {
            log.debug("Executing python 'SCIM Rejected Resource Operation Response' method");
            ScimType externalType = (ScimType) configuration.getExternalType();
            Map<String, SimpleCustomProperty> configurationAttributes = configuration.getConfigurationAttributes();

            rejectionError = externalType.rejectedResourceOperationResponse(context, entity, configurationAttributes);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveScriptError(configuration.getCustomScript(), e);
            throw e;
        }
        return rejectionError;
        
    }
    
}
