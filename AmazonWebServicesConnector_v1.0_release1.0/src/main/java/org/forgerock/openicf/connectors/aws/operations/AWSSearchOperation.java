package org.forgerock.openicf.connectors.aws.operations;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AmazonIdentityManagementException;
import com.amazonaws.services.identitymanagement.model.GetUserRequest;
import com.amazonaws.services.identitymanagement.model.GetUserResult;
import com.amazonaws.services.identitymanagement.model.ListUsersRequest;
import com.amazonaws.services.identitymanagement.model.ListUsersResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.User;

import org.forgerock.openicf.connectors.aws.AWSConfiguration;
import org.forgerock.openicf.connectors.aws.util.AWSConstants;
import org.forgerock.openicf.connectors.aws.util.AWSUtility;
import org.identityconnectors.common.Pair;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.spi.SearchResultsHandler;

/**
 * AWSSearchOperation class.
 *
 */
public class AWSSearchOperation {

    /**
     * Logger for AWSSearchOperation class.
     *
     */
    private static Log log = Log.getLog(AWSSearchOperation.class);

    /**
     * Configuration for AWS Connector.
     *
     */
    private AWSConfiguration configuration;

    /**
     * @param awsConfiguration
     *            AWSConfiguration. Constructor for SearchOperation.
     *
     */
    public AWSSearchOperation(AWSConfiguration awsConfiguration) {
        this.configuration = awsConfiguration;
    }

    /**
     * Search the resource for all objects that match the object class and
     * filter.
     * 
     * @param client
     *            AmazonIdentityManagement client to perform API calls.
     * 
     * @param filterPair
     *            Reduces the number of entries to only those that match the
     *            {Filter} provided, if any. May be null.
     * @param resultsHandler
     *            Class responsible for working with the objects returned from
     *            the search.
     * @param operationOptions
     *            AttributeSet to retrieve in search operation.
     */
    public void search(AmazonIdentityManagement client,
            Pair<String, String> filterPair, ResultsHandler resultsHandler,
            OperationOptions operationOptions) {
        int count = 0;
        boolean status = false;
        String marker = operationOptions.getPagedResultsCookie();
        Integer pageSize = configuration.getPageSize();
        if (operationOptions.getPageSize() != null
                && operationOptions.getPageSize() > 0) {
            pageSize = operationOptions.getPageSize();
        }
        do {
            try {
                ListUsersResult listUsersResult = null;
                List<User> userList = new ArrayList<User>();
                if (filterPair == null) {
                    log.info("Executing Full Search");
                    listUsersResult = client
                            .listUsers(new ListUsersRequest().withMarker(marker)
                                    .withMaxItems(pageSize));
                    userList = listUsersResult.getUsers();
                } else if (filterPair.first.equals(Name.NAME)) {
                    log.info("Executing Search with UserName Filter :{0}",
                            filterPair.second);
                    GetUserResult getUserResult = client
                            .getUser(new GetUserRequest()
                                    .withUserName(filterPair.second));
                    userList.add(getUserResult.getUser());
                } else if (filterPair.first
                        .equals(AWSConstants.AWS_ATTRIBUTE_PATH)) {
                    log.info("Executing Search with " + "Path Filter :{0}",
                            filterPair.second);
                    listUsersResult = client.listUsers(new ListUsersRequest()
                            .withMarker(marker).withMaxItems(pageSize)
                            .withPathPrefix(filterPair.second));
                    userList = listUsersResult.getUsers();
                }

                log.info("Search User Count: {0}", count += userList.size());
                userList.forEach(user -> {
                    ConnectorObject connectorObject = AWSUtility
                            .buildConnectorObject(user,
                                    operationOptions.getAttributesToGet(),
                                    client);

                    if (connectorObject != null
                            && !resultsHandler.handle(connectorObject)) {
                        log.ok("User Import Successful: {0}",
                                connectorObject.getAttributes());
                        return;
                    }
                });

                if (listUsersResult != null) {
                    if (operationOptions.getPageSize() != null) {

                        ((SearchResultsHandler) resultsHandler)
                                .handleResult(new SearchResult(
                                        listUsersResult.getMarker(), -1));
                        status = false;
                    } else {
                        marker = listUsersResult.getMarker();
                        status = listUsersResult.getIsTruncated();
                    }
                }
            } catch (NoSuchEntityException ex) {
                log.error("Search user failed, " + "user does not exists: {0}",
                        filterPair.second);
            } catch (AmazonIdentityManagementException aimex) {
                log.error("Search user failed");
                throw new ConnectorException(configuration.getMessage(
                        AWSConstants.AWS_SEARCH_USER_FAILED_EX, aimex));
            }
        } while (status);

        log.ok("Total users imported from AWS: {0}", count);
    }
}