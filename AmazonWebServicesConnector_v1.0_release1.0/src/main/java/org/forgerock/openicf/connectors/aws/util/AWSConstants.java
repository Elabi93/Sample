package org.forgerock.openicf.connectors.aws.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SPIOperation;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

/**
 * AWSConstants class.
 */
public class AWSConstants {

    public static final String AWS_CONNECTOR_DISPLAY = "Connector.displayName";
    public static final String AWS_CONFIG_ATTRIBUTE_ACCESSKEY_HELP = "ACCESSKEYID_HELP";
    public static final String AWS_CONFIG_ATTRIBUTE_ACCESSKEY_DISPLAY = "ACCESSKEYID_DISPLAY";
    public static final String AWS_CONFIG_ATTRIBUTE_SECRETKEY_HELP = "SECRETKEY_HELP";
    public static final String AWS_CONFIG_ATTRIBUTE_SECRETKEY_DISPLAY = "SECRETKEY_DISPLAY";
    public static final String AWS_CONFIG_ATTRIBUTE_ROLE_HELP = "ROLE_HELP";
    public static final String AWS_CONFIG_ATTRIBUTE_ROLE_DISPLAY = "ROLE_DISPLAY";
    public static final String AWS_CONFIG_ATTRIBUTE_PAGESIZE_HELP = "PAGESIZE_HELP";
    public static final String AWS_CONFIG_ATTRIBUTE_PAGESIZE_DISPLAY = "PAGESIZE_DISPLAY";
    public static final String AWS_CONFIG_ATTRIBUTE_CREDENTAILSEXPIRATION_HELP = "CREDENTIALSEXPIRATION_HELP";
    public static final String AWS_CONFIG_ATTRIBUTE_CREDENTAILSEXPIRATION_DISPLAY = "CREDENTIALSEXPIRATION_DISPLAY";

    public static final Integer AWS_CONFIG_DEFAULT_PAGESIZE = 100;
    public static final Integer AWS_CONFIG_DEFAULT_CREDENTIALS_EXPIRATION = 900;

    public static final String AWS_ATTRIBUTE_USERNAME = "UserName";
    public static final String AWS_ATTRIBUTE_PATH = "Path";
    public static final String AWS_ATTRIBUTE_ARN = "Arn";
    public static final String AWS_ATTRIBUTE_ID = "UserId";
    public static final String AWS_ATTRIBUTE_PASSWORD_LAST_USED = "PasswordLastUsed";
    public static final String AWS_ATTRIBUTE_CREATE_DATE = "CreateDate";
    public static final String AWS_ATTRIBUTE_PERMISSION_BOUNDARY = "PermissionsBoundary";
    public static final String AWS_ATTRIBUTE_TAGS = "Tags";
    public static final String AWS_ATTRIBUTE_MEMBER = "member";
    public static final String AWS_ATTRIBUTE_TAGS_KEY = "Key";
    public static final String AWS_ATTRIBUTE_TAGS_VALUE = "Value";
    public static final String AWS_ATTRIBUTE_TAGS_MEMBER_KEY = "Tags.member.Key";
    public static final String AWS_ATTRIBUTE_TAGS_MEMBER_VALUE = "Tags.member.Value";
    public static final String AWS_ATTRIBUTE_UNKNOWN = "unknown";

    public static final String AWS_PERIOD_OPERATOR = ".";
    public static final String AWS_SPLIT_BY_DOT = "\\.";

    public static final String AWS_CONFIG_ATTRIBUTE_ACCESSKEY = "accessKeyId";
    public static final String AWS_CONFIG_ATTRIBUTE_SECRETKEY = "secretKey";
    public static final String AWS_CONFIG_ATTRIBUTE_PAGESIZE = "pageSize";
    public static final String AWS_CONFIG_ATTRIBUTE_CREDENTIALS_EXPIRATION = "credentialsExpiration";
    public static final String AWS_CONFIG_ATTRIBUTE_ROLENAME = "roleName";
    public static final String AWS_ROLE_SESSION_NAME = "None";
    public static final String AWS_DEFAULT_PATH = "/";
    public static final String AWS_TAGS_MEMBER = "Tags.member";

    public static final String AWS_USERNAME_BLANK = "UserName attribute cannot be blank";

