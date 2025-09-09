package de.keksuccino.fancymenu.util;

/**
 * Utility class for operating system detection
 */
public class OsUtils {
    
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    
    /**
     * Private constructor to prevent instantiation
     */
    private OsUtils() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }
    
    /**
     * Check if the current operating system is Windows
     * 
     * @return true if running on Windows, false otherwise
     */
    public static boolean isWindows() {
        return OS_NAME.contains("win");
    }
    
    /**
     * Check if the current operating system is Linux
     * 
     * @return true if running on Linux, false otherwise
     */
    public static boolean isLinux() {
        return OS_NAME.contains("linux") || OS_NAME.contains("unix") || OS_NAME.contains("nix");
    }
    
    /**
     * Check if the current operating system is macOS
     * 
     * @return true if running on macOS, false otherwise
     */
    public static boolean isMacOS() {
        return OS_NAME.contains("mac") || OS_NAME.contains("darwin");
    }
    
    /**
     * Get the raw OS name string
     * 
     * @return the operating system name
     */
    public static String getOsName() {
        return System.getProperty("os.name");
    }
    
    /**
     * Get a simplified OS type
     * 
     * @return simplified OS type (WINDOWS, LINUX, MACOS, or UNKNOWN)
     */
    public static OsType getOsType() {
        if (isWindows()) {
            return OsType.WINDOWS;
        } else if (isLinux()) {
            return OsType.LINUX;
        } else if (isMacOS()) {
            return OsType.MACOS;
        } else {
            return OsType.UNKNOWN;
        }
    }
    
    /**
     * Enum representing different OS types
     */
    public enum OsType {

        WINDOWS("Windows"),
        LINUX("Linux"),
        MACOS("macOS"),
        UNKNOWN("Unknown");
        
        private final String displayName;
        
        OsType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }

    }

}
