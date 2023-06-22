package de.keksuccino.fancymenu.customization.variables;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.Legacy;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertiesSerializer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class VariableHandler {

    protected static final File VARIABLES_FILE = new File(FancyMenu.MOD_DIR.getPath() + "/user_variables.db");

    protected static final Map<String, Variable> VARIABLES = new HashMap<>();

    public static void init() {

        readFromFile();

        //Reset variables that have "Reset on Launch" enabled
        for (Variable v : getVariables()) {
            if (v.resetOnLaunch) v.value = "";
        }

    }

    public static void setVariable(@NotNull String name, @Nullable String value) {
        Variable v = getVariable(name);
        if (v == null) {
            v = new Variable(name);
            VARIABLES.put(name, v);
        }
        v.value = value;
        writeToFile();
    }

    public static void removeVariable(String name) {
        VARIABLES.remove(name);
        writeToFile();
    }

    @Nullable
    public static Variable getVariable(String name) {
        return VARIABLES.get(name);
    }

    @NotNull
    public static List<Variable> getVariables() {
        return new ArrayList<>(VARIABLES.values());
    }

    @NotNull
    public static List<String> getVariableNames() {
        return new ArrayList<>(VARIABLES.keySet());
    }

    public static void clearVariables() {
        VARIABLES.clear();
        writeToFile();
    }

    public static boolean variableExists(@NotNull String name) {
        return getVariable(name) != null;
    }

    protected static void writeToFile() {
        try {
            if (!VARIABLES_FILE.exists()) {
                VARIABLES_FILE.createNewFile();
            }
            PropertyContainerSet set = new PropertyContainerSet("user_variables");
            for (Variable v : VARIABLES.values()) {
                set.putContainer(v.serialize());
            }
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
            VARIABLES.clear();
            PropertyContainerSet set = PropertiesSerializer.deserializePropertyContainerSet(VARIABLES_FILE.getPath());
            if (set != null) {
                if (set.getType().equals("cached_variables")) {
                    readFromLegacyFile();
                } else {
                    List<PropertyContainer> secs = set.getContainersOfType("variable");
                    for (PropertyContainer c : secs) {
                        Variable v = Variable.deserialize(c);
                        if (v != null) {
                            VARIABLES.put(v.name, v);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Legacy("This reads variables from v2 variable files. Remove this in the future.")
    protected static void readFromLegacyFile() {
        try {
            if (!VARIABLES_FILE.exists()) {
                writeToFile();
            }
            VARIABLES.clear();
            PropertyContainerSet set = PropertiesSerializer.deserializePropertyContainerSet(VARIABLES_FILE.getPath());
            if (set != null) {
                List<PropertyContainer> secs = set.getContainersOfType("variables");
                if (!secs.isEmpty()) {
                    PropertyContainer sec = secs.get(0);
                    for (Map.Entry<String, String> m : sec.getProperties().entrySet()) {
                        Variable v = new Variable(m.getKey());
                        v.value = m.getValue();
                        VARIABLES.put(m.getKey(), v);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
