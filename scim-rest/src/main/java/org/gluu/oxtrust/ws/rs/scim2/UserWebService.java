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
import static org.gluu.oxtrust.model.scim2.patch.PatchOperationType.REMOVE;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.management.InvalidAttributeValueException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxtrust.model.GluuCustomPerson;

import org.gluu.oxtrust.model.exception.SCIMException;
import org.gluu.oxtrust.model.scim2.BaseScimResource;
import org.gluu.oxtrust.model.scim2.ErrorScimType;
import org.gluu.oxtrust.model.scim2.SearchRequest;
import org.gluu.oxtrust.model.scim2.patch.PatchOperation;
import org.gluu.oxtrust.model.scim2.patch.PatchRequest;
import org.gluu.oxtrust.model.scim2.user.UserResource;
import org.gluu.oxtrust.model.scim2.util.DateUtil;
import org.gluu.oxtrust.model.scim2.util.ScimResourceUtil;
import org.gluu.oxtrust.service.filter.ProtectedApi;
import org.gluu.oxtrust.model.scim.ScimCustomPerson;
import org.gluu.oxtrust.service.scim2.Scim2PatchService;
import org.gluu.oxtrust.service.scim2.Scim2UserService;
import org.gluu.oxtrust.service.scim2.interceptor.RefAdjusted;
import org.gluu.persist.exception.operation.DuplicateEntryException;
import org.gluu.persist.model.PagedResult;
import org.gluu.persist.model.SortOrder;

/**
 * Implementation of /Users endpoint. Methods here are intercepted.
 * Filter org.gluu.oxtrust.filter.AuthorizationProcessingFilter secures invocations
 */
@Named
@Path("/scim/v2/Users")
public class UserWebService extends BaseScimWebService implements IUserWebService {

    @Inject
    private Scim2UserService scim2UserService;

    @Inject
    private Scim2PatchService scim2PatchService;
    
    private String userResourceType;

    public Response validateExistenceOfUser(String id) {

        Response response = null;
        if (StringUtils.isNotEmpty(id) && personService.getPersonByInum(id) == null) {
            log.info("Person with inum {} not found", id);
            response = getErrorResponse(Response.Status.NOT_FOUND, 
                    "User with id " + id + " not found");
        }
        return response;

    }

    private void checkUidExistence(String uid) throws DuplicateEntryException {
        if (personService.getPersonByUid(uid) != null) {
            throw new DuplicateEntryException("Duplicate UID value: " + uid);
        }
    }
    
