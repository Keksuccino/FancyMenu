package de.keksuccino.fancymenu.customization.variables;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertiesSerializer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariableHandler {

    protected static final File VARIABLES_FILE = new File(FancyMenu.MOD_DIR.getPath() + "/user_variables.db");

    protected static Map<String, String> variables = new HashMap<>();

    public static void init() {

        readFromFile();

        String variablesToResetOnLaunch = FancyMenu.getConfig().getOrDefault("variables_to_reset_on_launch", "").replace(" ", "");
        if (variablesToResetOnLaunch.contains(",")) {
            for (String s : variablesToResetOnLaunch.split(",")) {
                if (s.length() > 0) {
                    removeVariable(s);
                }
            }
        }

    }

    public static List<String> getVariableNames() {
        List<String> l = new ArrayList<>();
        l.addAll(variables.keySet());
        return l;
    }

    @Nullable
    public static String getVariable(String name) {
        return variables.get(name);
    }

    public static void setVariable(String name, String value) {
        variables.put(name, value);
        writeToFile();
    }

    public static void removeVariable(String name) {
        variables.remove(name);
        writeToFile();
    }

    public static void clearVariables() {
        variables.clear();
        writeToFile();
    }

    protected static void writeToFile() {
        try {
            if (!VARIABLES_FILE.exists()) {
                VARIABLES_FILE.createNewFile();
            }
            PropertyContainerSet set = new PropertyContainerSet("cached_variables");
            PropertyContainer sec = new PropertyContainer("variables");
            for (Map.Entry<String, String> m : variables.entrySet()) {
                sec.putProperty(m.getKey(), m.getValue());
            }
            set.putContainer(sec);
            PropertiesSerializer.serializePropertyContainerSet(set, VARIABLES_FILE.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static void readFromFile() {
        try {
            if (!VARIABLES_FILE.exists()) {
                writeToFile();
            }
            variables.clear();
            PropertyContainerSet set = PropertiesSerializer.deserializePropertyContainerSet(VARIABLES_FILE.getPath());
            if (set != null) {
                List<PropertyContainer> secs = set.getSectionsOfType("variables");
                if (!secs.isEmpty()) {
                    PropertyContainer sec = secs.get(0);
                    for (Map.Entry<String, String> m : sec.getProperties().entrySet()) {
                        variables.put(m.getKey(), m.getValue());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
