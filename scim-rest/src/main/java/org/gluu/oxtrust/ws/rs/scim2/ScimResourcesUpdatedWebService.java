package org.gluu.oxtrust.ws.rs.scim2;

//import org.gluu.oxtrust.ldap.service.IGroupService;
//import org.gluu.oxtrust.model.GluuGroup;
import org.apache.commons.lang.StringUtils;
import org.gluu.model.attribute.AttributeDataType;
import org.gluu.oxtrust.model.scim.ScimCustomPerson;
//import org.gluu.oxtrust.model.scim2.group.GroupResource;
import org.gluu.oxtrust.model.scim2.util.DateUtil;
import org.gluu.oxtrust.model.scim2.util.IntrospectUtil;
import org.gluu.oxtrust.service.AttributeService;
import org.gluu.oxtrust.service.antlr.scimFilter.ScimFilterParserService;
//import org.gluu.oxtrust.service.scim2.Scim2GroupService;
import org.gluu.oxtrust.service.filter.ProtectedApi;
import org.gluu.oxtrust.util.ServiceUtil;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.model.SortOrder;
import org.gluu.search.filter.Filter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.gluu.oxtrust.model.scim2.Constants.UTF8_CHARSET_FRAGMENT;

@Named
@Path("/scim")
public class ScimResourcesUpdatedWebService extends BaseScimWebService {

    @Inject
    private PersistenceEntryManager entryManager;

    @Inject
    private ScimFilterParserService scimFilterParserService;

    @Inject
    private AttributeService attributeService;

    private boolean ldapBackend;

    private Map<String, AttributeDataType> attributeDataTypes;
/*
    @Inject
    private UserWebService userWebService;
    @Inject
    private IGroupService groupService;

    @Inject
    private Scim2GroupService scim2GroupService;

    @Inject
    private GroupWebService groupWebService;
*/

