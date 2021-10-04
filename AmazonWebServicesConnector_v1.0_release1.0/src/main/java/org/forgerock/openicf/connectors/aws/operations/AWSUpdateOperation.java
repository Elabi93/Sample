package org.forgerock.openicf.connectors.aws.operations;

import java.util.Map;
import java.util.Set;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AmazonIdentityManagementException;
import com.amazonaws.services.identitymanagement.model.CreateLoginProfileRequest;
import com.amazonaws.services.identitymanagement.model.DeleteUserPermissionsBoundaryRequest;
import com.amazonaws.services.identitymanagement.model.EntityTemporarilyUnmodifiableException;
import com.amazonaws.services.identitymanagement.model.GetUserRequest;
import com.amazonaws.services.identitymanagement.model.GetUserResult;
import com.amazonaws.services.identitymanagement.model.ListUserTagsRequest;
import com.amazonaws.services.identitymanagement.model.ListUserTagsResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.PolicyNotAttachableException;
import com.amazonaws.services.identitymanagement.model.PutUserPermissionsBoundaryRequest;
import com.amazonaws.services.identitymanagement.model.Tag;
import com.amazonaws.services.identitymanagement.model.TagUserRequest;
import com.amazonaws.services.identitymanagement.model.UntagUserRequest;
import com.amazonaws.services.identitymanagement.model.UpdateLoginProfileRequest;
import com.amazonaws.services.identitymanagement.model.UpdateUserRequest;

import org.forgerock.openicf.connectors.aws.AWSConfiguration;
import org.forgerock.openicf.connectors.aws.util.AWSConstants;
import org.forgerock.openicf.connectors.aws.util.AWSUtility;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;

/**
 * AWSUpdateOperation class
 */
public class AWSUpdateOperation {

    /**
     * Logger for AWSUpdateOperation class
     */
    private static Log log = Log.getLog(AWSUpdateOperation.class);

    /**
     * Configuration for AWS Connector.
     *
     */
    private AWSConfiguration configuration;

    /**
     * @param awsConfiguration
     *            AWSConfiguration. Constructor for UpdateOperation.
     *
     */
    public AWSUpdateOperation(AWSConfiguration awsConfiguration) {
        this.configuration = awsConfiguration;
    }

