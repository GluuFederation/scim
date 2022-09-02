package org.gluu.oxtrust.model.scim;

import org.gluu.persist.model.base.CustomObjectAttribute;
import org.gluu.persist.annotation.*;
import org.gluu.persist.model.base.Entry;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@DataEntry
@ObjectClass("gluuPerson")
public class ScimCustomPerson extends Entry implements Serializable {

    private static final long serialVersionUID = -234934710284710189L;

    @CustomObjectClass
    private String[] customObjectClasses;

    @AttributeName(name = "oxCreationTimestamp")
    private Date creationDate;

    @AttributeName
    private Date updatedAt;

    @AttributeName(name = "oxPPID")
    private List<String> oxPPID;

    @AttributesList(name = "name", value = "values", multiValued = "multiValued", sortByName = true, attributesConfiguration = {
            @AttributeName(name = "inum", ignoreDuringUpdate = true),
            @AttributeName(name = "uid"),
            @AttributeName(name = "userPassword", ignoreDuringRead = true) })
    private List<CustomObjectAttribute> typedCustomAttributes = new ArrayList<>();

    public Date getCreationDate() {
        return creationDate;
    }

    public String getDisplayName() {
        return getAttribute("displayName");
    }

    public String getGivenName() {
        return getAttribute("givenName");
    }

    public String getInum() {
        return getAttribute("inum");
    }

    public List<String> getMemberOf() {
        return getAttributeList("memberOf");
    }

    public List<String> getOxPPID() {
        return oxPPID;
    }

    public String getPreferredLanguage() {
        return getAttribute("preferredLanguage");
    }

    public String getSurname() {
        return getAttribute("sn");
    }

    public String getTimezone() {
        return getAttribute("zoneinfo");
    }

    public String getUid() {
        return getAttribute("uid");
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public String getUserPassword() {
        return getAttribute("userPassword");
    }

    public List<String> getAttributeList(String attributeName) {

        List<Object> list = Optional.ofNullable(getTypedAttribute(attributeName))
                .map(CustomObjectAttribute::getValues).orElse(Collections.emptyList())
                .stream().filter(Objects::nonNull).collect(Collectors.toList());
        List<String> result = new ArrayList<>();

        for (Object obj : list) {
            //Ugly ugly hack
            if (obj.getClass().equals(Date.class)) {
                long millis = Date.class.cast(obj).getTime();
                result.add(Instant.ofEpochMilli(millis).toString());
            } else {
                result.add(obj.toString());
            }
        }
        return result;

    }

    public String getAttribute(String attributeName) {
        List<String> values = getAttributeList(attributeName);
        return values.isEmpty() ? null : values.get(0);
    }

    public String[] getAttributes(String attributeName) {
        List<String> values = getAttributeList(attributeName);
        return values.isEmpty() ? null : values.toArray(new String[0]);
    }

    public List<CustomObjectAttribute> getTypedCustomAttributes() {
        return typedCustomAttributes;
    }

    public String[] getCustomObjectClasses() {
        return customObjectClasses;
    }

    public void setCustomObjectClasses(String[] customObjectClasses) {
        this.customObjectClasses = customObjectClasses;
    }

    public void setTypedCustomAttributes(List<CustomObjectAttribute> typedCustomAttributes) {
        this.typedCustomAttributes = typedCustomAttributes;
    }

    public CustomObjectAttribute getTypedAttribute(String attributeName) {
        return typedCustomAttributes.stream().filter(tca -> tca.getName().equals(attributeName))
                .findFirst().orElse(null);
    }

    public void setCommonName(String value) {
        setAttribute("cn", value);
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setInum(String value) {
        setAttribute("inum", value);
    }

    public void setOxPPID(List<String> oxPPID) {
        this.oxPPID = oxPPID;
    }

    public void setUid(String value) {
        setCustomAttribute("uid", value);
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setUserPassword(String value) {
        setCustomAttribute("userPassword", value);
    }

    public CustomObjectAttribute setAttribute(String attributeName, String[] attributeValue) {
    	return setCustomAttribute(attributeName, Arrays.asList(attributeValue));
    }

    public CustomObjectAttribute setAttribute(String attributeName, String attributeValue) {
        if (attributeValue == null || attributeValue.length() == 0) {
            return setCustomAttribute(attributeName, Collections.emptyList());
        } else {
        	return setCustomAttribute(attributeName, attributeValue);
        }
    }

    public CustomObjectAttribute setCustomAttribute(String attributeName, Object attributeValue) {
        CustomObjectAttribute attribute = new CustomObjectAttribute(attributeName, attributeValue);
        typedCustomAttributes.remove(attribute);
        typedCustomAttributes.add(attribute);
        
        return attribute;
    }

    public CustomObjectAttribute setCustomAttribute(String attributeName, List<Object> attributeValue) {
        CustomObjectAttribute attribute = new CustomObjectAttribute(attributeName, attributeValue);
        attribute.setMultiValued(true);
        typedCustomAttributes.remove(attribute);
        typedCustomAttributes.add(attribute);
        
        return attribute;
    }

}
