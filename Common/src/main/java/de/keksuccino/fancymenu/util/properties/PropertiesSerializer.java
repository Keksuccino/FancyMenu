package de.keksuccino.fancymenu.util.properties;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.keksuccino.fancymenu.util.Legacy;
import de.keksuccino.konkrete.file.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

@Legacy("This is basically the Konkrete serializer class and is _very_ outdated, so better rewrite this at some point.")
public class PropertiesSerializer {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Returns a new {@link PropertyContainer} instance or NULL if the given file wasn't a valid properties file.
     */
    @Nullable
    public static PropertyContainerSet deserializePropertyContainerSet(String filePath) {
        try {
            File f = new File(filePath);
            if (f.exists() && f.isFile()) {
                List<String> lines = FileUtils.getFileLines(f);
                List<PropertyContainer> data = new ArrayList<>();
                String propertiesType = null;
                PropertyContainer currentData = null;
                boolean insideData = false;
                for (String s : lines) {
                    String comp = s.replace(" ", "");
                    //Set type of container set
                    if (comp.startsWith("type=") && !insideData) {
                        propertiesType = comp.split("=", 2)[1];
                        continue;
                    }
                    //Start new container
                    if (comp.endsWith("{")) {
                        if (!insideData) {
                            insideData = true;
                        } else {
                            LOGGER.warn("[FANCYMENU] Invalid PropertyContainer found in '" + filePath + "'! (Leaking container; Missing '}')");
                            data.add(currentData);
                        }
                        currentData = new PropertyContainer(comp.split("[{]")[0]);
                        continue;
                    }
                    //Finish container
                    if (comp.startsWith("}") && insideData) {
                        data.add(currentData);
                        insideData = false;
                        continue;
                    }
                    //Collect all properties of container
                    if (insideData && comp.contains("=")) {
                        String value = s.split("=", 2)[1];
                        if (value.startsWith(" ")) {
                            value = value.substring(1);
                        }
                        currentData.putProperty(comp.split("=", 2)[0], value);
                    }
                }
                if (propertiesType != null) {
                    PropertyContainerSet set = new PropertyContainerSet(propertiesType);
                    for (PropertyContainer d : data) {
                        set.putContainer(d);
                    }
                    return set;
                } else {
                    LOGGER.error("[FANCYMENU] Failed to deserialize PropertyContainerSet! Invalid properties file found: " + filePath + " (Missing type)");
                }
            } else {
                LOGGER.error("[FANCYMENU] Failed to deserialize PropertyContainerSet! File not found!");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void serializePropertyContainerSet(PropertyContainerSet set, String path) {
        try {
            List<PropertyContainer> l = set.getContainers();
            File f = new File(path);
            //Check if path has valid name
            if (f.getName().contains(".") && !f.getName().startsWith(".")) {
                File parent = f.getParentFile();
                if ((parent != null) && parent.isDirectory() && !parent.exists()) {
                    parent.mkdirs();
                }
                f.createNewFile();
                String data = "";
                data += "type = " + set.getType() + "\n\n";
                for (PropertyContainer ps : l) {
                    data += ps.getType() + " {\n";
                    for (Map.Entry<String, String> e : ps.getProperties().entrySet()) {
                        data += "  " + e.getKey() + " = " + e.getValue() + "\n";
                    }
                    data += "}\n\n";
                }
                FileUtils.writeTextToFile(f, false, data);
            } else {
                LOGGER.error("[FANCYMENU] Failed to serialize PropertyContainerSet! Invalid file name: " + path + " (File name should look like 'some_name.extension')");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