    /**
     * Update the AWS user account specified by the {@link Uid}, replacing the
     * current values of each attribute with the values provided.
     * 
     * @param client
     *            AmazonIdentityManagement client to perform API calls.
     * 
     * @param uid
     *            Uid of the user account to modify. Must not be null.
     * 
     * @param updateAttributes
     *            Set of new {@link Attribute}. The values in this set represent
     *            the new, merged values to be applied to the AWS user account.
     *            This set may also include {@link OperationalAttributes
     *            operational attributes}. Must not be null.
     * @return Uid of the updated user.
     */
    public Uid update(AmazonIdentityManagement client, Uid uid,
            Set<Attribute> updateAttributes) {
        log.info("Updating User: {0}", uid.getUidValue());

        AWSUtility.validateRequestPayload(updateAttributes, configuration);

        try {
            if (uid == null || uid.getUidValue() == null)
                throw new ConnectorException(
                        configuration.getMessage(
                                AWSConstants.AWS_EX_NULL_POINTER, uid));
            else if (uid.getUidValue()
                    .equalsIgnoreCase(AWSConstants.AWS_ATTRIBUTE_UNKNOWN))
                throw new UnknownUidException(
                        configuration.getMessage(AWSConstants.AWS_EX_UNKNOWNUID,
                                uid.getUidValue()));

            if (AttributeUtil.find(AWSConstants.AWS_ATTRIBUTE_PATH,
                    updateAttributes) != null) {
                String path = AttributeUtil.getAsStringValue(
                        AttributeUtil.find(AWSConstants.AWS_ATTRIBUTE_PATH,
                                updateAttributes));
                path = StringUtil.isBlank(path)
                        ? AWSConstants.AWS_DEFAULT_PATH
                        : path;

                log.ok("Updating User {0} with path: {1}", uid.getUidValue(),
                        path);
                client.updateUser(
                        new UpdateUserRequest().withUserName(uid.getUidValue())
                                .withNewPath(path));
            }

            if (AttributeUtil.find(OperationalAttributes.PASSWORD_NAME,
                    updateAttributes) != null) {
                log.ok("Updating password for User: {0}", uid.getUidValue());
                try {
                    AttributeUtil.getPasswordValue(updateAttributes)
                            .access(newPassword -> {
                                client.updateLoginProfile(
                                        new UpdateLoginProfileRequest()
                                                .withUserName(uid.getUidValue())
                                                .withPassword(new String(
                                                        newPassword)));
                            });
                } catch (NoSuchEntityException nsex) {
                    AttributeUtil.getPasswordValue(updateAttributes)
                            .access(newPassword -> {
                                client.createLoginProfile(
                                        new CreateLoginProfileRequest()
                                                .withUserName(uid.getUidValue())
                                                .withPassword(new String(
                                                        newPassword)));
                            });
                }
            }

            try {
                if (AttributeUtil.find(
                        AWSConstants.AWS_ATTRIBUTE_PERMISSION_BOUNDARY,
                        updateAttributes) != null) {
                    String permissionsBoundary = AttributeUtil
                            .getStringValue(AttributeUtil.find(
                                    AWSConstants.AWS_ATTRIBUTE_PERMISSION_BOUNDARY,
                                    updateAttributes));
                    if (!StringUtil.isBlank(permissionsBoundary)) {
                        log.info("Updating Permissions Boundary for User: {0}",
                                uid.getUidValue());
                        client.putUserPermissionsBoundary(
                                new PutUserPermissionsBoundaryRequest()
                                        .withUserName(uid.getUidValue())
                                        .withPermissionsBoundary(
                                                permissionsBoundary));
                    } else {
                        GetUserResult getUserResult = client.getUser(
                                new GetUserRequest()
                                        .withUserName(uid.getUidValue()));
                        if (getUserResult.getUser()
                                .getPermissionsBoundary() != null) {
                            log.info(
                                    "Permissions Boundary Detached from User: {0}",
                                    uid.getUidValue());
                            client.deleteUserPermissionsBoundary(
                                    new DeleteUserPermissionsBoundaryRequest()
                                            .withUserName(uid
                                                    .getUidValue()));
                        }
                    }
                }
            } catch (PolicyNotAttachableException pnaex) {
                log.info(
                        "This Permission Boundary Cannot be set to an IAM User: {0}",
                        pnaex.getMessage());
            } catch (AmazonIdentityManagementException ex) {
                log.info(
                        "This Permission Boundary Cannot be set to this User: {0}",
                        ex.getMessage());
            }

            Map<String, Object> keysAndValues = AWSUtility
                    .reBuildingTags(updateAttributes);
            if (keysAndValues.size() > 0) {
                log.info("Updating Tags for User: {0}", uid.getUidValue());
                keysAndValues.forEach((key, value) -> {

                    if (StringUtil.isBlank(key)) {
                        log.info("Tag Key Must be Blank");
                    } else if (!StringUtil.isBlank(key)
                            && !StringUtil.isBlank(value.toString())) {
                        client.tagUser(new TagUserRequest()
                                .withUserName(uid.getUidValue())
                                .withTags(new Tag().withKey(key)
                                        .withValue(value.toString())));
                    } else if (StringUtil.isBlank(value.toString())) {
                        ListUserTagsResult listUserTagsResult = client
                                .listUserTags(new ListUserTagsRequest()
                                        .withUserName(uid.getUidValue()));
                        if (listUserTagsResult.getTags().size() > 0) {
                            log.info("Tags Detached from User: {0}",
                                    uid.getUidValue());
                            client.untagUser(new UntagUserRequest()
                                    .withUserName(uid.getUidValue())
                                    .withTagKeys(key));
                        }
                    }
                });
            }

            Name nameAttr = AttributeUtil
                    .getNameFromAttributes(updateAttributes);
            if (nameAttr != null) {
                if (StringUtil.isBlank(nameAttr.getNameValue())) {
                    log.error(
                            "User update failed, "
                                    + "UserName attribute cannot be blank: {0}",
                            nameAttr.getNameValue());
                    throw new ConnectorException(
                            configuration.getMessage(
                                    AWSConstants.AWS_UPDATE_USER_FAILED_EX,
                                    AWSConstants.AWS_USERNAME_BLANK));
                }

                log.ok("Updating User " + "{0} with new UserName {1}",
                        uid.getUidValue(),
                        nameAttr.getNameValue());
                client.updateUser(
                        new UpdateUserRequest().withUserName(uid.getUidValue())
                                .withNewUserName(nameAttr.getNameValue()));
                return new Uid(nameAttr.getNameValue());
            }
        } catch (NoSuchEntityException nsex) {
            log.error("Update user failed, user does not exists: {0}" + nsex,
                    uid.getUidValue());
            throw new UnknownUidException(
                    configuration.getMessage(
                            AWSConstants.AWS_USER_NOT_EXISTS_EX, nsex));
        } catch (EntityTemporarilyUnmodifiableException etuex) {
            log.error("Update user failed, user temporarily unmodifiable",
                    uid.getUidValue());
            throw new EntityTemporarilyUnmodifiableException(
                    configuration.getMessage(
                            AWSConstants.AWS_PROFILE_UNMODIFIABLE_EX, etuex));
        } catch (AmazonIdentityManagementException aimex) {
            log.error("Update user failed: {0}", aimex);
            throw new ConnectorException(
                    configuration.getMessage(
                            AWSConstants.AWS_UPDATE_USER_FAILED_EX, aimex));
        }

        log.ok("User updated successfully: {0}", uid.getUidValue());
        return uid;
    }
}
