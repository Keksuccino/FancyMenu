package de.keksuccino.fancymenu.customization.backend.world;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;

import java.io.File;
import java.util.List;

public class LastWorldHandler {

    public static final File LAST_WORLD_SAVE_FILE = new File(FancyMenu.INSTANCE_DATA_DIR.getPath() + "/last_world.fmdata");

    protected static boolean isServer = false;
    protected static String lastWorld = "";

    public static void init() {

        readFile();

    }

    public static void setLastWorld(String world, boolean isServer) {
        if (world == null) {
            world = "";
        }
        lastWorld = world;
        LastWorldHandler.isServer = isServer;
        writeFile();
    }

    public static String getLastWorld() {
        return lastWorld;
    }

    public static boolean isLastWorldServer() {
        return isServer;
    }

    protected static void readFile() {
        try {
            if (!LAST_WORLD_SAVE_FILE.isFile()) {
                writeFile();
            }
            PropertiesSet set = PropertiesSerializer.getProperties(LAST_WORLD_SAVE_FILE.getPath());
            if (set != null) {
                List<PropertiesSection> secs = set.getPropertiesOfType("last_world");
                if (!secs.isEmpty()) {
                    PropertiesSection sec = secs.get(0);
                    String isServerString = sec.getEntryValue("is_server");
                    if ((isServerString != null) && isServerString.equals("true")) {
                        isServer = true;
                    }
                    String worldString = sec.getEntryValue("world");
                    if (worldString != null) {
                        lastWorld = worldString;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static void writeFile() {
        try {
            if (!LAST_WORLD_SAVE_FILE.isFile()) {
                LAST_WORLD_SAVE_FILE.createNewFile();
            }
            PropertiesSet set = new PropertiesSet("last_world");
            PropertiesSection sec = new PropertiesSection("last_world");
            sec.addEntry("is_server", "" + isServer);
            sec.addEntry("world", lastWorld);
            set.addProperties(sec);
            PropertiesSerializer.writeProperties(set, LAST_WORLD_SAVE_FILE.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
