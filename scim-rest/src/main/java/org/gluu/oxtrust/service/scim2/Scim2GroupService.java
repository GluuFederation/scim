package org.gluu.oxtrust.service.scim2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.gluu.model.GluuStatus;
import org.gluu.oxtrust.model.GluuCustomPerson;
import org.gluu.oxtrust.model.GluuGroup;
import org.gluu.oxtrust.model.scim2.BaseScimResource;
import org.gluu.oxtrust.model.scim2.Meta;
import org.gluu.oxtrust.model.scim2.group.GroupResource;
import org.gluu.oxtrust.model.scim2.group.Member;
import org.gluu.oxtrust.model.scim2.user.UserResource;
import org.gluu.oxtrust.model.scim2.util.DateUtil;
import org.gluu.oxtrust.model.scim2.util.ScimResourceUtil;
import org.gluu.oxtrust.service.IGroupService;
import org.gluu.oxtrust.service.IPersonService;
import org.gluu.oxtrust.service.OrganizationService;
import org.gluu.oxtrust.service.antlr.scimFilter.ScimFilterParserService;
import org.gluu.oxtrust.service.external.ExternalScimService;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.model.PagedResult;
import org.gluu.persist.model.SortOrder;
import org.gluu.search.filter.Filter;

import org.slf4j.Logger;

@ApplicationScoped
public class Scim2GroupService implements Serializable {

	private static final long serialVersionUID = 1555887165477267426L;

	@Inject
	private Logger log;

	@Inject
	private IPersonService personService;

	@Inject
	private IGroupService groupService;

	@Inject
	private ExternalScimService externalScimService;

	@Inject
	private OrganizationService organizationService;

	@Inject
	private ExtensionService extService;

	@Inject
	private ScimFilterParserService scimFilterParserService;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

    @Inject
    private UserPersistenceHelper userPersistenceHelper;

	private void transferAttributesToGroup(GroupResource res, GluuGroup group,
            boolean skipMembersValidation, boolean fillMissingDisplayKnownMember, String usersUrl) {

		// externalId (so oxTrustExternalId) not part of LDAP schema
		group.setAttribute("oxTrustMetaCreated", res.getMeta().getCreated());
		group.setAttribute("oxTrustMetaLastModified", res.getMeta().getLastModified());
		// When creating group, location will be set again when having an inum
		group.setAttribute("oxTrustMetaLocation", res.getMeta().getLocation());

		group.setDisplayName(res.getDisplayName());
		group.setStatus(GluuStatus.ACTIVE);
		group.setOrganization(organizationService.getDnForOrganization());

		Set<Member> members = res.getMembers();
		if (members != null && members.size() > 0) {
                    
                    Set<String> groupMembers = group.getMembers().stream()
                            .map(userPersistenceHelper::getUserInumFromDN).collect(
                                    Collectors.toCollection(HashSet::new));

			List<String> listMembers = new ArrayList<>();
			List<Member> invalidMembers = new ArrayList<>();

                        // Add the members, and complement the $refs and users' display names in res
			for (Member member : members) {
				GluuCustomPerson person;
                            // it's not null as it is required in GroupResource
				String inum = member.getValue();

                                if (!skipMembersValidation && (!groupMembers.contains(inum) ||
                                        (fillMissingDisplayKnownMember && member.getDisplay() == null))) {
                                    //when the member is known, data carried in getDisplay is trusted

                                    person = personService.getPersonByInum(inum);
                                    if (person != null) {
                                        member.setDisplay(person.getDisplayName());
                                    }
                                } else {
                                    person = new GluuCustomPerson();
                                    person.setDn(personService.getDnForPerson(inum));
                                }

				if (person == null) {
					log.info("Member identified by {} does not exist. Ignored", inum);
					invalidMembers.add(member);
				} else {
					member.setRef(usersUrl + "/" + inum);
					member.setType(ScimResourceUtil.getType(UserResource.class));
                                        
                                        if (skipMembersValidation) {
                                            //when no validation takes place, display values may not be
                                            //reliable (ie. supplied by application consuming the service)
                                            member.setDisplay(null);
                                        }

					listMembers.add(person.getDn());
				}
			}
			group.setMembers(listMembers);

			members.removeAll(invalidMembers);
                        if (members.isEmpty()) {
                            res.setMembers(null);
                        }                        
		} else {
			group.setMembers(new ArrayList<>());
		}
	}

