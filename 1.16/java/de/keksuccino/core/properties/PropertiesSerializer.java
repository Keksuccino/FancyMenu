package de.keksuccino.core.properties;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.keksuccino.core.file.FileUtils;

public class PropertiesSerializer {
	
	/**
	 * Returns a new {@link PropertiesSet} instance or null if the given file was not a valid properties file.
	 */
	public static PropertiesSet getProperties(String filePath) {
		File f = new File(filePath);
		if (f.exists() && f.isFile()) {
			List<String> lines = FileUtils.getFileLines(f);
			List<PropertiesSection> data = new ArrayList<PropertiesSection>();
			String propertiesType = null;
			PropertiesSection currentData = null;
			boolean insideData = false;
			
			for (String s : lines) {
				String comp = s.replace(" ", "");
				//Setting the type of the PropertiesSet
				if (comp.startsWith("type=") && !insideData) {
					propertiesType = comp.split("[=]", 2)[1];
					continue;
				}
				
				//Starting a new data section
				if (comp.endsWith("{")) {
					if (!insideData) {
						insideData = true;
					} else {
						System.out.println("######################### WARNING #########################");
						System.out.println("Invalid properties found in '" + filePath + "'! (Leaking properties section; Missing '}')");
						System.out.println("###########################################################");
						if (currentData != null) {
							data.add(currentData);
						}
					}
					currentData = new PropertiesSection(comp.split("[{]")[0]);
					continue;
				}
				
				//Finishing the data section
				if (comp.startsWith("}") && insideData) {
					data.add(currentData);
					insideData = false;
					continue;
				}
				
				//Collecting all entrys inside the data section
				if (insideData && comp.contains("=")) {
					String value = s.split("[=]", 2)[1];
					if (value.startsWith(" ")) {
						value = value.substring(1);
					}
					currentData.addEntry(comp.split("[=]", 2)[0], value);
				}
			}
			
			if (propertiesType != null) {
				PropertiesSet set = new PropertiesSet(propertiesType);
				for (PropertiesSection d : data) {
					set.addProperties(d);
				}
				
				return set;
			} else {
				System.out.println("######################### WARNING #########################");
				System.out.println("Invalid properties file found: " + filePath + " (Missing properties type)");
				System.out.println("###########################################################");
			}
		}
		return null;
	}

}
