package de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements;

import de.keksuccino.fancymenu.api.visibilityrequirements.VisibilityRequirement;
import de.keksuccino.fancymenu.api.visibilityrequirements.VisibilityRequirementRegistry;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser;
import de.keksuccino.fancymenu.menu.servers.ServerCache;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.fml.ModList;

import java.util.*;

public class VisibilityRequirementContainer {

    public boolean forceShow = false;
    public boolean forceHide = false;

    //Visibility Requirements
    //VR show-if values are always the requirement that must be met to show the element.
    //So if the system should check for the main hand item and it's show-if value is set to FALSE, the element is visible if NO ITEM IS IN THE MAIN HAND.
    public boolean vrCheckForSingleplayer = false;
    public boolean vrShowIfSingleplayer = false;

    public boolean vrCheckForMultiplayer = false;
    public boolean vrShowIfMultiplayer = false;

    public boolean vrCheckForWindowWidth = false;
    public boolean vrShowIfWindowWidth = false;
    public String vrWindowWidth = null;

    public boolean vrCheckForWindowHeight = false;
    public boolean vrShowIfWindowHeight = false;
    public String vrWindowHeight = null;

    public boolean vrCheckForWindowWidthBiggerThan = false;
    public boolean vrShowIfWindowWidthBiggerThan = false;
    public String vrWindowWidthBiggerThan = null;

    public boolean vrCheckForWindowHeightBiggerThan = false;
    public boolean vrShowIfWindowHeightBiggerThan = false;
    public String vrWindowHeightBiggerThan = null;

    public boolean vrCheckForButtonHovered = false;
    public boolean vrShowIfButtonHovered = false;
    public String vrButtonHovered = null;

    public boolean vrCheckForWorldLoaded = false;
    public boolean vrShowIfWorldLoaded = false;

    public boolean vrCheckForLanguage = false;
    public boolean vrShowIfLanguage = false;
    public String vrLanguage = null;

    public boolean vrCheckForFullscreen = false;
    public boolean vrShowIfFullscreen = false;

    public boolean vrCheckForOsWindows = false;
    public boolean vrShowIfOsWindows = false;

    public boolean vrCheckForOsMac = false;
    public boolean vrShowIfOsMac = false;

    public boolean vrCheckForOsLinux = false;
    public boolean vrShowIfOsLinux = false;

    public boolean vrCheckForModLoaded = false;
    public boolean vrShowIfModLoaded = false;
    public List<String> vrModLoaded = new ArrayList<String>();

    public boolean vrCheckForServerOnline = false;
    public boolean vrShowIfServerOnline = false;
    public String vrServerOnline = null;

    public boolean vrCheckForGuiScale = false;
    public boolean vrShowIfGuiScale = false;
    public List<String> vrGuiScale = new ArrayList<>();

    public Map<String, RequirementPackage> customRequirements = new LinkedHashMap<>();

    public CustomizationItemBase item;

