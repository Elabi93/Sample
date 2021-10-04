package org.forgerock.openicf.connectors.aws.operations;

import java.util.List;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.amazonaws.services.identitymanagement.model.AmazonIdentityManagementException;
import com.amazonaws.services.identitymanagement.model.AttachedPolicy;
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteConflictException;
import com.amazonaws.services.identitymanagement.model.DeleteLoginProfileRequest;
import com.amazonaws.services.identitymanagement.model.DeleteUserPermissionsBoundaryRequest;
import com.amazonaws.services.identitymanagement.model.DeleteUserRequest;
import com.amazonaws.services.identitymanagement.model.DetachUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.EntityTemporarilyUnmodifiableException;
import com.amazonaws.services.identitymanagement.model.GetUserRequest;
import com.amazonaws.services.identitymanagement.model.GetUserResult;
import com.amazonaws.services.identitymanagement.model.Group;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysRequest;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysResult;
import com.amazonaws.services.identitymanagement.model.ListAttachedUserPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListAttachedUserPoliciesResult;
import com.amazonaws.services.identitymanagement.model.ListGroupsForUserRequest;
import com.amazonaws.services.identitymanagement.model.ListGroupsForUserResult;
import com.amazonaws.services.identitymanagement.model.ListUserTagsRequest;
import com.amazonaws.services.identitymanagement.model.ListUserTagsResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.RemoveUserFromGroupRequest;
import com.amazonaws.services.identitymanagement.model.Tag;
import com.amazonaws.services.identitymanagement.model.UntagUserRequest;

import org.forgerock.openicf.connectors.aws.AWSConfiguration;
import org.forgerock.openicf.connectors.aws.util.AWSConstants;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Uid;

/**
 * AWSDeleteOperation class
 */
public class AWSDeleteOperation {

    /**
     * Logger for AWSDeleteOperation class
     */
    private static final Log log = Log.getLog(AWSDeleteOperation.class);

    /**
     * configuration - AWS Configuration
     */
    private AWSConfiguration configuration;

    /**
     * 
     * @param awsConfiguration
     *            - AWSConfiguration. Constructor for UpdateOperation.
     */
    public AWSDeleteOperation(AWSConfiguration awsConfiguration) {
        this.configuration = awsConfiguration;
    }

    /**
     * Delete the AWS user account of specified Uid.
     * 
     * @param client
     *            AmazonIdentityManagement client to perform API calls.
     * 
     * @param uid
     *            The unique id that specifies the account to delete.
     */
    @SuppressWarnings("unused")
    public void delete(AmazonIdentityManagement client, Uid uid) {
        log.ok("Deleting User with UID:{0}", uid.getUidValue());

        if (uid == null)
            throw new UnknownUidException(
                    configuration.getMessage(
                            AWSConstants.AWS_USER_NOT_EXISTS_EX,
                            uid));

        try {
            ListAccessKeysResult listAccessKeysResult = client
                    .listAccessKeys(new ListAccessKeysRequest()
                            .withUserName(uid.getUidValue()));
            boolean accessKeyResult = listAccessKeysResult
                    .getAccessKeyMetadata().isEmpty();

            if (!accessKeyResult) {
                List<AccessKeyMetadata> listAccessKeys = listAccessKeysResult
                        .getAccessKeyMetadata();

                listAccessKeys.forEach(accessKey -> {
                    client.deleteAccessKey(new DeleteAccessKeyRequest()
                            .withAccessKeyId(accessKey.getAccessKeyId())
                            .withUserName(uid.getUidValue()));
                    log.ok("User AccessKey Deleted: {0}", uid.getUidValue());
                });
            }

            ListAttachedUserPoliciesResult listAttachedUserPoliciesResult = client
                    .listAttachedUserPolicies(
                            new ListAttachedUserPoliciesRequest()
                                    .withUserName(uid.getUidValue()));

            boolean attachedUserPolicies = listAttachedUserPoliciesResult
                    .getAttachedPolicies().isEmpty();

            if (!attachedUserPolicies) {
                List<AttachedPolicy> attachedPolicies = listAttachedUserPoliciesResult
                        .getAttachedPolicies();

                attachedPolicies.forEach(policy -> {
                    client.detachUserPolicy(new DetachUserPolicyRequest()
                            .withPolicyArn(policy.getPolicyArn())
                            .withUserName(uid.getUidValue()));
                    log.ok("Policy Detached from User: {0}", uid.getUidValue());
                });
            }

            ListGroupsForUserResult listGroupsForUserResult = client
                    .listGroupsForUser(new ListGroupsForUserRequest()
                            .withUserName(uid.getUidValue()));
            boolean groupStatus = listGroupsForUserResult.getGroups().isEmpty();

            if (!groupStatus) {
                List<Group> groups = listGroupsForUserResult.getGroups();
                groups.forEach(group -> {
                    client.removeUserFromGroup(new RemoveUserFromGroupRequest()
                            .withGroupName(group.getGroupName())
                            .withUserName(uid.getUidValue()));
                    log.ok("Group Detached from User: {0}", uid.getUidValue());
                });
            }

            GetUserResult getUserResult = client.getUser(
                    new GetUserRequest().withUserName(uid.getUidValue()));
            if (getUserResult.getUser().getPermissionsBoundary() != null) {
                log.info("Permissions Boundary Detached from User: {0}",
                        uid.getUidValue());
                client.deleteUserPermissionsBoundary(
                        new DeleteUserPermissionsBoundaryRequest()
                                .withUserName(uid.getUidValue()));
            }

            ListUserTagsResult listUserTagsResult = client
                    .listUserTags(new ListUserTagsRequest()
                            .withUserName(uid.getUidValue()));
            List<Tag> tags = listUserTagsResult.getTags();
            if (tags.size() > 0) {
                log.info("Tags Detached from User: {0}", uid.getUidValue());
                tags.forEach(tag -> {
                    client.untagUser(new UntagUserRequest()
                            .withUserName(uid.getUidValue())
                            .withTagKeys(tag.getKey()));
                });
            }

            try {
                client.deleteLoginProfile(new DeleteLoginProfileRequest()
                        .withUserName(uid.getUidValue()));
                log.ok("User Login Profile Deleted: {0}", uid.getUidValue());
            } catch (NoSuchEntityException nsex) {
                log.ok("User login profile not found for the user:{0}",
                        uid.getUidValue());
            }

            client.deleteUser(
                    new DeleteUserRequest().withUserName(uid.getUidValue()));
            log.ok("User Deleted Successfully: {0}", uid.getUidValue());
        } catch (DeleteConflictException dcex) {
            log.error("User delete failed, conflict entity : {0}",
                    uid.getUidValue());
            throw new DeleteConflictException(configuration
                    .getMessage(AWSConstants.AWS_DELETE_CONFLICT_EX, dcex));
        } catch (NoSuchEntityException nsex) {
            log.error("User delete failed, user does not exists: {0}",
                    uid.getUidValue());
            throw new UnknownUidException(configuration
                    .getMessage(AWSConstants.AWS_USER_NOT_EXISTS_EX, nsex));
        } catch (EntityTemporarilyUnmodifiableException etuex) {
            log.error("User delete failed,user modification is restricted: {0}",
                    uid.getUidValue());
            throw new ConnectorException(configuration.getMessage(
                    AWSConstants.AWS_PROFILE_UNMODIFIABLE_EX, etuex));
        } catch (AmazonIdentityManagementException aimex) {
            log.error("User delete failed: {0}", uid.getUidValue());
            throw new ConnectorException(configuration
                    .getMessage(AWSConstants.AWS_DELETE_USER_FAILED_EX, aimex));
        }
    }

}