	private void assignComputedAttributesToGroup(GluuGroup gluuGroup) throws Exception {

		String inum = groupService.generateInumForNewGroup();
		String dn = groupService.getDnForGroup(inum);

		gluuGroup.setInum(inum);
		gluuGroup.setDn(dn);
	}

	public void transferAttributesToGroupResource(GluuGroup gluuGroup, GroupResource res,
            boolean fillMembersDisplay, String groupsUrl, String usersUrl) {

		res.setId(gluuGroup.getInum());

		Meta meta = new Meta();
		meta.setResourceType(ScimResourceUtil.getType(res.getClass()));
		meta.setCreated(gluuGroup.getAttribute("oxTrustMetaCreated"));
		meta.setLastModified(gluuGroup.getAttribute("oxTrustMetaLastModified"));
		meta.setLocation(gluuGroup.getAttribute("oxTrustMetaLocation"));
		if (meta.getLocation() == null)
			meta.setLocation(groupsUrl + "/" + gluuGroup.getInum());

		res.setMeta(meta);
		res.setDisplayName(gluuGroup.getDisplayName());

		// Transfer members from GluuGroup to GroupResource
		List<String> memberDNs = gluuGroup.getMembers();
		if (memberDNs != null) {
			Set<Member> members = new HashSet<>();

			for (String dn : memberDNs) {
				GluuCustomPerson person = null;
                                
                                if (fillMembersDisplay) {
                                    try {                                    
					person = personService.getPersonByDn(dn);
                                    } catch (Exception e) {
                                        log.warn("Wrong member entry {} found in group {}",
                                                dn, gluuGroup.getDisplayName());
                                    }
                                }
                                
                                if (person == null) {
                                    person = new GluuCustomPerson();
                                    person.setInum(userPersistenceHelper.getUserInumFromDN(dn));
                                }
                                
                                Member aMember = new Member();
                                aMember.setValue(person.getInum());
                                aMember.setRef(usersUrl + "/" + person.getInum());
                                aMember.setType(ScimResourceUtil.getType(UserResource.class));
                                aMember.setDisplay(person.getDisplayName());

                                members.add(aMember);
			}
			res.setMembers(members);
		}
	}

        public GluuGroup preCreateGroup(GroupResource group, boolean skipMembersValidation,
                boolean fillDisplay, String usersUrl) throws Exception {

            log.info("Preparing to create group {}", group.getDisplayName());

            GluuGroup gluuGroup = new GluuGroup();
            transferAttributesToGroup(group, gluuGroup, skipMembersValidation, fillDisplay,
                    usersUrl);
            assignComputedAttributesToGroup(gluuGroup);
            
            return gluuGroup;
            
        }
	/**
	 * Inserts a new group in LDAP based on the SCIM Resource passed There is no
	 * need to check attributes mutability in this case as there are no original
	 * attributes (the resource does not exist yet)
	 * 
	 * @param group
	 *            A GroupResource object with all info as received by the web
	 *            service
	 * @param groupsUrl Base URL associated to group resources in SCIM (eg. .../scim/v2/Groups)
	 * @param usersUrl Base URL associated to user resources in SCIM (eg. .../scim/v2/Users)
	 * @throws Exception In case of unexpected error
	 */
	public void createGroup(GluuGroup gluuGroup, GroupResource group, boolean fillMembersDisplay,
                String groupsUrl, String usersUrl) throws Exception {

		String location = groupsUrl + "/" + gluuGroup.getInum();
		gluuGroup.setAttribute("oxTrustMetaLocation", location);

		log.info("Persisting group {}", group.getDisplayName());

		if (externalScimService.isEnabled()) {
			boolean result = externalScimService.executeScimCreateGroupMethods(gluuGroup);
			if (!result) {
				throw new WebApplicationException("Failed to execute SCIM script successfully",
						Status.PRECONDITION_FAILED);
			}
			groupService.addGroup(gluuGroup);
			syncMemberAttributeInPerson(gluuGroup.getDn(), null, gluuGroup.getMembers());

			// Copy back to group the info from gluuGroup
			transferAttributesToGroupResource(gluuGroup, group, fillMembersDisplay,
                            groupsUrl, usersUrl);
			externalScimService.executeScimPostCreateGroupMethods(gluuGroup);
		} else {
			groupService.addGroup(gluuGroup);
			group.getMeta().setLocation(location);
			// We are ignoring the id value received (group.getId())
			group.setId(gluuGroup.getInum());
			syncMemberAttributeInPerson(gluuGroup.getDn(), null, gluuGroup.getMembers());
		}

	}

