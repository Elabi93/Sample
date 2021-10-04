package org.forgerock.openicf.connectors.aws.operations;

import java.util.Map;
import java.util.Set;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AmazonIdentityManagementException;
import com.amazonaws.services.identitymanagement.model.CreateLoginProfileRequest;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.CreateUserResult;
import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;
import com.amazonaws.services.identitymanagement.model.InvalidInputException;
import com.amazonaws.services.identitymanagement.model.PolicyNotAttachableException;
import com.amazonaws.services.identitymanagement.model.PutUserPermissionsBoundaryRequest;
import com.amazonaws.services.identitymanagement.model.Tag;
import com.amazonaws.services.identitymanagement.model.TagUserRequest;

import org.forgerock.openicf.connectors.aws.AWSConfiguration;
import org.forgerock.openicf.connectors.aws.util.AWSConstants;
import org.forgerock.openicf.connectors.aws.util.AWSUtility;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;

/**
 * AWSCreateOperation class
 */
public class AWSCreateOperation {

    /**
     * Logger for AWSCreateOperation class.
     *
     */
    private static Log log = Log.getLog(AWSCreateOperation.class);

    /**
     * CreateUserResult variable - AWS createUserResult.
     *
     */
    private CreateUserResult createUserResult;

    /**
     * Configuration for AWS Connector..
     *
     */
    private AWSConfiguration awsConfiguration;

    /**
     * @param awsConfiguration
     *            - AWSConfiguration. Constructor for CreateOperation.
     *
     */
    public AWSCreateOperation(AWSConfiguration awsConfiguration) {
        this.awsConfiguration = awsConfiguration;
    }

    /**
     * Create a AWS IAM user based on the specified attributes.
     * 
     * @param client
     *            AmazonIdentityManagement client to perform API calls.
     * 
     * @param createAttributes
     *            includes all the attributes necessary to create the workplace
     *            user account (including the <code>ObjectClass</code>
     *            attribute).
     * 
     * @return the unique id for the account that is created.
     */
    public Uid create(AmazonIdentityManagement client,
            Set<Attribute> createAttributes) {
        log.ok("Creating User with UserName {0}",
                AttributeUtil.getNameFromAttributes(createAttributes)
                        .getNameValue());

        AWSUtility.validateRequestPayload(createAttributes, awsConfiguration);

        try {
            String userName = (AttributeUtil.find(Name.NAME,
                    createAttributes) != null)
                            ? AttributeUtil.getStringValue(AttributeUtil
                                    .find(Name.NAME, createAttributes))
                            : null;

            if (StringUtil.isBlank(userName)) {
                log.error("Username attribute cannot be blank "
                        + "for Creating User");
            }

            String path = (AttributeUtil.find(AWSConstants.AWS_ATTRIBUTE_PATH,
                    createAttributes) != null)
                            ? AttributeUtil.getStringValue(AttributeUtil
                                    .find(AWSConstants.AWS_ATTRIBUTE_PATH,
                                            createAttributes))
                            : null;

            if (StringUtil.isBlank(path)) {
                log.info("Creating User {0} With Default Path", userName);
                createUserResult = client.createUser(
                        new CreateUserRequest().withUserName(userName));
            } else {
                log.info("Creating User {0} With Path {1}", userName, path);
                createUserResult = client.createUser(
                        new CreateUserRequest().withUserName(userName)
                                .withPath(path));
            }

            if (AttributeUtil.find(OperationalAttributes.PASSWORD_NAME,
                    createAttributes) != null) {
                AttributeUtil.getPasswordValue(createAttributes)
                        .access(password -> {
                            client.createLoginProfile(
                                    new CreateLoginProfileRequest(
                                            createUserResult.getUser()
                                                    .getUserName(),
                                            new String(password)));
                        });
            }

            try {
                if (AttributeUtil.find(
                        AWSConstants.AWS_ATTRIBUTE_PERMISSION_BOUNDARY,
                        createAttributes) != null) {
                    String permissionsBoundary = AttributeUtil
                            .getStringValue(AttributeUtil.find(
                                    AWSConstants.AWS_ATTRIBUTE_PERMISSION_BOUNDARY,
                                    createAttributes));

                    log.info("Creating Permissions Boundary for User: {0}",
                            userName);
                    client.putUserPermissionsBoundary(
                            new PutUserPermissionsBoundaryRequest()
                                    .withUserName(userName)
                                    .withPermissionsBoundary(
                                            permissionsBoundary));
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
                    .reBuildingTags(createAttributes);
            if (!keysAndValues.isEmpty()) {
                log.info("Creating Tags for User: {0}", userName);
                keysAndValues.forEach((key, value) -> {
                    if (StringUtil.isBlank(key)) {
                        log.info("Tag Key Must be Blank");
                    } else if (!StringUtil.isBlank(key)
                            && !StringUtil.isBlank(value.toString())) {
                        client.tagUser(new TagUserRequest()
                                .withUserName(userName).withTags(
                                        new Tag().withKey(key)
                                                .withValue(value.toString())));
                    } else {
                        log.error("Tag Key Or Value Must not be Blank.");
                    }
                });
            }
        } catch (EntityAlreadyExistsException eaex) {
            log.error("Create user failed, user already exists:{0}",
                    AttributeUtil.getNameFromAttributes(createAttributes)
                            .getNameValue());
            throw new AlreadyExistsException(
                    awsConfiguration.getMessage(
                            AWSConstants.AWS_USER_ALREADY_EXISTS_EX, eaex));
        } catch (InvalidInputException iex) {
            log.error("AWS - Bad Request : {0}",
                    AttributeUtil.getNameFromAttributes(createAttributes)
                            .getNameValue());
            throw new InvalidInputException(
                    awsConfiguration.getMessage(AWSConstants.AWS_BADREQUEST_EX,
                            iex));
        } catch (AmazonIdentityManagementException aimex) {
            log.error("Create user failed :{0}",
                    AttributeUtil.getNameFromAttributes(createAttributes)
                            .getNameValue());
            throw new ConnectorException(
                    awsConfiguration.getMessage(
                            AWSConstants.AWS_CREATE_USER_FAILED_EX, aimex));
        }
        log.info("User created successfully: {0}",
                createUserResult.getUser().getUserName());
        return new Uid(createUserResult.getUser().getUserName());
    }
}
