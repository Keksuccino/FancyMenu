package de.keksuccino.resources;

public class ResourceUtils {
	
	public static String[] splitToNamespaceAndPath(String fullPath, char splitter) {
		String[] s = new String[] { "minecraft", fullPath };
		int i = fullPath.indexOf(splitter);
		if (i >= 0) {
			s[1] = fullPath.substring(i + 1, fullPath.length());
			if (i >= 1) {
				s[0] = fullPath.substring(0, i);
			}
		}
		return s;
	}

}
