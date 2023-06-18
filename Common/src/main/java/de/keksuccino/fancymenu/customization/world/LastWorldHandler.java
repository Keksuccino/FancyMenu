package de.keksuccino.fancymenu.customization.world;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertiesSerializer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;

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
            PropertyContainerSet set = PropertiesSerializer.deserializePropertyContainerSet(LAST_WORLD_SAVE_FILE.getPath());
            if (set != null) {
                List<PropertyContainer> secs = set.getSectionsOfType("last_world");
                if (!secs.isEmpty()) {
                    PropertyContainer sec = secs.get(0);
                    String isServerString = sec.getValue("is_server");
                    if ((isServerString != null) && isServerString.equals("true")) {
                        isServer = true;
                    }
                    String worldString = sec.getValue("world");
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
            PropertyContainerSet set = new PropertyContainerSet("last_world");
            PropertyContainer sec = new PropertyContainer("last_world");
            sec.putProperty("is_server", "" + isServer);
            sec.putProperty("world", lastWorld);
            set.putContainer(sec);
            PropertiesSerializer.serializePropertyContainerSet(set, LAST_WORLD_SAVE_FILE.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
