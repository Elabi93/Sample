package org.forgerock.openicf.connectors.aws.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AmazonIdentityManagementException;
import com.amazonaws.services.identitymanagement.model.ListUserTagsRequest;
import com.amazonaws.services.identitymanagement.model.ListUserTagsResult;
import com.amazonaws.services.identitymanagement.model.ListUsersRequest;
import com.amazonaws.services.identitymanagement.model.ListUsersResult;
import com.amazonaws.services.identitymanagement.model.Tag;
import com.amazonaws.services.identitymanagement.model.User;

import org.forgerock.openicf.connectors.aws.AWSConfiguration;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoUtil;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;

/**
 * AWSUtility class
 */
public class AWSUtility {

	/**
	 * Logger for AWS Utility
	 */
	private static Log log = Log.getLog(AWSUtility.class);

	/**
	 * Validating the guarded String
	 * 
	 * @param secretKey Guarded String to validate.
	 */
	public static void validateGuardedString(GuardedString secretKey) {
		log.ok("Validating GuardedString Attribute");
		secretKey.access(key -> {
			if (key == null || key.length == 0) {
				throw new ConfigurationException();
			}
		});
	}

	/**
	 * Test Operation
	 * 
	 * @param awsConfiguration configuration for AWS.
	 * @param client           the amazon identity management client.
	 */
	public static void testConnection(AWSConfiguration awsConfiguration, AmazonIdentityManagement client) {
		log.info("Testing AWS Connection");
		try {
			ListUsersResult listUsersResult = client.listUsers(new ListUsersRequest().withMarker(null).withMaxItems(1));
			if (!(listUsersResult.getUsers().isEmpty()))
				log.ok("Test Connection Successfull");
		} catch (AmazonIdentityManagementException aimex) {
			log.error("Invalid Credentials: {0}", aimex);
			throw new ConnectionFailedException(
					awsConfiguration.getMessage(AWSConstants.AWS_INVALID_CREDENTIALS_EX, aimex));
		}
	}

	/**
	 * Validating request payload.
	 *
	 * @param attributesSet attributesSet for validation.
	 * @param configuration AWS connector configuration.
	 */
	public static void validateRequestPayload(Set<Attribute> attributesSet, AWSConfiguration configuration) {
		Set<AttributeInfo> schemaAttribInfos = AWSSchema.getSchema().findObjectClassInfo(ObjectClass.ACCOUNT_NAME)
				.getAttributeInfo();
		Set<String> unknownAttrs = new HashSet<String>();
		for (Attribute attr : attributesSet) {
			if (attr.getName().contains(AWSConstants.AWS_TAGS_MEMBER)) {
				continue;
			}
			if (AttributeInfoUtil.find(attr.getName(), schemaAttribInfos) == null) {
				unknownAttrs.add(attr.getName());
			}
		}
		if (!unknownAttrs.isEmpty()) {
			log.error("Unknown attribute found: {0}", unknownAttrs);
			throw new ConnectorException(
					configuration.getMessage(AWSConstants.AWS_ATTRIBUTE_UNKNOWN_EXCEPTION, unknownAttrs));
		}
	}