    public VisibilityRequirementContainer(PropertiesSection properties, CustomizationItemBase item) {

        this.item = item;

        //VR: Is Singleplayer
        String vrStringShowIfSingleplayer = properties.getEntryValue("vr:showif:singleplayer");
        if (vrStringShowIfSingleplayer != null) {
            this.vrCheckForSingleplayer = true;
            if (vrStringShowIfSingleplayer.equalsIgnoreCase("true")) {
                this.vrShowIfSingleplayer = true;
            }
        }

        //VR: Is Multiplayer
        String vrStringShowIfMultiplayer = properties.getEntryValue("vr:showif:multiplayer");
        if (vrStringShowIfMultiplayer != null) {
            this.vrCheckForMultiplayer = true;
            if (vrStringShowIfMultiplayer.equalsIgnoreCase("true")) {
                this.vrShowIfMultiplayer = true;
            }
        }

        //VR: Is Window Width
        String vrStringShowIfWindowWidth = properties.getEntryValue("vr:showif:windowwidth");
        if (vrStringShowIfWindowWidth != null) {
            if (vrStringShowIfWindowWidth.equalsIgnoreCase("true")) {
                this.vrShowIfWindowWidth = true;
            }
            String windowWidth = properties.getEntryValue("vr:value:windowwidth");
            if (windowWidth != null) {
                this.vrWindowWidth = windowWidth;
                this.vrCheckForWindowWidth = true;
            }
        }

        //VR: Is Window Height
        String vrStringShowIfWindowHeight = properties.getEntryValue("vr:showif:windowheight");
        if (vrStringShowIfWindowHeight != null) {
            if (vrStringShowIfWindowHeight.equalsIgnoreCase("true")) {
                this.vrShowIfWindowHeight = true;
            }
            String windowHeight = properties.getEntryValue("vr:value:windowheight");
            if (windowHeight != null) {
                this.vrWindowHeight = windowHeight;
                this.vrCheckForWindowHeight = true;
            }
        }

        //VR: Is Window Width Bigger Than
        String vrStringShowIfWindowWidthBiggerThan = properties.getEntryValue("vr:showif:windowwidthbiggerthan");
        if (vrStringShowIfWindowWidthBiggerThan != null) {
            if (vrStringShowIfWindowWidthBiggerThan.equalsIgnoreCase("true")) {
                this.vrShowIfWindowWidthBiggerThan = true;
            }
            String windowWidth = properties.getEntryValue("vr:value:windowwidthbiggerthan");
            if (windowWidth != null) {
                this.vrCheckForWindowWidthBiggerThan = true;
                this.vrWindowWidthBiggerThan = windowWidth;
            }
        }

        //VR: Is Window Height Bigger Than
        String vrStringShowIfWindowHeightBiggerThan = properties.getEntryValue("vr:showif:windowheightbiggerthan");
        if (vrStringShowIfWindowHeightBiggerThan != null) {
            if (vrStringShowIfWindowHeightBiggerThan.equalsIgnoreCase("true")) {
                this.vrShowIfWindowHeightBiggerThan = true;
            }
            String windowHeight = properties.getEntryValue("vr:value:windowheightbiggerthan");
            if (windowHeight != null) {
                this.vrCheckForWindowHeightBiggerThan = true;
                this.vrWindowHeightBiggerThan = windowHeight;
            }
        }

        //VR: Is Button Hovered
        String vrStringShowIfButtonHovered = properties.getEntryValue("vr:showif:buttonhovered");
        if (vrStringShowIfButtonHovered != null) {
            if (vrStringShowIfButtonHovered.equalsIgnoreCase("true")) {
                this.vrShowIfButtonHovered = true;
            }
            String buttonID = properties.getEntryValue("vr:value:buttonhovered");
            if (buttonID != null) {
                this.vrCheckForButtonHovered = true;
                this.vrButtonHovered = buttonID;
            }
        }

        //VR: Is World Loaded
        String vrStringShowIfWorldLoaded = properties.getEntryValue("vr:showif:worldloaded");
        if (vrStringShowIfWorldLoaded != null) {
            this.vrCheckForWorldLoaded = true;
            if (vrStringShowIfWorldLoaded.equalsIgnoreCase("true")) {
                this.vrShowIfWorldLoaded = true;
            }
        }

        //VR: Is Language
        String vrStringShowIfLanguage = properties.getEntryValue("vr:showif:language");
        if (vrStringShowIfLanguage != null) {
            if (vrStringShowIfLanguage.equalsIgnoreCase("true")) {
                this.vrShowIfLanguage = true;
            }
            String language = properties.getEntryValue("vr:value:language");
            if (language != null) {
                this.vrCheckForLanguage = true;
                this.vrLanguage = language;
            }
        }

        //VR: Is Fullscreen
        String vrStringShowIfFullscreen = properties.getEntryValue("vr:showif:fullscreen");
        if (vrStringShowIfFullscreen != null) {
            this.vrCheckForFullscreen = true;
            if (vrStringShowIfFullscreen.equalsIgnoreCase("true")) {
                this.vrShowIfFullscreen = true;
            }
        }

        //VR: Is OS Windows
        String vrStringShowIfOsWindows = properties.getEntryValue("vr:showif:oswindows");
        if (vrStringShowIfOsWindows != null) {
            this.vrCheckForOsWindows = true;
            if (vrStringShowIfOsWindows.equalsIgnoreCase("true")) {
                this.vrShowIfOsWindows = true;
            }
        }

        //VR: Is OS Mac
        String vrStringShowIfOsMac = properties.getEntryValue("vr:showif:osmac");
        if (vrStringShowIfOsMac != null) {
            this.vrCheckForOsMac = true;
            if (vrStringShowIfOsMac.equalsIgnoreCase("true")) {
                this.vrShowIfOsMac = true;
            }
        }

        //VR: Is OS Linux
        String vrStringShowIfOsLinux = properties.getEntryValue("vr:showif:oslinux");
        if (vrStringShowIfOsLinux != null) {
            this.vrCheckForOsLinux = true;
            if (vrStringShowIfOsLinux.equalsIgnoreCase("true")) {
                this.vrShowIfOsLinux = true;
            }
        }

        //VR: Is Mod Loaded
        String vrStringShowIfModLoaded = properties.getEntryValue("vr:showif:modloaded");
        if (vrStringShowIfModLoaded != null) {
            if (vrStringShowIfModLoaded.equalsIgnoreCase("true")) {
                this.vrShowIfModLoaded = true;
            }
            String modID = properties.getEntryValue("vr:value:modloaded");
            if (modID != null) {
                this.vrModLoaded.clear();
                if (modID.contains(",")) {
                    for (String s : modID.replace(" ", "").split("[,]")) {
                        this.vrModLoaded.add(s);
                    }
                } else {
                    this.vrModLoaded.add(modID.replace(" ", ""));
                }
                if (!this.vrModLoaded.isEmpty()) {
                    this.vrCheckForModLoaded = true;
                }
            }
        }

        //VR: Is Server Online
        String vrStringShowIfServerOnline = properties.getEntryValue("vr:showif:serveronline");
        if (vrStringShowIfServerOnline != null) {
            if (vrStringShowIfServerOnline.equalsIgnoreCase("true")) {
                this.vrShowIfServerOnline = true;
            }
            String ip = properties.getEntryValue("vr:value:serveronline");
            if (ip != null) {
                this.vrCheckForServerOnline = true;
                this.vrServerOnline = ip;
            }
        }

        //VR: Is Gui Scale
        String vrStringShowIfGuiScale = properties.getEntryValue("vr:showif:guiscale");
        if (vrStringShowIfGuiScale != null) {
            if (vrStringShowIfGuiScale.equalsIgnoreCase("true")) {
                this.vrShowIfGuiScale = true;
            }
            String guiScale = properties.getEntryValue("vr:value:guiscale");
            if (guiScale != null) {
                this.vrGuiScale.clear();
                if (guiScale.contains(",")) {
                    for (String s : guiScale.replace(" ", "").split("[,]")) {
                        this.vrGuiScale.add(s);
                    }
                } else {
                    if (guiScale.length() > 0) {
                        this.vrGuiScale.add(guiScale.replace(" ", ""));
                    }
                }
                List<String> l = new ArrayList<>();
                for (String s : this.vrGuiScale) {
                    if (MathUtils.isDouble(s)) {
                        l.add("double:" + s);
                    } else {
                        if (s.startsWith(">")) {
                            String value = s.split("[>]", 2)[1];
                            if (MathUtils.isDouble(value)) {
                                l.add("biggerthan:" + value);
                            }
                        } else if (s.startsWith("<")) {
                            String value = s.split("[<]", 2)[1];
                            if (MathUtils.isDouble(value)) {
                                l.add("smallerthan:" + value);
                            }
                        }
                    }
                }
                this.vrGuiScale = l;
                if (!this.vrGuiScale.isEmpty()) {
                    this.vrCheckForGuiScale = true;
                }
            }
        }

        //CUSTOM VISIBILITY REQUIREMENTS (API)
        this.customRequirements.clear();
        for (VisibilityRequirement v : VisibilityRequirementRegistry.getRequirements()) {
            RequirementPackage p = new RequirementPackage();
            p.requirement = v;
            this.customRequirements.put(v.getIdentifier(), p);
        }
        for (RequirementPackage p : this.customRequirements.values()) {
            VisibilityRequirement v = p.requirement;
            String stringShowIf = properties.getEntryValue("vr_custom:showif:" + v.getIdentifier());
            if (stringShowIf != null) {
                if (p != null) {
                    if (stringShowIf.equalsIgnoreCase("true")) {
                        p.showIf = true;
                    }
                    p.value = properties.getEntryValue("vr_custom:value:" + v.getIdentifier());
                    p.checkFor = true;
                }
            }
        }

    }

