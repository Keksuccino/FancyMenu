package de.keksuccino.fancymenu.customization.screenidentifiers;

import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScreenIdentifierHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static final ResourceLocation IDENTIFIERS_DATABASE_LOCATION = new ResourceLocation("fancymenu", "menu_identifiers.db");

    @Nullable
    protected static IdentifierDatabase database;

    public static void init() {
        database = new IdentifierDatabase();
    }

    public static boolean isIdentifierOfScreen(@NotNull String screenIdentifier, @NotNull Screen screen) {
        if ((screen instanceof CustomGuiBaseScreen c) && c.getIdentifier().equals(screenIdentifier)) return true;
        if (screen instanceof CustomGuiBaseScreen) return false;
        if (screen.getClass().getName().equals(screenIdentifier)) return true;
        if (screenIdentifier.equals(UniversalScreenIdentifierRegistry.getUniversalIdentifierFor(screen))) return true;
        if (screen.getClass().getName().equals(tryFixInvalidIdentifierWithNonUniversal(screenIdentifier))) return true;
        return false;
    }

    @NotNull
    public static String getIdentifierOfScreen(@NotNull Screen screen) {
        if (screen instanceof CustomGuiBaseScreen c) return c.getIdentifier();
        String universal = UniversalScreenIdentifierRegistry.getUniversalIdentifierFor(screen);
        if (universal != null) return universal;
        return screen.getClass().getName();
    }

    public static boolean isValidIdentifier(@NotNull String screenIdentifier) {
        if (CustomGuiHandler.guiExists(screenIdentifier)) return true;
        if (UniversalScreenIdentifierRegistry.universalIdentifierExists(screenIdentifier)) return true;
        try {
            Class.forName(screenIdentifier, false, ScreenIdentifierHandler.class.getClassLoader());
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    public static boolean identifiersEqual(@Nullable String firstScreenIdentifier, @Nullable String secondScreenIdentifier) {
        if ((firstScreenIdentifier == null) || (secondScreenIdentifier == null)) return false;
        if (firstScreenIdentifier.equals(secondScreenIdentifier)) return true;
        return getBestIdentifier(firstScreenIdentifier).equals(getBestIdentifier(secondScreenIdentifier));
    }

    /**
     * Prioritizes <b>universal identifiers</b> and returns the universal identifier if one was found for the given identifier.<br>
     * Tries to fix the given identifier if it's invalid and returns the fixed identifier if fixing was successful.<br>
     * Returns the given identifier without any changes if no universal identifier was found and there was no fix needed or it couldn't be fixed.
     **/
    @NotNull
    public static String getBestIdentifier(@NotNull String screenIdentifier) {
        if (CustomGuiHandler.guiExists(screenIdentifier)) return screenIdentifier;
        screenIdentifier = tryFixInvalidIdentifierWithNonUniversal(screenIdentifier);
        String universal = UniversalScreenIdentifierRegistry.getUniversalIdentifierFor(screenIdentifier);
        return (universal != null) ? universal : screenIdentifier;
    }

    /**
     * Tries to fix invalid identifiers.<br>
     * Will NOT return <b>universal identifiers</b> EXCEPT the given identifier is a universal identifier.<br>
     * Returns the given identifier without any changes if there was no fix needed or it was not possible to fix it.
     **/
    @NotNull
    public static String tryFixInvalidIdentifierWithNonUniversal(@NotNull String potentiallyInvalidScreenIdentifier) {
        if (isValidIdentifier(potentiallyInvalidScreenIdentifier)) return potentiallyInvalidScreenIdentifier;
        if (database != null) {
            String fixed = database.getValidScreenIdentifierFor(potentiallyInvalidScreenIdentifier);
            if (fixed != null) return fixed;
        }
        return potentiallyInvalidScreenIdentifier;
    }

    protected static class IdentifierDatabase {

        protected List<List<String>> identifierGroups = new ArrayList<>();

        public IdentifierDatabase() {
            try {
                PropertyContainerSet set = PropertiesParser.deserializeSetFromStream(Minecraft.getInstance().getResourceManager().open(IDENTIFIERS_DATABASE_LOCATION));
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
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Nullable
        public String getValidScreenIdentifierFor(@NotNull String screenIdentifier) {
            try {
                for (List<String> l : identifierGroups) {
                    if (l.contains(screenIdentifier)) {
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
