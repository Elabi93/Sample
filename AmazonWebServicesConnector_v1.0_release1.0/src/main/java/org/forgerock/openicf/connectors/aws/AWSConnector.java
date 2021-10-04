package org.forgerock.openicf.connectors.aws;

import java.util.Set;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;

import org.forgerock.openicf.connectors.aws.operations.AWSCreateOperation;
import org.forgerock.openicf.connectors.aws.operations.AWSDeleteOperation;
import org.forgerock.openicf.connectors.aws.operations.AWSFilterTranslator;
import org.forgerock.openicf.connectors.aws.operations.AWSSearchOperation;
import org.forgerock.openicf.connectors.aws.operations.AWSUpdateOperation;
import org.forgerock.openicf.connectors.aws.util.AWSConnection;
import org.forgerock.openicf.connectors.aws.util.AWSConstants;
import org.forgerock.openicf.connectors.aws.util.AWSSchema;
import org.forgerock.openicf.connectors.aws.util.AWSUtility;
import org.identityconnectors.common.Pair;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

/**
 * AWSConnector class.
 */
@ConnectorClass(configurationClass = AWSConfiguration.class, displayNameKey = AWSConstants.AWS_CONNECTOR_DISPLAY)
public class AWSConnector
        implements Connector, SchemaOp, TestOp, CreateOp, UpdateOp, DeleteOp, SearchOp<Pair<String, String>> {

    /**
     * Logger for AWSConnector class.
     *
     */
    private static Log log = Log.getLog(AWSConnector.class);
    /**
     * AWSConfiguration for AWS Connector.
     */
    private AWSConfiguration awsConfiguration;
    /**
     * AmazonIdentityManagement variable - AWS client.
     *
     */
    private AmazonIdentityManagement client;
    /**
     * AWSConnection - object for AWS Connection class.
     */
    private AWSConnection connection;

    /**
     * Initialize the AWS connector with its configuration.
     *
     * @param config populated with information in order to initialize the
     *               {@link AWSConnector}.
     */
    @Override
    public void init(Configuration config) {
        log.ok("Intializing AWS Configuration");
        this.awsConfiguration = (AWSConfiguration) config;
        awsConfiguration.validate();
        this.connection = new AWSConnection(awsConfiguration);
        client = connection.clientBuilder();
        log.ok("Connector initialized successfully");
    }

    /**
     * Return the configuration that was passed to {@link init(Configuration)}.
     *
     * @return awsConfiguration.
     */
    @Override
    public Configuration getConfiguration() {
        log.ok("Getting AWS Configuration");
        return this.awsConfiguration;
    }

    /**
     * Test Operation
     */
    @Override
    public void test() {
        log.ok("Test Operation");
        AWSUtility.testConnection(awsConfiguration, client);
    }

    /**
     * Retrieve the attribute schema of {@link AWSConnector}.
     * 
     * @return schema.
     */
    @Override
    public Schema schema() {
        log.ok("Schema Build initialized");
        return AWSSchema.getSchema();
    }

    /**
     * Create a AWS IAM User based on the specified attributes.
     * 
     * @param objectClass      Type of object to create. Must not be null.
     * 
     * @param createAttributes Includes all the attributes necessary to create the
     *                         AWS user account (including the
     *                         <code>ObjectClass</code> attribute).
     * 
     * @param operationOptions Additional options that impact the way this operation
     *                         runs. May be null.
     * @return The unique id for the account that is created.
     */
    @Override
    public Uid create(ObjectClass objectClass, Set<Attribute> createAttributes, OperationOptions operationOptions) {
        log.ok("Create Operation");
        AWSUtility.validateObjectClass(objectClass, awsConfiguration);
        return new AWSCreateOperation(awsConfiguration).create(client, createAttributes);
    }

    /**
     * Creating FilterTranslator Expressions.
     * 
     * @param objectClass      Reduces the number of entries to only those that
     *                         match the {@link ObjectClass} provided.
     * 
     * @param operationOptions additional options that impact the way this operation
     *                         runs. May be null.
     * 
     * @return filterPair
     */
    @Override
    public FilterTranslator<Pair<String, String>> createFilterTranslator(ObjectClass objectClass,
            OperationOptions operationOptions) {
        log.ok("Filter Translator");
        AWSUtility.validateObjectClass(objectClass, awsConfiguration);
        return new AWSFilterTranslator();
    }

    /**
     * Search the resource for all objects that match the object class and filter.
     *
     * @param objectClass      Reduces the number of entries to only those that
     *                         match the {@link ObjectClass} provided.
     * 
     * @param filterPair       Reduces the number of entries to only those that
     *                         match the {@link Filter} provided, if any. May be
     *                         null.
     * 
     * @param resultsHandler   Class responsible for working with the objects
     *                         returned from the search.
     * 
     * @param operationOptions Additional options that impact the way this operation
     *                         runs. May be null.
     */
    @Override
    public void executeQuery(ObjectClass objectClass, Pair<String, String> filterPair, ResultsHandler resultsHandler,
            OperationOptions operationOptions) {
        log.ok("Search Operation");
        AWSUtility.validateObjectClass(objectClass, awsConfiguration);
        new AWSSearchOperation(awsConfiguration).search(client, filterPair, resultsHandler, operationOptions);
    }

    /**
     * Update the AWS user account specified by the {@link Uid}, replacing the
     * current values of each attribute with the values provided.
     * 
     * @param objectClass      Type of object to modify. Must not be null.
     * 
     * @param uid              Uid of the user account to modify. Must not be null.
     * 
     * @param updAttributes    Set of Attributes to update the aws account user.
     * 
     * @param operationOptions Additional options that impact the way this operation
     *                         runs. May be null.
     * 
     * @return Uid of the updated user.
     */
    @Override
    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> updAttributes,
            OperationOptions operationOptions) {
        log.ok("Update Operation");
        AWSUtility.validateObjectClass(objectClass, awsConfiguration);
        return new AWSUpdateOperation(awsConfiguration).update(client, uid, updAttributes);
    }

    /**
     * Delete the AWS user account of specified Uid.
     *
     * @param objectClass      Type of object to delete.
     * 
     * @param uid              The unique id of aws account to delete.
     * 
     * @param operationOptions Additional options that impact the way this operation
     *                         is run. May be null.
     */
    @Override
    public void delete(ObjectClass objectClass, Uid uid, OperationOptions operationOptions) {
        log.ok("Delete Operation");
        AWSUtility.validateObjectClass(objectClass, awsConfiguration);
        new AWSDeleteOperation(awsConfiguration).delete(client, uid);
    }

    /**
     * Disposing Connection
     */
    @Override
    public void dispose() {
        log.ok("AWS Connector Disposed");
    }

}