    private static Object replacePlaceholdersIn(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return PlaceholderParser.replacePlaceholders((String)value);
        } else if (value instanceof List<?>) {
            List<String> vals = new ArrayList<>();
            for (Object o : ((List<?>)value)) {
                if (o instanceof String) {
                    vals.add(PlaceholderParser.replacePlaceholders(((String)o)));
                }
            }
            return vals;
        }
        return null;
    }

    public boolean isVisible() {

        if (forceShow) {
            return true;
        }
        if (forceHide) {
            return false;
        }

        String cachedvrWindowWidth = vrWindowWidth;
        String cachedvrWindowHeight = vrWindowHeight;
        String cachedvrWindowWidthBiggerThan = vrWindowWidthBiggerThan;
        String cachedvrWindowHeightBiggerThan = vrWindowHeightBiggerThan;
        String cachedvrButtonHovered = vrButtonHovered;
        String cachedvrLanguage = vrLanguage;
        List<String> cachedvrModLoaded = vrModLoaded;
        String cachedvrServerOnline = vrServerOnline;
        List<String> cachedvrGuiScale = vrGuiScale;

        vrWindowWidth = (String) replacePlaceholdersIn(vrWindowWidth);
        vrWindowHeight = (String) replacePlaceholdersIn(vrWindowHeight);
        vrWindowWidthBiggerThan = (String) replacePlaceholdersIn(vrWindowWidthBiggerThan);
        vrWindowHeightBiggerThan = (String) replacePlaceholdersIn(vrWindowHeightBiggerThan);
        vrButtonHovered = (String) replacePlaceholdersIn(vrButtonHovered);
        vrLanguage = (String) replacePlaceholdersIn(vrLanguage);
        vrModLoaded = (List<String>) replacePlaceholdersIn(vrModLoaded);
        vrServerOnline = (String) replacePlaceholdersIn(vrServerOnline);
        vrGuiScale = (List<String>) replacePlaceholdersIn(vrGuiScale);

        try {

            //VR: Is Singleplayer
            if (this.vrCheckForSingleplayer) {
                if (this.vrShowIfSingleplayer) {
                    if (!VisibilityRequirementHandler.isSingleplayer) {
                        return false;
                    }
                } else {
                    if (VisibilityRequirementHandler.isSingleplayer) {
                        return false;
                    }
                }
            }

            //VR: Is Multiplayer
            if (this.vrCheckForMultiplayer) {
                if (this.vrShowIfMultiplayer) {
                    if (!VisibilityRequirementHandler.worldLoaded) {
                        return false;
                    }
                    if (VisibilityRequirementHandler.isSingleplayer) {
                        return false;
                    }
                } else {
                    if (!VisibilityRequirementHandler.isSingleplayer && VisibilityRequirementHandler.worldLoaded) {
                        return false;
                    }
                }
            }

            //VR: Is Window Width
            if (this.vrCheckForWindowWidth) {
                if (this.vrWindowWidth != null) {
                    List<Integer> l = new ArrayList<>();
                    if (this.vrWindowWidth.contains(",")) {
                        for (String s : this.vrWindowWidth.replace(" ", "").split("[,]")) {
                            if (MathUtils.isInteger(s)) {
                                l.add(Integer.parseInt(s));
                            }
                        }
                    } else {
                        if (MathUtils.isInteger(this.vrWindowWidth.replace(" ", ""))) {
                            l.add(Integer.parseInt(this.vrWindowWidth.replace(" ", "")));
                        }
                    }
                    if (!l.isEmpty()) {
                        if (this.vrShowIfWindowWidth) {
                            if (!l.contains(Minecraft.getInstance().getWindow().getWidth())) {
                                return false;
                            }
                        } else {
                            if (l.contains(Minecraft.getInstance().getWindow().getWidth())) {
                                return false;
                            }
                        }
                    }
                }
            }

            //VR: Is Window Height
            if (this.vrCheckForWindowHeight) {
                if (this.vrWindowHeight != null) {
                    List<Integer> l = new ArrayList<>();
                    if (this.vrWindowHeight.contains(",")) {
                        for (String s : this.vrWindowHeight.replace(" ", "").split("[,]")) {
                            if (MathUtils.isInteger(s)) {
                                l.add(Integer.parseInt(s));
                            }
                        }
                    } else {
                        if (MathUtils.isInteger(this.vrWindowHeight.replace(" ", ""))) {
                            l.add(Integer.parseInt(this.vrWindowHeight.replace(" ", "")));
                        }
                    }
                    if (!l.isEmpty()) {
                        if (this.vrShowIfWindowHeight) {
                            if (!l.contains(Minecraft.getInstance().getWindow().getHeight())) {
                                return false;
                            }
                        } else {
                            if (l.contains(Minecraft.getInstance().getWindow().getHeight())) {
                                return false;
                            }
                        }
                    }
                }
            }

            //VR: Is Window Width Bigger Than
            if (this.vrCheckForWindowWidthBiggerThan) {
                if (MathUtils.isInteger(this.vrWindowWidthBiggerThan)) {
                    if (this.vrShowIfWindowWidthBiggerThan) {
                        if (Minecraft.getInstance().getWindow().getWidth() <= Integer.parseInt(this.vrWindowWidthBiggerThan)) {
                            return false;
                        }
                    } else {
                        if (Minecraft.getInstance().getWindow().getWidth() >= Integer.parseInt(this.vrWindowWidthBiggerThan)) {
                            return false;
                        }
                    }
                }
            }

            //VR: Is Window Height Bigger Than
            if (this.vrCheckForWindowHeightBiggerThan) {
                if (MathUtils.isInteger(this.vrWindowHeightBiggerThan)) {
                    if (this.vrShowIfWindowHeightBiggerThan) {
                        if (Minecraft.getInstance().getWindow().getHeight() <= Integer.parseInt(this.vrWindowHeightBiggerThan)) {
                            return false;
                        }
                    } else {
                        if (Minecraft.getInstance().getWindow().getHeight() >= Integer.parseInt(this.vrWindowHeightBiggerThan)) {
                            return false;
                        }
                    }
                }
            }

            //VR: Is Button Hovered
            if (this.vrCheckForButtonHovered) {
                if (this.vrButtonHovered != null) {
                    AbstractWidget w = null;
                    try {
                        if (this.vrButtonHovered.startsWith("vanillabtn:")) {
                            String idRaw = this.vrButtonHovered.split("[:]", 2)[1];
                            if (MathUtils.isLong(idRaw)) {
                                w = ButtonCache.getButtonForId(Long.parseLong(idRaw)).getButton();
                            } else if (idRaw.startsWith("button_compatibility_id:")) {
                                w = ButtonCache.getButtonForCompatibilityId(idRaw).getButton();
                            }
                        } else {
                            w = ButtonCache.getCustomButton(this.vrButtonHovered);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (w != null) {
                        if (this.vrShowIfButtonHovered) {
                            if (!w.isHoveredOrFocused()) {
                                return false;
                            }
                        } else {
                            if (w.isHoveredOrFocused()) {
                                return false;
                            }
                        }
                    }
                }
            }

            //VR: Is World Loaded
            if (this.vrCheckForWorldLoaded) {
                if (this.vrShowIfWorldLoaded) {
                    if (!VisibilityRequirementHandler.worldLoaded) {
                        return false;
                    }
                } else {
                    if (VisibilityRequirementHandler.worldLoaded) {
                        return false;
                    }
                }
            }

            //VR: Is Language
            if (this.vrCheckForLanguage) {
                if ((this.vrLanguage != null) && (Minecraft.getInstance().options.languageCode != null)) {
                    if (this.vrShowIfLanguage) {
                        if (!(Minecraft.getInstance().options.languageCode.equals(this.vrLanguage))) {
                            return false;
                        }
                    } else {
                        if (Minecraft.getInstance().options.languageCode.equals(this.vrLanguage)) {
                            return false;
                        }
                    }
                }
            }

            //VR: Is Fullscreen
            if (this.vrCheckForFullscreen) {
                if (this.vrShowIfFullscreen) {
                    if (!Minecraft.getInstance().getWindow().isFullscreen()) {
                        return false;
                    }
                } else {
                    if (Minecraft.getInstance().getWindow().isFullscreen()) {
                        return false;
                    }
                }
            }

            //VR: Is OS Windows
            if (this.vrCheckForOsWindows) {
                if (this.vrShowIfOsWindows) {
                    if (!VisibilityRequirementHandler.isWindows()) {
                        return false;
                    }
                } else {
                    if (VisibilityRequirementHandler.isWindows()) {
                        return false;
                    }
                }
            }

            //VR: Is OS Mac
            if (this.vrCheckForOsMac) {
                if (this.vrShowIfOsMac) {
                    if (!VisibilityRequirementHandler.isMacOS()) {
                        return false;
                    }
                } else {
                    if (VisibilityRequirementHandler.isMacOS()) {
                        return false;
                    }
                }
            }

            //VR: Is OS Linux
            if (this.vrCheckForOsLinux) {
                boolean linux = (!VisibilityRequirementHandler.isWindows() && !VisibilityRequirementHandler.isMacOS());
                if (this.vrShowIfOsLinux) {
                    if (!linux) {
                        return false;
                    }
                } else {
                    if (linux) {
                        return false;
                    }
                }
            }

            //VR: Is Mod Loaded
            if (this.vrCheckForModLoaded) {
                if (this.vrModLoaded != null) {
                    if (this.vrShowIfModLoaded) {
                        for (String s : this.vrModLoaded) {
                            if (s.equalsIgnoreCase("optifine")) {
                                if (!Konkrete.isOptifineLoaded) {
                                    return false;
                                }
                            } else {
                                if (!ModList.get().isLoaded(s)) {
                                    return false;
                                }
                            }
                        }
                    } else {
                        for (String s : this.vrModLoaded) {
                            if (s.equalsIgnoreCase("optifine")) {
                                if (Konkrete.isOptifineLoaded) {
                                    return false;
                                }
                            } else {
                                if (ModList.get().isLoaded(s)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }

            //VR: Is Server Online
            if (this.vrCheckForServerOnline) {
                ServerData sd = ServerCache.getServer(this.vrServerOnline);
                if (this.vrShowIfServerOnline) {
                    if ((sd != null) && (sd.ping == -1)) {
                        return false;
                    }
                } else {
                    if ((sd != null) && (sd.ping != -1)) {
                        return false;
                    }
                }
            }

            //VR: Is Gui Scale
            if (this.vrCheckForGuiScale) {
                if (this.vrShowIfGuiScale) {
                    for (String condition : this.vrGuiScale) {
                        if (!checkForGuiScale(condition)) {
                            return false;
                        }
                    }
                } else {
                    for (String condition : this.vrGuiScale) {
                        if (checkForGuiScale(condition)) {
                            return false;
                        }
                    }
                }
            }

            //CUSTOM VISIBILITY REQUIREMENTS (API)
            for (RequirementPackage p : this.customRequirements.values()) {

                if (p.checkFor) {
                    if (p.showIf) {
                        if (!p.requirement.isRequirementMet(PlaceholderParser.replacePlaceholders(p.value))) {
                            return false;
                        }
                    } else {
                        if (p.requirement.isRequirementMet(PlaceholderParser.replacePlaceholders(p.value))) {
                            return false;
                        }
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        vrWindowWidth = cachedvrWindowWidth;
        vrWindowHeight = cachedvrWindowHeight;
        vrWindowWidthBiggerThan = cachedvrWindowWidthBiggerThan;
        vrWindowHeightBiggerThan = cachedvrWindowHeightBiggerThan;
        vrButtonHovered = cachedvrButtonHovered;
        vrLanguage = cachedvrLanguage;
        vrModLoaded = cachedvrModLoaded;
        vrServerOnline = cachedvrServerOnline;
        vrGuiScale = cachedvrGuiScale;

        return true;

    }

    protected static boolean checkForGuiScale(String condition) {
        double windowScale = Minecraft.getInstance().getWindow().getGuiScale();
        if (condition.startsWith("double:")) {
            String value = condition.replace("double:", "");
            double valueScale = Double.parseDouble(value);
            return (windowScale == valueScale);
        } else if (condition.startsWith("biggerthan:")) {
            String value = condition.replace("biggerthan:", "");
            double valueScale = Double.parseDouble(value);
            return (windowScale > valueScale);
        } else if (condition.startsWith("smallerthan:")) {
            String value = condition.replace("smallerthan:", "");
            double valueScale = Double.parseDouble(value);
            return (windowScale < valueScale);
        }
        return false;
    }

    public static class RequirementPackage {

        public VisibilityRequirement requirement;
        public boolean showIf = false;
        public String value = null;
        public boolean checkFor = false;

    }

}
