package de.keksuccino.fancymenu.util.properties;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import de.keksuccino.fancymenu.util.Legacy;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.konkrete.input.StringUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("all")
public class PropertiesParser {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    public static PropertyContainerSet deserializeSetFromFile(@NotNull String filePath) {
        try {
            File f = new File(Objects.requireNonNull(filePath));
            if (f.exists() && f.isFile()) {
                String content = "";
                for (String s : FileUtils.getFileLines(f)) {
                    content += s + "\n";
                }
                return deserializeSetFromFancyString(content);
            } else {
                LOGGER.error("[FANCYMENU] Failed to deserialize PropertyContainerSet! File not found!");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the deserialized {@link PropertyContainerSet} or NULL if the given String was not a valid serialized set.
     */
    @Legacy("This is basically the Konkrete deserializer and is _very_ outdated, so better rewrite this at some point.")
    @Nullable
    public static PropertyContainerSet deserializeSetFromFancyString(@NotNull String serializedFancyString) {
        try {
            String[] lines = StringUtils.splitLines(Objects.requireNonNull(serializedFancyString).replace("\r", "\n"), "\n");
            List<PropertyContainer> data = new ArrayList<>();
            String propertiesType = null;
            PropertyContainer currentContainer = null;
            boolean insideData = false;
            for (String line : lines) {
                //Remove all types of spaces from line
                String compactLine = RegExUtils.replacePattern(line, "[\\p{Z}\\s]+", "");
                //Set type of container set
                if (compactLine.startsWith("type=") && !insideData) {
                    propertiesType = compactLine.split("=", 2)[1];
                    continue;
                }
                //Start new container
                if (compactLine.endsWith("{")) {
                    if (!insideData) {
                        insideData = true;
                    } else {
                        LOGGER.warn("[FANCYMENU] Broken PropertyContainer found! Leaking container, missing '}': " + ((currentContainer != null) ? serializeContainerToFancyString(currentContainer).replace("\n", "").replace("\r", "") : "null"));
                        data.add(currentContainer);
                    }
                    currentContainer = new PropertyContainer(compactLine.split("[{]")[0]);
                    continue;
                }
                //Finish container
                if (compactLine.startsWith("}") && insideData) {
                    data.add(currentContainer);
                    insideData = false;
                    continue;
                }
                //Collect all properties of container
                if (insideData && compactLine.contains("=")) {
                    String value = line.split("=", 2)[1];
                    if (value.startsWith(" ")) {
                        value = value.substring(1);
                    }
                    currentContainer.putProperty(compactLine.split("=", 2)[0], value);
                }
            }
            if (propertiesType != null) {
                PropertyContainerSet set = new PropertyContainerSet(propertiesType);
                for (PropertyContainer d : data) {
                    set.putContainer(d);
                }
                return set;
            } else {
                LOGGER.error("[FANCYMENU] Failed to deserialize PropertyContainerSet! Missing type: " + serializedFancyString.replace("\n", "").replace("\r", ""));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Nullable
    public static PropertyContainerSet deserializeSetFromStream(@NotNull InputStream in) {
        try {
            List<String> dbTextLines = FileUtils.readTextLinesFrom(in);
            String fancyString = PropertiesParser.buildFancyStringFromList(dbTextLines);
            return PropertiesParser.deserializeSetFromFancyString(fancyString);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void serializeSetToFile(@NotNull PropertyContainerSet set, @NotNull String filePath) {
        try {
            File f = new File(filePath);
            File parentDir = f.getParentFile();
            if ((parentDir != null) && !parentDir.isDirectory()) {
                parentDir.mkdirs();
            }
            f.createNewFile();
            FileUtils.writeTextToFile(f, false, serializeSetToFancyString(set));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NotNull
    public static String serializeContainerToFancyString(@NotNull PropertyContainer container) {
        String s = Objects.requireNonNull(container).getType() + " {\n";
        for (Map.Entry<String, String> e : container.getProperties().entrySet()) {
            s += "  " + e.getKey() + " = " + e.getValue() + "\n";
        }
        s += "}";
        return s;
    }

    @NotNull
    public static String serializeSetToFancyString(@NotNull PropertyContainerSet set) {
        String s = "type = " + Objects.requireNonNull(set).getType() + "\n\n";
        for (PropertyContainer c : set.getContainers()) {
            s += serializeContainerToFancyString(c);
            s += "\n\n";
        }
        return s;
    }

    @NotNull
    public static String buildFancyStringFromList(@NotNull List<String> list) {
        String fancy = "";
        for (String s : list) {
            fancy += s + "\n";
        }
        return fancy;
    }

    @NotNull
    public static String stringifyFancyString(@NotNull String fancyString) {
        return Objects.requireNonNull(fancyString)
                .replace("\n", "$prop_line_break$")
                .replace("\r", "$prop_line_break$")
                .replace("{", "$prop_brackets_open$")
                .replace("}", "$prop_brackets_close$");
    }

    @NotNull
    public static String unstringify(@NotNull String stringified) {
        return Objects.requireNonNull(stringified)
                .replace("$prop_line_break$", "\n")
                .replace("$prop_brackets_open$", "{")
                .replace("$prop_brackets_close$", "}");
    }

}