	/**
	 * Build connector object
	 * 
	 * @param user            - AWS user record.
	 * @param attributesToGet attributes to search.
	 * @param client          the amazon identity management client.
	 * @return connectorObject
	 */
	public static ConnectorObject buildConnectorObject(User user, String[] attributesToGet,
			AmazonIdentityManagement client) {
		ConnectorObjectBuilder connectorObjectBuilder = new ConnectorObjectBuilder();
		connectorObjectBuilder.setName(user.getUserName().toString());
		connectorObjectBuilder.setUid(user.getUserName().toString());
		ListUserTagsResult lTagsResult = client
				.listUserTags(new ListUserTagsRequest().withUserName(user.getUserName()));
		List<Tag> tagList = lTagsResult.getTags();
		if (attributesToGet != null) {
			for (String attr : attributesToGet) {
				switch (attr) {
				case AWSConstants.AWS_ATTRIBUTE_ID:
					connectorObjectBuilder
							.addAttribute(AttributeBuilder.build(AWSConstants.AWS_ATTRIBUTE_ID, user.getUserId()));
					break;
				case AWSConstants.AWS_ATTRIBUTE_ARN:
					connectorObjectBuilder
							.addAttribute(AttributeBuilder.build(AWSConstants.AWS_ATTRIBUTE_ARN, user.getArn()));
					break;
				case AWSConstants.AWS_ATTRIBUTE_PATH:
					connectorObjectBuilder
							.addAttribute(AttributeBuilder.build(AWSConstants.AWS_ATTRIBUTE_PATH, user.getPath()));
					break;
				case AWSConstants.AWS_ATTRIBUTE_CREATE_DATE:
					connectorObjectBuilder.addAttribute(AttributeBuilder.build(AWSConstants.AWS_ATTRIBUTE_CREATE_DATE,
							user.getCreateDate().toString()));
					break;
				case AWSConstants.AWS_ATTRIBUTE_PASSWORD_LAST_USED:
					if (user.getPasswordLastUsed() != null)
						connectorObjectBuilder.addAttribute(AttributeBuilder.build(
								AWSConstants.AWS_ATTRIBUTE_PASSWORD_LAST_USED, user.getPasswordLastUsed().toString()));
					break;
				case AWSConstants.AWS_ATTRIBUTE_PERMISSION_BOUNDARY:
					if (user.getPermissionsBoundary() != null)
						connectorObjectBuilder
								.addAttribute(AttributeBuilder.build(AWSConstants.AWS_ATTRIBUTE_PERMISSION_BOUNDARY,
										user.getPermissionsBoundary().getPermissionsBoundaryArn().toString()));
					break;
				case AWSConstants.AWS_ATTRIBUTE_TAGS_MEMBER_KEY:
					if (tagList.size() > 0) {
						List<Object> tagObject = tagList.stream().map(data -> data.getKey())
								.collect(Collectors.toList());
						connectorObjectBuilder.addAttribute(new Attribute[] {
								AttributeBuilder.build(AWSConstants.AWS_ATTRIBUTE_TAGS_MEMBER_KEY, tagObject) });
					}
					break;
				case AWSConstants.AWS_ATTRIBUTE_TAGS_MEMBER_VALUE:
					if (tagList.size() > 0) {
						List<Object> tagObject2 = tagList.stream().map(data -> data.getValue())
								.collect(Collectors.toList());
						connectorObjectBuilder.addAttribute(new Attribute[] {
								AttributeBuilder.build(AWSConstants.AWS_ATTRIBUTE_TAGS_MEMBER_VALUE, tagObject2) });
					}
					break;
				default:
					break;
				}
			}
		}
		log.ok("Importing User with : {0}", user.getUserName());
		connectorObjectBuilder.setObjectClass(ObjectClass.ACCOUNT);
		return connectorObjectBuilder.build();
	}

	/**
	 * Validate ObjectClass
	 * 
	 * @param objectClass      validating Object class. Must not be null.
	 * 
	 * @param awsConfiguration configuration for AWS.
	 */
	public static void validateObjectClass(ObjectClass objectClass, AWSConfiguration awsConfiguration) {
		log.ok("Validating ObjectClass");
		if (objectClass == null) {
			throw new ConnectorException(awsConfiguration.getMessage(AWSConstants.AWS_OBJECTCLASS_EX, objectClass));
		} else if (!ObjectClass.ACCOUNT.equals(objectClass)) {
			log.error("ObjectClass is invalid");
			throw new ConnectorException(
					awsConfiguration.getMessage(AWSConstants.AWS_OBJECTCLASS_EX, objectClass.getDisplayNameKey()));
		}
	}

	/**
	 * Generating Tags
	 * 
	 * @param attributes includes all the attributes necessary to create or update
	 *                   the workplace user account (including the
	 *                   <code>ObjectClass</code> attribute).
	 * @return keysAndValues
	 */
	public static Map<String, Object> reBuildingTags(Set<Attribute> attributes) {
		Map<String, Object> keys = new HashMap<String, Object>();
		Map<String, Object> values = new HashMap<String, Object>();
		Map<String, Object> keysAndValues = new HashMap<String, Object>();
		for (Attribute attr : attributes) {
			String attrName = attr.getName();
			String attrValue = AttributeUtil.getAsStringValue(attr);
			if (attrName.contains(AWSConstants.AWS_PERIOD_OPERATOR)) {
				String[] complexAttr = attrName.split(AWSConstants.AWS_SPLIT_BY_DOT);
				if (complexAttr[0].equals(AWSConstants.AWS_ATTRIBUTE_TAGS)
						&& complexAttr[1].equals(AWSConstants.AWS_ATTRIBUTE_MEMBER)) {
					String keyOrValue = complexAttr[complexAttr.length - 1];
					if (keyOrValue.equalsIgnoreCase(AWSConstants.AWS_ATTRIBUTE_TAGS_KEY)) {
						keys.put(attrName, attrValue);
					} else if (keyOrValue.equalsIgnoreCase(AWSConstants.AWS_ATTRIBUTE_TAGS_VALUE)) {
						values.put(attrName, attrValue);
					}
				}

			}
		}
		for (Map.Entry<String, Object> keyEntry : keys.entrySet()) {
			for (Map.Entry<String, Object> valueEntry : values.entrySet()) {
				String newKeyEntry = keyEntry.getKey().split(AWSConstants.AWS_SPLIT_BY_DOT)[2];
				String newValueEntry = valueEntry.getKey().split(AWSConstants.AWS_SPLIT_BY_DOT)[2];
				if (newKeyEntry.equals(newValueEntry)) {
					keysAndValues.put(keyEntry.getValue().toString(), valueEntry.getValue());
				}
			}
		}
		return keysAndValues;
	}
}