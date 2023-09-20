package de.keksuccino.fancymenu.customization.screen.identifier;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ScreenIdentifierHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ScreenIdentifierHandler.ScreenClasspathDatabase SCREEN_CLASSPATH_DATABASE = new ScreenClasspathDatabase();

    public static boolean isIdentifierOfScreen(@NotNull String screenIdentifier, @NotNull Screen screen) {
        return equalIdentifiers(screenIdentifier, getIdentifierOfScreen(screen));
    }

    /**
     * Prioritizes <b>universal identifiers</b>.<br>
     * Returns the non-universal (classpath) identifier if no universal identifier was found.<br>
     * Checks for {@link CustomGuiBaseScreen}s and returns its Custom GUI identifier if the given screen is a {@link CustomGuiBaseScreen}.
     **/
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
        if (ScreenCustomization.isScreenBlacklisted(screenIdentifier)) return false;
        try {
            Class.forName(screenIdentifier, false, ScreenIdentifierHandler.class.getClassLoader());
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    public static boolean equalIdentifiers(@Nullable String firstScreenIdentifier, @Nullable String secondScreenIdentifier) {
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
        String fixed = SCREEN_CLASSPATH_DATABASE.getValidScreenClasspathFor(potentiallyInvalidScreenIdentifier);
        if (fixed != null) return fixed;
        return potentiallyInvalidScreenIdentifier;
    }

    @NotNull
    public static String tryConvertToNonUniversal(@NotNull String screenIdentifier) {
        if (UniversalScreenIdentifierRegistry.universalIdentifierExists(screenIdentifier)) {
            return Objects.requireNonNull(UniversalScreenIdentifierRegistry.getScreenForUniversalIdentifier(screenIdentifier));
        }
        return tryFixInvalidIdentifierWithNonUniversal(screenIdentifier);
    }

    protected static class ScreenClasspathDatabase {

        protected List<List<String>> classpathGroups = new ArrayList<>();

        public ScreenClasspathDatabase() {
            try {
                PropertyContainerSet set = PropertiesParser.deserializeSetFromFancyString(SCREEN_CLASSPATH_DATABASE_SOURCE);
                if (set != null) {
                    for (PropertyContainer s : set.getContainersOfType("identifier-group")) {
                        List<String> l = new ArrayList<>();
                        for (Map.Entry<String, String> m : s.getProperties().entrySet()) {
                            l.add(m.getValue());
                        }
                        if (!l.isEmpty()) {
                            classpathGroups.add(l);
                        }
                    }
                } else {
                    LOGGER.error("[FANCYMENU] Failed to load screen classpath database source! This could lead to some layouts not loading correctly!");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Nullable
        public String getValidScreenClasspathFor(@NotNull String potentiallyInvalidScreenClasspath) {
            try {
                for (List<String> l : classpathGroups) {
                    if (l.contains(potentiallyInvalidScreenClasspath)) {
                        for (String s : l) {
                            if (ScreenCustomization.isScreenBlacklisted(s)) continue;
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

    private static final String SCREEN_CLASSPATH_DATABASE_SOURCE =
                    """
                    type = screen_classpath_database

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiMainMenu
                      forge_1.16 = net.minecraft.client.gui.screen.MainMenuScreen
                      forge_1.17 = net.minecraft.client.gui.screens.TitleScreen
                      fabric = net.minecraft.class_442
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiWorldSelection
                      forge_1.16 = net.minecraft.client.gui.screen.WorldSelectionScreen
                      forge_1.17 = net.minecraft.client.gui.screens.worldselection.SelectWorldScreen
                      fabric = net.minecraft.class_526
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiCreateWorld
                      forge_1.16 = net.minecraft.client.gui.screen.CreateWorldScreen
                      forge_1.17 = net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
                      fabric = net.minecraft.class_525
                    }

                    identifier-group {
                      forge_1.16 = net.minecraft.client.gui.screen.MultiplayerWarningScreen
                      forge_1.17 = net.minecraft.client.gui.screens.multiplayer.SafetyScreen
                      fabric = net.minecraft.class_4749
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiMultiplayer
                      forge_1.16 = net.minecraft.client.gui.screen.MultiplayerScreen
                      forge_1.17 = net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
                      fabric = net.minecraft.class_500
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiScreenAddServer
                      forge_1.16 = net.minecraft.client.gui.screen.AddServerScreen
                      forge_1.17 = net.minecraft.client.gui.screens.EditServerScreen
                      fabric = net.minecraft.class_422
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiScreenRealmsProxy
                      forge_1.16 = com.mojang.realmsclient.gui.screens.RealmsClientOutdatedScreen
                      forge_1.17 = [placeholder]
                      fabric = net.minecraft.class_4387
                    }

                    identifier-group {
                      forge_1.17 = com.mojang.realmsclient.RealmsMainScreen
                      fabric = net.minecraft.class_4325
                    }

                    identifier-group {
                      forge_1.12 = net.minecraftforge.fml.client.GuiModList
                      forge_1.16 = net.minecraftforge.fml.client.gui.screen.ModListScreen
                      forge_1.17 = net.minecraftforge.fmlclient.gui.screen.ModListScreen
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiOptions
                      forge_1.16 = net.minecraft.client.gui.screen.OptionsScreen
                      forge_1.17 = net.minecraft.client.gui.screens.OptionsScreen
                      fabric = net.minecraft.class_429
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiCustomizeSkin
                      forge_1.16 = net.minecraft.client.gui.screen.CustomizeSkinScreen
                      forge_1.17 = net.minecraft.client.gui.screens.SkinCustomizationScreen
                      fabric = net.minecraft.class_440
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiScreenOptionsSounds
                      forge_1.16 = net.minecraft.client.gui.screen.OptionsSoundsScreen
                      forge_1.17 = net.minecraft.client.gui.screens.SoundOptionsScreen
                      fabric = net.minecraft.class_443
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiVideoSettings
                      forge_1.16 = net.minecraft.client.gui.screen.VideoSettingsScreen
                      forge_1.17 = net.minecraft.client.gui.screens.VideoSettingsScreen
                      fabric = net.minecraft.class_446
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiControls
                      forge_1.16 = net.minecraft.client.gui.screen.ControlsScreen
                      forge_1.17 = net.minecraft.client.gui.screens.controls.ControlsScreen
                      fabric = net.minecraft.class_458
                    }

                    identifier-group {
                      forge_1.16 = net.minecraft.client.gui.screen.MouseSettingsScreen
                      forge_1.17 = net.minecraft.client.gui.screens.MouseSettingsScreen
                      fabric = net.minecraft.class_4288
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiLanguage
                      forge_1.16 = net.minecraft.client.gui.screen.LanguageScreen
                      forge_1.17 = net.minecraft.client.gui.screens.LanguageSelectScreen
                      fabric = net.minecraft.class_426
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.ScreenChatOptions
                      forge_1.16 = net.minecraft.client.gui.screen.ChatOptionsScreen
                      forge_1.17 = net.minecraft.client.gui.screens.ChatOptionsScreen
                      fabric = net.minecraft.class_404
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiScreenResourcePacks
                      forge_1.16 = net.minecraft.client.gui.screen.PackScreen
                      forge_1.17 = net.minecraft.client.gui.screens.packs.PackSelectionScreen
                      fabric = net.minecraft.class_5375
                    }

                    identifier-group {
                      forge_1.16 = net.minecraft.client.gui.AccessibilityScreen
                      forge_1.17 = net.minecraft.client.gui.screens.AccessibilityOptionsScreen
                      fabric = net.minecraft.class_4189
                    }

                    identifier-group {
                      forge_1.12 = [placeholder]
                      forge_1.16 = net.minecraft.client.gui.screen.ConfirmOpenLinkScreen
                      forge_1.17 = net.minecraft.client.gui.screens.ConfirmLinkScreen
                      fabric = net.minecraft.class_407
                    }

                    identifier-group {
                      forge_1.16 = net.minecraft.client.gui.screen.EditGamerulesScreen
                      forge_1.17 = net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen
                      fabric = net.minecraft.class_5235
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.advancements.GuiScreenAdvancements
                      forge_1.16 = net.minecraft.client.gui.advancements.AdvancementsScreen
                      forge_1.17 = net.minecraft.client.gui.screens.advancements.AdvancementsScreen
                      fabric = net.minecraft.class_457
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.achievement.GuiStats
                      forge_1.16 = net.minecraft.client.gui.screen.StatsScreen
                      forge_1.17 = net.minecraft.client.gui.screens.achievement.StatsScreen
                      fabric = net.minecraft.class_447
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.inventory.GuiContainerCreative
                      forge_1.16 = net.minecraft.client.gui.screen.inventory.CreativeScreen
                      forge_1.17 = net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
                      fabric = net.minecraft.class_481
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.inventory.GuiInventory
                      forge_1.16 = net.minecraft.client.gui.screen.inventory.InventoryScreen
                      forge_1.17 = net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
                      fabric = net.minecraft.class_490
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.inventory.GuiCrafting
                      forge_1.16 = net.minecraft.client.gui.screen.inventory.CraftingScreen
                      forge_1.17 = net.minecraft.client.gui.screens.inventory.CraftingScreen
                      fabric = net.minecraft.class_479
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiEnchantment
                      forge_1.16 = net.minecraft.client.gui.screen.EnchantmentScreen
                      forge_1.17 = net.minecraft.client.gui.screens.inventory.EnchantmentScreen
                      fabric = net.minecraft.class_486
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiRepair
                      forge_1.16 = net.minecraft.client.gui.screen.inventory.AnvilScreen
                      forge_1.17 = net.minecraft.client.gui.screens.inventory.AnvilScreen
                      fabric = net.minecraft.class_471
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.inventory.GuiChest
                      forge_1.16 = net.minecraft.client.gui.screen.inventory.ChestScreen
                      forge_1.17 = net.minecraft.client.gui.screens.inventory.ContainerScreen
                      fabric = net.minecraft.class_476
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.inventory.GuiBrewingStand
                      forge_1.16 = net.minecraft.client.gui.screen.inventory.BrewingStandScreen
                      forge_1.17 = net.minecraft.client.gui.screens.inventory.BrewingStandScreen
                      fabric = net.minecraft.class_472
                    }

                    identifier-group {
                      forge_1.16 = net.minecraft.client.gui.screen.inventory.SmithingTableScreen
                      forge_1.17 = net.minecraft.client.gui.screens.inventory.SmithingScreen
                      fabric = net.minecraft.class_4895
                    }

                    identifier-group {
                      forge_1.16 = net.minecraft.client.gui.screen.inventory.BlastFurnaceScreen
                      forge_1.17 = net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen
                      fabric = net.minecraft.class_3871
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.inventory.GuiFurnace
                      forge_1.16 = net.minecraft.client.gui.screen.inventory.FurnaceScreen
                      forge_1.17 = net.minecraft.client.gui.screens.inventory.FurnaceScreen
                      fabric = net.minecraft.class_3873
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiHopper
                      forge_1.16 = net.minecraft.client.gui.screen.HopperScreen
                      forge_1.17 = net.minecraft.client.gui.screens.inventory.HopperScreen
                      fabric = net.minecraft.class_488
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.inventory.GuiDispenser
                      forge_1.16 = net.minecraft.client.gui.screen.inventory.DispenserScreen
                      forge_1.17 = net.minecraft.client.gui.screens.inventory.DispenserScreen
                      fabric = net.minecraft.class_480
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiGameOver
                      forge_1.16 = net.minecraft.client.gui.screen.DeathScreen
                      forge_1.17 = net.minecraft.client.gui.screens.DeathScreen
                      fabric = net.minecraft.class_418
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiYesNo
                      forge_1.16 = net.minecraft.client.gui.screen.ConfirmScreen
                      forge_1.17 = net.minecraft.client.gui.screens.ConfirmScreen
                      fabric = net.minecraft.class_410
                    }

                    identifier-group {
                      forge_1.12 = net.minecraft.client.gui.GuiIngameMenu
                      forge_1.16 = net.minecraft.client.gui.screen.IngameMenuScreen
                      forge_1.17 = net.minecraft.client.gui.screens.PauseScreen
                      fabric = net.minecraft.class_433
                    }

                    identifier-group {
                      forge_1.16 = net.minecraft.client.gui.screen.WorldLoadProgressScreen
                      forge_1.17 = net.minecraft.client.gui.screens.LevelLoadingScreen
                      fabric = net.minecraft.class_3928
                    }

                    identifier-group {
                      forge_1.12 = [placeholder]
                      forge_1.16 = net.minecraft.client.gui.screen.DirtMessageScreen
                      forge_1.17 = net.minecraft.client.gui.screens.GenericDirtMessageScreen
                      fabric = net.minecraft.class_424
                    }

                    identifier-group {
                      forge_1.16 = net.minecraft.client.gui.screen.GamemodeSelectionScreen
                      forge_1.17 = net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen
                      fabric = net.minecraft.class_5289
                    }

                    identifier-group {
                      forge_1.16 = net.minecraft.client.gui.screen.LoomScreen
                      forge_1.18 = net.minecraft.client.gui.screens.inventory.LoomScreen
                      fabric = net.minecraft.class_494
                    }

                    identifier-group {
                      forge_1.18 = net.minecraft.client.gui.screens.controls.KeyBindsScreen
                      fabric = net.minecraft.class_6599
                    }

                    identifier-group {
                      forge_1.18 = net.minecraft.client.gui.screens.MouseSettingsScreen
                      fabric = net.minecraft.class_4288
                    }
                    """;

}
