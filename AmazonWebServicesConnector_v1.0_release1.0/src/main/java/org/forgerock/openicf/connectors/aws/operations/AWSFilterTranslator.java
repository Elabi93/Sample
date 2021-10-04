package org.forgerock.openicf.connectors.aws.operations;

import org.forgerock.openicf.connectors.aws.AWSConfiguration;
import org.forgerock.openicf.connectors.aws.util.AWSConstants;
import org.identityconnectors.common.Pair;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;

import com.amazonaws.services.identitymanagement.model.AmazonIdentityManagementException;

/**
 * AWSFilterTranslator Class
 */
public class AWSFilterTranslator
        extends AbstractFilterTranslator<Pair<String, String>> {

    /**
     * Logger for AWSFilterTranslator class
     */
    private static Log log = Log.getLog(AWSFilterTranslator.class);

    /**
     * Configuration for AWSFilterTranslator.
     */
    private AWSConfiguration configuration;
    /**
     * Pair variable filteredAttributes.
     *
     */
    private Pair<String, String> filteredAttributes;

    /**
     * @param filter
     *            Attribute for equal filter expression.
     * @param not
     *            True if this should be a NOT EQUALS
     * @return The filter query or null.
     */
    @Override
    protected Pair<String, String> createEqualsExpression(EqualsFilter filter,
            boolean not) {
        log.ok("Equals Filter :{0}", filter.getAttribute());
        try {
            Attribute data = filter.getAttribute();
            if (data.getName().equals(Name.NAME)
                    || data.getName().equals(Uid.NAME)
                    || data.getName()
                            .equals(AWSConstants.AWS_ATTRIBUTE_USERNAME)) {
                filteredAttributes = new Pair<String, String>(Name.NAME,
                        AttributeUtil.getAsStringValue(data));
            } else if (data.getName().equals(AWSConstants.AWS_ATTRIBUTE_PATH)) {
                filteredAttributes = new Pair<String, String>(
                        AWSConstants.AWS_ATTRIBUTE_PATH,
                        AttributeUtil.getAsStringValue(data));
            } else {
                log.ok(
                        "Search Users with EqualsFilter not supported for attribute: {0}",
                        data.getName());
            }
        } catch (AmazonIdentityManagementException ex) {
            log.error("AWS filter translator failed :{0}", ex);
            throw new ConnectorException(
                    configuration.getMessage(AWSConstants.EX_OPERATIONFAILED,
                            ex));
        }
        return filteredAttributes;
    }
}
