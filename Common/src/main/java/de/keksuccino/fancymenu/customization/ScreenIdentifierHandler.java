package de.keksuccino.fancymenu.customization;

import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScreenIdentifierHandler {

    //TODO "Universal Identifiers" adden (gleicher Identifier für screen, egal welcher Launcher) (nutzt combi aus class und string identifier)

    protected static final ResourceLocation IDENTIFIERS_DATABASE_LOCATION = new ResourceLocation("fancymenu", "menu_identifiers.db");

    @Nullable
    protected static IdentifierDatabase database;

    public static void init() {
        database = new IdentifierDatabase();
    }

    @Nullable
    public static String findValidScreenIdentifierFor(@NotNull String invalidScreenIdentifier) {
        if (database != null) return database.findValidScreenIdentifierFor(invalidScreenIdentifier);
        return null;
    }

    protected static class IdentifierDatabase {

        protected List<List<String>> identifierGroups = new ArrayList<>();

        public IdentifierDatabase() {
            try {
                List<String> dbTextLines = FileUtils.readTextLinesFrom(Minecraft.getInstance().getResourceManager().open(IDENTIFIERS_DATABASE_LOCATION));
                String fancyString = PropertiesParser.buildFancyStringFromList(dbTextLines);
                PropertyContainerSet set = PropertiesParser.deserializeSetFromFancyString(fancyString);
                if (set != null) {
                    for (PropertyContainer s : set.getContainersOfType("identifier-group")) {
                        List<String> l = new ArrayList<>();
                        for (Map.Entry<String, String> m : s.getProperties().entrySet()) {
                            l.add(m.getValue());
                        }
                        if (!l.isEmpty()) {
                            identifierGroups.add(l);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Nullable
        public String findValidScreenIdentifierFor(@NotNull String invalidScreenIdentifier) {
            try {
                for (List<String> l : identifierGroups) {
                    if (l.contains(invalidScreenIdentifier)) {
                        for (String s : l) {
                            try {
                                Class.forName(s, false, ScreenIdentifierHandler.class.getClassLoader());
                                return s;
                            } catch (Exception ignored) {}
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

    }

}
