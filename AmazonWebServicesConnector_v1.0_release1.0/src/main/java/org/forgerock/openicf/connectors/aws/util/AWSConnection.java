package org.forgerock.openicf.connectors.aws.util;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.AmazonIdentityManagementException;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;

import org.forgerock.openicf.connectors.aws.AWSConfiguration;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;

/**
 * AWSConnection class
 */
public class AWSConnection {

    /**
     * Logger for AWSConnection
     *
     */
    private static Log log = Log.getLog(AWSConnection.class);
    /**
     * Configuration for AWS Connector.
     *
     */
    private AWSConfiguration configuration;
    /**
     * AmazonIdentityManagement variable - AWS client.
     *
     */
    private AmazonIdentityManagement client;
    /**
     * BasicAWSCredentials variable - AWS awsBasicCredentials.
     *
     */
    private BasicAWSCredentials awsBasicCredentials;
    /**
     * Credentials variable - AWS credentials.
     *
     */
    private Credentials credentials;

    /**
     * @param awsConfiguration
     *            AWSConfiguration. Constructor for AWSConnection.
     *
     */
    public AWSConnection(AWSConfiguration awsConfiguration) {
        this.configuration = awsConfiguration;
    }

    /**
     * AWSCredentialsProviderBasicImpl AWS credential provider basic
     * implementation
     *
     */
    class AWSCredentialsProviderBasicImpl implements AWSCredentialsProvider {

        /**
         * Refresh AWS Basic Credentials
         */
        @Override
        public void refresh() {
            log.ok("Refreshing AWS Credentials Provider");
        }

        /**
         * Getting AWS Basic Credentials
         * 
         * return awsBasicCredentials
         */
        @Override
        public AWSCredentials getCredentials() {
            log.info("Initializing Basic AWS Credentials");
            configuration.getSecretKey().access(key -> {
                final BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(
                        configuration.getAccessKeyId(),
                        new String(key));
                awsBasicCredentials = basicAWSCredentials;
            });
            return awsBasicCredentials;
        }

    }

    /**
     * AWSCredentialsProviderSessionImpl AWS credential provider session
     * implementation
     *
     */
    class AWSCredentialsProviderSessionImpl implements AWSCredentialsProvider {

        /**
         *
         * @param awsCredentials
         *            - AWS Credentials.
         */
        public AWSCredentialsProviderSessionImpl(Credentials awsCredentials) {
            credentials = awsCredentials;
        }

        /**
         * Refresh AWS Credentials
         */
        @Override
        public void refresh() {
            log.ok("Refreshing AWS Credentials Provider");
        }

        /**
         * Getting AWS Session Credentials
         * 
         * return basic
         */
        @Override
        public AWSCredentials getCredentials() {
            log.info("Initializing  Basic AWS Session Credentials");
            BasicSessionCredentials basic = new BasicSessionCredentials(
                    credentials.getAccessKeyId(),
                    credentials.getSecretAccessKey(),
                    credentials.getSessionToken());
            return basic;
        }

    }

    /**
     * Client for accessing IAM
     * 
     * @return client
     */
    public AmazonIdentityManagement clientBuilder() {
        log.info("Initializing AWS Authorization");
        try {
            AWSCredentialsProvider provider = new AWSCredentialsProviderBasicImpl();
            client = AmazonIdentityManagementClient.builder()
                    .withCredentials(provider).withRegion(Regions.US_EAST_1)
                    .build();
            AWSSecurityTokenService awsSecurityTokenService = AWSSecurityTokenServiceClientBuilder
                    .standard()
                    .withRegion(Regions.US_EAST_1).withCredentials(provider)
                    .build();
            log.ok("Assuming Role");
            AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest();
            assumeRoleRequest.setRoleArn(configuration.getRoleARN());
            assumeRoleRequest
                    .setRoleSessionName(AWSConstants.AWS_ROLE_SESSION_NAME);
            assumeRoleRequest.setDurationSeconds(
                    configuration.getCredentialsExpiration());
            AssumeRoleResult assumeRoleResult = new AssumeRoleResult();
            assumeRoleResult = awsSecurityTokenService
                    .assumeRole(assumeRoleRequest);
            log.ok("Setting up Credentials");

            Credentials credentials = assumeRoleResult.getCredentials();
            provider = new AWSCredentialsProviderSessionImpl(credentials);
            client = AmazonIdentityManagementClient.builder()
                    .withCredentials(provider).withRegion(Regions.US_EAST_1)
                    .build();
            log.info("Authorization Initialized Successfully");
            return client;
        } catch (NoSuchEntityException ex) {
            log.error("Role {0} does not exist: {1}",
                    configuration.getRoleARN(), ex.getMessage());
            throw new ConnectionFailedException(configuration
                    .getMessage(AWSConstants.AWS_ROLE_NOT_EXISTS_EX, ex));
        } catch (AmazonIdentityManagementException ex) {
            log.error("AWS connection failed: {0}", ex.getMessage());
            throw new ConnectionFailedException(configuration
                    .getMessage(AWSConstants.AWS_INVALID_CREDENTIALS_EX, ex));
        }
    }
}
