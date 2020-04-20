//Copyright (c) 2020 Keksuccino
package de.keksuccino.core.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import de.keksuccino.core.config.ConfigEntry.EntryType;
import de.keksuccino.core.config.exceptions.InvalidValueException;
import de.keksuccino.core.math.MathUtils;

/**
 * Simple config system to locally store values.<br>
 * Can store strings (multiline), booleans and all common types of numbers.<br><br>
 * 
 * Caches values to prevent reading the config file everytime, which means, values can be called rapidly without stressing the system.
 * 
 * @author Keksuccino
 * @version 0.1.3
 */
public class Config {
	
	private String path;
	private File config;
	private Map<String, ConfigEntry> values = new HashMap<String, ConfigEntry>();
	private List<String> registeredValues = new ArrayList<String>();
	private String name = null;
	private List<String> categorys = new ArrayList<String>();
	
	public Config(String path) {
		this.path = path;
		this.config = new File(path);
		if (this.config.isFile()) {
			File f = this.config.getParentFile();
			if (!f.exists()) {
				f.mkdirs();
			}
		}
		this.init();
	}
	
	private void init() {
		List<String> l = this.getTextFileData();
		if (l.isEmpty()) {
			return;
		}
		
		String category = null;
		String desc = null;
		
		String valueName = null;
		EntryType type = null;
		String value = null;
		
		Boolean b = false;
		
		for (String s : l) {
			if (b) {
				if (new StringBuilder(s).reverse().toString().replace(" ", "").startsWith(";'")) {
					value += "\n" + new StringBuilder(new StringBuilder(s).reverse().toString().split(";", 2)[1].substring(1)).reverse().toString();
					
					if ((category != null) && (valueName != null) && (type != null) && !this.valueExists(valueName)) {
						this.values.put(valueName, new ConfigEntry(valueName, value, type, category, desc));
						
						if (!this.categorys.contains(category)) {
							this.categorys.add(category);
						}
					}
					
					desc = null;
					valueName = null;
					type = null;
					value = null;
					b = false;
				} else {
					value += "\n" + s;
				}
			}

			if (s.startsWith("##[")) {
				if (!s.contains("]")) {
					continue;
				}
				category = new StringBuilder(new StringBuilder(s.split("[\\[]", 2)[1]).reverse().toString().split("[\\]]")[1]).reverse().toString();
				continue;
			}
			
			if (s.startsWith("[")) {
				if (!s.contains("]")) {
					continue;
				}
				desc = new StringBuilder(new StringBuilder(s.split("[\\[]", 2)[1]).reverse().toString().split("[\\]]")[1]).reverse().toString();
				continue;
			}
			
			if ((s.length() > 0) && s.substring(1).startsWith(":") && s.contains("=") && s.contains("'")) {
				valueName = s.split("[:]", 2)[1].replace(" ", "").split("=")[0];
				if (s.startsWith("I:")) {
					type = EntryType.INTEGER;
				}
				if (s.startsWith("S:")) {
					type = EntryType.STRING;
				}
				if (s.startsWith("B:")) {
					type = EntryType.BOOLEAN;
				}
				if (s.startsWith("L:")) {
					type = EntryType.LONG;
				}
				if (s.startsWith("D:")) {
					type = EntryType.DOUBLE;
				}
				if (s.startsWith("F:")) {
					type = EntryType.FLOAT;
				}
				
				if (new StringBuilder(s).reverse().toString().replace(" ", "").startsWith(";'")) {
					value = new StringBuilder(new StringBuilder(s.split("'", 2)[1]).reverse().toString().split(";", 2)[1].substring(1)).reverse().toString();
					
					if ((category != null) && (valueName != null) && (type != null) && !this.valueExists(valueName)) {
						this.values.put(valueName, new ConfigEntry(valueName, value, type, category, desc));
						
						if (!this.categorys.contains(category)) {
							this.categorys.add(category);
						}
					}
					
					desc = null;
					valueName = null;
					type = null;
					value = null;
				} else {
					value = s.split("'", 2)[1];
					b = true;
				}
			}
		}
	}
	
