package org.gluu.oxtrust.ws.rs.scim2;

import static org.gluu.oxtrust.model.scim2.Constants.MEDIA_TYPE_SCIM_JSON;
import static org.gluu.oxtrust.model.scim2.Constants.QUERY_PARAM_ATTRIBUTES;
import static org.gluu.oxtrust.model.scim2.Constants.QUERY_PARAM_COUNT;
import static org.gluu.oxtrust.model.scim2.Constants.QUERY_PARAM_EXCLUDED_ATTRS;
import static org.gluu.oxtrust.model.scim2.Constants.QUERY_PARAM_FILTER;
import static org.gluu.oxtrust.model.scim2.Constants.QUERY_PARAM_SORT_BY;
import static org.gluu.oxtrust.model.scim2.Constants.QUERY_PARAM_SORT_ORDER;
import static org.gluu.oxtrust.model.scim2.Constants.QUERY_PARAM_START_INDEX;
import static org.gluu.oxtrust.model.scim2.Constants.UTF8_CHARSET_FRAGMENT;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.time.Instant;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.management.InvalidAttributeValueException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.gluu.oxtrust.model.exception.SCIMException;
import org.gluu.oxtrust.model.GluuFido2Device;
import org.gluu.oxtrust.model.scim2.*;
import org.gluu.oxtrust.model.scim2.fido.Fido2DeviceResource;
import org.gluu.oxtrust.model.scim2.patch.PatchRequest;
import org.gluu.oxtrust.model.scim2.util.DateUtil;
import org.gluu.oxtrust.model.scim2.util.ScimResourceUtil;
import org.gluu.oxtrust.service.Fido2DeviceService;
import org.gluu.oxtrust.service.antlr.scimFilter.ScimFilterParserService;
import org.gluu.oxtrust.service.filter.ProtectedApi;
import org.gluu.oxtrust.service.scim2.interceptor.RefAdjusted;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.PagedResult;
import org.gluu.persist.model.SortOrder;
import org.gluu.search.filter.Filter;

/**
 * Implementation of /Fido2Devices endpoint. Methods here are intercepted.
 * Filter org.gluu.oxtrust.ws.rs.scim2.AuthorizationProcessingFilter secures invocations
 */
@Named("scim2Fido2DeviceEndpoint")
@Path("/scim/v2/Fido2Devices")
public class Fido2DeviceWebService extends BaseScimWebService implements IFido2DeviceWebService {

    @Inject
    private Fido2DeviceService fidoDeviceService;

    @Inject
    private ScimFilterParserService scimFilterParserService;

    @Inject
    private PersistenceEntryManager entryManager;


    private Response validateExistenceOfDevice(String userId, String id) {
        
        //userId can be null here
        Response response = null;

        GluuFido2Device device = StringUtils.isEmpty(id) ? null : fidoDeviceService.getFido2DeviceById(userId, id);
        if (device == null) {
            log.info("Device with id {} not found", id);
            response = getErrorResponse(Response.Status.NOT_FOUND, ErrorScimType.INVALID_VALUE, 
                    "Resource " + id + " not found");
        }
        return response;

    }
    
    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    public Response createDevice() {
        log.debug("Executing web service method. createDevice");
        return getErrorResponse(Response.Status.NOT_IMPLEMENTED, "Not implemented; device registration only happens via the FIDO 2.0 API.");
    }