        public GroupResource buildGroupResource(GluuGroup gluuGroup, boolean fillMembersDisplay,
                String endpointUrl, String usersUrl) {

            GroupResource group = new GroupResource();
            if (externalScimService.isEnabled() && !externalScimService.executeScimGetGroupMethods(gluuGroup)) {
                throw new WebApplicationException("Failed to execute SCIM script successfully",
                        Status.PRECONDITION_FAILED);
            }
            transferAttributesToGroupResource(gluuGroup, group, fillMembersDisplay, endpointUrl, usersUrl);
            
            return group;
            
        }
        
	public GroupResource updateGroup(GluuGroup gluuGroup, GroupResource group,
                boolean skipMembersValidation, boolean fillMembersDisplay, String groupsUrl,
                String usersUrl) throws Exception {

		GroupResource tmpGroup = new GroupResource();
		transferAttributesToGroupResource(gluuGroup, tmpGroup, !skipMembersValidation,
                        groupsUrl, usersUrl);
		tmpGroup.getMeta().setLastModified(DateUtil.millisToISOString(System.currentTimeMillis()));

		tmpGroup = (GroupResource) ScimResourceUtil.transferToResourceReplace(group, tmpGroup,
				extService.getResourceExtensions(group.getClass()));
		replaceGroupInfo(gluuGroup, tmpGroup, skipMembersValidation, fillMembersDisplay,
                        groupsUrl, usersUrl);

		return tmpGroup;

	}

	public void deleteGroup(GluuGroup gluuGroup) throws Exception {
		log.info("Removing group and updating user's entries");

		if (externalScimService.isEnabled()) {
			boolean result = externalScimService.executeScimDeleteGroupMethods(gluuGroup);
			if (!result) {
				throw new WebApplicationException("Failed to execute SCIM script successfully",
						Status.PRECONDITION_FAILED);
			}
		}

		groupService.removeGroup(gluuGroup);

		if (externalScimService.isEnabled())
			externalScimService.executeScimPostDeleteGroupMethods(gluuGroup);

	}

	public void replaceGroupInfo(GluuGroup gluuGroup, GroupResource group,
                boolean skipMembersValidation, boolean fillMembersDisplay, String groupsUrl,
                String usersUrl) throws Exception {

		List<String> olderMembers = new ArrayList<>();
		if (gluuGroup.getMembers() != null)
			olderMembers.addAll(gluuGroup.getMembers());

		transferAttributesToGroup(group, gluuGroup, skipMembersValidation,
                        fillMembersDisplay, usersUrl);
		log.debug("replaceGroupInfo. Updating group info in LDAP");

		if (externalScimService.isEnabled()) {
			boolean result = externalScimService.executeScimUpdateGroupMethods(gluuGroup);
			if (!result) {
				throw new WebApplicationException("Failed to execute SCIM script successfully",
						Status.PRECONDITION_FAILED);
			}

			groupService.updateGroup(gluuGroup);
			syncMemberAttributeInPerson(gluuGroup.getDn(), olderMembers, gluuGroup.getMembers());

			// Copy back to user the info from gluuGroup
			transferAttributesToGroupResource(gluuGroup, group, fillMembersDisplay, groupsUrl, usersUrl);
			externalScimService.executeScimPostUpdateGroupMethods(gluuGroup);
		} else {
			groupService.updateGroup(gluuGroup);
			syncMemberAttributeInPerson(gluuGroup.getDn(), olderMembers, gluuGroup.getMembers());
		}

	}