	/**
	 * @return Unmodifiable list of all registered categorys of this config.
	 */
	public List<String> getCategorys() {
		List<String> l = new ArrayList<String>();
		l.addAll(categorys);
		return l;
	}
	
	public String getConfigName() {
		return this.name;
	}
	
	/**
	 * The name of the config displayed in the header of the config file.<br>
	 * It is needed to call {@code Config.syncConfig()} to write the name to the config file.
	 */
	public void setConfigName(String name) {
		this.name = name;
	}
	
	/**
	 * Synchronizes all changes with the config file.<br>
	 * Needs to be called every time a {@link ConfigEntry} was manually manipulated or new values were registered to the config.<br>
	 * This is <b>NOT</b> needed after setting a value by calling the {@code Config.setValue()} method!
	 */
	public void syncConfig() {
		String data = "";
		Boolean b = false;
		
		if (this.name != null) {
			data += "//" + this.name + "\n";
			b = true;
		}

		//Using getCategorys to prevent comodification
		for (String s : this.getCategorys()) {
			List<ConfigEntry> l = this.getEntrysForCategory(s);
			if (l.isEmpty()) {
				this.categorys.remove(s);
				continue;
			}
			
			if (!b) {
				b = true;
			} else {
				data += "\n\n\n";
			}

			data += "##[" + s + "]\n";
			
			for (ConfigEntry e : l) {
				String value = e.getValue();
				String valueName = e.getName();
				String desc = e.getDescription();
				EntryType type = e.getType();
				
				if ((value != null) && (valueName != null) && (type != null)) {
					if (desc != null) {
						data += "\n[" + desc + "]";
					}
					
					if (type == EntryType.STRING) {
						data += "\nS:" + valueName + " = '";
					}
					if (type == EntryType.INTEGER) {
						data += "\nI:" + valueName + " = '";
					}
					if (type == EntryType.BOOLEAN) {
						data += "\nB:" + valueName + " = '";
					}
					if (type == EntryType.LONG) {
						data += "\nL:" + valueName + " = '";
					}
					if (type == EntryType.DOUBLE) {
						data += "\nD:" + valueName + " = '";
					}
					if (type == EntryType.FLOAT) {
						data += "\nF:" + valueName + " = '";
					}
					
					data += value + "';";
				}
			}
		}
		
		File oldConfig = this.backupConfig();
		
		if (oldConfig == null) {
			System.out.println("############################################");
			System.out.println("WARNING: CONFIG BACKUP NOT SUCCESSFULL! (" + this.path + ")");
			System.out.println("############################################");
		}

		if (!this.config.exists()) {
			try {
				this.config.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		BufferedWriter writer = null;
		try {
        	writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.config), StandardCharsets.UTF_8));
            writer.write(data);
            writer.flush();
            oldConfig.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private File backupConfig() {
		File back = new File(this.config.getAbsolutePath() + ".backup");
		List<String> data = this.getTextFileData();
		String data2 = "";
		
		if (!back.exists()) {
			try {
				back.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		Boolean b = false;
		for (String s : data) {
			if (!b) {
				b = true;
			} else {
				data2 += "\n";
			}
			data2 += s;
		}
		
		BufferedWriter writer = null;
		try {
        	writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(back), StandardCharsets.UTF_8));
            writer.write(data2);
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (back.exists()) {
			return back;
		}
		return null;
	}
	
	public List<ConfigEntry> getEntrysForCategory(String category) {
		List<ConfigEntry> l = new ArrayList<ConfigEntry>();
		
		for (Map.Entry<String, ConfigEntry> m : this.values.entrySet()) {
			if (m.getValue().getCategory().equals(category)) {
				l.add(m.getValue());
			}
		}
		
		return l;
	}
	
	private List<String> getTextFileData() {
		List<String> l = new ArrayList<String>();
		if (this.config.exists()) {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.config), StandardCharsets.UTF_8));
	            String line;
	            while ((line = reader.readLine()) != null) {
	            	l.add(line);
	            }
	            reader.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return l;
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 * @param name
	 */
	public void registerValue(String uniqueName, Integer defaultValue, String category) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.INTEGER, null);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 * @param name
	 */
	public void registerValue(String uniqueName, Double defaultValue, String category) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.DOUBLE, null);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 * @param name
	 */
	public void registerValue(String uniqueName, Long defaultValue, String category) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.LONG, null);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 * @param name
	 */
	public void registerValue(String uniqueName, Float defaultValue, String category) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.FLOAT, null);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 * @param name
	 */
	public void registerValue(String uniqueName, Boolean defaultValue, String category) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.BOOLEAN, null);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 * @param name
	 */
	public void registerValue(String uniqueName, String defaultValue, String category) throws InvalidValueException {
		this.registerRawValue(uniqueName, defaultValue, category, EntryType.STRING, null);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 * @param name
	 */
	public void registerValue(String uniqueName, Integer defaultValue, String category, @Nullable String description) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.INTEGER, description);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 * @param name
	 */
	public void registerValue(String uniqueName, Double defaultValue, String category, @Nullable String description) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.DOUBLE, description);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 * @param name
	 */
	public void registerValue(String uniqueName, Float defaultValue, String category, @Nullable String description) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.FLOAT, description);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 * @param name
	 */
	public void registerValue(String uniqueName, Long defaultValue, String category, @Nullable String description) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.LONG, description);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 * @param name
	 */
	public void registerValue(String uniqueName, Boolean defaultValue, String category, @Nullable String description) throws InvalidValueException {
		this.registerRawValue(uniqueName, String.valueOf(defaultValue), category, EntryType.BOOLEAN, description);
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to synchronize the new values with the config file.
	 * @param name
	 */
	public void registerValue(String uniqueName, String defaultValue, String category, @Nullable String description) throws InvalidValueException {
		this.registerRawValue(uniqueName, defaultValue, category, EntryType.STRING, description);
	}

	private void registerRawValue(String uniqueName, String defaultValue, String category, EntryType type, @Nullable String description) throws InvalidValueException {
		if (uniqueName == null) {
			throw new InvalidValueException("Value name cannot be null!");
		}
		if (defaultValue == null) {
			throw new InvalidValueException("Default value cannot be null!");
		}
		if (category == null) {
			throw new InvalidValueException("Category cannot be null!");
		}
		if (type == null) {
			throw new InvalidValueException("Type cannot be null!");
		}
		
		if (!this.categorys.contains(category)) {
			this.categorys.add(category);
		}
		
		if (type == EntryType.BOOLEAN) {
			if (!defaultValue.equalsIgnoreCase("true") && !defaultValue.equalsIgnoreCase("false")) {
				throw new InvalidValueException("This value is not a valid BOOLEAN! (" + defaultValue + ")");
			}
		}
		if (type == EntryType.INTEGER) {
			if (!MathUtils.isInteger(defaultValue)) {
				throw new InvalidValueException("This value is not a valid INTEGER! (" + defaultValue + ")");
			}
		}
		if (type == EntryType.DOUBLE) {
			if (!MathUtils.isDouble(defaultValue)) {
				throw new InvalidValueException("This value is not a valid DOUBLE! (" + defaultValue + ")");
			}
		}
		if (type == EntryType.FLOAT) {
			if (!MathUtils.isFloat(defaultValue)) {
				throw new InvalidValueException("This value is not a valid FLOAT! (" + defaultValue + ")");
			}
		}
		if (type == EntryType.LONG) {
			if (!MathUtils.isLong(defaultValue)) {
				throw new InvalidValueException("This value is not a valid LONG! (" + defaultValue + ")");
			}
		}
		
		if (!this.valueExists(uniqueName)) {
			this.values.put(uniqueName, new ConfigEntry(uniqueName, defaultValue, type, category, description));
		}
		this.registeredValues.add(uniqueName);
	}
	
	/**
	 * All changes to a {@link ConfigEntry} must be manually synchronized with the config file by calling {@link Config.syncConfig()}!
	 * @return An editable {@link ConfigEntry}.
	 */
	public ConfigEntry getAsEntry(String name) {
		if (this.valueExists(name)) {
			return this.values.get(name);
		}
		return null;
	}
	
	/**
	 * All changes to a {@link ConfigEntry} must be manually synchronized with the config file by calling {@link Config.syncConfig()}!
	 * @return A list with all values of this config as editable {@link ConfigEntry}s.
	 */
	public List<ConfigEntry> getAllAsEntry() {
		List<ConfigEntry> l = new ArrayList<ConfigEntry>();
		l.addAll(values.values());
		return l;
	}

	public void setValue(String name, String value) throws InvalidValueException {
		if (!this.valueExists(name)) {
			return;
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.STRING) {
			e.setValue(value);
			this.syncConfig();
		} else {
			throw new InvalidValueException("This value's type is " + e.getType() + "! It isn't possible to set a STRING value to it!");
		}
	}
	
	public void setValue(String name, Integer value) throws InvalidValueException {
		if (!this.valueExists(name)) {
			return;
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.INTEGER) {
			e.setValue(String.valueOf(value));
			this.syncConfig();
		} else {
			throw new InvalidValueException("This value's type is " + e.getType() + "! It isn't possible to set an INTEGER value to it!");
		}
	}
	
	public void setValue(String name, Boolean value) throws InvalidValueException {
		if (!this.valueExists(name)) {
			return;
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.BOOLEAN) {
			e.setValue(String.valueOf(value));
			this.syncConfig();
		} else {
			throw new InvalidValueException("This value's type is " + e.getType() + "! It isn't possible to set a BOOLEAN value to it!");
		}
	}
	
	public void setValue(String name, Float value) throws InvalidValueException {
		if (!this.valueExists(name)) {
			return;
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.FLOAT) {
			e.setValue(String.valueOf(value));
			this.syncConfig();
		} else {
			throw new InvalidValueException("This value's type is " + e.getType() + "! It isn't possible to set a FLOAT value to it!");
		}
	}
	
	public void setValue(String name, Double value) throws InvalidValueException {
		if (!this.valueExists(name)) {
			return;
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.DOUBLE) {
			e.setValue(String.valueOf(value));
			this.syncConfig();
		} else {
			throw new InvalidValueException("This value's type is " + e.getType() + "! It isn't possible to set a DOUBLE value to it!");
		}
	}
	
	public void setValue(String name, Long value) throws InvalidValueException {
		if (!this.valueExists(name)) {
			return;
		}
		ConfigEntry e = this.getAsEntry(name);
		if (e.getType() == EntryType.LONG) {
			e.setValue(String.valueOf(value));
			this.syncConfig();
		} else {
			throw new InvalidValueException("This value's type is " + e.getType() + "! It isn't possible to set a LONG value to it!");
		}
	}
	
	/**
	 * It is needed to call {@code Config.syncConfig()} after this to remove the value from the config file.
	 * @param name
	 */
	public void unregisterValue(String name) {
		if (this.valueExists(name)) {
			this.values.remove(name);
			if (this.registeredValues.contains(name)) {
				this.registeredValues.remove(name);
			}
		}
	}
	
	public Boolean getBoolean(String name) throws InvalidValueException {
		if (!this.valueExists(name)) {
			throw new InvalidValueException("This value does not exist! (" + name + ")");
		}
		ConfigEntry e = this.getAsEntry(name);
		
		if (e.getType() == EntryType.BOOLEAN) {
			if (e.getValue().equalsIgnoreCase("true")) {
				return true;
			} else if (e.getValue().equalsIgnoreCase("false")) {
				return false;
			} else {
				throw new InvalidValueException("This value is not a valid BOOLEAN value!");
			}
		} else {
			throw new InvalidValueException("This value's type is not BOOLEAN!");
		}
	}
	
	public String getString(String name) throws InvalidValueException {
		if (!this.valueExists(name)) {
			throw new InvalidValueException("This value does not exist! (" + name + ")");
		}
		ConfigEntry e = this.getAsEntry(name);
		
		if (e.getType() == EntryType.STRING) {
			return e.getValue();
		} else {
			throw new InvalidValueException("This value's type is not STRING!");
		}
	}
	
	public Integer getInteger(String name) throws InvalidValueException {
		if (!this.valueExists(name)) {
			throw new InvalidValueException("This value does not exist! (" + name + ")");
		}
		ConfigEntry e = this.getAsEntry(name);
		
		if (e.getType() == EntryType.INTEGER) {
			if (MathUtils.isInteger(e.getValue())) {
				return Integer.parseInt(e.getValue());
			} else {
				throw new InvalidValueException("This value is not a valid INTEGER value!");
			}
		} else {
			throw new InvalidValueException("This value's type is not INTEGER!");
		}
	}
	
	public Double getDouble(String name) throws InvalidValueException {
		if (!this.valueExists(name)) {
			throw new InvalidValueException("This value does not exist! (" + name + ")");
		}
		ConfigEntry e = this.getAsEntry(name);
		
		if (e.getType() == EntryType.DOUBLE) {
			if (MathUtils.isDouble(e.getValue())) {
				return Double.parseDouble(e.getValue());
			} else {
				throw new InvalidValueException("This value is not a valid DOUBLE value!");
			}
		} else {
			throw new InvalidValueException("This value's type is not DOUBLE!");
		}
	}
	
	public Long getLong(String name) throws InvalidValueException {
		if (!this.valueExists(name)) {
			throw new InvalidValueException("This value does not exist! (" + name + ")");
		}
		ConfigEntry e = this.getAsEntry(name);
		
		if (e.getType() == EntryType.LONG) {
			if (MathUtils.isLong(e.getValue())) {
				return Long.parseLong(e.getValue());
			} else {
				throw new InvalidValueException("This value is not a valid LONG value!");
			}
		} else {
			throw new InvalidValueException("This value's type is not LONG!");
		}
	}
	
	public Float getFloat(String name) throws InvalidValueException {
		if (!this.valueExists(name)) {
			throw new InvalidValueException("This value does not exist! (" + name + ")");
		}
		ConfigEntry e = this.getAsEntry(name);
		
		if (e.getType() == EntryType.FLOAT) {
			if (MathUtils.isFloat(e.getValue())) {
				return Float.parseFloat(e.getValue());
			} else {
				throw new InvalidValueException("This value is not a valid LONG value!");
			}
		} else {
			throw new InvalidValueException("This value's type is not LONG!");
		}
	}
	
	public void setCategory(String valueName, String category) throws InvalidValueException {
		if (this.valueExists(valueName)) {
			ConfigEntry e = this.getAsEntry(valueName);
			e.setCategory(category);
			if (!this.categorys.contains(category)) {
				this.categorys.add(category);
			}
			this.syncConfig();
		} else {
			throw new InvalidValueException("This values does not exist! (" + valueName + ")");
		}
	}
	
	public void setDescription(String valueName, String description) throws InvalidValueException {
		if (this.valueExists(valueName)) {
			ConfigEntry e = this.getAsEntry(valueName);
			e.setDescription(description);
			this.syncConfig();
		} else {
			throw new InvalidValueException("This values does not exist! (" + valueName + ")");
		}
	}
	
	public boolean valueExists(String name) {
		return this.values.containsKey(name);
	}
	
	/**
	 * Will remove all (unused) values from the config file which were not registered via {@code Config.registerValue()}.<br><br>
	 * 
	 * <b>DON'T CALL THIS BEFORE YOU'VE REGISTERED ALL VALUES!</b>
	 */
	public void clearUnusedValues() {
		List<String> l = new ArrayList<String>();
		for (Map.Entry<String, ConfigEntry> m : this.values.entrySet()) {
			if (!this.registeredValues.contains(m.getKey())) {
				l.add(m.getKey());
			}
		}
		for (String s : l) {
			this.unregisterValue(s);
		}
		this.syncConfig();
	}
	
	public <T> T getOrDefault(String valueName, T defaultValue) {
		try {
			if (defaultValue instanceof Integer) {
				return (T) this.getInteger(valueName);
			}
			if (defaultValue instanceof Boolean) {
				return (T) this.getBoolean(valueName);
			}
			if (defaultValue instanceof String) {
				return (T) this.getString(valueName);
			}
			if (defaultValue instanceof Long) {
				return (T) this.getLong(valueName);
			}
			if (defaultValue instanceof Double) {
				return (T) this.getDouble(valueName);
			}
			if (defaultValue instanceof Float) {
				return (T) this.getFloat(valueName);
			}
		} catch (Exception e) {}
		return defaultValue;
	}

}