    @Path("UpdatedUsers")
    @GET
    @Produces(MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT)
    @ProtectedApi(scopes = { "https://gluu.org/scim/users.read" })
    public Response usersChangedAfter(@QueryParam("timeStamp") String isoDate,
                                      @QueryParam("start") int start,
                                      @QueryParam("pageSize") int itemsPerPage) {

        Response response;
        log.debug("Executing web service method. usersChangedAfter");

        try {
            if (start < 0 || itemsPerPage <=0) {
                return getErrorResponse(Response.Status.BAD_REQUEST, "No suitable value for 'start' or 'pageSize' params");
            }

            String date = ldapBackend ? DateUtil.ISOToGeneralizedStringDate(isoDate) : DateUtil.gluuCouchbaseISODate(isoDate);
            if (date == null) {
                response = getErrorResponse(Response.Status.BAD_REQUEST, "Unparsable date: " + isoDate);
            } else {
                log.info("Searching users updated or created after {} (starting at index {} - at most {} results)", date, start, itemsPerPage);
                Filter filter = Filter.createORFilter(
                        Filter.createGreaterOrEqualFilter("oxCreationTimestamp", date),
                        Filter.createGreaterOrEqualFilter("updatedAt", date));
                log.trace("Using filter {}", filter.toString());

                List<ScimCustomPerson> list = entryManager.findPagedEntries(personService.getDnForPerson(null), ScimCustomPerson.class,
                        filter, null,  "uid", SortOrder.ASCENDING, start, itemsPerPage, getMaxCount()).getEntries();

                response = Response.ok(getUserResultsAsJson(list)).build();
            }
        } catch (Exception e1) {
            log.error("Failure at usersChangedAfter method", e1);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e1.getMessage());
        }
        return response;

    }

    private String getUserResultsAsJson(List<ScimCustomPerson> list) throws Exception {

        List<Map<String, List<Object>>> resources = new ArrayList<>();
        long fresher = 0;

        for (ScimCustomPerson person : list) {
            long updatedAt = Optional.ofNullable(person.getUpdatedAt()).map(Date::getTime).orElse(0L);
            if (fresher < updatedAt) {
                fresher = updatedAt;
            }

            Map<String, List<Object>> map = new TreeMap<>();
            person.getTypedCustomAttributes().forEach(attr -> map.put(attr.getName(), new ArrayList<>(attr.getValues())));
            map.putAll(getNonCustomAttributes(person));

            //Do a best effort to supply output in proper data types
            for (String key : map.keySet()) {
                List<Object> values = map.get(key);
                for (int i = 0; i < values.size(); i++) {

                    Object rawValue = values.get(i);
                    String value = rawValue.toString();
                    Object finalValue = null;

                    AttributeDataType dataType = Optional.ofNullable(attributeDataTypes.get(key)).orElse(AttributeDataType.STRING);
                    switch (dataType) {
                        case DATE:
                            finalValue = getStringDateFrom(value);
                            break;
                        case BOOLEAN:
                            if (ldapBackend) {
                                value = value.toLowerCase();
                            }
                            if (value.equals(Boolean.TRUE.toString()) || value.equals(Boolean.FALSE.toString())) {
                                finalValue = Boolean.valueOf(value);
                            }
                            break;
                        case NUMERIC:
                            try {
                                finalValue = new Integer(value);
                            } catch (Exception e) {
                                log.warn("{} is not a numeric value!", value);
                            }
                            break;
                    }

                    if (finalValue == null) {
                        if (rawValue.getClass().equals(Date.class)) {
                            Instant instant = Instant.ofEpochMilli(Date.class.cast(rawValue).getTime());
                            finalValue = DateTimeFormatter.ISO_INSTANT.format(instant);
                        } else {
                            finalValue = getStringDateFrom(value);
                            finalValue = finalValue == null ? value : finalValue;
                        }
                    }
                    values.set(i, finalValue);
                }
            }

            resources.add(map);
        }
        return getResultsAsJson(resources, fresher);

    }

    private Map<String, List<Object>> getNonCustomAttributes(ScimCustomPerson person) {

        Map<String, List<Object>> map = new HashMap<>();
        Field[] fields = ScimCustomPerson.class.getDeclaredFields();

        for (Field field : fields) {
            try {
                AttributeName annotation = field.getAnnotation(AttributeName.class);
                if (annotation != null) {

                    String fieldName = field.getName();
                    String attribute = StringUtils.isEmpty(annotation.name()) ? fieldName : annotation.name();
                    Method getter = IntrospectUtil.getGetter(fieldName, ScimCustomPerson.class);

                    if (getter != null) {
                        Object value = getter.invoke(person);
                        if (value != null) {
                            map.put(attribute, new ArrayList<>(Collections.singletonList(value)));
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return map;

    }

    private String getResultsAsJson(List<?> resources, long fresher) throws Exception {

        int total = resources.size();
        log.info("Found {} matching entries", total);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        if (fresher > 0) {
            result.put("latestUpdateAt", DateUtil.millisToISOString(fresher));
        }
        result.put("results", resources);

        return ServiceUtil.getObjectMapper().writeValueAsString(result);

    }

    private String getStringDateFrom(String str) {
        return ldapBackend ? DateUtil.generalizedToISOStringDate(str) : DateUtil.gluuCouchbaseISODate(str);
    }

    @PostConstruct
    private void init() {
        init(ScimResourcesUpdatedWebService.class);
        ldapBackend = scimFilterParserService.isLdapBackend();
        attributeDataTypes = new HashMap<>();
        attributeService.getAllAttributes().forEach(ga -> attributeDataTypes.put(ga.getName(), ga.getDataType()));
    }

/*
    //Groups endpoint not necessary, but if needed, we need to guarantee first that oxTrustMetaLastModified is refreshed
    //whenever the group is updated in GUI or via SCIM (or cust script)
    //@Path("UpdatedGroups")
    //@GET
    @Produces(MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT)
    @ProtectedApi
    public Response groupsChangedAfter(@QueryParam("timeStamp") String isoDate) {

        Response response;
        log.debug("Executing web service method. groupsChangedAfter");

        try {
            String date = ZonedDateTime.parse(isoDate).format(DateTimeFormatter.ISO_INSTANT);
            //In database, oxTrustMetaLastModified is just a string (not date)

            Filter filter = Filter.createORFilter(
                    Filter.createNOTFilter(Filter.createPresenceFilter("oxTrustMetaLastModified")),
                    Filter.createGreaterOrEqualFilter("oxTrustMetaLastModified", date));
            List<GluuGroup> list = entryManager.findEntries(groupService.getDnForGroup(null), GluuGroup.class, filter);
            response = Response.ok(getGroupResultsAsJson(list)).build();

        } catch (DateTimeParseException e) {
            response = getErrorResponse(Response.Status.BAD_REQUEST, e.getMessage());
        } catch (Exception e1) {
            log.error("Failure at groupsChangedAfter method", e1);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e1.getMessage());
        }
        return response;

    }

    private String getGroupResultsAsJson(List<GluuGroup> list) throws Exception {

        List<BaseScimResource> resources = new ArrayList<>();
        long fresher = 0;

        for (GluuGroup group : list) {
            GroupResource scimGroup = new GroupResource();
            scim2GroupService.transferAttributesToGroupResource(group, scimGroup, userWebService.getEndpointUrl(), groupWebService.getEndpointUrl());
            resources.add(scimGroup);

            String modified = group.getAttribute("oxTrustMetaLastModified");
            try {
                if (modified != null) {
                    long updatedAt = ZonedDateTime.parse(modified).toInstant().toEpochMilli();
                    if (fresher < updatedAt) {
                        fresher = updatedAt;
                    }
                }
            } catch (Exception e) {
                log.error("Error parsing supposed ISO date {}", modified);
            }
        }
        return  getResultsAsJson(resources, fresher);

    }
    */

}
