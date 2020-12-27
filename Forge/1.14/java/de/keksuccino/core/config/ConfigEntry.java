//Copyright (c) 2020 Keksuccino
package de.keksuccino.core.config;

import javax.annotation.Nullable;

public class ConfigEntry {

	private String name;
	private String value;
	private String category;
	private String desc;
	private EntryType type;

	/**
	 * All changes to a {@link ConfigEntry} must be manually synchronized with the config file by calling {@link Config.syncConfig()}!
	 */
	public ConfigEntry(String name, String value, EntryType type, String category, @Nullable String description) {
		this.name = name;
		this.value = value;
		this.type = type;
		this.category = category;
		this.desc = description;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getValue() {
		return this.value;
	}
	
	public EntryType getType() {
		return this.type;
	}
	
	public String getCategory() {
		return this.category;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public void setCategory(String category) {
		this.category = category;
	}
	
	public void setDescription(String description) {
		this.desc = description;
	}
	
	/**
	 * @return The value description <b>or null</b> if the value has no description.
	 */
	public String getDescription() {
		return this.desc;
	}
	
	public static enum EntryType {
		INTEGER,
		STRING,
		DOUBLE,
		LONG,
		FLOAT,
		BOOLEAN;
	}
	
}