    private void checkUidExistence(String uid, String id) throws DuplicateEntryException {

        // Validate if there is an attempt to supply a userName already in use by a user other than current
        List<GluuCustomPerson> list = null;
        try {
            list = personService.findPersonsByUids(Collections.singletonList(uid), new String[]{"inum"});
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        if (list != null &&
            list.stream().filter(p -> !p.getInum().equals(id)).findAny().isPresent()) {
            throw new DuplicateEntryException("Duplicate UID value: " + uid);
        }

    }

    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    @RefAdjusted
    public Response createUser(
            UserResource user,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. createUser");

            executeDefaultValidation(user);
            checkUidExistence(user.getUserName());
            assignMetaInformation(user);
            ScimResourceUtil.adjustPrimarySubAttributes(user);

            ScimCustomPerson person = scim2UserService.preCreateUser(user);
            response = externalContraintsService.applyEntityCheck(person, httpHeaders,
                    uriInfo, HttpMethod.POST, userResourceType);
            if (response != null) return response;

            scim2UserService.createUser(person, user, endpointUrl);
            String json = resourceSerializer.serialize(user, attrsList, excludedAttrsList);
            response = Response.created(new URI(user.getMeta().getLocation())).entity(json).build();
        } catch (DuplicateEntryException e) {
            log.error(e.getMessage());
            response = getErrorResponse(Response.Status.CONFLICT, ErrorScimType.UNIQUENESS, e.getMessage());
        } catch (SCIMException e) {
            log.error("Validation check at createUser returned: {}", e.getMessage());
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_VALUE, e.getMessage());
        } catch (Exception e) {
            log.error("Failure at createUser method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path("{id}")
    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    @RefAdjusted
    public Response getUserById(
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. getUserById");

            response = validateExistenceOfUser(id);
            if (response != null) return response;

            ScimCustomPerson person = userPersistenceHelper.getPersonByInum(id);
            response = externalContraintsService.applyEntityCheck(person, httpHeaders,
                    uriInfo, HttpMethod.GET, userResourceType);
            if (response != null) return response;
            
            UserResource user = new UserResource();
            scim2UserService.buildUserResource(person, user, endpointUrl);
            String json = resourceSerializer.serialize(user, attrsList, excludedAttrsList);
            response = Response.ok(new URI(user.getMeta().getLocation())).entity(json).build();
        } catch (Exception e) {
            log.error("Failure at getUserById method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    /**
     * This implementation differs from spec in the following aspects:
     * - Passing a null value for an attribute, does not modify the attribute in the destination, however passing an
     * empty array for a multivalued attribute does clear the attribute. Thus, to clear single-valued attribute, PATCH
     * operation should be used
     */
    @Path("{id}")
    @PUT
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    @RefAdjusted
    public Response updateUser(
            UserResource user,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. updateUser");

            //Check if the ids match in case the user coming has one
            if (user.getId() != null && !user.getId().equals(id))
                throw new SCIMException("Parameter id does not match with id attribute of User");

            response = validateExistenceOfUser(id);
            if (response !=null) return response;

            executeValidation(user, true);
            if (StringUtils.isNotEmpty(user.getUserName())) {
                checkUidExistence(user.getUserName(), id);
            }

            ScimResourceUtil.adjustPrimarySubAttributes(user);            
            ScimCustomPerson person = userPersistenceHelper.getPersonByInum(id);
            response = externalContraintsService.applyEntityCheck(person, httpHeaders,
                    uriInfo, HttpMethod.PUT, userResourceType);
            if (response != null) return response;

            UserResource updatedResource = scim2UserService.updateUser(person, user, endpointUrl);
            String json = resourceSerializer.serialize(updatedResource, attrsList, excludedAttrsList);
            response = Response.ok(new URI(updatedResource.getMeta().getLocation())).entity(json).build();

        } catch (DuplicateEntryException e) {
            log.error(e.getMessage());
            response = getErrorResponse(Response.Status.CONFLICT, ErrorScimType.UNIQUENESS, e.getMessage());
        } catch (SCIMException e) {
            log.error("Validation check at updateUser returned: {}", e.getMessage());
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_VALUE, e.getMessage());
        } catch (InvalidAttributeValueException e) {
            log.error(e.getMessage());
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.MUTABILITY, e.getMessage());
        } catch (Exception e) {
            log.error("Failure at updateUser method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path("{id}")
    @DELETE
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    public Response deleteUser(@PathParam("id") String id) {

        Response response;
        try {
            log.debug("Executing web service method. deleteUser");
            
            response = validateExistenceOfUser(id);
            if (response != null) return response;
            
            ScimCustomPerson person = userPersistenceHelper.getPersonByInum(id);
            response = externalContraintsService.applyEntityCheck(person, httpHeaders,
                    uriInfo, HttpMethod.DELETE, userResourceType);
            if (response != null) return response;
            
            scim2UserService.deleteUser(person);
            response = Response.noContent().build();
        } catch (Exception e) {
            log.error("Failure at deleteUser method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    @RefAdjusted
    public Response searchUsers(
            @QueryParam(QUERY_PARAM_FILTER) String filter,
            @QueryParam(QUERY_PARAM_START_INDEX) Integer startIndex,
            @QueryParam(QUERY_PARAM_COUNT) Integer count,
            @QueryParam(QUERY_PARAM_SORT_BY) String sortBy,
            @QueryParam(QUERY_PARAM_SORT_ORDER) String sortOrder,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. searchUsers");
            
            SearchRequest searchReq = new SearchRequest();
            response = prepareSearchRequest(searchReq.getSchemas(), filter, sortBy, 
                    sortOrder, startIndex, count, attrsList, excludedAttrsList, searchReq);

            if (response != null) return response;
            
            sortBy = translateSortByAttribute(UserResource.class, sortBy);
            PagedResult<BaseScimResource> resources = scim2UserService.searchUsers(filter, sortBy, SortOrder.getByValue(sortOrder),
                    startIndex, count, endpointUrl, getMaxCount());

            String json = getListResponseSerialized(resources.getTotalEntriesCount(), startIndex, resources.getEntries(), attrsList, excludedAttrsList, count==0);
            response = Response.ok(json).location(new URI(endpointUrl)).build();
        } catch (SCIMException e) {
            log.error(e.getMessage(), e);
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_FILTER, e.getMessage());
        } catch (Exception e) {
            log.error("Failure at searchUsers method", e);
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
    public Response searchUsersPost(SearchRequest searchRequest) {

        log.debug("Executing web service method. searchUsersPost");
        
        SearchRequest searchReq = new SearchRequest();
        Response response = prepareSearchRequest(searchRequest.getSchemas(), searchRequest.getFilter(), 
                searchRequest.getSortBy(), searchRequest.getSortOrder(), searchRequest.getStartIndex(), 
                searchRequest.getCount(), searchRequest.getAttributesStr(), searchRequest.getExcludedAttributesStr(), 
                searchReq);

        if (response != null) return response;

        //Calling searchUsers here does not provoke that method's interceptor being called (only this one's)
        URI uri = null;
        response = searchUsers(searchRequest.getFilter(),searchRequest.getStartIndex(), searchRequest.getCount(),
                searchRequest.getSortBy(), searchRequest.getSortOrder(), searchRequest.getAttributesStr(), searchRequest.getExcludedAttributesStr());

        try {
            uri = new URI(endpointUrl + "/" + SEARCH_SUFFIX);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Response.fromResponse(response).location(uri).build();

    }

    @Path("{id}")
    @PATCH
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi
    @RefAdjusted
    public Response patchUser(
            PatchRequest request,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try{
            log.debug("Executing web service method. patchUser");
            
            response = inspectPatchRequest(request, UserResource.class);
            if (response != null) return response;
            
            response = validateExistenceOfUser(id);
            if (response != null) return response;
            
            ScimCustomPerson person=userPersistenceHelper.getPersonByInum(id);
            response = externalContraintsService.applyEntityCheck(person, httpHeaders,
                    uriInfo, HttpMethod.PATCH, userResourceType);
            if (response != null) return response;

            UserResource user=new UserResource();
            //Fill user instance with all info from person
            scim2UserService.transferAttributesToUserResource(person, user, endpointUrl);

            //Apply patches one by one in sequence
            for (PatchOperation po : request.getOperations()) {
                //Handle special case: https://github.com/GluuFederation/oxTrust/issues/800
                if (po.getType().equals(REMOVE) && po.getPath().equals("pairwiseIdentifiers")){
                    //If this block weren't here, the implementation will throw error because read-only attribute cannot be altered
                    person.setOxPPID(null);
                    user.setPairwiseIdentifiers(null);
                    scim2UserService.removePPIDsBranch(person.getDn());
                } else {
                    user = (UserResource) scim2PatchService.applyPatchOperation(user, po);
                }
            }

            //Throws exception if final representation does not pass overall validation
            log.debug("patchUser. Revising final resource representation still passes validations");
            executeDefaultValidation(user);
            ScimResourceUtil.adjustPrimarySubAttributes(user);

            //Update timestamp
            user.getMeta().setLastModified(DateUtil.millisToISOString(System.currentTimeMillis()));

            //Replaces the information found in person with the contents of user
            scim2UserService.replacePersonInfo(person, user, endpointUrl);

            String json = resourceSerializer.serialize(user, attrsList, excludedAttrsList);
            response = Response.ok(new URI(user.getMeta().getLocation())).entity(json).build();
        } catch (InvalidAttributeValueException e) {
            log.error(e.getMessage(), e);
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.MUTABILITY, e.getMessage());
        } catch (SCIMException e) {
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_SYNTAX, e.getMessage());
        } catch (Exception e) {
            log.error("Failure at patchUser method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @PostConstruct
    public void setup(){
        //Do not use getClass() here...
        init(UserWebService.class);
        userResourceType = ScimResourceUtil.getType(UserResource.class);
    }

}
