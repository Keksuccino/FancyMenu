package de.keksuccino.fancymenu.customization.layout;

import de.keksuccino.fancymenu.audio.SoundRegistry;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundRegistry;
import de.keksuccino.fancymenu.customization.background.SerializedMenuBackground;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.vanilla.VanillaButtonElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanilla.VanillaButtonElementBuilder;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.v2.PlaceholderParser;
import de.keksuccino.fancymenu.misc.Legacy;
import de.keksuccino.fancymenu.utils.ListUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Layout extends LayoutBase {

    public String menuIdentifier;
    public File layoutFile;

    public boolean renderElementsBehindVanilla = false;
    public boolean randomMode = false;
    public String randomGroup = "1";
    public boolean randomOnlyFirstTime = false;
    public List<String> universalLayoutMenuWhitelist = new ArrayList<>();
    public List<String> universalLayoutMenuBlacklist = new ArrayList<>();
    public LoadingRequirementContainer layoutWideLoadingRequirementContainer = new LoadingRequirementContainer();
    public List<SerializedElement> serializedElements = new ArrayList<>();
    public List<SerializedElement> serializedVanillaButtonElements = new ArrayList<>();

    //TODO compatibility layer für alte top-level customization sections wie addbackgroundanimation, backgroundtexture, etc. (siehe alte Customizationlayer klasse)

    public PropertiesSet serialize() {

        PropertiesSet set = new PropertiesSet("fancymenu_layout");
        PropertiesSection meta = new PropertiesSection("layout-meta");

        set.addProperties(meta);

        meta.addEntry("identifier", this.menuIdentifier);
        meta.addEntry("render_custom_elements_behind_vanilla", "" + this.renderElementsBehindVanilla);

        meta.addEntry("randommode", "" + this.randomMode);
        meta.addEntry("randomgroup", this.randomGroup);
        meta.addEntry("randomonlyfirsttime", "" + this.randomOnlyFirstTime);

        if (!this.universalLayoutMenuWhitelist.isEmpty()) {
            String wl = "";
            for (String s : this.universalLayoutMenuWhitelist) {
                wl += s + ";";
            }
            meta.addEntry("universal_layout_whitelist", wl);
        }
        if (!this.universalLayoutMenuBlacklist.isEmpty()) {
            String bl = "";
            for (String s : this.universalLayoutMenuBlacklist) {
                bl += s + ";";
            }
            meta.addEntry("universal_layout_blacklist", bl);
        }

        if (this.customMenuTitle != null) {
            meta.addEntry("custom_menu_title", this.customMenuTitle);
        }

        if (this.overrideMenuWith != null) {
            PropertiesSection sec = new PropertiesSection("customization");
            sec.addEntry("action", "overridemenu");
            sec.addEntry("identifier", this.overrideMenuWith);
            set.addProperties(sec);
        }

        if (this.forcedScale != -1F) {
            PropertiesSection ps = new PropertiesSection("customization");
            ps.addEntry("action", "setscale");
            ps.addEntry("scale", "" + this.forcedScale);
            set.addProperties(ps);
        }

        if ((this.autoScalingWidth != 0) && (this.autoScalingHeight != 0)) {
            PropertiesSection ps = new PropertiesSection("customization");
            ps.addEntry("action", "autoscale");
            ps.addEntry("basewidth", "" + this.autoScalingWidth);
            ps.addEntry("baseheight", "" + this.autoScalingHeight);
            set.addProperties(ps);
        }

        if (this.menuBackground != null) {
            SerializedMenuBackground serializedMenuBackground = this.menuBackground.builder.serializedBackgroundInternal(this.menuBackground);
            if (serializedMenuBackground != null) {
                set.addProperties(serializedMenuBackground);
            }
        }

        if (this.openAudio != null) {
            PropertiesSection ps = new PropertiesSection("customization");
            ps.addEntry("action", "setopenaudio");
            ps.addEntry("path", this.openAudio);
            set.addProperties(ps);
        }

        if (this.closeAudio != null) {
            PropertiesSection ps = new PropertiesSection("customization");
            ps.addEntry("action", "setcloseaudio");
            ps.addEntry("path", this.closeAudio);
            set.addProperties(ps);
        }

        //Background Options Section
        PropertiesSection s = new PropertiesSection("customization");
        s.addEntry("action", "backgroundoptions");
        s.addEntry("keepaspectratio", "" + this.keepBackgroundAspectRatio);
        set.addProperties(s);

        this.layoutWideLoadingRequirementContainer.serializeContainerToExistingPropertiesSection(meta);

        this.serializedElements.forEach(set::addProperties);
        this.serializedVanillaButtonElements.forEach(set::addProperties);

        return set;

    }

    @Nullable
    public static Layout deserialize(@NotNull PropertiesSet serialized, @Nullable File layoutFile) {

        if (serialized.getPropertiesType().equalsIgnoreCase("menu") || serialized.getPropertiesType().equalsIgnoreCase("fancymenu_layout")) {

            Layout layout = new Layout();
            layout.layoutFile = layoutFile;

            List<PropertiesSection> metaList = serialized.getPropertiesOfType("layout-meta");
            if (metaList.isEmpty()) {
                metaList = serialized.getPropertiesOfType("customization-meta");
            }
            if (!metaList.isEmpty()) {

                PropertiesSection meta = metaList.get(0);

                layout.setMenuIdentifier(meta.getEntryValue("identifier"));

                String defaultRandomLayoutGroup = "-100397";
                String randomMode = meta.getEntryValue("randommode");
                if ((randomMode != null) && randomMode.equalsIgnoreCase("true")) {
                    layout.randomMode = true;
                    layout.randomGroup = meta.getEntryValue("randomgroup");
                    if (layout.randomGroup == null) {
                        layout.randomGroup = defaultRandomLayoutGroup;
                    }
                    String randomOnlyFirstTime = meta.getEntryValue("randomonlyfirsttime");
                    if ((randomOnlyFirstTime != null) && randomOnlyFirstTime.equalsIgnoreCase("true")) {
                        layout.randomOnlyFirstTime = true;
                    }
                }

                layout.customMenuTitle = meta.getEntryValue("custom_menu_title");

                layout.layoutWideLoadingRequirementContainer = LoadingRequirementContainer.deserializeRequirementContainer(meta);

                String renderBehindVanilla = meta.getEntryValue("render_custom_elements_behind_vanilla");
                if (renderBehindVanilla == null) {
                    String legacyRenderingOrder = meta.getEntryValue("renderorder");
                    if ((legacyRenderingOrder != null) && legacyRenderingOrder.equals("background")) {
                        renderBehindVanilla = "true";
                    }
                }
                if ((renderBehindVanilla != null) && renderBehindVanilla.equals("true")) {
                    layout.renderElementsBehindVanilla = true;
                }

                if (layout.isUniversalLayout()) {
                    String whitelistRaw = meta.getEntryValue("universal_layout_whitelist");
                    String blacklistRaw = meta.getEntryValue("universal_layout_blacklist");
                    if ((whitelistRaw != null) && whitelistRaw.contains(";")) {
                        for (String s : whitelistRaw.split(";")) {
                            if (s.length() > 0) {
                                layout.universalLayoutMenuWhitelist.add(ScreenCustomization.findValidMenuIdentifierFor(s));
                            }
                        }
                    }
                    if ((blacklistRaw != null) && blacklistRaw.contains(";")) {
                        for (String s : blacklistRaw.split(";")) {
                            if (s.length() > 0) {
                                layout.universalLayoutMenuBlacklist.add(ScreenCustomization.findValidMenuIdentifierFor(s));
                            }
                        }
                    }
                }

            }

            //Handle vanilla button elements
            for (PropertiesSection sec : serialized.getPropertiesOfType("vanilla_button")) {
                layout.serializedVanillaButtonElements.add(convertSectionToElement(sec, "vanilla_button"));
            }
            //Handle legacy vanilla button customizations
            layout.serializedVanillaButtonElements.addAll(convertLegacyVanillaButtonCustomizations(serialized));

            //Handle normal elements
            for (PropertiesSection sec : ListUtils.mergeLists(serialized.getPropertiesOfType("element"), serialized.getPropertiesOfType("customization"))) {
                String elementType = sec.getEntryValue("element_type");
                if (elementType == null) {
                    elementType = sec.getEntryValue("action");
                }
                if (elementType != null) {
                    elementType = elementType.replace("custom_layout_element:", "");
                    if (ElementRegistry.hasBuilder(elementType)) {
                        SerializedElement e = convertSectionToElement(sec);
                        e.addEntry("element_type", elementType);
                        layout.serializedElements.add(e);
                    }
                }
            }

            //Handle menu backgrounds
            List<PropertiesSection> menuBackgroundSections = serialized.getPropertiesOfType("menu_background");
            if (!menuBackgroundSections.isEmpty()) {
                PropertiesSection menuBack = menuBackgroundSections.get(0);
                String backgroundIdentifier = menuBack.getEntryValue("background_type");
                if (backgroundIdentifier != null) {
                    MenuBackgroundBuilder<?> builder = MenuBackgroundRegistry.getBuilder(backgroundIdentifier);
                    if (builder != null) {
                        layout.menuBackground = builder.deserializeBackground(convertSectionToBackground(menuBack));
                    }
                }
            }

            //Handle everything else
            for (PropertiesSection sec : serialized.getPropertiesOfType("customization")) {

                String action = sec.getEntryValue("action");

                if ((action != null) && action.equals("setscale")) {
                    String scale = sec.getEntryValue("scale");
                    if ((scale != null) && (MathUtils.isInteger(scale.replace(" ", "")) || MathUtils.isDouble(scale.replace(" ", "")))) {
                        int newscale = (int) Double.parseDouble(scale.replace(" ", ""));
                        if (newscale <= 0) {
                            newscale = 1;
                        }
                        layout.forcedScale = newscale;
                    }
                }

                if ((action != null) && action.equals("autoscale")) {
                    String baseWidth = sec.getEntryValue("basewidth");
                    if (MathUtils.isInteger(baseWidth)) {
                        layout.autoScalingWidth = Integer.parseInt(baseWidth);
                    }
                    String baseHeight = sec.getEntryValue("baseheight");
                    if (MathUtils.isInteger(baseHeight)) {
                        layout.autoScalingHeight = Integer.parseInt(baseHeight);
                    }
                }

                if ((action != null) && action.equalsIgnoreCase("backgroundoptions")) {
                    String keepAspect = sec.getEntryValue("keepaspectratio");
                    if ((keepAspect != null) && keepAspect.equalsIgnoreCase("true")) {
                        layout.keepBackgroundAspectRatio = true;
                    }
                }

                if ((action != null) && action.equalsIgnoreCase("setcloseaudio")) {
                    String path = AbstractElement.fixBackslashPath(sec.getEntryValue("path"));
                    if (path != null) {
                        File f = new File(path);
                        if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
                            path = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + path;
                            f = new File(path);
                        }
                        if (f.isFile() && f.exists() && f.getName().endsWith(".wav")) {
                            try {
                                layout.closeAudio = "closesound_" + path + Files.size(f.toPath());
                                SoundRegistry.registerSound(layout.closeAudio, path);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }

                if ((action != null) && action.equalsIgnoreCase("setopenaudio")) {
                    if (ScreenCustomization.isNewMenu()) {
                        String path = AbstractElement.fixBackslashPath(sec.getEntryValue("path"));
                        if (path != null) {
                            File f = new File(path);
                            if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
                                path = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + path;
                                f = new File(path);
                            }
                            if (f.isFile() && f.exists() && f.getName().endsWith(".wav")) {
                                try {
                                    layout.openAudio = "opensound_" + path + Files.size(f.toPath());
                                    SoundRegistry.registerSound(layout.openAudio, path);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                }

            }

            return layout;

        }

        return null;

    }

    public void setMenuIdentifier(String identifier) {
        if (identifier != null) {
            this.menuIdentifier = ScreenCustomization.findValidMenuIdentifierFor(identifier);
        }
    }

    public boolean isUniversalLayout() {
        return (this.menuIdentifier != null) && this.menuIdentifier.equals("%fancymenu:universal_layout%");
    }

    public boolean layoutWideLoadingRequirementsMet() {
        if (this.layoutWideLoadingRequirementContainer != null) {
            return this.layoutWideLoadingRequirementContainer.requirementsMet();
        }
        return true;
    }

    @NotNull
    public OrderedElementCollection buildElementInstances() {
        OrderedElementCollection collection = new OrderedElementCollection();
        for (SerializedElement serialized : this.serializedElements) {
            String elementType = serialized.getEntryValue("element_type");
            if (elementType != null) {
                ElementBuilder<?, ?> builder = ElementRegistry.getBuilder(elementType);
                if (builder != null) {
                    AbstractElement element = builder.deserializeElementInternal(serialized);
                    if (element != null) {
                        if (this.renderElementsBehindVanilla) {
                            collection.backgroundElements.add(element);
                        } else {
                            collection.foregroundElements.add(element);
                        }
                    }
                }
            }
        }
        return collection;
    }

    @NotNull
    public List<VanillaButtonElement> buildVanillaButtonElementInstances() {
        List<VanillaButtonElement> elements = new ArrayList<>();
        for (SerializedElement serialized : this.serializedVanillaButtonElements) {
            VanillaButtonElement element = VanillaButtonElementBuilder.INSTANCE.deserializeElementInternal(serialized);
            if (element != null) {
                elements.add(element);
            }
        }
        return elements;
    }

    @NotNull
    public Layout copy() {

        Layout layout = new Layout();

        layout.menuIdentifier = this.menuIdentifier;
        layout.layoutFile = this.layoutFile;

        layout.overrideMenuWith = this.overrideMenuWith;
        layout.menuBackground = this.menuBackground;
        layout.keepBackgroundAspectRatio = this.keepBackgroundAspectRatio;
        layout.openAudio = this.openAudio;
        layout.closeAudio = this.closeAudio;
        layout.renderElementsBehindVanilla = this.renderElementsBehindVanilla;
        layout.randomMode = this.randomMode;
        layout.randomGroup = this.randomGroup;
        layout.randomOnlyFirstTime = this.randomOnlyFirstTime;
        layout.forcedScale = this.forcedScale;
        layout.autoScalingWidth = this.autoScalingWidth;
        layout.autoScalingHeight = this.autoScalingHeight;
        layout.customMenuTitle = this.customMenuTitle;
        layout.universalLayoutMenuWhitelist = this.universalLayoutMenuWhitelist;
        layout.universalLayoutMenuBlacklist = this.universalLayoutMenuBlacklist;

        if (this.layoutWideLoadingRequirementContainer != null) {
            PropertiesSection loadingRequirementsSec = new PropertiesSection("loading_requirements");
            this.layoutWideLoadingRequirementContainer.serializeContainerToExistingPropertiesSection(loadingRequirementsSec);
            layout.layoutWideLoadingRequirementContainer = LoadingRequirementContainer.deserializeRequirementContainer(loadingRequirementsSec);
        }

        for (SerializedElement e : this.serializedElements) {
            layout.serializedElements.add(convertSectionToElement(e));
        }
        for (SerializedElement e : this.serializedVanillaButtonElements) {
            layout.serializedVanillaButtonElements.add(convertSectionToElement(e, "vanilla_button"));
        }

        return layout;

    }

    @Legacy("This converts old button customization sections and should get removed in the future.")
    @NotNull
    protected static List<SerializedElement> convertLegacyVanillaButtonCustomizations(PropertiesSet layout) {

        Map<String, VanillaButtonElement> elements = new HashMap<>();

        for (PropertiesSection sec : layout.getPropertiesOfType("customization")) {
            VanillaButtonElement element = VanillaButtonElementBuilder.INSTANCE.buildDefaultInstance();
            String action = sec.getEntryValue("action");
            String identifier = sec.getEntryValue("identifier");
            if ((identifier != null) && identifier.startsWith("%id=")) {
                identifier = identifier.replace("%id=", "");
                identifier = new StringBuilder(new StringBuilder(identifier).reverse().substring(1)).reverse().toString();
            } else {
                identifier = null;
            }
            if ((action != null) && (identifier != null)) {

                element.vanillaButtonIdentifier = identifier;

                boolean addElement = false;

                if (action.equalsIgnoreCase("addhoversound")) {
                    element.hoverSound = sec.getEntryValue("path");
                    if (element.hoverSound != null) {
                        File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(element.hoverSound));
                        if (f.exists() && f.isFile()) {
                            SoundRegistry.registerSound(element.hoverSound, element.hoverSound);
                            addElement = true;
                        } else {
                            element.hoverSound = null;
                        }
                    }
                }

                if (action.equalsIgnoreCase("sethoverlabel")) {
                    element.hoverLabel = sec.getEntryValue("label");
                    if (element.hoverLabel != null) {
                        addElement = true;
                    }
                }

                if (action.equalsIgnoreCase("renamebutton") || action.equalsIgnoreCase("setbuttonlabel")) {
                    element.label = sec.getEntryValue("value");
                    if (element.label != null) {
                        addElement = true;
                    }
                }

                if (action.equalsIgnoreCase("movebutton")) {

                    String x = sec.getEntryValue("x");
                    String y = sec.getEntryValue("y");
                    if (x != null) {
                        x = PlaceholderParser.replacePlaceholders(x);
                        if (MathUtils.isInteger(x)) {
                            element.baseX = Integer.parseInt(x);
                        }
                    }
                    if (y != null) {
                        y = PlaceholderParser.replacePlaceholders(y);
                        if (MathUtils.isInteger(y)) {
                            element.baseY = Integer.parseInt(y);
                        }
                    }

                    String anchor = sec.getEntryValue("orientation");
                    if (anchor != null) {
                        element.anchorPoint = ElementAnchorPoints.getAnchorPointByName(anchor);
                        if (element.anchorPoint == null) {
                            element.anchorPoint = ElementAnchorPoints.VANILLA;
                        }
                        if (element.anchorPoint != ElementAnchorPoints.VANILLA) {
                            addElement = true;
                        }
                    }

                    element.anchorPointElementIdentifier = sec.getEntryValue("orientation_element");

                }

                if (action.equalsIgnoreCase("setbuttondescription")) {
                    element.tooltip = sec.getEntryValue("description");
                    if (element.tooltip != null) {
                        addElement = true;
                    }
                }

                if (action.equalsIgnoreCase("hidebuttonfor")) {
                    String seconds = sec.getEntryValue("seconds");
                    String onlyFirstTime = sec.getEntryValue("onlyfirsttime");
                    String fadeIn = sec.getEntryValue("fadein");
                    String fadeInSpeed = sec.getEntryValue("fadeinspeed");
                    if ((onlyFirstTime != null) && onlyFirstTime.equalsIgnoreCase("true")) {
                        element.appearanceDelay = AbstractElement.AppearanceDelay.FIRST_TIME;
                    } else {
                        element.appearanceDelay = AbstractElement.AppearanceDelay.EVERY_TIME;
                    }
                    if ((seconds != null) && MathUtils.isFloat(seconds)) {
                        element.appearanceDelayInSeconds = Float.parseFloat(seconds);
                    }
                    if ((fadeIn != null) && fadeIn.equalsIgnoreCase("true")) {
                        if ((fadeInSpeed != null) && MathUtils.isFloat(fadeInSpeed)) {
                            element.fadeIn = true;
                            element.fadeInSpeed = Float.parseFloat(fadeInSpeed);
                        }
                    }
                    addElement = true;
                }

                if (action.equalsIgnoreCase("hidebutton")) {
                    element.vanillaButtonHidden = true;
                    addElement = true;
                }

                if (action.equalsIgnoreCase("setbuttontexture")) {
                    String loopBackAnimations = sec.getEntryValue("loopbackgroundanimations");
                    if ((loopBackAnimations != null) && loopBackAnimations.equalsIgnoreCase("false")) {
                        element.loopBackgroundAnimations = false;
                    }
                    String restartBackAnimationsOnHover = sec.getEntryValue("restartbackgroundanimations");
                    if ((restartBackAnimationsOnHover != null) && restartBackAnimationsOnHover.equalsIgnoreCase("false")) {
                        element.restartBackgroundAnimationsOnHover = false;
                    }
                    element.backgroundTextureNormal = sec.getEntryValue("backgroundnormal");
                    element.backgroundTextureHover = sec.getEntryValue("backgroundhovered");
                    element.backgroundAnimationNormal = sec.getEntryValue("backgroundanimationnormal");
                    element.backgroundAnimationHover = sec.getEntryValue("backgroundanimationhovered");
                    addElement = true;
                }

                if (action.equalsIgnoreCase("setbuttonclicksound")) {
                    element.clickSound = sec.getEntryValue("path");
                    if (element.clickSound != null) {
                        File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(element.clickSound));
                        if (f.exists() && f.isFile() && f.getPath().toLowerCase().endsWith(".wav")) {
                            SoundHandler.registerSound(f.getPath(), f.getPath());
                            addElement = true;
                        } else {
                            element.clickSound = null;
                        }
                    }
                }

                if (action.equalsIgnoreCase("vanilla_button_visibility_requirements")) {
                    element.loadingRequirementContainer = LoadingRequirementContainer.deserializeRequirementContainer(sec);
                    addElement = true;
                }

                if (action.equalsIgnoreCase("clickbutton")) {
                    String clicks = sec.getEntryValue("clicks");
                    if ((clicks != null) && (MathUtils.isInteger(clicks))) {
                        element.automatedButtonClicks = Integer.parseInt(clicks);
                        addElement = true;
                    }
                }

                if (addElement) {
                    if (!elements.containsKey(identifier)) {
                        elements.put(identifier, element);
                    } else {
                        elements.put(identifier, VanillaButtonElement.stackElements(elements.get(identifier), element));
                    }
                }

            }
        }

        List<SerializedElement> l = new ArrayList<>();
        for (VanillaButtonElement e : elements.values()) {
            SerializedElement serialized = VanillaButtonElementBuilder.INSTANCE.serializeElementInternal(e);
            if (serialized != null) {
                l.add(serialized);
            }
        }

        return l;

    }

    public static class OrderedElementCollection {

        public List<AbstractElement> foregroundElements = new ArrayList<>();
        public List<AbstractElement> backgroundElements = new ArrayList<>();

    }

}