	public PagedResult<BaseScimResource> searchGroups(String filter, String sortBy, SortOrder sortOrder, int startIndex,
			int count, String groupsUrl, String usersUrl, int maxCount, boolean fillMembersDisplay) throws Exception {

		Filter ldapFilter = scimFilterParserService.createFilter(filter, Filter.createPresenceFilter("inum"), GroupResource.class);
		log.info("Executing search for groups using: ldapfilter '{}', sortBy '{}', sortOrder '{}', startIndex '{}', count '{}'",
				ldapFilter.toString(), sortBy, sortOrder.getValue(), startIndex, count);

		PagedResult<GluuGroup> list = ldapEntryManager.findPagedEntries(groupService.getDnForGroup(null),
				GluuGroup.class, ldapFilter, null, sortBy, sortOrder, startIndex - 1, count, maxCount);
		List<BaseScimResource> resources = new ArrayList<>();

		if (externalScimService.isEnabled() && !externalScimService.executeScimPostSearchGroupsMethods(list)) {
			throw new WebApplicationException("Failed to execute SCIM script successfully", Status.PRECONDITION_FAILED);
		}

		for (GluuGroup group : list.getEntries()) {
			GroupResource scimGroup = new GroupResource();
			transferAttributesToGroupResource(group, scimGroup, fillMembersDisplay, groupsUrl, usersUrl);
			resources.add(scimGroup);
		}
		log.info("Found {} matching entries - returning {}", list.getTotalEntriesCount(), list.getEntries().size());

		PagedResult<BaseScimResource> result = new PagedResult<>();
		result.setEntries(resources);
		result.setTotalEntriesCount(list.getTotalEntriesCount());

		return result;

	}
        
        public boolean membersDisplayInPath(String strPath) {
            
            List<String> paths = Arrays.asList(strPath.replaceAll("\\s", "").split(","));
            String prefix = ScimResourceUtil.getDefaultSchemaUrn(GroupResource.class) + ":";
            String parent = "members";
            String path = parent + ".display";
            
            return Stream.of(parent, path, prefix + parent, prefix + path)
                    .filter(paths::contains).findFirst().isPresent();

        }

	private void syncMemberAttributeInPerson(String groupDn, List<String> beforeMemberDns,
			List<String> afterMemberDns) {

		log.debug("syncMemberAttributeInPerson. Updating memberOf attribute in user LDAP entries");
		log.trace("Before member dns {}; After member dns {}", beforeMemberDns, afterMemberDns);

		// Build 2 sets of DNs
		Set<String> before = new HashSet<>();
		if (beforeMemberDns != null)
			before.addAll(beforeMemberDns);

		Set<String> after = new HashSet<>();
		if (afterMemberDns != null)
			after.addAll(afterMemberDns);

		// Do removals
		for (String dn : before) {
			if (!after.contains(dn)) {
				try {
					GluuCustomPerson gluuPerson = personService.getPersonByDn(dn);

					List<String> memberOf = new ArrayList<>();
					memberOf.addAll(gluuPerson.getMemberOf());
					memberOf.remove(groupDn);

					gluuPerson.setMemberOf(memberOf);
					personService.updatePerson(gluuPerson);
				} catch (Exception e) {
					log.error("An error occurred while removing group {} from user {}", groupDn, dn);
					log.error(e.getMessage(), e);
				}
			}
		}

		// Do insertions
		for (String dn : after) {
			if (!before.contains(dn)) {
				try {
					GluuCustomPerson gluuPerson = personService.getPersonByDn(dn);

					List<String> memberOf = new ArrayList<>();
					memberOf.add(groupDn);

					if (gluuPerson.getMemberOf() != null)
						memberOf.addAll(gluuPerson.getMemberOf());

					gluuPerson.setMemberOf(memberOf);
					personService.updatePerson(gluuPerson);
				} catch (Exception e) {
					log.error("An error occurred while adding group {} to user {}", groupDn, dn);
					log.error(e.getMessage(), e);
				}
			}
		}

	}

}
