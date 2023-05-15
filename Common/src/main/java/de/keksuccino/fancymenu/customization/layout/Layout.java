package de.keksuccino.fancymenu.customization.layout;

import de.keksuccino.fancymenu.audio.SoundRegistry;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundRegistry;
import de.keksuccino.fancymenu.customization.background.SerializedMenuBackground;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.DeepScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.deep.DeepScreenCustomizationLayerRegistry;
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
import de.keksuccino.fancymenu.properties.PropertyContainer;
import de.keksuccino.fancymenu.properties.PropertyContainerSet;
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
    public List<SerializedElement> serializedDeepElements = new ArrayList<>();
    @Nullable
    public DeepScreenCustomizationLayer<?> deepScreenCustomizationLayer = null;

    //TODO compatibility layer für alte top-level customization sections wie addbackgroundanimation, backgroundtexture, etc. (siehe alte Customizationlayer klasse)

    //TODO compatibility layer für legacy items (image, animation, etc.) adden

    public PropertyContainerSet serialize() {

        PropertyContainerSet set = new PropertyContainerSet("fancymenu_layout");
        PropertyContainer meta = new PropertyContainer("layout-meta");

        set.putContainer(meta);

        meta.putProperty("identifier", this.menuIdentifier);
        meta.putProperty("render_custom_elements_behind_vanilla", "" + this.renderElementsBehindVanilla);

        meta.putProperty("randommode", "" + this.randomMode);
        meta.putProperty("randomgroup", this.randomGroup);
        meta.putProperty("randomonlyfirsttime", "" + this.randomOnlyFirstTime);

        if (!this.universalLayoutMenuWhitelist.isEmpty()) {
            String wl = "";
            for (String s : this.universalLayoutMenuWhitelist) {
                wl += s + ";";
            }
            meta.putProperty("universal_layout_whitelist", wl);
        }
        if (!this.universalLayoutMenuBlacklist.isEmpty()) {
            String bl = "";
            for (String s : this.universalLayoutMenuBlacklist) {
                bl += s + ";";
            }
            meta.putProperty("universal_layout_blacklist", bl);
        }

        if (this.customMenuTitle != null) {
            meta.putProperty("custom_menu_title", this.customMenuTitle);
        }

        if (this.overrideMenuWith != null) {
            PropertyContainer sec = new PropertyContainer("customization");
            sec.putProperty("action", "overridemenu");
            sec.putProperty("identifier", this.overrideMenuWith);
            set.putContainer(sec);
        }

        if (this.forcedScale != -1F) {
            PropertyContainer ps = new PropertyContainer("customization");
            ps.putProperty("action", "setscale");
            ps.putProperty("scale", "" + this.forcedScale);
            set.putContainer(ps);
        }

        if ((this.autoScalingWidth != 0) && (this.autoScalingHeight != 0)) {
            PropertyContainer ps = new PropertyContainer("customization");
            ps.putProperty("action", "autoscale");
            ps.putProperty("basewidth", "" + this.autoScalingWidth);
            ps.putProperty("baseheight", "" + this.autoScalingHeight);
            set.putContainer(ps);
        }

        if (this.menuBackground != null) {
            SerializedMenuBackground serializedMenuBackground = this.menuBackground.builder.serializedBackgroundInternal(this.menuBackground);
            if (serializedMenuBackground != null) {
                set.putContainer(serializedMenuBackground);
            }
        }

        if (this.openAudio != null) {
            PropertyContainer ps = new PropertyContainer("customization");
            ps.putProperty("action", "setopenaudio");
            ps.putProperty("path", this.openAudio);
            set.putContainer(ps);
        }

        if (this.closeAudio != null) {
            PropertyContainer ps = new PropertyContainer("customization");
            ps.putProperty("action", "setcloseaudio");
            ps.putProperty("path", this.closeAudio);
            set.putContainer(ps);
        }

        //Background Options Section
        PropertyContainer s = new PropertyContainer("customization");
        s.putProperty("action", "backgroundoptions");
        s.putProperty("keepaspectratio", "" + this.keepBackgroundAspectRatio);
        set.putContainer(s);

        this.layoutWideLoadingRequirementContainer.serializeContainerToExistingPropertiesSection(meta);

        this.serializedElements.forEach(set::putContainer);
        this.serializedVanillaButtonElements.forEach(set::putContainer);
        this.serializedDeepElements.forEach(set::putContainer);

        return set;

    }

    @Nullable
    public static Layout deserialize(@NotNull PropertyContainerSet serialized, @Nullable File layoutFile) {

        if (serialized.getType().equalsIgnoreCase("menu") || serialized.getType().equalsIgnoreCase("fancymenu_layout")) {

            Layout layout = new Layout();
            layout.layoutFile = layoutFile;

            List<PropertyContainer> metaList = serialized.getSectionsOfType("layout-meta");
            if (metaList.isEmpty()) {
                metaList = serialized.getSectionsOfType("customization-meta");
            }
            if (!metaList.isEmpty()) {

                PropertyContainer meta = metaList.get(0);

                layout.setMenuIdentifier(meta.getValue("identifier"));

                String defaultRandomLayoutGroup = "-100397";
                String randomMode = meta.getValue("randommode");
                if ((randomMode != null) && randomMode.equalsIgnoreCase("true")) {
                    layout.randomMode = true;
                    layout.randomGroup = meta.getValue("randomgroup");
                    if (layout.randomGroup == null) {
                        layout.randomGroup = defaultRandomLayoutGroup;
                    }
                    String randomOnlyFirstTime = meta.getValue("randomonlyfirsttime");
                    if ((randomOnlyFirstTime != null) && randomOnlyFirstTime.equalsIgnoreCase("true")) {
                        layout.randomOnlyFirstTime = true;
                    }
                }

                layout.customMenuTitle = meta.getValue("custom_menu_title");

                layout.layoutWideLoadingRequirementContainer = LoadingRequirementContainer.deserializeRequirementContainer(meta);

                String renderBehindVanilla = meta.getValue("render_custom_elements_behind_vanilla");
                if (renderBehindVanilla == null) {
                    String legacyRenderingOrder = meta.getValue("renderorder");
                    if ((legacyRenderingOrder != null) && legacyRenderingOrder.equals("background")) {
                        renderBehindVanilla = "true";
                    }
                }
                if ((renderBehindVanilla != null) && renderBehindVanilla.equals("true")) {
                    layout.renderElementsBehindVanilla = true;
                }

                if (layout.isUniversalLayout()) {
                    String whitelistRaw = meta.getValue("universal_layout_whitelist");
                    String blacklistRaw = meta.getValue("universal_layout_blacklist");
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
            for (PropertyContainer sec : serialized.getSectionsOfType("vanilla_button")) {
                SerializedElement serializedVanilla = convertSectionToElement(sec);
                serializedVanilla.setType("vanilla_button");
                layout.serializedVanillaButtonElements.add(serializedVanilla);
            }
            //Handle legacy vanilla button customizations
            layout.serializedVanillaButtonElements.addAll(convertLegacyVanillaButtonCustomizations(serialized));

            //Handle normal elements
            for (PropertyContainer sec : ListUtils.mergeLists(serialized.getSectionsOfType("element"), serialized.getSectionsOfType("customization"))) {
                String elementType = sec.getValue("element_type");
                if (elementType == null) {
                    elementType = sec.getValue("action");
                }
                if (elementType != null) {
                    elementType = elementType.replace("custom_layout_element:", "");
                    if (ElementRegistry.hasBuilder(elementType)) {
                        SerializedElement e = convertSectionToElement(sec);
                        e.putProperty("element_type", elementType);
                        layout.serializedElements.add(e);
                    }
                }
            }

            //Handle deep elements
            layout.deepScreenCustomizationLayer = ((layout.menuIdentifier != null) && !layout.isUniversalLayout()) ? DeepScreenCustomizationLayerRegistry.getLayer(layout.menuIdentifier) : null;
            if (layout.deepScreenCustomizationLayer != null) {
                for (PropertyContainer sec : ListUtils.mergeLists(serialized.getSectionsOfType("deep_element"), serialized.getSectionsOfType("customization"))) {
                    String elementType = sec.getValue("element_type");
                    if (elementType == null) {
                        elementType = sec.getValue("action");
                    }
                    if (elementType != null) {
                        elementType = elementType.replace("deep_customization_element:", "");
                        if (layout.deepScreenCustomizationLayer.hasBuilder(elementType)) {
                            SerializedElement e = convertSectionToElement(sec);
                            e.putProperty("element_type", elementType);
                            layout.serializedDeepElements.add(e);
                        }
                    }
                }
            }

            //Handle menu backgrounds
            List<PropertyContainer> menuBackgroundSections = serialized.getSectionsOfType("menu_background");
            if (!menuBackgroundSections.isEmpty()) {
                PropertyContainer menuBack = menuBackgroundSections.get(0);
                String backgroundIdentifier = menuBack.getValue("background_type");
                if (backgroundIdentifier != null) {
                    MenuBackgroundBuilder<?> builder = MenuBackgroundRegistry.getBuilder(backgroundIdentifier);
                    if (builder != null) {
                        layout.menuBackground = builder.deserializeBackgroundInternal(convertSectionToBackground(menuBack));
                    }
                }
            }

            //Handle everything else
            for (PropertyContainer sec : serialized.getSectionsOfType("customization")) {

                String action = sec.getValue("action");

                if ((action != null) && action.equals("setscale")) {
                    String scale = sec.getValue("scale");
                    if ((scale != null) && (MathUtils.isInteger(scale.replace(" ", "")) || MathUtils.isDouble(scale.replace(" ", "")))) {
                        int newscale = (int) Double.parseDouble(scale.replace(" ", ""));
                        if (newscale <= 0) {
                            newscale = 1;
                        }
                        layout.forcedScale = newscale;
                    }
                }

                if ((action != null) && action.equals("autoscale")) {
                    String baseWidth = sec.getValue("basewidth");
                    if (MathUtils.isInteger(baseWidth)) {
                        layout.autoScalingWidth = Integer.parseInt(baseWidth);
                    }
                    String baseHeight = sec.getValue("baseheight");
                    if (MathUtils.isInteger(baseHeight)) {
                        layout.autoScalingHeight = Integer.parseInt(baseHeight);
                    }
                }

                if ((action != null) && action.equalsIgnoreCase("backgroundoptions")) {
                    String keepAspect = sec.getValue("keepaspectratio");
                    if ((keepAspect != null) && keepAspect.equalsIgnoreCase("true")) {
                        layout.keepBackgroundAspectRatio = true;
                    }
                }

                if ((action != null) && action.equalsIgnoreCase("setcloseaudio")) {
                    String path = AbstractElement.fixBackslashPath(sec.getValue("path"));
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
                        String path = AbstractElement.fixBackslashPath(sec.getValue("path"));
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
            String elementType = serialized.getValue("element_type");
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
    public List<AbstractDeepElement> buildDeepElementInstances() {
        List<AbstractDeepElement> elements = new ArrayList<>();
        if (this.deepScreenCustomizationLayer != null) {
            for (SerializedElement serialized : this.serializedDeepElements) {
                String elementType = serialized.getValue("element_type");
                if (elementType != null) {
                    DeepElementBuilder<?, ?, ?> builder = this.deepScreenCustomizationLayer.getBuilder(elementType);
                    if (builder != null) {
                        AbstractDeepElement element = builder.deserializeElementInternal(serialized);
                        if (element != null) {
                            elements.add(element);
                        }
                    }
                }
            }
        }
        return elements;
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

        Layout copy = new Layout();

        copy.menuIdentifier = this.menuIdentifier;
        copy.layoutFile = this.layoutFile;

        copy.overrideMenuWith = this.overrideMenuWith;
        copy.menuBackground = this.menuBackground;
        copy.keepBackgroundAspectRatio = this.keepBackgroundAspectRatio;
        copy.openAudio = this.openAudio;
        copy.closeAudio = this.closeAudio;
        copy.renderElementsBehindVanilla = this.renderElementsBehindVanilla;
        copy.randomMode = this.randomMode;
        copy.randomGroup = this.randomGroup;
        copy.randomOnlyFirstTime = this.randomOnlyFirstTime;
        copy.forcedScale = this.forcedScale;
        copy.autoScalingWidth = this.autoScalingWidth;
        copy.autoScalingHeight = this.autoScalingHeight;
        copy.customMenuTitle = this.customMenuTitle;
        copy.universalLayoutMenuWhitelist = this.universalLayoutMenuWhitelist;
        copy.universalLayoutMenuBlacklist = this.universalLayoutMenuBlacklist;

        if (this.layoutWideLoadingRequirementContainer != null) {
            PropertyContainer loadingRequirementsSec = new PropertyContainer("loading_requirements");
            this.layoutWideLoadingRequirementContainer.serializeContainerToExistingPropertiesSection(loadingRequirementsSec);
            copy.layoutWideLoadingRequirementContainer = LoadingRequirementContainer.deserializeRequirementContainer(loadingRequirementsSec);
        }

        for (SerializedElement e : this.serializedElements) {
            copy.serializedElements.add(convertSectionToElement(e));
        }
        for (SerializedElement e : this.serializedVanillaButtonElements) {
            SerializedElement serializedVanilla = convertSectionToElement(e);
            serializedVanilla.setType("vanilla_button");
            copy.serializedVanillaButtonElements.add(serializedVanilla);
        }
        for (SerializedElement e : this.serializedDeepElements) {
            SerializedElement serializedDeep = convertSectionToElement(e);
            serializedDeep.setType("deep_element");
            copy.serializedDeepElements.add(serializedDeep);
        }

        copy.deepScreenCustomizationLayer = this.deepScreenCustomizationLayer;

        return copy;

    }

    @Legacy("This converts old button customization sections and should get removed in the future.")
    @NotNull
    protected static List<SerializedElement> convertLegacyVanillaButtonCustomizations(PropertyContainerSet layout) {

        Map<String, VanillaButtonElement> elements = new HashMap<>();

        for (PropertyContainer sec : layout.getSectionsOfType("customization")) {
            VanillaButtonElement element = VanillaButtonElementBuilder.INSTANCE.buildDefaultInstance();
            String action = sec.getValue("action");
            String identifier = sec.getValue("identifier");
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
                    element.hoverSound = sec.getValue("path");
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
                    element.hoverLabel = sec.getValue("label");
                    if (element.hoverLabel != null) {
                        addElement = true;
                    }
                }

                if (action.equalsIgnoreCase("renamebutton") || action.equalsIgnoreCase("setbuttonlabel")) {
                    element.label = sec.getValue("value");
                    if (element.label != null) {
                        addElement = true;
                    }
                }

                if (action.equalsIgnoreCase("movebutton")) {

                    String x = sec.getValue("x");
                    String y = sec.getValue("y");
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

                    String anchor = sec.getValue("orientation");
                    if (anchor != null) {
                        element.anchorPoint = ElementAnchorPoints.getAnchorPointByName(anchor);
                        if (element.anchorPoint == null) {
                            element.anchorPoint = ElementAnchorPoints.VANILLA;
                        }
                        if (element.anchorPoint != ElementAnchorPoints.VANILLA) {
                            addElement = true;
                        }
                    }

                    element.anchorPointElementIdentifier = sec.getValue("orientation_element");

                }

                if (action.equalsIgnoreCase("setbuttondescription")) {
                    element.tooltip = sec.getValue("description");
                    if (element.tooltip != null) {
                        addElement = true;
                    }
                }

                if (action.equalsIgnoreCase("hidebuttonfor")) {
                    String seconds = sec.getValue("seconds");
                    String onlyFirstTime = sec.getValue("onlyfirsttime");
                    String fadeIn = sec.getValue("fadein");
                    String fadeInSpeed = sec.getValue("fadeinspeed");
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
                    String loopBackAnimations = sec.getValue("loopbackgroundanimations");
                    if ((loopBackAnimations != null) && loopBackAnimations.equalsIgnoreCase("false")) {
                        element.loopBackgroundAnimations = false;
                    }
                    String restartBackAnimationsOnHover = sec.getValue("restartbackgroundanimations");
                    if ((restartBackAnimationsOnHover != null) && restartBackAnimationsOnHover.equalsIgnoreCase("false")) {
                        element.restartBackgroundAnimationsOnHover = false;
                    }
                    element.backgroundTextureNormal = sec.getValue("backgroundnormal");
                    element.backgroundTextureHover = sec.getValue("backgroundhovered");
                    element.backgroundAnimationNormal = sec.getValue("backgroundanimationnormal");
                    element.backgroundAnimationHover = sec.getValue("backgroundanimationhovered");
                    addElement = true;
                }

                if (action.equalsIgnoreCase("setbuttonclicksound")) {
                    element.clickSound = sec.getValue("path");
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
                    String clicks = sec.getValue("clicks");
                    if ((clicks != null) && (MathUtils.isInteger(clicks))) {
                        element.automatedButtonClicks = Integer.parseInt(clicks);
                        addElement = true;
                    }
                }

                if (addElement) {
                    if (!elements.containsKey(identifier)) {
                        elements.put(identifier, element);
                    } else {
                        VanillaButtonElement stack = VanillaButtonElementBuilder.INSTANCE.stackElementsInternal(VanillaButtonElementBuilder.INSTANCE.buildDefaultInstance(), elements.get(identifier), element);
                        if (stack != null) {
                            elements.put(identifier, stack);
                        }
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
