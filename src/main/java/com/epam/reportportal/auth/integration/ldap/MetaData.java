package com.epam.reportportal.auth.integration.ldap;

import com.epam.ta.reportportal.entity.JsonbObject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.codehaus.jackson.annotate.JsonTypeName;

import java.util.Map;

/**
 * @author Ivan Budayeu
 */
@JsonTypeName("metadata")
public class MetaData extends JsonbObject {
	private Map<String, Object> metadata;

	@JsonCreator
	public MetaData(@JsonProperty("metadata") Map<String, Object> metadata) {

		this.metadata = metadata;
	}

	public Map<String, Object> getDetails() {
		return metadata;
	}

	public void setDetails(Map<String, Object> details) {
		this.metadata = metadata;
	}
}
