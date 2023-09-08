package de.keksuccino.fancymenu.customization.setupsharing;

import com.google.common.io.Files;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.animation.ResourcePackAnimationRenderer;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.util.rendering.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.util.rendering.ui.popup.FMPopup;
import de.keksuccino.fancymenu.util.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.util.rendering.ui.popup.FMYesNoPopup;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertiesParser;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

//TODO completely rewrite this! all layout assets are now in config/fancymenu/assets, so just export config/fancymenu and that's it
public class SetupSharingHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final File MENU_IDENTIFIERS_DATABASE_FILE = new File(Minecraft.getInstance().gameDirectory, "config/fancymenu/menu_identifiers.db");
    public static final File FM_SETUPS_DIR = new File(Minecraft.getInstance().gameDirectory, "fancymenu_setups/exported_setups");
    public static final File SETUP_BACKUP_DIR = new File(Minecraft.getInstance().gameDirectory, "fancymenu_setups/.backups");

    protected static MenuIdentifierDatabase menuIdentifierDatabase = null;

    public static void init() {
        try {

            FM_SETUPS_DIR.mkdirs();
            SETUP_BACKUP_DIR.mkdirs();

            ResourceLocation rl = new ResourceLocation("fancymenu", "menu_identifiers.db");
            InputStream in = Minecraft.getInstance().getResourceManager().open(rl);
            FileUtils.copyInputStreamToFile(in, MENU_IDENTIFIERS_DATABASE_FILE);
            if (MENU_IDENTIFIERS_DATABASE_FILE.isFile()) {
                menuIdentifierDatabase = new MenuIdentifierDatabase(MENU_IDENTIFIERS_DATABASE_FILE);
            }

            LayoutHandler.reloadLayouts();
            ScreenCustomization.readCustomizableScreensFromFile();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void importSetup() {
        try {
            FMYesNoPopup importConfirmPop = new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
                if (call) {
                    FMTextInputPopup pop = new FMTextInputPopup(new Color(0, 0, 0, 0), I18n.get("fancymenu.helper.setupsharing.import.enterpath"), null, 240, (call2) -> {
                        importSetupWithPathRaw(call2);
                    });
                    PopupHandler.displayPopup(pop);
                }
            }, I18n.get("fancymenu.helper.setupsharing.import.confirm"));
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
            }, I18n.get("fancymenu.helper.setupsharing.import.confirm"));
            PopupHandler.displayPopup(importConfirmPop);
        } catch (Exception e) {
            e.printStackTrace();
            displayImportErrorPopup();
        }
    }

    protected static void importSetupWithPathRaw(String setupPath) {
        if (setupPath != null) {
            setupPath = AbstractElement.fixBackslashPath(setupPath);
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
                                                ScreenCustomization.reloadFancyMenu();
                                            }, I18n.get("fancymenu.helper.setupsharing.import.success"));
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
                        FMNotificationPopup pop2 = new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, I18n.get("fancymenu.helper.setupsharing.import.invalidsetup"));
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
            ScreenCustomization.reloadFancyMenu();
        }, I18n.get("fancymenu.helper.setupsharing.import.error"));
        PopupHandler.displayPopup(pop);
    }

    public static void exportSetup() {
        try {
            FMYesNoPopup exportConfirmPopup = new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
                if (call) {
                    String setupNameDefault = "exported_fm_setup_" + getTimestamp();
                    FMTextInputPopup pop = new FMTextInputPopup(new Color(0, 0, 0, 0), I18n.get("fancymenu.helper.setupsharing.export.entername"), null, 240, (call2) -> {
                        if (call2 != null) {
                            new Thread(() -> {
                                String setupName = call2;
                                if (setupName.replace(" ", "").equals("")) {
                                    setupName = setupNameDefault;
                                }
                                StatusPopup exportBlockerPopup = new StatusPopup(I18n.get("fancymenu.helper.setupsharing.import.exportingsetup"));
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
            }, I18n.get("fancymenu.helper.setupsharing.export.confirm"));
            PopupHandler.displayPopup(exportConfirmPopup);
        } catch (Exception e) {
            e.printStackTrace();
            displayExportErrorPopup();
        }
    }

    protected static void copyTempImportPath(File setup, File temp, Runnable onFinish) {
        new Thread(() -> {
            try {
                StatusPopup copyBlockerPopup = new StatusPopup(I18n.get("fancymenu.helper.setupsharing.import.preparing"));
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
                StatusPopup backupBlockerPopup = new StatusPopup(I18n.get("fancymenu.helper.setupsharing.restore.backingup"));
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
                LOGGER.info("[FANCYMENU] Deleting old setup backup: " + f.getPath());
                FileUtils.deleteDirectory(f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Sorts from OLDEST to NEWEST. The oldest file is the first in the array. **/
    public static File[] sortByDateModified(File[] files) {
        if (files == null) return new File[]{};
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
            de.keksuccino.fancymenu.util.file.FileUtils.openFile(exportFailsFile);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    protected static void displayExportErrorPopup() {
        FMNotificationPopup pop = new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, I18n.get("fancymenu.helper.setupsharing.export.error"));
        PopupHandler.displayPopup(pop);
    }

    protected static void displayExportSuccessPopup(File exportTo) {
        FMYesNoPopup pop = new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call2) -> {
            try {
                if (call2) {
                    de.keksuccino.fancymenu.util.file.FileUtils.openFile(exportTo);
                }
            } catch (Exception e) {
                e.printStackTrace();
                displayExportErrorPopup();
            }
        }, I18n.get("fancymenu.helper.setupsharing.export.success"));
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
        }, I18n.get("fancymenu.helper.setupsharing.export.unabletoexport", "" + unableToExportList.size()));
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

            File exportToTemp = new File(Minecraft.getInstance().gameDirectory, "fm_export_temp_folder_" + UUID.randomUUID());
            File exportToTempSetup = new File(exportToTemp.getPath() + "/setup");
            exportToTempSetup.mkdirs();

            List<Layout> layouts = new ArrayList<>();
            layouts.addAll(LayoutHandler.getEnabledLayouts());
            layouts.addAll(LayoutHandler.getDisabledLayouts());

            //Export all layout resources
            List<String> exportedResources = new ArrayList<>();
            List<String> failedToExportResources = new ArrayList<>();
            for (Layout l : layouts) {
                for (String s : getLayoutResources(l.serialize())) {
                    try {
                        if (!exportedResources.contains(s) && !failedToExportResources.contains(s)) {
                            File oriFile = new File(s);
                            if (oriFile.isFile()) {
                                File target = new File(exportToTempSetup.getAbsolutePath().replace("\\", "/") + "/" + getShortPath(oriFile.getPath()));
                                File targetParent = target.getParentFile();
                                if (targetParent != null) {
                                    targetParent.mkdirs();
                                }
                                FileUtils.copyFile(oriFile, target);
                                exportedResources.add(s);
                            } else if (oriFile.isDirectory()) {
                                String targetDir = exportToTempSetup.getAbsolutePath().replace("\\", "/") + "/" + getShortPath(oriFile.getPath());
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
                                        File resTarget = new File(exportToTempSetup.getAbsolutePath().replace("\\", "/") + "/" + getShortPath(resPath.getPath()));
                                        resTarget.mkdirs();
                                        FileUtils.copyDirectory(resPath, resTarget);
                                        if ((packMetaPath != null) && packMetaPath.isFile()) {
                                            File packMetaTarget = new File(exportToTempSetup.getAbsolutePath().replace("\\", "/") + "/" + getShortPath(packMetaPath.getPath()));
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
                                        File resTarget = new File(exportToTempSetup.getAbsolutePath().replace("\\", "/") + "/" + getShortPath(resPath.getPath()));
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

            //Export setup (config/fancymenu)
            try {
                File fmDir = FancyMenu.MOD_DIR;
                File target = new File(exportToTempSetup.getAbsolutePath().replace("\\", "/") + "/config/fancymenu");
                target.mkdirs();
                FileUtils.copyDirectory(fmDir, target);
            } catch (Exception e2) {
                e2.printStackTrace();
                return null;
            }

            //Generate setup.properties in setup root
            writeSetupPropertiesFile(exportToTemp.getAbsolutePath().replace("\\", "/") + "/setup.properties");

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

            PropertyContainerSet set = new PropertyContainerSet("fancymenu_setup");

            PropertyContainer meta = new PropertyContainer("setup-meta");
            meta.putProperty("modloader", FancyMenu.MOD_LOADER);
            meta.putProperty("mcversion", SharedConstants.getCurrentVersion().getName());
            meta.putProperty("fmversion", FancyMenu.VERSION);
            set.putContainer(meta);

            PropertyContainer mods = new PropertyContainer("mod-list");
            int i = 1;
            for (String id : Services.PLATFORM.getLoadedModIds()) {
                if (!id.equals("fancymenu") && !id.equals("konkrete") && !id.equals("loadmyresources") && !id.equals("forge") && !id.equals("mcp") && !id.equals("minecraft") && !id.equals("fml")) {
                    mods.putProperty("" + i, id);
                }
            }
            set.putContainer(mods);

            PropertiesParser.serializeSetToFile(set, saveToPathWithFileName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SetupProperties deserializePropertiesFile(String propsFilePath) {

        SetupProperties sp = new SetupProperties();

        try {

            PropertyContainerSet set = PropertiesParser.deserializeSetFromFile(propsFilePath);
            if (set != null) {
                List<PropertyContainer> metas = set.getContainersOfType("setup-meta");
                if (!metas.isEmpty()) {
                    PropertyContainer meta = metas.get(0);
                    sp.modLoader = meta.getValue("modloader");
                    sp.mcVersion = meta.getValue("mcversion");
                    sp.fmVersion = meta.getValue("fmversion");
                }
                List<PropertyContainer> modLists = set.getContainersOfType("mod-list");
                if (!modLists.isEmpty()) {
                    PropertyContainer modList = modLists.get(0);
                    for (Map.Entry<String, String> m : modList.getProperties().entrySet()) {
                        sp.modList.add(m.getValue());
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return sp;

    }

    protected static List<String> getLayoutResources(PropertyContainerSet layout) {
        List<String> l = new ArrayList<>();
        for (PropertyContainer s : layout.getContainersOfType("customization")) {
            String action = s.getValue("action");
            if (action != null) {
                for (Map.Entry<String, String> m : s.getProperties().entrySet()) {
                    //Skip values of button actions
                    if (action.equalsIgnoreCase("addbutton") && (m.getKey() != null) && m.getKey().equals("value")) {
                        continue;
                    }
                    String shortPath = AbstractElement.fixBackslashPath(getShortPath(AbstractElement.fixBackslashPath(m.getValue())));
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
        path = AbstractElement.fixBackslashPath(path);
        File pathRaw = new File(path);
        if (!pathRaw.exists()) {
            return null;
        }
        File home = Minecraft.getInstance().gameDirectory;
        if (pathRaw.getAbsolutePath().replace("\\", "/").startsWith(home.getAbsolutePath().replace("\\", "/"))) {
            path = pathRaw.getAbsolutePath().replace("\\", "/").replace(home.getAbsolutePath().replace("\\", "/"), "");
            if (path.startsWith("\\") || path.startsWith("/")) {
                path = path.substring(1);
            }
        }
        return AbstractElement.fixBackslashPath(path);
    }

    protected static List<AdvancedAnimationMeta> getAnimationMetas() {
        List<AdvancedAnimationMeta> l = new ArrayList<>();
        for (String s : AnimationHandler.getExternalAnimationNames()) {
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
                    File lmrAni = new File(Minecraft.getInstance().gameDirectory, "resources/" + namespace);
                    if (lmrAni.isDirectory()) {
                        meta.type = AnimationType.LMR;
                        meta.resourcesPath = lmrAni.getPath();
                        return meta;
                    }
                    File resPackDir = new File(Minecraft.getInstance().gameDirectory, "resourcepacks");
                    if (resPackDir.isDirectory()) {
                        for (File f : Objects.requireNonNull(resPackDir.listFiles())) {
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
                    this.setupProperties = SetupSharingHandler.deserializePropertiesFile(this.setupPropertiesPath.getPath());
                }
            }
        }

        public void startImport() {
            if (this.isValidSetup()) {
                new Thread(() -> {
                    while(isThreadRunning) {
                        try {
                            if (doStep) {

                                // STEP 1 : CHECK MOD LOADER
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
                                            }, LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.setupsharing.import.differentmodloader", setupProperties.modLoader, FancyMenu.MOD_LOADER)));
                                            PopupHandler.displayPopup(pop);
                                        } else {
                                            step = 2;
                                        }
                                    }
                                }

                                // STEP 2 : CHECK MC VERSION
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
                                            }, LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.setupsharing.import.differentmcversion", setupProperties.mcVersion, SharedConstants.getCurrentVersion().getName())));
                                            PopupHandler.displayPopup(pop);
                                        } else {
                                            step = 3;
                                        }
                                    }
                                }

                                // STEP 3 : CHECK FM VERSION
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
                                            }, LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.setupsharing.import.differentfmversion", setupProperties.fmVersion, FancyMenu.VERSION)));
                                            PopupHandler.displayPopup(pop);
                                        } else {
                                            step = 4;
                                        }
                                    }
                                }

                                // STEP 4 : CHECK MOD LIST
                                if (step == 4) {
                                    //well, I should probably implement that at some point, but not sure if that will ever happen tbh lmao
                                    step = 5;
                                }

                                // STEP 5 : FIX MENU IDENTIFIERS
                                if (step == 5) {
                                    if (!allMenuIdentifiersValid()) {
                                        doStep = false;
                                        FMYesNoPopup pop = new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
                                            if (call) {
                                                fixMenuIdentifiers();
                                            }
                                            step = 6;
                                            doStep = true;
                                        }, LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.setupsharing.import.invalididentifiers")));
                                        PopupHandler.displayPopup(pop);
                                    } else {
                                        step = 6;
                                    }
                                }

                                // STEP 6 : IMPORT SETUP
                                if (step == 6) {
                                    try {
                                        importBlockerPopup = new StatusPopup(I18n.get("fancymenu.helper.setupsharing.import.importingsetup"));
                                        PopupHandler.displayPopup(importBlockerPopup);
                                        if (this.setupInstancePath.isDirectory()) {
                                            File targetRaw = Minecraft.getInstance().gameDirectory;
                                            File targetDir = new File(targetRaw.getAbsolutePath().replace("\\", "/"));
                                            if (targetDir.isDirectory()) {
                                                try {
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
                                                LOGGER.info("[FANCYMENU] Setup successfully imported!");
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
                for (Map.Entry<String, PropertyContainerSet> m : getLayouts().entrySet()) {
                    List<PropertyContainer> metas = m.getValue().getContainersOfType("customization-meta");
                    if (metas.isEmpty()) {
                        metas = m.getValue().getContainersOfType("type-meta");
                    }
                    if (!metas.isEmpty()) {
                        PropertyContainer meta = metas.get(0);
                        String identifier = meta.getValue("identifier");
                        if (identifier != null) {
                            if (!isValidMenuIdentifier(identifier)) {
                                return false;
                            }
                        }
                    }
                }
                File customizableMenusFile = new File(this.setupInstancePath.getPath() + "/config/fancymenu/customizablemenus.txt");
                if (customizableMenusFile.isFile()) {
                    PropertyContainerSet menus = PropertiesParser.deserializeSetFromFile(customizableMenusFile.getPath());
                    if (menus != null) {
                        for (PropertyContainer sec : menus.getContainers()) {
                            if (!isValidMenuIdentifier(sec.getType())) {
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
                for (Map.Entry<String, PropertyContainerSet> m : getLayouts().entrySet()) {
                    List<PropertyContainer> metas = m.getValue().getContainersOfType("customization-meta");
                    if (metas.isEmpty()) {
                        metas = m.getValue().getContainersOfType("type-meta");
                    }
                    if (!metas.isEmpty()) {
                        PropertyContainer meta = metas.get(0);
                        String identifier = meta.getValue("identifier");
                        if (identifier != null) {
                            if (!isValidMenuIdentifier(identifier)) {
                                String fixedIdentifier = menuIdentifierDatabase.findValidIdentifierFor(identifier);
                                if (fixedIdentifier != null) {
                                    meta.removeProperty("identifier");
                                    meta.putProperty("identifier", fixedIdentifier);
                                    PropertiesParser.serializeSetToFile(m.getValue(), m.getKey());
                                    LOGGER.info("[FANCYMENU] SETUP IMPORT: Identifier fixed: " + identifier + " -> " + fixedIdentifier);
                                } else {
                                    LOGGER.warn("[FANCYMENU] SETUP IMPORT: Unable to fix identifier: " + identifier);
                                }
                            }
                        }
                    }
                }
                File customizableMenusFile = new File(this.setupInstancePath.getPath() + "/config/fancymenu/customizablemenus.txt");
                if (customizableMenusFile.isFile()) {
                    PropertyContainerSet menus = PropertiesParser.deserializeSetFromFile(customizableMenusFile.getPath());
                    if (menus != null) {
                        int fixed = 0;
                        PropertyContainerSet newMenus = new PropertyContainerSet("customizablemenus");
                        for (PropertyContainer sec : menus.getContainers()) {
                            if (!isValidMenuIdentifier(sec.getType())) {
                                String fixedIdentifier = menuIdentifierDatabase.findValidIdentifierFor(sec.getType());
                                if (fixedIdentifier != null) {
                                    PropertyContainer newSec = new PropertyContainer(fixedIdentifier);
                                    newMenus.putContainer(newSec);
                                    fixed++;
                                    LOGGER.info("[FANCYMENU] SETUP IMPORT: CUSTOMIZABLE MENUS FILE: Identifier fixed: " + sec.getType() + " -> " + fixedIdentifier);
                                } else {
                                    newMenus.putContainer(sec);
                                    LOGGER.warn("[FANCYMENU] SETUP IMPORT: CUSTOMIZABLE MENUS FILE: Unable to fix identifier: " + sec.getType());
                                }
                            } else {
                                newMenus.putContainer(sec);
                            }
                        }
                        if (fixed > 0) {
                            PropertiesParser.serializeSetToFile(newMenus, customizableMenusFile.getPath());
                            LOGGER.warn("[FANCYMENU] SETUP IMPORT: CUSTOMIZABLE MENUS FILE: Fixed identifiers successfully written to file!");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        protected Map<String, PropertyContainerSet> getLayouts() {
            Map<String, PropertyContainerSet> m = new HashMap<>();
            try {
                if (this.setupInstancePath != null) {
                    File cusPath = new File(this.setupInstancePath.getPath() + "/config/fancymenu/customization");
                    File disPath = new File(this.setupInstancePath.getPath() + "/config/fancymenu/customization/.disabled");
                    if (cusPath.isDirectory()) {
                        List<File> layouts = new ArrayList<>(Arrays.asList(Objects.requireNonNull(cusPath.listFiles())));
                        if (disPath.isDirectory()) {
                            layouts.addAll(Arrays.asList(Objects.requireNonNull(disPath.listFiles())));
                        }
                        for (File f : layouts) {
                            if (f.isFile() && f.getName().toLowerCase().endsWith(".txt")) {
                                PropertyContainerSet set = PropertiesParser.deserializeSetFromFile(f.getPath());
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
                    Class.forName(identifier, false, SetupSharingHandler.class.getClassLoader());
                }
                return true;
            } catch (Exception ignored) {}
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
                        for (File f : Objects.requireNonNull(guiPath.listFiles())) {
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
                PropertyContainerSet set = PropertiesParser.deserializeSetFromFile(dbFile.getPath());
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

        public String findValidIdentifierFor(String invalidIdentifier) {
            try {
                if (invalidIdentifier != null) {
                    for (List<String> l : identifierGroups) {
                        if (l.contains(invalidIdentifier)) {
                            for (String s : l) {
                                try {
                                    Class.forName(s, false, SetupSharingHandler.class.getClassLoader());
                                    return s;
                                } catch (Exception ignored) {}
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