    public static final String EX_INVALIDFIELDS = "ex.invalidFields";
    public static final String AWS_INVALID_CREDENTIALS_EX = "ex.invalidCredentials";
    public static final String AWS_CREATE_USER_FAILED_EX = "ex.createUserFailed";
    public static final String AWS_DELETE_USER_FAILED_EX = "ex.deleteUserFailed";
    public static final String AWS_SEARCH_USER_FAILED_EX = "ex.searchUserFailed";
    public static final String AWS_UPDATE_USER_FAILED_EX = "ex.updateUserFailed";
    public static final String AWS_USER_ALREADY_EXISTS_EX = "ex.userAlreadyExists";
    public static final String AWS_USER_NOT_EXISTS_EX = "ex.userDoesNotExists";
    public static final String AWS_PROFILE_UNMODIFIABLE_EX = "ex.userProfileUnModifiable";
    public static final String AWS_ROLE_NOT_EXISTS_EX = "ex.roleDoesNotExist";
    public static final String AWS_OBJECTCLASS_EX = "ex.objectClassInvalid";
    public static final String AWS_BADREQUEST_EX = "ex.badRequest";
    public static final String AWS_DELETE_CONFLICT_EX = "ex.deleteConflict";
    public static final String AWS_EX_UNKNOWNUID = "ex.UnknownUid";
    public static final String AWS_EX_NULL_POINTER = "ex.NullPointer";
    public static final String AWS_ATTRIBUTE_UNKNOWN_EXCEPTION = "ex.unknownAttrFound";
    public static final String EX_OPERATIONFAILED = "ex.operationFailed";

    public static final Set<String> AWS_REQUIRED_ATTRIBUTES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(Name.NAME)));

    public static final Set<String> AWS_OTHER_ATTRIBUTTES = Collections
            .unmodifiableSet(new HashSet<>(
                    Arrays.asList(OperationalAttributes.PASSWORD_NAME,
                            AWS_ATTRIBUTE_PATH, AWS_ATTRIBUTE_ARN,
                            AWS_ATTRIBUTE_CREATE_DATE,
                            AWS_ATTRIBUTE_PASSWORD_LAST_USED,
                            AWS_ATTRIBUTE_PERMISSION_BOUNDARY,
                            AWS_ATTRIBUTE_TAGS_MEMBER_KEY,
                            AWS_ATTRIBUTE_TAGS_MEMBER_VALUE)));

    public static final Set<String> AWS_CREATEABLE_ATTRIBUTES = Collections
            .unmodifiableSet(
                    new HashSet<>(Arrays.asList(Name.NAME, AWS_ATTRIBUTE_PATH,
                            OperationalAttributes.PASSWORD_NAME,
                            AWS_ATTRIBUTE_PERMISSION_BOUNDARY,
                            AWS_ATTRIBUTE_TAGS_MEMBER_KEY,
                            AWS_ATTRIBUTE_TAGS_MEMBER_VALUE)));

    public static final Set<String> AWS_UPDATEABLE_ATTRIBUTES = Collections
            .unmodifiableSet(
                    new HashSet<>(Arrays.asList(Name.NAME, AWS_ATTRIBUTE_PATH,
                            OperationalAttributes.PASSWORD_NAME,
                            AWS_ATTRIBUTE_PERMISSION_BOUNDARY,
                            AWS_ATTRIBUTE_TAGS_MEMBER_VALUE)));

    public static final Set<String> AWS_READABLE_ATTRIBUTES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(Name.NAME,
                    AWS_ATTRIBUTE_PATH, AWS_ATTRIBUTE_ARN,
                    AWS_ATTRIBUTE_CREATE_DATE, AWS_ATTRIBUTE_PASSWORD_LAST_USED,
                    AWS_ATTRIBUTE_PERMISSION_BOUNDARY, AWS_ATTRIBUTE_ID,
                    AWS_ATTRIBUTE_TAGS_MEMBER_KEY,
                    AWS_ATTRIBUTE_TAGS_MEMBER_VALUE)));

    public static final Set<String> AWS_RETURNBYDEFAULT_ATTRIBUTES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(Name.NAME,
                    AWS_ATTRIBUTE_PATH, AWS_ATTRIBUTE_ARN,
                    AWS_ATTRIBUTE_CREATE_DATE, AWS_ATTRIBUTE_PASSWORD_LAST_USED,
                    AWS_ATTRIBUTE_PERMISSION_BOUNDARY, AWS_ATTRIBUTE_ID,
                    AWS_ATTRIBUTE_TAGS_MEMBER_KEY,
                    AWS_ATTRIBUTE_TAGS_MEMBER_VALUE)));

    public static final Set<String> AWS_MULTIVALUED_COMPLEX_ATTRIBUTES = Collections
            .unmodifiableSet(new HashSet<>(
                    Arrays.asList(AWS_ATTRIBUTE_TAGS, AWS_ATTRIBUTE_MEMBER)));

    public static final Set<Class<? extends SPIOperation>> ACCOUNT_OPERATIONS = Collections
            .unmodifiableSet(new HashSet<Class<? extends SPIOperation>>(
                    Arrays.asList(SchemaOp.class,
                            TestOp.class, UpdateOp.class, SearchOp.class,
                            CreateOp.class, DeleteOp.class)));
  
}