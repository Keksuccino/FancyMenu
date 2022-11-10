package de.keksuccino.fancymenu.menu.fancy.helper;

import com.google.common.io.Files;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.animation.ResourcePackAnimationRenderer;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomizationProperties;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMYesNoPopup;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.commons.io.FileUtils;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class SetupSharingEngine {

    //--- 4
    public static final File MENU_IDENTIFIERS_DATABASE_FILE = new File(Minecraft.getInstance().gameDirectory, "config/fancymenu/menu_identifiers.db");
    public static final File FM_SETUPS_DIR = new File(Minecraft.getInstance().gameDirectory, "fancymenu_setups/exported_setups");
    public static final File SETUP_BACKUP_DIR = new File(Minecraft.getInstance().gameDirectory, "fancymenu_setups/.backups");
    //---------------------

    protected static MenuIdentifierDatabase menuIdentifierDatabase = null;

    public static void init() {
        try {

            FM_SETUPS_DIR.mkdirs();
            SETUP_BACKUP_DIR.mkdirs();

            ResourceLocation rl = new ResourceLocation("fancymenu", "menu_identifiers.db");
            InputStream in = Minecraft.getInstance().getResourceManager().open(rl);
            if (in != null) {
                FileUtils.copyInputStreamToFile(in, MENU_IDENTIFIERS_DATABASE_FILE);
            }
            if (MENU_IDENTIFIERS_DATABASE_FILE.isFile()) {
                menuIdentifierDatabase = new MenuIdentifierDatabase(MENU_IDENTIFIERS_DATABASE_FILE);
            }

            MenuCustomizationProperties.loadProperties();
            MenuCustomization.updateCustomizeableMenuCache();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void importSetup() {
        try {
            FMYesNoPopup importConfirmPop = new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
                if (call) {
                    FMTextInputPopup pop = new FMTextInputPopup(new Color(0, 0, 0, 0), Locals.localize("fancymenu.helper.setupsharing.import.enterpath"), null, 240, (call2) -> {
                        importSetupWithPathRaw(call2);
                    });
                    PopupHandler.displayPopup(pop);
                }
            }, Locals.localize("fancymenu.helper.setupsharing.import.confirm"));
            PopupHandler.displayPopup(importConfirmPop);
        } catch (Exception e) {
            e.printStackTrace();
            displayImportErrorPopup();
        }
    }

    public static void importSetupFromSavedSetups() {
        try {
            FMYesNoPopup importConfirmPop = new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
                if (call) {
                    ChooseSavedSetupPopup pop = new ChooseSavedSetupPopup((call2) -> {
                        if (call2 != null) {
                            importSetupWithPathRaw(call2.getPath());
                        }
                    });
                    PopupHandler.displayPopup(pop);
                }
            }, Locals.localize("fancymenu.helper.setupsharing.import.confirm"));
            PopupHandler.displayPopup(importConfirmPop);
        } catch (Exception e) {
            e.printStackTrace();
            displayImportErrorPopup();
        }
    }

    protected static void importSetupWithPathRaw(String setupPath) {
        if (setupPath != null) {
            setupPath = CustomizationItemBase.fixBackslashPath(setupPath);
            try {
                File appData = getFancyMenuAppData();
                if (appData != null) {
                    File setup = new File(setupPath);
                    if (isValidSetup(setup.getPath())) {

                        File temp = new File(appData.getPath() + "/temp/fancymenu_setup_temp_" + UUID.randomUUID());
                        temp.mkdirs();

                        copyTempImportPath(setup, temp, () -> {
                            backupCurrentSetup(() -> {
                                try {

                                    SetupImporter i = new SetupImporter(temp.getPath(), (onFinished) -> {
                                        if (onFinished.wasImportSuccessful()) {
                                            FMNotificationPopup pop2 = new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, () -> {
                                                CustomizationHelper.reloadSystemAndMenu();
                                            }, Locals.localize("fancymenu.helper.setupsharing.import.success"));
                                            PopupHandler.displayPopup(pop2);
                                        } else if (!onFinished.wasCanceledByUser()) {
                                            displayImportErrorPopup();
                                        }
                                        try {
                                            FileUtils.deleteDirectory(temp);
                                        } catch (Exception e3) {
                                            e3.printStackTrace();
                                        }
                                    });
                                    i.startImport();
                                } catch (Exception e2) {
                                    e2.printStackTrace();
                                    displayImportErrorPopup();
                                }
                            });
                        });

                    } else {
                        FMNotificationPopup pop2 = new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("fancymenu.helper.setupsharing.import.invalidsetup"));
                        PopupHandler.displayPopup(pop2);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                displayImportErrorPopup();
            }
        }
    }

    protected static void displayImportErrorPopup() {
        FMNotificationPopup pop = new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, () -> {
            CustomizationHelper.reloadSystemAndMenu();
        }, Locals.localize("fancymenu.helper.setupsharing.import.error"));
        PopupHandler.displayPopup(pop);
    }

    public static void exportSetup() {
        try {
            FMYesNoPopup exportConfirmPopup = new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
                if (call) {
                    String setupNameDefault = "exported_fm_setup_" + getTimestamp();
                    FMTextInputPopup pop = new FMTextInputPopup(new Color(0, 0, 0, 0), Locals.localize("fancymenu.helper.setupsharing.export.entername"), null, 240, (call2) -> {
                        if (call2 != null) {
                            new Thread(() -> {
                                String setupName = call2;
                                if (setupName.replace(" ", "").equals("")) {
                                    setupName = setupNameDefault;
                                }
                                StatusPopup exportBlockerPopup = new StatusPopup(Locals.localize("fancymenu.helper.setupsharing.import.exportingsetup"));
                                PopupHandler.displayPopup(exportBlockerPopup);
                                try {
                                    FM_SETUPS_DIR.mkdirs();
                                    List<String> unableToExportList = exportSetupRaw(FM_SETUPS_DIR.getPath(), setupName);
                                    if (unableToExportList != null) {
                                        if (unableToExportList.isEmpty()) {
                                            displayExportSuccessPopup(FM_SETUPS_DIR);
                                        } else {
                                            displayFailedToExportSomeElementsPopup(unableToExportList, FM_SETUPS_DIR);
                                        }
                                    } else {
                                        displayExportErrorPopup();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    displayExportErrorPopup();
                                }
                                exportBlockerPopup.setDisplayed(false);
                            }).start();
                        }
                    });
                    pop.setText(setupNameDefault);
                    PopupHandler.displayPopup(pop);
                }
            }, Locals.localize("fancymenu.helper.setupsharing.export.confirm"));
            PopupHandler.displayPopup(exportConfirmPopup);
        } catch (Exception e) {
            e.printStackTrace();
            displayExportErrorPopup();
        }
    }

    protected static void copyTempImportPath(File setup, File temp, Runnable onFinish) {
        new Thread(() -> {
            try {
                StatusPopup copyBlockerPopup = new StatusPopup(Locals.localize("fancymenu.helper.setupsharing.import.preparing"));
                PopupHandler.displayPopup(copyBlockerPopup);
                FileUtils.copyDirectory(setup, temp);
                copyBlockerPopup.setDisplayed(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            onFinish.run();
        }).start();
    }

    protected static void backupCurrentSetup(Runnable onFinish) {
        new Thread(() -> {
            try {
                StatusPopup backupBlockerPopup = new StatusPopup(Locals.localize("fancymenu.helper.setupsharing.restore.backingup"));
                PopupHandler.displayPopup(backupBlockerPopup);
                SETUP_BACKUP_DIR.mkdirs();
                deleteOldBackups();
                exportSetupRaw(SETUP_BACKUP_DIR.getPath(), "fm_setup_backup_" + getTimestamp());
                backupBlockerPopup.setDisplayed(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            onFinish.run();
        }).start();
    }

    protected static void deleteOldBackups() {
        try {
            File[] backups = SETUP_BACKUP_DIR.listFiles();
            sortByDateModified(backups);
            List<File> backupsList = new ArrayList<>();
            for (File f : backups) {
                if (isValidSetup(f.getPath())) {
                    backupsList.add(f);
                }
            }
            List<File> delete = new ArrayList<>();
            int maxBackups = 5;
            int backupsCount = backupsList.size();
            int i = 0;
            while (backupsCount > maxBackups-1) {
                delete.add(backupsList.get(i));
                i++;
                backupsCount--;
            }
            for (File f : delete) {
                FancyMenu.LOGGER.info("[FANCYMENU] Deleting old setup backup: " + f.getPath());
                FileUtils.deleteDirectory(f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Sorts from OLDEST to NEWEST. The oldest file is the first in the array. **/
    protected static File[] sortByDateModified(File[] files) {
        try {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }

    protected static boolean generateUnableToExportLog(List<String> list, File exportTo) {
        try {
            List<String> l = new ArrayList<>();
            l.add("UNABLE TO EXPORT SOME SETUP ELEMENTS:");
            l.add("");
            l.addAll(list);
            File exportFailsFile = new File(exportTo.getPath() + "/export_fail_log_" + getTimestamp() + ".txt");
            exportFailsFile.createNewFile();
            de.keksuccino.konkrete.file.FileUtils.writeTextToFile(exportFailsFile, false, l.toArray(new String[0]));
            CustomizationHelper.openFile(exportFailsFile);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    protected static void displayExportErrorPopup() {
        FMNotificationPopup pop = new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("fancymenu.helper.setupsharing.export.error"));
        PopupHandler.displayPopup(pop);
    }

    protected static void displayExportSuccessPopup(File exportTo) {
        FMYesNoPopup pop = new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call2) -> {
            try {
                if (call2) {
                    CustomizationHelper.openFile(exportTo);
                }
            } catch (Exception e) {
                e.printStackTrace();
                displayExportErrorPopup();
            }
        }, Locals.localize("fancymenu.helper.setupsharing.export.success"));
        PopupHandler.displayPopup(pop);
    }

    protected static void displayFailedToExportSomeElementsPopup(List<String> unableToExportList, File exportTo) {
        FMYesNoPopup pop = new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call2) -> {
            if (call2) {
                if (!generateUnableToExportLog(unableToExportList, exportTo)) {
                    displayExportErrorPopup();
                }
            }
            displayExportSuccessPopup(exportTo);
        }, Locals.localize("fancymenu.helper.setupsharing.export.unabletoexport", "" + unableToExportList.size()));
        PopupHandler.displayPopup(pop);
    }

    public static List<String> exportSetupRaw(String baseDir, String setupName) {

        List<String> unableToExport = new ArrayList<>();

        try {

            if (baseDir == null) {
                return null;
            }
            if (setupName == null) {
                return null;
            }

            //--- 4
            File exportToTemp = new File(Minecraft.getInstance().gameDirectory, "fm_export_temp_folder_" + UUID.randomUUID());
            File exportToTempSetup = new File(exportToTemp.getPath() + "/setup");
            exportToTempSetup.mkdirs();

            List<PropertiesSet> layouts = new ArrayList<>();
            layouts.addAll(MenuCustomizationProperties.getProperties());
            layouts.addAll(MenuCustomizationProperties.getDisabledProperties());

            //Export all layout resources
            List<String> exportedResources = new ArrayList<>();
            List<String> failedToExportResources = new ArrayList<>();
            for (PropertiesSet l : layouts) {
                for (String s : getLayoutResources(l)) {
                    try {
                        if (!exportedResources.contains(s) && !failedToExportResources.contains(s)) {
                            File oriFile = new File(s);
                            if (oriFile.isFile()) {
                                File target = new File(exportToTempSetup.getAbsolutePath() + "/" + getShortPath(oriFile.getPath()));
                                File targetParent = target.getParentFile();
                                if (targetParent != null) {
                                    targetParent.mkdirs();
                                }
                                FileUtils.copyFile(oriFile, target);
                                exportedResources.add(s);
                            } else if (oriFile.isDirectory()) {
                                String targetDir = exportToTempSetup.getAbsolutePath() + "/" + getShortPath(oriFile.getPath());
                                File target = new File(targetDir);
                                target.mkdirs();
                                FileUtils.copyDirectory(oriFile, target);
                                exportedResources.add(s);
                            }
                            if (!exportedResources.contains(s)) {
                                failedToExportResources.add(s);
                            }
                        }
                    } catch (Exception e2) {
                        if (!failedToExportResources.contains(s)) {
                            failedToExportResources.add(s);
                        }
                        e2.printStackTrace();
                    }
                }
            }
            for (String s : failedToExportResources) {
                unableToExport.add("RESOURCE: " + s);
            }

            //Export all active animation resources (frames for different pack types)
            List<String> exportedAnimations = new ArrayList<>();
            List<String> failedToExportAnimations = new ArrayList<>();
            for (AdvancedAnimationMeta m : getAnimationMetas()) {
                try {
                    if (m.propertiesPath != null) {
                        if (!exportedAnimations.contains(m.name) && !failedToExportAnimations.contains(m.name)) {
                            if (m.type == AnimationType.LEGACY) {
                                File aniPath = new File(m.propertiesPath);
                                if (aniPath.isDirectory()) {
                                    exportedAnimations.add(m.name);
                                }
                            }
                            if (m.type == AnimationType.PACK) {
                                File propsPath = new File(m.propertiesPath);
                                File resPath = new File(m.resourcesPath);
                                File packMetaPath = null;
                                try {
                                    packMetaPath = resPath.getAbsoluteFile().getParentFile();
                                    if (packMetaPath != null) {
                                        packMetaPath = packMetaPath.getParentFile();
                                    }
                                    if (packMetaPath != null) {
                                        packMetaPath = new File(packMetaPath.getPath() + "/pack.mcmeta");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (propsPath.isDirectory()) {
                                    if (resPath.isDirectory()) {
                                        File resTarget = new File(exportToTempSetup.getAbsolutePath() + "/" + getShortPath(resPath.getPath()));
                                        resTarget.mkdirs();
                                        FileUtils.copyDirectory(resPath, resTarget);
                                        if ((packMetaPath != null) && packMetaPath.isFile()) {
                                            File packMetaTarget = new File(exportToTempSetup.getAbsolutePath() + "/" + getShortPath(packMetaPath.getPath()));
                                            FileUtils.copyFile(packMetaPath, packMetaTarget);
                                            exportedAnimations.add(m.name);
                                        }
                                    }
                                }
                            }
                            if (m.type == AnimationType.LMR) {
                                File propsPath = new File(m.propertiesPath);
                                File resPath = new File(m.resourcesPath);
                                if (propsPath.isDirectory()) {
                                    if (resPath.isDirectory()) {
                                        File resTarget = new File(exportToTempSetup.getAbsolutePath() + "/" + getShortPath(resPath.getPath()));
                                        resTarget.mkdirs();
                                        FileUtils.copyDirectory(resPath, resTarget);
                                        exportedAnimations.add(m.name);
                                    }
                                }
                            }
                            if (!exportedAnimations.contains(m.name)) {
                                if (!failedToExportAnimations.contains(m.name)) {
                                    failedToExportAnimations.add(m.name);
                                }
                            }
                        }
                    } else {
                        if (!failedToExportAnimations.contains(m.name)) {
                            failedToExportAnimations.add(m.name);
                        }
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                    if (!failedToExportAnimations.contains(m.name)) {
                        failedToExportAnimations.add(m.name);
                    }
                }
            }
            for (String s : failedToExportAnimations) {
                unableToExport.add("ANIMATION: " + s);
            }

            //Export custom splash text file from mod config
            try {
                String splashPath = FancyMenu.config.getOrDefault("splashtextfile", "");
                if (splashPath.toLowerCase().endsWith(".txt")) {
                    File splashFile = new File(splashPath);
                    if (splashFile.isFile()) {
                        File parent = splashFile.getParentFile();
                        if (parent != null) {
                            File targetParent = new File(exportToTempSetup.getAbsolutePath() + "/" + getShortPath(parent.getPath()));
                            targetParent.mkdirs();
                        }
                        File copyTo = new File(exportToTempSetup.getAbsolutePath() + "/" + getShortPath(splashFile.getPath()));
                        FileUtils.copyFile(splashFile, copyTo);
                    } else {
                        unableToExport.add("CONFIG: CUSTOM VANILLA SPLASH FILE: " + splashPath);
                    }
                }
            } catch (Exception e2) {
                unableToExport.add("CONFIG: CUSTOM VANILLA SPLASH FILE: " + FancyMenu.config.getOrDefault("splashtextfile", ""));
                e2.printStackTrace();
            }

            //Export setup (config/fancymenu)
            try {
                //--- 2
                File fmDir = FancyMenu.MOD_DIR;
                File target = new File(exportToTempSetup.getAbsolutePath() + "/config/fancymenu");
                target.mkdirs();
                FileUtils.copyDirectory(fmDir, target);
            } catch (Exception e2) {
                e2.printStackTrace();
                return null;
            }

            //Generate setup.properties in setup root
            writeSetupPropertiesFile(exportToTemp.getAbsolutePath() + "/setup.properties");

            //Copy exported setup from temp dir to target + delete temp dir
            try {
                String uniqueSetupName = getUniqueSetupName(baseDir, setupName);
                File exportTo = new File(baseDir + "/" + uniqueSetupName);
                exportTo.mkdirs();
                FileUtils.copyDirectory(exportToTemp, exportTo);
                FileUtils.deleteDirectory(exportToTemp);
            } catch (Exception e2) {
                e2.printStackTrace();
                return null;
            }

            return unableToExport;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    protected static String getUniqueSetupName(String baseDir, String name) {
        String s = name;
        try {
            File f = new File(baseDir + "/" + name);
            int i = 2;
            while (f.isDirectory()) {
                s = name + "_" + i;
                i++;
                f = new File(baseDir + "/" + s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    public static boolean isValidSetup(String pathToSetup) {
        try {
            File setupRoot = new File(pathToSetup);
            if (setupRoot.isDirectory()) {
                File instance = new File(setupRoot.getPath() + "/setup");
                File props = new File(setupRoot.getPath() + "/setup.properties");
                return (instance.isDirectory() && props.isFile());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    protected static void writeSetupPropertiesFile(String saveToPathWithFileName) {
        try {

            PropertiesSet set = new PropertiesSet("fancymenu_setup");

            PropertiesSection meta = new PropertiesSection("setup-meta");
            meta.addEntry("modloader", FancyMenu.MOD_LOADER);
            meta.addEntry("mcversion", SharedConstants.getCurrentVersion().getName());
            meta.addEntry("fmversion", FancyMenu.VERSION);
            set.addProperties(meta);

            PropertiesSection mods = new PropertiesSection("mod-list");
            int i = 1;
            for (IModInfo info : ModList.get().getMods()) {
                String id = info.getModId();
                if (!id.equals("fancymenu") && !id.equals("konkrete") && !id.equals("loadmyresources") && !id.equals("forge") && !id.equals("mcp") && !id.equals("minecraft") && !id.equals("fml")) {
                    mods.addEntry("" + i, id);
                }
            }
            set.addProperties(mods);

            PropertiesSerializer.writeProperties(set, saveToPathWithFileName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SetupProperties deserializePropertiesFile(String propsFilePath) {

        SetupProperties sp = new SetupProperties();

        try {

            PropertiesSet set = PropertiesSerializer.getProperties(propsFilePath);
            if (set != null) {
                List<PropertiesSection> metas = set.getPropertiesOfType("setup-meta");
                if (!metas.isEmpty()) {
                    PropertiesSection meta = metas.get(0);
                    sp.modLoader = meta.getEntryValue("modloader");
                    sp.mcVersion = meta.getEntryValue("mcversion");
                    sp.fmVersion = meta.getEntryValue("fmversion");
                }
                List<PropertiesSection> modLists = set.getPropertiesOfType("mod-list");
                if (!modLists.isEmpty()) {
                    PropertiesSection modList = modLists.get(0);
                    for (Map.Entry<String, String> m : modList.getEntries().entrySet()) {
                        sp.modList.add(m.getValue());
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return sp;

    }

    protected static List<String> getLayoutResources(PropertiesSet layout) {
        List<String> l = new ArrayList<>();
        for (PropertiesSection s : layout.getPropertiesOfType("customization")) {
            String action = s.getEntryValue("action");
            if (action != null) {
                for (Map.Entry<String, String> m : s.getEntries().entrySet()) {
                    //Skip values of button actions
                    if (action.equalsIgnoreCase("addbutton") && (m.getKey() != null) && m.getKey().equals("value")) {
                        continue;
                    }
                    String shortPath = CustomizationItemBase.fixBackslashPath(getShortPath(CustomizationItemBase.fixBackslashPath(m.getValue())));
                    if (shortPath != null) {
                        File f = new File(shortPath);
                        if ((!m.getValue().replace(" ", "").equals("")) && f.exists()) {
                            if (!shortPath.endsWith("config/fancymenu") && !shortPath.endsWith("config/fancymenu/")) {
                                l.add(shortPath);
                            }
                        }
                    }
                }
            }
        }
        return l;
    }

    public static String getShortPath(String path) {
        if (path == null) {
            return null;
        }
        path = CustomizationItemBase.fixBackslashPath(path);
        File pathRaw = new File(path);
        if (!pathRaw.exists()) {
            return null;
        }
        //--- 2
        File home = Minecraft.getInstance().gameDirectory;
        if (pathRaw.getAbsolutePath().startsWith(home.getAbsolutePath())) {
            path = pathRaw.getAbsolutePath().replace(home.getAbsolutePath(), "");
            if (path.startsWith("\\") || path.startsWith("/")) {
                path = path.substring(1);
            }
        }
        return CustomizationItemBase.fixBackslashPath(path);
    }

    protected static List<AdvancedAnimationMeta> getAnimationMetas() {
        List<AdvancedAnimationMeta> l = new ArrayList<>();
        for (String s : AnimationHandler.getCustomAnimationNames()) {
            AdvancedAnimationMeta meta = new AdvancedAnimationMeta();
            meta.name = s;
            if (AnimationHandler.animationExists(s)) {
                AdvancedAnimationMeta tempMeta = getAdvancedAnimationMeta(s);
                if (tempMeta != null) {
                    meta.type = tempMeta.type;
                    meta.propertiesPath = tempMeta.propertiesPath;
                    meta.resourcesPath = tempMeta.resourcesPath;
                }
            }
            l.add(meta);
        }
        return l;
    }

    public static AdvancedAnimationMeta getAdvancedAnimationMeta(String advancedAnimationName) {
        if (AnimationHandler.animationExists(advancedAnimationName)) {
            IAnimationRenderer a = AnimationHandler.getAnimation(advancedAnimationName);
            AdvancedAnimationMeta meta = new AdvancedAnimationMeta();
            meta.name = advancedAnimationName;
            if (a instanceof AdvancedAnimation) {
                meta.propertiesPath = ((AdvancedAnimation) a).propertiesPath;
                IAnimationRenderer mainAni = ((AdvancedAnimation) a).getMainAnimationRenderer();
                if (mainAni == null) {
                    return null;
                }
                if (mainAni instanceof ResourcePackAnimationRenderer) {
                    String namespace = ((ResourcePackAnimationRenderer) mainAni).getPath();
                    //--- 2
                    File lmrAni = new File(Minecraft.getInstance().gameDirectory, "resources/" + namespace);
                    if (lmrAni.isDirectory()) {
                        meta.type = AnimationType.LMR;
                        meta.resourcesPath = lmrAni.getPath();
                        return meta;
                    }
                    //--- 2
                    File resPackDir = new File(Minecraft.getInstance().gameDirectory, "resourcepacks");
                    if (resPackDir.isDirectory()) {
                        for (File f : resPackDir.listFiles()) {
                            File packAni = new File(f.getPath() + "/assets/" + namespace);
                            if (packAni.isDirectory()) {
                                meta.type = AnimationType.PACK;
                                meta.resourcesPath = packAni.getPath();
                                return meta;
                            }
                        }
                    }
                } else {
                    meta.type = AnimationType.LEGACY;
                    return meta;
                }
            }
        }
        return null;
    }

    public static String getTimestamp() {
        Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.DAY_OF_MONTH);
        int month = c.get(Calendar.MONTH) + 1;
        int year = c.get(Calendar.YEAR);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int min = c.get(Calendar.MINUTE);
        int sec = c.get(Calendar.SECOND);

        return day + "-" + month + "-" + year + "-" + hour + "-" + min + "-" + sec;
    }

    public static File getFancyMenuAppData() {
        try {
            String appDataDir = null;
            String osName = (System.getProperty("os.name")).toLowerCase();
            if (osName.contains("win")) {
                appDataDir = System.getenv("AppData");
            } else {
                appDataDir = System.getProperty("user.home");
            }
            if (appDataDir != null) {
                appDataDir += "/fancymenu";
                File f = new File(appDataDir);
                f.mkdirs();
                return f;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static MenuIdentifierDatabase getIdentifierDatabase() {
        return menuIdentifierDatabase;
    }

    public static class AdvancedAnimationMeta {
        public String name;
        public String propertiesPath;
        public String resourcesPath;
        public AnimationType type;
    }

    public enum AnimationType {
        LEGACY,
        LMR,
        PACK;
    }

    public static class SetupProperties {
        public String modLoader;
        public String mcVersion;
        public String fmVersion;
        public List<String> modList = new ArrayList<>();
    }

    public static class SetupImporter {

        protected volatile String setupPath;
        protected volatile File setupPropertiesPath;
        protected volatile SetupProperties setupProperties;
        protected volatile File setupInstancePath;

        protected volatile Consumer<SetupImporter> onFinished = null;
        protected volatile boolean importSuccessful = false;
        protected volatile boolean canceledByUser = false;

        protected volatile boolean doStep = true;
        protected volatile int step = 1;
        protected volatile boolean isThreadRunning = true;

        protected volatile StatusPopup importBlockerPopup = null;

        public SetupImporter(String setupPath, Consumer<SetupImporter> onFinished) {
            this.onFinished = onFinished;
            this.setupPath = setupPath;
            if (this.setupPath != null) {
                this.setupInstancePath = new File(this.setupPath + "/setup");
                this.setupPropertiesPath = new File(this.setupPath + "/setup.properties");
                if (this.isValidSetup()) {
                    this.setupProperties = SetupSharingEngine.deserializePropertiesFile(this.setupPropertiesPath.getPath());
                }
            }
        }

        public void startImport() {
            if (this.isValidSetup()) {
                new Thread(() -> {
                    while(isThreadRunning) {
                        try {
                            if (doStep) {

                                /** STEP 1 : CHECK MOD LOADER **/
                                if (step == 1) {
                                    if (setupProperties.modLoader != null) {
                                        if (!setupProperties.modLoader.equals(FancyMenu.MOD_LOADER)) {
                                            doStep = false;
                                            FMYesNoPopup pop = new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
                                                if (call) {
                                                    step = 2;
                                                    doStep = true;
                                                } else {
                                                    finish(true);
                                                }
                                            }, StringUtils.splitLines(Locals.localize("fancymenu.helper.setupsharing.import.differentmodloader", setupProperties.modLoader, FancyMenu.MOD_LOADER), "%n%"));
                                            PopupHandler.displayPopup(pop);
                                        } else {
                                            step = 2;
                                        }
                                    }
                                }

                                /** STEP 2 : CHECK MC VERSION **/
                                if (step == 2) {
                                    if (setupProperties.mcVersion != null) {
                                        if (!setupProperties.mcVersion.equals(SharedConstants.getCurrentVersion().getName())) {
                                            doStep = false;
                                            FMYesNoPopup pop = new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
                                                if (call) {
                                                    step = 3;
                                                    doStep = true;
                                                } else {
                                                    finish(true);
                                                }
                                            }, StringUtils.splitLines(Locals.localize("fancymenu.helper.setupsharing.import.differentmcversion", setupProperties.mcVersion, SharedConstants.getCurrentVersion().getName()), "%n%"));
                                            PopupHandler.displayPopup(pop);
                                        } else {
                                            step = 3;
                                        }
                                    }
                                }

                                /** STEP 3 : CHECK FM VERSION **/
                                if (step == 3) {
                                    if (setupProperties.fmVersion != null) {
                                        if (!setupProperties.fmVersion.equals(FancyMenu.VERSION)) {
                                            doStep = false;
                                            FMYesNoPopup pop = new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
                                                if (call) {
                                                    step = 4;
                                                    doStep = true;
                                                } else {
                                                    finish(true);
                                                }
                                            }, StringUtils.splitLines(Locals.localize("fancymenu.helper.setupsharing.import.differentfmversion", setupProperties.fmVersion, FancyMenu.VERSION), "%n%"));
                                            PopupHandler.displayPopup(pop);
                                        } else {
                                            step = 4;
                                        }
                                    }
                                }

                                /** STEP 4 : CHECK MOD LIST **/
                                if (step == 4) {
                                    step = 5;
                                }

                                /** STEP 5 : FIX MENU IDENTIFIERS **/
                                if (step == 5) {
                                    if (!allMenuIdentifiersValid()) {
                                        doStep = false;
                                        FMYesNoPopup pop = new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
                                            if (call) {
                                                fixMenuIdentifiers();
                                            }
                                            step = 6;
                                            doStep = true;
                                        }, StringUtils.splitLines(Locals.localize("fancymenu.helper.setupsharing.import.invalididentifiers"), "%n%"));
                                        PopupHandler.displayPopup(pop);
                                    } else {
                                        step = 6;
                                    }
                                }

                                /** STEP 6 : IMPORT SETUP **/
                                if (step == 6) {
                                    try {
                                        importBlockerPopup = new StatusPopup(Locals.localize("fancymenu.helper.setupsharing.import.importingsetup"));
                                        PopupHandler.displayPopup(importBlockerPopup);
                                        if (this.setupInstancePath.isDirectory()) {
                                            //--- 2
                                            File targetRaw = Minecraft.getInstance().gameDirectory;
                                            File targetDir = new File(targetRaw.getAbsolutePath());
                                            if (targetDir.isDirectory()) {
                                                try {
                                                    //--- 2
                                                    File fmFolder = FancyMenu.MOD_DIR;
                                                    File customizationFolder = new File(fmFolder.getPath() + "/customization");
                                                    File customizableMenusFile = new File(fmFolder.getPath() + "/customizablemenus.txt");
                                                    //Delete most important stuff first, in case something fails later when deleting the whole dir
                                                    if (customizationFolder.isDirectory()) {
                                                        FileUtils.deleteDirectory(customizationFolder);
                                                    }
                                                    if (customizableMenusFile.isFile()) {
                                                        FileUtils.forceDelete(customizableMenusFile);
                                                    }
                                                    if (fmFolder.isDirectory()) {
                                                        FileUtils.deleteDirectory(fmFolder);
                                                    }
                                                } catch (Exception e2) {
                                                    e2.printStackTrace();
                                                }
                                                FileUtils.copyDirectory(this.setupInstancePath, targetDir);
                                                importSuccessful = true;
                                                finish(false);
                                                FancyMenu.LOGGER.info("[FANCYMENU] Setup successfully imported!");
                                            }
                                        }
                                    } catch (Exception e2) {
                                        e2.printStackTrace();
                                        finish(false);
                                    }
                                }

                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            finish(false);
                        }
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {
                            e.printStackTrace();
                            finish(false);
                        }
                    }
                }).start();
            } else {
                finish(false);
            }
        }

        protected void finish(boolean canceledByUser) {
            this.canceledByUser = canceledByUser;
            this.doStep = false;
            this.step = -10000;
            this.isThreadRunning = false;
            if (this.importBlockerPopup != null) {
                this.importBlockerPopup.setDisplayed(false);
            }
            if (this.onFinished != null) {
                this.onFinished.accept(this);
            }
        }

        public boolean isValidSetup() {
            if ((this.setupInstancePath != null) && (this.setupPropertiesPath != null)) {
                return (this.setupPropertiesPath.isFile() && this.setupInstancePath.isDirectory());
            }
            return false;
        }

        public boolean wasImportSuccessful() {
            return this.importSuccessful;
        }

        public boolean wasCanceledByUser() {
            return this.canceledByUser;
        }

        protected boolean allMenuIdentifiersValid() {
            try {
                for (Map.Entry<String, PropertiesSet> m : getLayouts().entrySet()) {
                    List<PropertiesSection> metas = m.getValue().getPropertiesOfType("customization-meta");
                    if (metas.isEmpty()) {
                        metas = m.getValue().getPropertiesOfType("type-meta");
                    }
                    if (!metas.isEmpty()) {
                        PropertiesSection meta = metas.get(0);
                        String identifier = meta.getEntryValue("identifier");
                        if (identifier != null) {
                            if (!isValidMenuIdentifier(identifier)) {
                                return false;
                            }
                        }
                    }
                }
                File customizableMenusFile = new File(this.setupInstancePath.getPath() + "/config/fancymenu/customizablemenus.txt");
                if (customizableMenusFile.isFile()) {
                    PropertiesSet menus = PropertiesSerializer.getProperties(customizableMenusFile.getPath());
                    if (menus != null) {
                        for (PropertiesSection sec : menus.getProperties()) {
                            if (!isValidMenuIdentifier(sec.getSectionType())) {
                                return false;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        protected void fixMenuIdentifiers() {
            try {
                for (Map.Entry<String, PropertiesSet> m : getLayouts().entrySet()) {
                    List<PropertiesSection> metas = m.getValue().getPropertiesOfType("customization-meta");
                    if (metas.isEmpty()) {
                        metas = m.getValue().getPropertiesOfType("type-meta");
                    }
                    if (!metas.isEmpty()) {
                        PropertiesSection meta = metas.get(0);
                        String identifier = meta.getEntryValue("identifier");
                        if (identifier != null) {
                            if (!isValidMenuIdentifier(identifier)) {
                                String fixedIdentifier = menuIdentifierDatabase.findValidIdentifierFor(identifier);
                                if (fixedIdentifier != null) {
                                    meta.removeEntry("identifier");
                                    meta.addEntry("identifier", fixedIdentifier);
                                    PropertiesSerializer.writeProperties(m.getValue(), m.getKey());
                                    FancyMenu.LOGGER.info("[FANCYMENU] SETUP IMPORT: Identifier fixed: " + identifier + " -> " + fixedIdentifier);
                                } else {
                                    FancyMenu.LOGGER.warn("[FANCYMENU] SETUP IMPORT: Unable to fix identifier: " + identifier);
                                }
                            }
                        }
                    }
                }
                File customizableMenusFile = new File(this.setupInstancePath.getPath() + "/config/fancymenu/customizablemenus.txt");
                if (customizableMenusFile.isFile()) {
                    PropertiesSet menus = PropertiesSerializer.getProperties(customizableMenusFile.getPath());
                    if (menus != null) {
                        int fixed = 0;
                        PropertiesSet newMenus = new PropertiesSet("customizablemenus");
                        for (PropertiesSection sec : menus.getProperties()) {
                            if (!isValidMenuIdentifier(sec.getSectionType())) {
                                String fixedIdentifier = menuIdentifierDatabase.findValidIdentifierFor(sec.getSectionType());
                                if (fixedIdentifier != null) {
                                    PropertiesSection newSec = new PropertiesSection(fixedIdentifier);
                                    newMenus.addProperties(newSec);
                                    fixed++;
                                    FancyMenu.LOGGER.info("[FANCYMENU] SETUP IMPORT: CUSTOMIZABLE MENUS FILE: Identifier fixed: " + sec.getSectionType() + " -> " + fixedIdentifier);
                                } else {
                                    newMenus.addProperties(sec);
                                    FancyMenu.LOGGER.warn("[FANCYMENU] SETUP IMPORT: CUSTOMIZABLE MENUS FILE: Unable to fix identifier: " + sec.getSectionType());
                                }
                            } else {
                                newMenus.addProperties(sec);
                            }
                        }
                        if (fixed > 0) {
                            PropertiesSerializer.writeProperties(newMenus, customizableMenusFile.getPath());
                            FancyMenu.LOGGER.warn("[FANCYMENU] SETUP IMPORT: CUSTOMIZABLE MENUS FILE: Fixed identifiers successfully written to file!");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        protected Map<String, PropertiesSet> getLayouts() {
            Map<String, PropertiesSet> m = new HashMap<>();
            try {
                if (this.setupInstancePath != null) {
                    File cusPath = new File(this.setupInstancePath.getPath() + "/config/fancymenu/customization");
                    File disPath = new File(this.setupInstancePath.getPath() + "/config/fancymenu/customization/.disabled");
                    if (cusPath.isDirectory()) {
                        List<File> layouts = new ArrayList<>();
                        layouts.addAll(Arrays.asList(cusPath.listFiles()));
                        if (disPath.isDirectory()) {
                            layouts.addAll(Arrays.asList(disPath.listFiles()));
                        }
                        for (File f : layouts) {
                            if (f.isFile() && f.getName().toLowerCase().endsWith(".txt")) {
                                PropertiesSet set = PropertiesSerializer.getProperties(f.getPath());
                                if (set != null) {
                                    m.put(f.getPath(), set);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return m;
        }

        protected boolean isValidMenuIdentifier(String identifier) {
            try {
                if (identifier.equals("%fancymenu:universal_layout%")) {
                    return true;
                }
                if (!isCustomGuiName(identifier)) {
                    Class.forName(identifier);
                }
                return true;
            } catch (Exception e2) {}
            return false;
        }

        protected boolean isCustomGuiName(String name) {
            return getCustomGuiNames().contains(name);
        }

        protected List<String> getCustomGuiNames() {
            List<String> l = new ArrayList<>();
            try {
                if (this.setupInstancePath != null) {
                    File guiPath = new File(this.setupInstancePath.getPath() + "/config/fancymenu/customguis");
                    if (guiPath.isDirectory()) {
                        for (File f : guiPath.listFiles()) {
                            if (f.isFile() && f.getName().toLowerCase().endsWith(".txt")) {
                                l.add(Files.getNameWithoutExtension(f.getPath()));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return l;
        }

    }

    public static class MenuIdentifierDatabase {

        protected List<List<String>> identifierGroups = new ArrayList<>();

        public MenuIdentifierDatabase(File dbFile) {
            try {
                PropertiesSet set = PropertiesSerializer.getProperties(dbFile.getPath());
                if (set != null) {
                    for (PropertiesSection s : set.getPropertiesOfType("identifier-group")) {
                        List<String> l = new ArrayList<>();
                        for (Map.Entry<String, String> m : s.getEntries().entrySet()) {
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

        public String findValidIdentifierFor(String invalidIdentifier) {
            try {
                if (invalidIdentifier != null) {
                    for (List<String> l : identifierGroups) {
                        if (l.contains(invalidIdentifier)) {
                            for (String s : l) {
                                try {
                                    Class.forName(s);
                                    return s;
                                } catch (Exception e2) {}
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    public static class StatusPopup extends FMPopup {

        String status;

        public StatusPopup(String status) {
            super(240);
            this.status = status;
        }

        @Override
        public void render(PoseStack matrix, int mouseX, int mouseY, Screen renderIn) {
            super.render(matrix, mouseX, mouseY, renderIn);
            int centerX = renderIn.width / 2;
            int centerY = renderIn.height / 2;
            drawCenteredString(matrix, Minecraft.getInstance().font, this.status, centerX, centerY, -1);
        }

    }

}
