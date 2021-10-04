package org.forgerock.openicf.connectors.aws.util;

import java.util.HashSet;
import java.util.Set;

import org.forgerock.openicf.connectors.aws.AWSConnector;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;

/**
 * AWSSchema class
 */
public class AWSSchema {

    /**
     * Log variable for AWSUtility.
     *
     */
    private static Log log = Log.getLog(AWSUtility.class);

    /**
     * Schema variable - AWS Attribute schema.
     *
     */
    private static Schema schema;

    /**
     *
     * @return Schema - AWS Attribute Schema.
     *
     */
    public static Schema getSchema() {
        log.ok("Initializing Scheme Building");
        if (schema != null) {
            return schema;
        }

        SchemaBuilder schemaBuilder = new SchemaBuilder(AWSConnector.class);
        ObjectClassInfoBuilder objectClassInfoBuilder = new ObjectClassInfoBuilder();
        objectClassInfoBuilder.setType(ObjectClass.ACCOUNT_NAME);
        Set<String> attributes = new HashSet<String>();

        attributes.addAll(AWSConstants.AWS_REQUIRED_ATTRIBUTES);
        attributes.addAll(AWSConstants.AWS_OTHER_ATTRIBUTTES);
        attributes.forEach(attr -> {
            AttributeInfoBuilder attributeInfoBuilder = new AttributeInfoBuilder();
            attributeInfoBuilder.setName(attr);
            attributeInfoBuilder.setType(getAttributeType(attr));
            attributeInfoBuilder.setCreateable(
                    AWSConstants.AWS_CREATEABLE_ATTRIBUTES.contains(attr));
            attributeInfoBuilder.setUpdateable(
                    AWSConstants.AWS_UPDATEABLE_ATTRIBUTES.contains(attr));
            attributeInfoBuilder.setReadable(
                    AWSConstants.AWS_READABLE_ATTRIBUTES.contains(attr));
            attributeInfoBuilder.setReturnedByDefault(
                    AWSConstants.AWS_RETURNBYDEFAULT_ATTRIBUTES.contains(attr));
            attributeInfoBuilder.setRequired(
                    AWSConstants.AWS_REQUIRED_ATTRIBUTES.contains(attr));
            if (attr.contains(AWSConstants.AWS_PERIOD_OPERATOR)) {
                String[] complexAttributes = attr
                        .split(AWSConstants.AWS_SPLIT_BY_DOT);
                attributeInfoBuilder
                        .setMultiValued(
                                AWSConstants.AWS_MULTIVALUED_COMPLEX_ATTRIBUTES
                                        .contains(complexAttributes[0])
                                        && AWSConstants.AWS_MULTIVALUED_COMPLEX_ATTRIBUTES
                                                .contains(
                                                        complexAttributes[1]));
            } else {
                attributeInfoBuilder.setMultiValued(
                        AWSConstants.AWS_MULTIVALUED_COMPLEX_ATTRIBUTES
                                .contains(attr));
            }
            objectClassInfoBuilder
                    .addAttributeInfo(attributeInfoBuilder.build());
        });

        ObjectClassInfo objectClassInfo = objectClassInfoBuilder.build();
        schemaBuilder.defineObjectClass(objectClassInfo);

        schemaBuilder.clearSupportedObjectClassesByOperation();
        AWSConstants.ACCOUNT_OPERATIONS.forEach((operation) -> {
            schemaBuilder.addSupportedObjectClass(operation, objectClassInfo);
        });

        schema = schemaBuilder.build();
        return schema;
    }

    /**
     * 
     * @param attr
     *            - Attribute Name
     * 
     * @return - Class<?> type of class
     */
    private static Class<?> getAttributeType(String attr) {
        if (attr.equals(OperationalAttributes.PASSWORD_NAME)) {
            return GuardedString.class;
        }
        return String.class;
    }

}
