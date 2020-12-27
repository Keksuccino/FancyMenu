package de.keksuccino.core.properties;

import java.util.ArrayList;
import java.util.List;

public class PropertiesSet {
	
	private String propertiesType;
	private List<PropertiesSection> properties = new ArrayList<PropertiesSection>();
	
	public PropertiesSet(String propertiesType) {
		this.propertiesType = propertiesType;
	}
	
	public void addProperties(PropertiesSection data) {
		if (!this.properties.contains(data)) {
			this.properties.add(data);
		}
	}
	
	public List<PropertiesSection> getProperties() {
		return this.properties;
	}
	
	public List<PropertiesSection> getPropertiesOfType(String type) {
		List<PropertiesSection> props = new ArrayList<PropertiesSection>();
		for (PropertiesSection d : this.properties) {
			if (d.getSectionType().equals(type)) {
				props.add(d);
			}
		}
		return props;
	}
	
	public String getPropertiesType() {
		return this.propertiesType;
	}

}
