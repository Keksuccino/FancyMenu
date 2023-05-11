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
import de.keksuccino.fancymenu.customization.element.elements.button.vanilla.VanillaButtonElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanilla.VanillaButtonElementBuilder;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSet;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Layout extends LayoutBase {

    public String menuIdentifier;
    public File layoutFile;

    public boolean renderCustomElementsBehindVanilla = false;
    public boolean randomMode = false;
    public String randomGroup = "1";
    public boolean randomOnlyFirstTime = false;
    public List<String> universalLayoutMenuWhitelist = new ArrayList<>();
    public List<String> universalLayoutMenuBlacklist = new ArrayList<>();
    public LoadingRequirementContainer layoutWideLoadingRequirementContainer = new LoadingRequirementContainer();
    public List<SerializedElement> serializedElements = new ArrayList<>();
    public List<SerializedElement> serializedVanillaButtonElements = new ArrayList<>();

    public PropertiesSet serialize() {

        PropertiesSet set = new PropertiesSet("fancymenu_layout");
        PropertiesSection meta = new PropertiesSection("layout-meta");

        set.addProperties(meta);

        meta.addEntry("identifier", this.menuIdentifier);
        meta.addEntry("render_custom_elements_behind_vanilla", "" + this.renderCustomElementsBehindVanilla);

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
                    layout.renderCustomElementsBehindVanilla = true;
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

            //TODO add compatibility layer to load button customizations in old format
            //Handle vanilla buttons
            for (PropertiesSection sec : serialized.getPropertiesOfType("vanilla_button")) {
                layout.serializedVanillaButtonElements.add(convertSectionToElement(sec, "vanilla_button"));
            }

            //Handle elements
            List<PropertiesSection> potentialSerializedElements = new ArrayList<>(serialized.getPropertiesOfType("element"));
            potentialSerializedElements.addAll(serialized.getPropertiesOfType("customization"));
            for (PropertiesSection sec : potentialSerializedElements) {
                String elementType = sec.getEntryValue("element_type");
                if (elementType == null) {
                    elementType = sec.getEntryValue("action");
                }
                if (elementType != null) {
                    elementType = elementType.replace("custom_layout_element:", "");
                    if (ElementRegistry.hasBuilder(elementType)) {
                        layout.serializedElements.add(convertSectionToElement(sec));
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

            //Handle everything that's not elements or other stuff
            for (PropertiesSection sec : serialized.getPropertiesOfType("customization")) {

                String action = sec.getEntryValue("action");

                if ((action != null) && action.startsWith("add_element:")) {
                    layout.serializedElements.add(convertSectionToElement(sec));
                    continue;
                }

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
            if (elementType == null) {
                elementType = serialized.getEntryValue("action");
            }
            if (elementType != null) {
                elementType = elementType.replace("custom_layout_element:", "");
                ElementBuilder<?, ?> builder = ElementRegistry.getBuilder(elementType);
                if (builder != null) {
                    AbstractElement element = builder.deserializeElementInternal(serialized);
                    if (element != null) {
                        if (this.renderCustomElementsBehindVanilla) {
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
        layout.renderCustomElementsBehindVanilla = this.renderCustomElementsBehindVanilla;
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

        return layout;

    }

    public static class OrderedElementCollection {

        public List<AbstractElement> foregroundElements = new ArrayList<>();
        public List<AbstractElement> backgroundElements = new ArrayList<>();

    }

}