    @Path("{id}")
    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    @RefAdjusted
    public Response getF2DeviceById(@PathParam("id") String id,
                                  @QueryParam("userId") String userId,
                                  @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
                                  @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try{
            log.debug("Executing web service method. getF2DeviceById");
            
            response = validateExistenceOfDevice(userId, id);
            if (response != null) return response;
            
            Fido2DeviceResource fidoResource = new Fido2DeviceResource();

            GluuFido2Device device = fidoDeviceService.getFido2DeviceById(userId, id);

            transferAttributesToFido2Resource(device, fidoResource, endpointUrl, getUserInumFromDN(device.getDn()));

            String json = resourceSerializer.serialize(fidoResource, attrsList, excludedAttrsList);
            response = Response.ok(new URI(fidoResource.getMeta().getLocation())).entity(json).build();
        } catch (SCIMException e) {
            log.error(e.getMessage());
            response = getErrorResponse(Response.Status.NOT_FOUND, ErrorScimType.INVALID_VALUE, e.getMessage());
        } catch (Exception e) {
            log.error("Failure at getF2DeviceById method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path("{id}")
    @PUT
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    @RefAdjusted
    public Response updateF2Device(
            Fido2DeviceResource fidoDeviceResource,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList){

        Response response;
        try {
            log.debug("Executing web service method. updateDevice");

            //remove externalId, no place to store it in LDAP
            fidoDeviceResource.setExternalId(null);

            if (fidoDeviceResource.getId() != null && !fidoDeviceResource.getId().equals(id))
                throw new SCIMException("Parameter id does not match id attribute of Device");

            response = validateExistenceOfDevice(fidoDeviceResource.getUserId(), id);

            if (response != null) return response;
            
            executeValidation(fidoDeviceResource, true);
                
            String userId = fidoDeviceResource.getUserId();
            GluuFido2Device device = fidoDeviceService.getFido2DeviceById(userId, id);
            if (device == null)
                return getErrorResponse(Response.Status.NOT_FOUND, ErrorScimType.INVALID_VALUE, 
                        "Resource " + id + " not found");

            Fido2DeviceResource updatedResource = new Fido2DeviceResource();
            transferAttributesToFido2Resource(device, updatedResource, endpointUrl, userId);

            updatedResource.getMeta().setLastModified(DateUtil.millisToISOString(System.currentTimeMillis()));

            updatedResource = (Fido2DeviceResource) ScimResourceUtil.transferToResourceReplace(fidoDeviceResource,
                    updatedResource, extService.getResourceExtensions(updatedResource.getClass()));
            transferAttributesToDevice(updatedResource, device);

            fidoDeviceService.updateFido2Device(device);

            String json =resourceSerializer.serialize(updatedResource, attrsList, excludedAttrsList);
            response = Response.ok(new URI(updatedResource.getMeta().getLocation())).entity(json).build();
        } catch (SCIMException e) {
            log.error("Validation check at updateF2Device returned: {}", e.getMessage());
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_VALUE, e.getMessage());
        }
        catch (InvalidAttributeValueException e){
            log.error(e.getMessage());
            response=getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.MUTABILITY, e.getMessage());
        }
        catch (Exception e){
            log.error("Failure at updateDevice method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path("{id}")
    @DELETE
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    public Response deleteF2Device(@PathParam("id") String id) {

        Response response;
        try {
            log.debug("Executing web service method. deleteDevice");

            response = validateExistenceOfDevice(null, id);
            if (response != null) return response;
            
            //No need to check id being non-null. fidoDeviceService will give null if null is provided
            GluuFido2Device device = fidoDeviceService.getFido2DeviceById(null, id);
            if (device != null) {
                fidoDeviceService.removeFido2Device(device);
                response = Response.noContent().build();
            } else {
                response = getErrorResponse(Response.Status.NOT_FOUND, ErrorScimType.INVALID_VALUE, 
                        "Resource " + id + " not found");
            }
        } catch (Exception e){
            log.error("Failure at deleteDevice method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                    "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    @RefAdjusted
    public Response searchF2Devices(
            @QueryParam("userId") String userId,
            @QueryParam(QUERY_PARAM_FILTER) String filter,
            @QueryParam(QUERY_PARAM_START_INDEX) Integer startIndex,
            @QueryParam(QUERY_PARAM_COUNT) Integer count,
            @QueryParam(QUERY_PARAM_SORT_BY) String sortBy,
            @QueryParam(QUERY_PARAM_SORT_ORDER) String sortOrder,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. searchDevices");
            
            SearchRequest searchReq = new SearchRequest();
            response = prepareSearchRequest(searchReq.getSchemas(), filter, null, sortBy, sortOrder, startIndex, count,
                attrsList, excludedAttrsList, searchReq);

            if (response != null) return response;
            
            response = validateExistenceOfUser(userId);
            if (response != null) return response;

            PagedResult<BaseScimResource> resources = searchDevices(userId, searchReq.getFilter(), 
                    translateSortByAttribute(Fido2DeviceResource.class, searchReq.getSortBy()), 
                    SortOrder.getByValue(searchReq.getSortOrder()), searchReq.getStartIndex(),
                    searchReq.getCount(), endpointUrl);

            String json = getListResponseSerialized(resources.getTotalEntriesCount(), 
                    searchReq.getStartIndex(), resources.getEntries(), searchReq.getAttributesStr(),
                    searchReq.getExcludedAttributesStr(), searchReq.getCount() == 0);
            response = Response.ok(json).location(new URI(endpointUrl)).build();
        } catch (SCIMException e) {
            log.error(e.getMessage(), e);
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_FILTER, e.getMessage());
        } catch (Exception e){
            log.error("Failure at searchF2Devices method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path(SEARCH_SUFFIX)
    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    @RefAdjusted
    public Response searchF2DevicesPost(SearchRequest searchRequest, @QueryParam("userId") String userId) {

        log.debug("Executing web service method. searchDevicesPost");

        SearchRequest searchReq = new SearchRequest();
        Response response = prepareSearchRequest(searchRequest.getSchemas(), searchRequest.getFilter(), null, searchRequest.getSortBy(),
                searchRequest.getSortOrder(), searchRequest.getStartIndex(), searchRequest.getCount(),
                searchRequest.getAttributesStr(), searchRequest.getExcludedAttributesStr(), searchReq);

        if (response != null) return response;
            
        response = validateExistenceOfUser(userId);
        if (response != null) return response;

        URI uri=null;
        response = searchF2Devices(userId, searchReq.getFilter(), searchReq.getStartIndex(), 
                searchReq.getCount(), searchReq.getSortBy(), searchReq.getSortOrder(), 
                searchRequest.getAttributesStr(), searchRequest.getExcludedAttributesStr());

        try {
            uri = new URI(endpointUrl + "/" + SEARCH_SUFFIX);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Response.fromResponse(response).location(uri).build();

    }

    private void transferAttributesToFido2Resource(GluuFido2Device fidoDevice, Fido2DeviceResource res, String url, String userId) {

        res.setId(fidoDevice.getId());

        Meta meta=new Meta();
        meta.setResourceType(ScimResourceUtil.getType(res.getClass()));
        meta.setCreated(DateUtil.millisToISOString(fidoDevice.getCreationDate().getTime()));
        meta.setLastModified(DateUtil.millisToISOString(fidoDevice.getRegistrationData().getUpdatedDate().getTime()));
        meta.setLocation(url + "/" + fidoDevice.getId());

        res.setMeta(meta);
        res.setUserId(userId);
        res.setCreationDate(meta.getCreated());
        res.setCounter(fidoDevice.getRegistrationData().getCounter());

        res.setStatus(fidoDevice.getRegistrationStatus());
        res.setDisplayName(fidoDevice.getDisplayName());

    }

    private void transferAttributesToDevice(Fido2DeviceResource res, GluuFido2Device device){

        device.setId(res.getId());

        device.getRegistrationData().setCounter(res.getCounter());
        device.setRegistrationStatus(res.getStatus());
        device.setDisplayName(res.getDisplayName());
        
        Instant instant = Instant.parse(res.getMeta().getLastModified());
        device.getRegistrationData().setUpdatedDate(new Date(instant.toEpochMilli()));

    }

    private PagedResult<BaseScimResource> searchDevices(String userId, String filter, String sortBy, SortOrder sortOrder, int startIndex,
                                                        int count, String url) throws Exception {

        Filter ldapFilter=scimFilterParserService.createFilter(filter, Filter.createPresenceFilter("oxId"), Fido2DeviceResource.class);
        log.info("Executing search for fido devices using: ldapfilter '{}', sortBy '{}', sortOrder '{}', startIndex '{}', count '{}', userId '{}'",
                ldapFilter.toString(), sortBy, sortOrder.getValue(), startIndex, count, userId);

        //workaround for https://github.com/GluuFederation/scim/issues/1: 
        //Currently, searching with SUB scope in Couchbase requires some help (beyond use of baseDN) 
        if (StringUtils.isNotEmpty(userId)) {
        	ldapFilter=Filter.createANDFilter(ldapFilter, Filter.createEqualityFilter("personInum", userId));
        }

        PagedResult<GluuFido2Device> list;
        try {
            list = entryManager.findPagedEntries(fidoDeviceService.getDnForFido2Device(null, userId),
                    GluuFido2Device.class, ldapFilter, null, sortBy, sortOrder, startIndex - 1, count, getMaxCount());
        } catch (Exception e) {
            log.info("Returning an empty listViewReponse");
            log.error(e.getMessage(), e);
            list = new PagedResult<>();
            list.setEntries(new ArrayList<>());
        }
        List<BaseScimResource> resources=new ArrayList<>();

        for (GluuFido2Device device : list.getEntries()){
            Fido2DeviceResource scimDev=new Fido2DeviceResource();
            transferAttributesToFido2Resource(device, scimDev, url, getUserInumFromDN(device.getDn()));
            resources.add(scimDev);
        }
        log.info ("Found {} matching entries - returning {}", list.getTotalEntriesCount(), list.getEntries().size());

        PagedResult<BaseScimResource> result = new PagedResult<>();
        result.setEntries(resources);
        result.setTotalEntriesCount(list.getTotalEntriesCount());

        return result;

    }

    @Path("{id}")
    @PATCH
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    @RefAdjusted
    public Response patchF2Device(
            PatchRequest request,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList){

        log.debug("Executing web service method. patchDevice");
        return getErrorResponse(Response.Status.NOT_IMPLEMENTED, "Patch operation not supported for FIDO devices");
    }

    @PostConstruct
    public void setup(){
        init(Fido2DeviceWebService.class);
    }

}
