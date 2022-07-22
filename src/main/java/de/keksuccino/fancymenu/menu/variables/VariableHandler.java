package de.keksuccino.fancymenu.menu.variables;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
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
    }

    //TODO übernehmen
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
            PropertiesSet set = new PropertiesSet("cached_variables");
            PropertiesSection sec = new PropertiesSection("variables");
            for (Map.Entry<String, String> m : variables.entrySet()) {
                sec.addEntry(m.getKey(), m.getValue());
            }
            set.addProperties(sec);
            PropertiesSerializer.writeProperties(set, VARIABLES_FILE.getPath());
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
            PropertiesSet set = PropertiesSerializer.getProperties(VARIABLES_FILE.getPath());
            if (set != null) {
                List<PropertiesSection> secs = set.getPropertiesOfType("variables");
                if (!secs.isEmpty()) {
                    PropertiesSection sec = secs.get(0);
                    for (Map.Entry<String, String> m : sec.getEntries().entrySet()) {
                        variables.put(m.getKey(), m.getValue());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
