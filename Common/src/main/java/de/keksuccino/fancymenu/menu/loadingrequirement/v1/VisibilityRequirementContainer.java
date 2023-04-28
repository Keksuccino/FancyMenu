
package de.keksuccino.fancymenu.menu.loadingrequirement.v1;

import de.keksuccino.fancymenu.api.visibilityrequirements.VisibilityRequirement;
import de.keksuccino.fancymenu.api.visibilityrequirements.VisibilityRequirementRegistry;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirementRegistry;
import de.keksuccino.konkrete.properties.PropertiesSection;

import java.util.*;

@Deprecated
public class VisibilityRequirementContainer {

    public boolean vrCheckForSingleplayer = false; // //
    public boolean vrShowIfSingleplayer = false;

    public boolean vrCheckForMultiplayer = false; // //
    public boolean vrShowIfMultiplayer = false;

    public boolean vrCheckForWindowWidth = false; // //
    public boolean vrShowIfWindowWidth = false;
    public String vrWindowWidth = null;

    public boolean vrCheckForWindowHeight = false; // //
    public boolean vrShowIfWindowHeight = false;
    public String vrWindowHeight = null;

    public boolean vrCheckForWindowWidthBiggerThan = false; // //
    public boolean vrShowIfWindowWidthBiggerThan = false;
    public String vrWindowWidthBiggerThan = null;

    public boolean vrCheckForWindowHeightBiggerThan = false; // //
    public boolean vrShowIfWindowHeightBiggerThan = false;
    public String vrWindowHeightBiggerThan = null;

    public boolean vrCheckForButtonHovered = false; // //
    public boolean vrShowIfButtonHovered = false;
    public String vrButtonHovered = null;

    public boolean vrCheckForWorldLoaded = false; // //
    public boolean vrShowIfWorldLoaded = false;

    public boolean vrCheckForLanguage = false; // //
    public boolean vrShowIfLanguage = false;
    public String vrLanguage = null;

    public boolean vrCheckForFullscreen = false; // //
    public boolean vrShowIfFullscreen = false;

    public boolean vrCheckForOsWindows = false; // //
    public boolean vrShowIfOsWindows = false;

    public boolean vrCheckForOsMac = false; // //
    public boolean vrShowIfOsMac = false;

    public boolean vrCheckForOsLinux = false; // //
    public boolean vrShowIfOsLinux = false;

    public boolean vrCheckForModLoaded = false; // //
    public boolean vrShowIfModLoaded = false;
    public String vrModLoaded = null;

    public boolean vrCheckForServerOnline = false; // //
    public boolean vrShowIfServerOnline = false;
    public String vrServerOnline = null;

    public boolean vrCheckForGuiScale = false; // //
    public boolean vrShowIfGuiScale = false;
    public String vrGuiScale = null;

    public Map<String, RequirementPackage> customRequirements = new LinkedHashMap<>();

    @Deprecated
    public VisibilityRequirementContainer(PropertiesSection properties) {

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
            this.vrModLoaded = modID;
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
            this.vrGuiScale = guiScale;
        }

        //CUSTOM VISIBILITY REQUIREMENTS (API)
        this.customRequirements.clear();
        for (VisibilityRequirement r : VisibilityRequirementRegistry.getRequirements()) {
            if ((r != null) && (r.getIdentifier() != null)) {
                String stringShowIf = properties.getEntryValue("vr_custom:showif:" + r.getIdentifier());
                if (stringShowIf != null) {
                    RequirementPackage p = new RequirementPackage();
                    p.requirement = r;
                    if (stringShowIf.equalsIgnoreCase("true")) {
                        p.showIf = true;
                    }
                    p.value = properties.getEntryValue("vr_custom:value:" + r.getIdentifier());
                    p.checkFor = true;
                    this.customRequirements.put(r.getIdentifier(), p);
                }
            }
        }

    }

    public static class RequirementPackage {

        public VisibilityRequirement requirement;
        public boolean showIf = false;
        public String value = null;
        public boolean checkFor = false;

    }

}
