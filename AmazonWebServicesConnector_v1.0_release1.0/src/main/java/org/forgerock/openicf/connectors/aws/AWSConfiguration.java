package org.forgerock.openicf.connectors.aws;

import java.util.HashSet;
import java.util.Set;

import org.forgerock.openicf.connectors.aws.util.AWSConstants;
import org.forgerock.openicf.connectors.aws.util.AWSUtility;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * AWSConfiguration class.
 */
public class AWSConfiguration extends AbstractConfiguration {

    /**
     * Logger for AWSConfiguration class.
     */
    private static Log log = Log.getLog(AWSConfiguration.class);

    /**
     * AccessKeyId to make programmatic calls to AWS API's.
     *
     */
    private String accessKeyId;
    /**
     * SecretKey to make programmatic calls to AWS API's.
     *
     */
    private GuardedString secretKey;
    /**
     * RoleARN - Amazon Resource Name(ARN) for the role which has IAMFullAccess
     * permission.
     *
     */
    private String roleARN;
    /**
     * PageSize for setting page limit.
     *
     */
    private int pageSize = AWSConstants.AWS_CONFIG_DEFAULT_PAGESIZE;
    /**
     * CredentialsExpiration - Time (in seconds) to configure the duration in
     * which the temporary credentials would expire.
     *
     */
    private int credentialsExpiration = AWSConstants.AWS_CONFIG_DEFAULT_CREDENTIALS_EXPIRATION;

    /**
     * @return AccessKeyId to make programmatic calls to AWS API's.
     */
    @ConfigurationProperty(
            order = 1,
            helpMessageKey = AWSConstants.AWS_CONFIG_ATTRIBUTE_ACCESSKEY_HELP,
            displayMessageKey = AWSConstants.AWS_CONFIG_ATTRIBUTE_ACCESSKEY_DISPLAY,
            required = true)
    public String getAccessKeyId() {
        return this.accessKeyId;
    }

    /**
     * @param AccessKeyId
     *            to make programmatic calls to AWS API's.
     *
     */
    public void setAccessKeyId(final String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    /**
     * @return secretKey value.
     */
    @ConfigurationProperty(
            order = 2,
            helpMessageKey = AWSConstants.AWS_CONFIG_ATTRIBUTE_SECRETKEY_HELP,
            displayMessageKey = AWSConstants.AWS_CONFIG_ATTRIBUTE_SECRETKEY_DISPLAY,
            required = true,
            confidential = true)
    public GuardedString getSecretKey() {
        return this.secretKey;
    }

    /**
     * @param secretKey
     *            to make programmatic calls to AWS API's.
     *
     */
    public void setSecretKey(GuardedString secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * @return The Amazon Resource Name(ARN) value of the role configured for
     *         provisioning in the service provider.
     */
    @ConfigurationProperty(
            order = 3,
            helpMessageKey = AWSConstants.AWS_CONFIG_ATTRIBUTE_ROLE_HELP,
            displayMessageKey = AWSConstants.AWS_CONFIG_ATTRIBUTE_ROLE_DISPLAY,
            required = true)
    public String getRoleARN() {
        return roleARN;
    }

    /**
     * @param roleARN
     *            The Amazon Resource Name(ARN) value of the role configured for
     *            provisioning in the service provider.
     */
    public void setRoleARN(String roleARN) {
        this.roleARN = roleARN;
    }

    /**
     * @return pageSize value.
     */
    @ConfigurationProperty(
            order = 4,
            helpMessageKey = AWSConstants.AWS_CONFIG_ATTRIBUTE_PAGESIZE_HELP,
            displayMessageKey = AWSConstants.AWS_CONFIG_ATTRIBUTE_PAGESIZE_DISPLAY,
            required = false)
    public int getPageSize() {
        return this.pageSize;
    }

    /**
     * @param pageSize
     *            - set pageSize.
     *
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * @return credential expiration time.
     */
    @ConfigurationProperty(
            order = 5,
            helpMessageKey = AWSConstants.AWS_CONFIG_ATTRIBUTE_CREDENTAILSEXPIRATION_HELP,
            displayMessageKey = AWSConstants.AWS_CONFIG_ATTRIBUTE_CREDENTAILSEXPIRATION_DISPLAY,
            required = false)
    public int getCredentialsExpiration() {
        return credentialsExpiration;
    }

    /**
     * @param credentialsExpiration
     *            - set credential expiration time in seconds.
     *
     */
    public void setCredentialsExpiration(int credentialsExpiration) {
        this.credentialsExpiration = credentialsExpiration;
    }

    /**
     * AWS Configuration mandatory attributes validation.
     */
    @Override
    public void validate() {
        log.ok("Validating AWS Configuration attributes");
        Set<String> invalidFields = new HashSet<>();

        if (StringUtil.isBlank(accessKeyId)) {
            invalidFields.add(AWSConstants.AWS_CONFIG_ATTRIBUTE_ACCESSKEY);
        }

        if (StringUtil.isBlank(roleARN)) {
            invalidFields.add(AWSConstants.AWS_CONFIG_ATTRIBUTE_ROLENAME);
        }

        try {
            AWSUtility.validateGuardedString(secretKey);
        } catch (ConfigurationException ex) {
            invalidFields.add(AWSConstants.AWS_CONFIG_ATTRIBUTE_SECRETKEY);
        }

        if (pageSize < 1 || pageSize > 1000) {
            invalidFields.add(AWSConstants.AWS_CONFIG_ATTRIBUTE_PAGESIZE);
        }

        if (credentialsExpiration < 900 || credentialsExpiration > 43200) {
            invalidFields.add(
                    AWSConstants.AWS_CONFIG_ATTRIBUTE_CREDENTIALS_EXPIRATION);
        }

        if (!invalidFields.isEmpty()) {
            log.error("Mandatory configuration attributes missing:" + " {0}",
                    invalidFields);
            throw new ConfigurationException(getMessage(
                    AWSConstants.EX_INVALIDFIELDS, invalidFields));
        } else {
            log.ok("AWS Configuration Paramaeters Validation Success");
        }
    }

    /**
     * Return customized messages.
     * 
     * @param key
     *            value.
     * @param objects
     *            collection.
     * @return message - connector message during Exception.
     */
    public String getMessage(String key, Object... objects) {
        return getConnectorMessages().format(key, key, objects);
    }
}
