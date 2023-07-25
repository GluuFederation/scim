package org.gluu.oxtrust.service.scim2;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.gluu.persist.model.base.CustomObjectAttribute;
import org.gluu.oxtrust.model.GluuGroup;
import org.gluu.oxtrust.model.scim.ScimCustomPerson;
import org.gluu.oxtrust.model.scim2.user.Email;
import org.gluu.oxtrust.model.scim2.util.DateUtil;
import org.gluu.oxtrust.service.AttributeService;
import org.gluu.oxtrust.service.IGroupService;
import org.gluu.oxtrust.service.IPersonService;
import org.gluu.oxtrust.util.ServiceUtil;
import org.gluu.model.GluuAttribute;
import org.gluu.persist.ldap.impl.LdapEntryManagerFactory;
import org.gluu.persist.PersistenceEntryManager;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class UserPersistenceHelper {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private IPersonService personService;

    @Inject
    private AttributeService attributeService;

    @Inject
    private IGroupService groupService;
    
    private Map<String, GluuAttribute> attributesMap;
    
    public String getUserInumFromDN(String deviceDn){
        String baseDn = personService.getDnForPerson(null).replaceAll("\\s*", "");
        deviceDn = deviceDn.replaceAll("\\s*", "").replaceAll("," + baseDn, "");
        return deviceDn.substring(deviceDn.indexOf("inum=") + 5);
    }

    public void addCustomObjectClass(ScimCustomPerson person) {
    	if (LdapEntryManagerFactory.PERSISTENCE_TYPE.equals(persistenceEntryManager.getPersistenceType())) {
            String[] customObjectClasses = Optional.ofNullable(person.getCustomObjectClasses()).orElse(new String[0]);
            Set<String> customObjectClassesSet = new HashSet<>(Stream.of(customObjectClasses).collect(Collectors.toList()));
            customObjectClassesSet.add(attributeService.getCustomOrigin());
            person.setCustomObjectClasses(customObjectClassesSet.toArray(new String[0]));
        }
    }

    public void addPerson(ScimCustomPerson person) throws Exception {
        //It is guaranteed that no duplicate UID occurs when this method is called
        person.setCreationDate(new Date());
        applyMultiValued(person.getTypedCustomAttributes());
        persistenceEntryManager.persist(person);
    }

    public ScimCustomPerson getPersonByInum(String inum) {

        ScimCustomPerson person = null;
        try {
            person = persistenceEntryManager.find(ScimCustomPerson.class, personService.getDnForPerson(inum));
        } catch (Exception e) {
            log.warn("Failed to find Person by Inum {}", inum);
        }
        return person;

    }

    public void updatePerson(ScimCustomPerson person) {

        Date updateDate = new Date();
        person.setUpdatedAt(updateDate);
        if (person.getAttribute("oxTrustMetaLastModified") != null) {
            person.setAttribute("oxTrustMetaLastModified", DateUtil.millisToISOString(updateDate.getTime()));
        }
        applyMultiValued(person.getTypedCustomAttributes());
        persistenceEntryManager.merge(person);

    }

    /**
     * "Detaches" a person from all groups he is currently member of
     * @param person The person in question
     * @throws Exception
     */
    public void removeUserFromGroups(ScimCustomPerson person) {

        String dn = person.getDn();
        List<String> groups = person.getMemberOf();
        
        for (String oneGroup : groups) {
            try {
                GluuGroup aGroup = groupService.getGroupByDn(oneGroup);
                List<String> groupMembers = aGroup.getMembers();
                int idx = Optional.ofNullable(groupMembers).map(l -> l.indexOf(dn)).orElse(-1);
                
                if (idx >= 0) {
                    List<String> newMembers = new ArrayList<>();
                    newMembers.addAll(groupMembers.subList(0, idx));
                    newMembers.addAll(groupMembers.subList(idx + 1, groupMembers.size()));
                    
                    aGroup.setMembers(newMembers.isEmpty() ? null : newMembers);
                    groupService.updateGroup(aGroup);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }

    }

    /**
     * One-way sync from "oxTrustEmail" to "mail". Ultimately this is persisted so
     * "mail" will be updated by values from "oxTrustEmail".
     *
     * @param customPerson Represents the user object to be modified 
     * @return Modified user object
     * @throws Exception If (json) values in oxTrustEmail cannot be parsed 
     */
    public ScimCustomPerson syncEmailForward(ScimCustomPerson customPerson) throws Exception {

        log.info("syncing email ...");
        List<String> oxTrustEmails = customPerson.getAttributeList("oxTrustEmail");
        int len = oxTrustEmails.size();

        if (len > 0) {
            ObjectMapper mapper = ServiceUtil.getObjectMapper();
            List<String> newMails = new ArrayList<>(len);
            int prima = -1;
            
            for (int i = 0; i < len; i++) {
                Email email = mapper.readValue(oxTrustEmails.get(i), Email.class);
                newMails.add(email.getValue());
                
                if (prima == -1 && Optional.ofNullable(email.getPrimary()).orElse(false)) {
                    prima = i;
                }
            }
            if (prima >= 1) {
                newMails.add(0, newMails.remove(prima));
            }
            customPerson.setAttribute("mail", newMails.toArray(new String[0]));
        } else {
            customPerson.setAttribute("mail", new String[0]);
        }
        return customPerson;

    }

    public void removePerson(ScimCustomPerson person) {
        persistenceEntryManager.removeRecursively(person.getDn(), person.getClass());
    }

    @PostConstruct
    private void init() {
    	attributesMap = attributeService.getAllAttributes().stream().collect(
    		    Collectors.toMap(GluuAttribute::getName, Function.identity(),
    		    	(name, attr) -> attr)    //Avoid exception due to duplicate keys
    		);
    }
    
	private void applyMultiValued(List<CustomObjectAttribute> customAttributes) {
        
		for (CustomObjectAttribute customAttribute : customAttributes) {
			
			Optional.ofNullable(attributesMap.get(customAttribute.getName()))
			    .map(GluuAttribute::getOxMultiValuedAttribute)
			    .map(Boolean::booleanValue)
			    .ifPresent(mv -> customAttribute.setMultiValued(mv));
			    
			//when any of the optionals above is empty, it means "we aint' sure" about cardinality
		}
	}
    
}
