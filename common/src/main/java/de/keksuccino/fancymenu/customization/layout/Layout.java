package de.keksuccino.fancymenu.customization.layout;

import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElement;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.util.SerializationUtils;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundRegistry;
import de.keksuccino.fancymenu.customization.background.SerializedMenuBackground;
import de.keksuccino.fancymenu.customization.background.backgrounds.animation.AnimationMenuBackground;
import de.keksuccino.fancymenu.customization.background.backgrounds.image.ImageMenuBackground;
import de.keksuccino.fancymenu.customization.background.backgrounds.panorama.PanoramaMenuBackground;
import de.keksuccino.fancymenu.customization.background.backgrounds.slideshow.SlideshowMenuBackground;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.DeepScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.deep.DeepScreenCustomizationLayerRegistry;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.Elements;
import de.keksuccino.fancymenu.customization.element.elements.animation.AnimationElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElementBuilder;
import de.keksuccino.fancymenu.customization.element.elements.image.ImageElement;
import de.keksuccino.fancymenu.customization.element.elements.shape.ShapeElement;
import de.keksuccino.fancymenu.customization.element.elements.slideshow.SlideshowElement;
import de.keksuccino.fancymenu.customization.element.elements.splash.SplashTextElement;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.slideshow.SlideshowHandler;
import de.keksuccino.fancymenu.util.Legacy;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.*;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class Layout extends LayoutBase {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final String UNIVERSAL_LAYOUT_IDENTIFIER = "%fancymenu:universal_layout%";

    @Nullable
    public String screenIdentifier;
    public File layoutFile;
    public long lastEditedTime = -1;
    protected boolean enabled = true;
    public int layoutIndex = 0;
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
    public DeepScreenCustomizationLayer deepScreenCustomizationLayer = null;
    @Legacy("Remove this in the future.")
    public boolean legacyLayout = false;

    @NotNull
    public static Layout buildUniversal() {
        return new Layout();
    }

    @NotNull
    public static Layout buildForScreen(@NotNull Screen screen) {
        return new Layout(screen);
    }

    @NotNull
    public static Layout buildForScreen(@NotNull String screenIdentifier) {
        return new Layout(screenIdentifier);
    }

    public Layout() {
        this.setToUniversalLayout();
    }

    public Layout(@NotNull Screen screen) {
        this.screenIdentifier = ScreenIdentifierHandler.getIdentifierOfScreen(screen);
    }

    public Layout(@NotNull String screenIdentifier) {
        this.setScreenIdentifier(screenIdentifier);
    }

    @NotNull
    public PropertyContainerSet serialize() {

        Objects.requireNonNull(this.screenIdentifier);

        PropertyContainerSet set = new PropertyContainerSet("fancymenu_layout");
        PropertyContainer meta = new PropertyContainer("layout-meta");

        set.putContainer(meta);

        meta.putProperty("identifier", this.screenIdentifier);
        meta.putProperty("render_custom_elements_behind_vanilla", "" + this.renderElementsBehindVanilla);
        meta.putProperty("last_edited_time", "" + this.lastEditedTime);
        meta.putProperty("is_enabled", "" + this.enabled);

        meta.putProperty("randommode", "" + this.randomMode);
        meta.putProperty("randomgroup", this.randomGroup);
        meta.putProperty("randomonlyfirsttime", "" + this.randomOnlyFirstTime);

        meta.putProperty("layout_index", "" + this.layoutIndex);

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

        if (this.forcedScale != 0) {
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
            ps.putProperty("path", this.openAudio.getSourceWithPrefix());
            set.putContainer(ps);
        }

        if (this.closeAudio != null) {
            PropertyContainer ps = new PropertyContainer("customization");
            ps.putProperty("action", "setcloseaudio");
            ps.putProperty("path", this.closeAudio.getSourceWithPrefix());
            set.putContainer(ps);
        }

        //Background Options Container
        PropertyContainer s = new PropertyContainer("customization");
        s.putProperty("action", "backgroundoptions");
        s.putProperty("keepaspectratio", "" + this.preserveBackgroundAspectRatio);
        set.putContainer(s);

        //Scroll List Customizations Container
        PropertyContainer scrollListContainer = new PropertyContainer("scroll_list_customization");
        scrollListContainer.putProperty("preserve_scroll_list_header_footer_aspect_ratio", "" + this.preserveScrollListHeaderFooterAspectRatio);
        if (this.scrollListHeaderTexture != null) {
            scrollListContainer.putProperty("scroll_list_header_texture", this.scrollListHeaderTexture.getSourceWithPrefix());
        }
        if (this.scrollListFooterTexture != null) {
            scrollListContainer.putProperty("scroll_list_footer_texture", this.scrollListFooterTexture.getSourceWithPrefix());
        }
        scrollListContainer.putProperty("render_scroll_list_header_shadow", "" + this.renderScrollListHeaderShadow);
        scrollListContainer.putProperty("render_scroll_list_footer_shadow", "" + this.renderScrollListFooterShadow);
        scrollListContainer.putProperty("show_scroll_list_header_footer_preview_in_editor", "" + this.showScrollListHeaderFooterPreviewInEditor);
        scrollListContainer.putProperty("repeat_scroll_list_header_texture", "" + this.repeatScrollListHeaderTexture);
        scrollListContainer.putProperty("repeat_scroll_list_footer_texture", "" + this.repeatScrollListFooterTexture);
        scrollListContainer.putProperty("show_screen_background_overlay_on_custom_background", "" + this.showScreenBackgroundOverlayOnCustomBackground);
        //TODO übernehmen
        scrollListContainer.putProperty("apply_vanilla_background_blur", "" + this.applyVanillaBackgroundBlur);
        set.putContainer(scrollListContainer);

        this.layoutWideLoadingRequirementContainer.serializeToExistingPropertyContainer(meta);

        this.serializedElements.forEach(set::putContainer);
        if (!this.isUniversalLayout()) {
            this.serializedVanillaButtonElements.forEach(set::putContainer);
            this.serializedDeepElements.forEach(set::putContainer);
        }

        return set;

    }

    @Nullable
    public static Layout deserialize(@NotNull PropertyContainerSet serialized, @Nullable File layoutFile) {

        if (serialized.getType().equalsIgnoreCase("menu") || serialized.getType().equalsIgnoreCase("fancymenu_layout")) {

            Layout layout = new Layout();
            layout.layoutFile = layoutFile;

            layout.legacyLayout = serialized.getType().equalsIgnoreCase("menu");

            PropertyContainer meta = serialized.getFirstContainerOfType("layout-meta");
            if (meta == null) {
                meta = serialized.getFirstContainerOfType("customization-meta");
            }
            if (meta != null) {

                layout.setScreenIdentifier(meta.getValue("identifier"));

                layout.layoutIndex = SerializationUtils.deserializeNumber(Integer.class, layout.layoutIndex, meta.getValue("layout_index"));

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

                String lastEdited = meta.getValue("last_edited_time");
                if ((lastEdited != null) && MathUtils.isLong(lastEdited)) {
                    layout.lastEditedTime = Long.parseLong(lastEdited);
                }
                if ((lastEdited == null) && layout.legacyLayout) {
                    layout.lastEditedTime = System.currentTimeMillis();
                }

                String isEnabled = meta.getValue("is_enabled");
                if ((isEnabled != null) && isEnabled.equals("false")) {
                    layout.enabled = false;
                }

                layout.customMenuTitle = meta.getValue("custom_menu_title");

                layout.layoutWideLoadingRequirementContainer = LoadingRequirementContainer.deserializeToSingleContainer(meta);

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
                                layout.universalLayoutMenuWhitelist.add(ScreenIdentifierHandler.getBestIdentifier(s));
                            }
                        }
                    }
                    if ((blacklistRaw != null) && blacklistRaw.contains(";")) {
                        for (String s : blacklistRaw.split(";")) {
                            if (s.length() > 0) {
                                layout.universalLayoutMenuBlacklist.add(ScreenIdentifierHandler.getBestIdentifier(s));
                            }
                        }
                    }
                }

            }

            //Handle vanilla button elements
            for (PropertyContainer sec : serialized.getContainersOfType("vanilla_button")) {
                SerializedElement serializedVanilla = convertContainerToSerializedElement(sec);
                serializedVanilla.setType("vanilla_button");
                layout.serializedVanillaButtonElements.add(serializedVanilla);
            }
            //Convert legacy vanilla button customizations
            if (layout.legacyLayout) {
                layout.serializedVanillaButtonElements.addAll(convertLegacyVanillaButtonCustomizations(serialized));
            }

            //Handle normal elements
            for (PropertyContainer sec : ListUtils.mergeLists(serialized.getContainersOfType("element"), serialized.getContainersOfType("customization"))) {
                String elementType = sec.getValue("element_type");
                if (elementType == null) {
                    elementType = sec.getValue("action");
                }
                if (elementType != null) {
                    elementType = elementType.replace("custom_layout_element:", "");
                    if (ElementRegistry.hasBuilder(elementType)) {
                        SerializedElement e = convertContainerToSerializedElement(sec);
                        e.putProperty("element_type", elementType);
                        layout.serializedElements.add(e);
                    }
                }
            }

            //Convert legacy elements (+ restore original element order)
            if (layout.legacyLayout) {
                List<List<?>> elementsAndOrder = convertLegacyElements(serialized);
                List<SerializedElement> normalElements = new ArrayList<>(layout.serializedElements);
                List<SerializedElement> legacyElements = (List<SerializedElement>) elementsAndOrder.get(0);
                List<SerializedElement> combined = new ArrayList<>();
                List<String> elementOrder = (List<String>) elementsAndOrder.get(1);
                for (String identifier : elementOrder) {
                    for (SerializedElement s : normalElements) {
                        String actionId = s.getValue("actionid");
                        if (actionId == null) actionId = s.getValue("instance_identifier");
                        if ((actionId != null) && actionId.equals(identifier)) {
                            combined.add(s);
                            continue;
                        }
                    }
                    for (SerializedElement e : legacyElements) {
                        String actionId = e.getValue("actionid");
                        if (actionId == null) actionId = e.getValue("instance_identifier");
                        if ((actionId != null) && actionId.equals(identifier)) {
                            combined.add(e);
                            continue;
                        }
                    }
                }
                for (SerializedElement e : normalElements) {
                    if (!combined.contains(e)) combined.add(e);
                }
                layout.serializedElements.clear();
                layout.serializedElements.addAll(combined);
            }

            //Handle deep elements
            layout.deepScreenCustomizationLayer = ((layout.screenIdentifier != null) && !layout.isUniversalLayout()) ? DeepScreenCustomizationLayerRegistry.getLayer(layout.screenIdentifier) : null;
            if (layout.deepScreenCustomizationLayer != null) {
                for (PropertyContainer sec : ListUtils.mergeLists(serialized.getContainersOfType("deep_element"), serialized.getContainersOfType("customization"))) {
                    String elementType = sec.getValue("element_type");
                    if (elementType == null) {
                        elementType = sec.getValue("action");
                    }
                    if (elementType != null) {
                        elementType = elementType.replace("deep_customization_element:", "");
                        if (layout.deepScreenCustomizationLayer.hasBuilder(elementType)) {
                            SerializedElement e = convertContainerToSerializedElement(sec);
                            e.setType("deep_element");
                            e.putProperty("element_type", elementType);
                            layout.serializedDeepElements.add(e);
                        }
                    }
                }
            }

            //Handle menu backgrounds
            List<PropertyContainer> menuBackgroundSections = serialized.getContainersOfType("menu_background");
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
            //Convert legacy backgrounds
            if (layout.legacyLayout) {
                MenuBackground legacyBackground = convertLegacyMenuBackground(serialized);
                if (legacyBackground != null) {
                    layout.menuBackground = legacyBackground;
                }
            }

            //Handle Scroll List Customizations
            PropertyContainer scrollListCustomizations = serialized.getFirstContainerOfType("scroll_list_customization");
            if (scrollListCustomizations != null) {
                String preserveScrollHeaderFooterAspect = scrollListCustomizations.getValue("preserve_scroll_list_header_footer_aspect_ratio");
                if (preserveScrollHeaderFooterAspect != null) {
                    if (preserveScrollHeaderFooterAspect.equals("true")) layout.preserveScrollListHeaderFooterAspectRatio = true;
                    if (preserveScrollHeaderFooterAspect.equals("false")) layout.preserveScrollListHeaderFooterAspectRatio = false;
                }
                layout.scrollListHeaderTexture = SerializationUtils.deserializeImageResourceSupplier(scrollListCustomizations.getValue("scroll_list_header_texture"));
                layout.scrollListFooterTexture = SerializationUtils.deserializeImageResourceSupplier(scrollListCustomizations.getValue("scroll_list_footer_texture"));
                String renderScrollHeaderShadow = scrollListCustomizations.getValue("render_scroll_list_header_shadow");
                if (renderScrollHeaderShadow != null) {
                    if (renderScrollHeaderShadow.equals("true")) layout.renderScrollListHeaderShadow = true;
                    if (renderScrollHeaderShadow.equals("false")) layout.renderScrollListHeaderShadow = false;
                }
                String renderScrollFooterShadow = scrollListCustomizations.getValue("render_scroll_list_footer_shadow");
                if (renderScrollFooterShadow != null) {
                    if (renderScrollFooterShadow.equals("true")) layout.renderScrollListFooterShadow = true;
                    if (renderScrollFooterShadow.equals("false")) layout.renderScrollListFooterShadow = false;
                }
                String showListHeaderFooter = scrollListCustomizations.getValue("show_scroll_list_header_footer_preview_in_editor");
                if ((showListHeaderFooter != null)) {
                    if (showListHeaderFooter.equals("true")) layout.showScrollListHeaderFooterPreviewInEditor = true;
                    if (showListHeaderFooter.equals("false")) layout.showScrollListHeaderFooterPreviewInEditor = false;
                }
                layout.showScreenBackgroundOverlayOnCustomBackground = SerializationUtils.deserializeBoolean(layout.showScreenBackgroundOverlayOnCustomBackground, scrollListCustomizations.getValue("show_screen_background_overlay_on_custom_background"));
                layout.repeatScrollListHeaderTexture = SerializationUtils.deserializeBoolean(layout.repeatScrollListHeaderTexture, scrollListCustomizations.getValue("repeat_scroll_list_header_texture"));
                layout.repeatScrollListFooterTexture = SerializationUtils.deserializeBoolean(layout.repeatScrollListFooterTexture, scrollListCustomizations.getValue("repeat_scroll_list_footer_texture"));
                //TODO übernehmen
                layout.applyVanillaBackgroundBlur = SerializationUtils.deserializeBoolean(layout.applyVanillaBackgroundBlur, scrollListCustomizations.getValue("apply_vanilla_background_blur"));
            }

            //Handle everything else
            for (PropertyContainer sec : serialized.getContainersOfType("customization")) {

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
                        layout.autoScalingWidth = Integer.parseInt(Objects.requireNonNull(baseWidth));
                    }
                    String baseHeight = sec.getValue("baseheight");
                    if (MathUtils.isInteger(baseHeight)) {
                        layout.autoScalingHeight = Integer.parseInt(Objects.requireNonNull(baseHeight));
                    }
                }

                if ((action != null) && action.equalsIgnoreCase("backgroundoptions")) {
                    String keepAspect = sec.getValue("keepaspectratio");
                    if ((keepAspect != null) && keepAspect.equalsIgnoreCase("true")) {
                        layout.preserveBackgroundAspectRatio = true;
                    }
                }

                if ((action != null) && action.equalsIgnoreCase("setcloseaudio")) {
                    layout.closeAudio = SerializationUtils.deserializeAudioResourceSupplier(sec.getValue("path"));
                }

                if ((action != null) && action.equalsIgnoreCase("setopenaudio")) {
                    layout.openAudio = SerializationUtils.deserializeAudioResourceSupplier(sec.getValue("path"));
                }

            }

            return layout;

        }

        return null;

    }

    public boolean saveToFileIfPossible() {
        if (this.layoutFile != null) {
            return LayoutHandler.saveLayoutToFile(this, GameDirectoryUtils.getAbsoluteGameDirectoryPath(this.layoutFile.getAbsolutePath()));
        }
        return false;
    }

    public Layout updateLastEditedTime() {
        this.lastEditedTime = System.currentTimeMillis();
        return this;
    }

    public Layout setScreenIdentifier(String screenIdentifier) {
        if (screenIdentifier != null) {
            if (screenIdentifier.equals(UNIVERSAL_LAYOUT_IDENTIFIER)) return this.setToUniversalLayout();
            this.screenIdentifier = ScreenIdentifierHandler.getBestIdentifier(screenIdentifier);
        }
        return this;
    }

    public boolean isUniversalLayout() {
        return (this.screenIdentifier != null) && this.screenIdentifier.equals(UNIVERSAL_LAYOUT_IDENTIFIER);
    }

    public Layout setToUniversalLayout() {
        this.screenIdentifier = UNIVERSAL_LAYOUT_IDENTIFIER;
        return this;
    }

    @NotNull
    public String getLayoutName() {
        if (this.layoutFile != null) return com.google.common.io.Files.getNameWithoutExtension(this.layoutFile.getPath());
        return "Nameless Layout";
    }

    public LayoutStatus getStatus() {
        return this.isEnabled() ? LayoutStatus.ENABLED : LayoutStatus.DISABLED;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public Layout setEnabled(boolean enabled, boolean reInitCurrentScreen) {
        this.enabled = enabled;
        this.saveToFileIfPossible();
        if (reInitCurrentScreen) ScreenCustomization.reInitCurrentScreen();
        return this;
    }

    public void delete(boolean reInitCurrentScreen) {
        LayoutHandler.deleteLayout(this, reInitCurrentScreen);
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
                        element.setParentLayout(this);
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
                            element.setParentLayout(this);
                            elements.add(element);
                        }
                    }
                }
            }
        }
        return elements;
    }

    @NotNull
    public List<VanillaWidgetElement> buildVanillaButtonElementInstances() {
        List<VanillaWidgetElement> elements = new ArrayList<>();
        for (SerializedElement serialized : this.serializedVanillaButtonElements) {
            VanillaWidgetElement element = VanillaWidgetElementBuilder.INSTANCE.deserializeElementInternal(serialized);
            if (element != null) {
                element.setParentLayout(this);
                elements.add(element);
            }
        }
        return elements;
    }

    @NotNull
    public Layout copy() {
       return Objects.requireNonNull(deserialize(this.serialize(), this.layoutFile));
    }

    @Legacy("Converts legacy elements to the new format. Remove this later.")
    @NotNull
    protected static List<List<?>> convertLegacyElements(PropertyContainerSet layout) {

        List<SerializedElement> elements = new ArrayList<>();
        List<String> elementOrder = new ArrayList<>();

        for (PropertyContainer sec : layout.getContainersOfType("customization")) {

            String action = sec.getValue("action");
            if (action != null) {

                if (action.startsWith("custom_layout_element:")) {
                    String identifier = sec.getValue("actionid");
                    if (identifier != null) {
                        elementOrder.add(identifier);
                    }
                }

                if (action.equalsIgnoreCase("addtexture")) {
                    ImageElement e = Elements.IMAGE.deserializeElementInternal(convertContainerToSerializedElement(sec));
                    if (e != null) {
                        e.stayOnScreen = false;
                        e.textureSupplier = SerializationUtils.deserializeImageResourceSupplier(sec.getValue("path"));
                        elements.add(Elements.IMAGE.serializeElementInternal(e));
                        elementOrder.add(e.getInstanceIdentifier());
                    }
                }

                if (action.equalsIgnoreCase("addwebtexture")) {
                    ImageElement e = Elements.IMAGE.deserializeElementInternal(convertContainerToSerializedElement(sec));
                    if (e != null) {
                        e.stayOnScreen = false;
                        e.textureSupplier = SerializationUtils.deserializeImageResourceSupplier(sec.getValue("url"));
                        elements.add(Elements.IMAGE.serializeElementInternal(e));
                        elementOrder.add(e.getInstanceIdentifier());
                    }
                }

//                if (action.equalsIgnoreCase("addanimation")) {
//                    AnimationElement e = Elements.ANIMATION.deserializeElementInternal(convertContainerToSerializedElement(sec));
//                    if (e != null) {
//                        e.stayOnScreen = false;
//                        e.animationName = sec.getValue("name");
//                        elements.add(Elements.ANIMATION.serializeElementInternal(e));
//                        elementOrder.add(e.getInstanceIdentifier());
//                    }
//                }

                if (action.equalsIgnoreCase("addshape")) {
                    ShapeElement e = Elements.SHAPE.deserializeElementInternal(convertContainerToSerializedElement(sec));
                    if (e != null) {
                        e.stayOnScreen = false;
                        String c = sec.getValue("color");
                        if (c != null) e.color = DrawableColor.of(c);
                        elements.add(Elements.SHAPE.serializeElementInternal(e));
                        elementOrder.add(e.getInstanceIdentifier());
                    }
                }

                if (action.equalsIgnoreCase("addslideshow")) {
                    SlideshowElement e = Elements.SLIDESHOW.deserializeElementInternal(convertContainerToSerializedElement(sec));
                    if (e != null) {
                        e.stayOnScreen = false;
                        e.slideshowName = sec.getValue("name");
                        elements.add(Elements.SLIDESHOW.serializeElementInternal(e));
                        elementOrder.add(e.getInstanceIdentifier());
                    }
                }

                if (action.equalsIgnoreCase("addbutton")) {
                    ButtonElement e = Elements.BUTTON.deserializeElementInternal(convertContainerToSerializedElement(sec));
                    if (e != null) {
                        e.stayOnScreen = false;
                        elements.add(Elements.BUTTON.serializeElementInternal(e));
                        elementOrder.add(e.getInstanceIdentifier());
                    }
                }

                if (action.equalsIgnoreCase("addsplash")) {
                    SplashTextElement e = Elements.SPLASH_TEXT.deserializeElementInternal(convertContainerToSerializedElement(sec));
                    if (e != null) {
                        String text = sec.getValue("text");
                        if (text != null) {
                            e.source = text;
                            e.sourceMode = SplashTextElement.SourceMode.DIRECT_TEXT;
                        }
                        String path = sec.getValue("splashfilepath");
                        if (path != null) {
                            e.source = path;
                            e.sourceMode = SplashTextElement.SourceMode.TEXT_FILE;
                        }
                        String vanillaLikeString = sec.getValue("vanilla-like");
                        if ((vanillaLikeString != null) && vanillaLikeString.equals("true")) {
                            e.source = null;
                            e.sourceMode = SplashTextElement.SourceMode.VANILLA;
                        }
                        String baseColor = sec.getValue("basecolor");
                        if (baseColor != null) {
                            e.baseColor = DrawableColor.of(baseColor);
                        }
                        e.stayOnScreen = false;
                        elements.add(Elements.SPLASH_TEXT.serializeElementInternal(e));
                        elementOrder.add(e.getInstanceIdentifier());
                    }
                }

            }

        }

        return List.of(elements, elementOrder);

    }

    @Legacy("This converts old menu background sections to new background elements. Remove this in the future.")
    @SuppressWarnings("all")
    @Nullable
    protected static MenuBackground convertLegacyMenuBackground(PropertyContainerSet layout) {

        for (PropertyContainer sec : layout.getContainersOfType("customization")) {

            String action = sec.getValue("action");
            if (action != null) {

                if (action.equalsIgnoreCase("setbackgroundslideshow")) {
                    String name = sec.getValue("name");
                    if (name != null) {
                        if (SlideshowHandler.slideshowExists(name)) {
                            MenuBackgroundBuilder<?> builder = MenuBackgroundRegistry.getBuilder("slideshow");
                            if (builder != null) {
                                SlideshowMenuBackground b = new SlideshowMenuBackground((MenuBackgroundBuilder<SlideshowMenuBackground>)builder);
                                b.slideshowName = name;
                                return b;
                            }
                        }
                    }
                }
                if (action.equalsIgnoreCase("setbackgroundpanorama")) {
                    String name = sec.getValue("name");
                    if (name != null) {
                        if (PanoramaHandler.panoramaExists(name)) {
                            MenuBackgroundBuilder<?> builder = MenuBackgroundRegistry.getBuilder("panorama");
                            if (builder != null) {
                                PanoramaMenuBackground b = new PanoramaMenuBackground((MenuBackgroundBuilder<PanoramaMenuBackground>)builder);
                                b.panoramaName = name;
                                return b;
                            }
                        }
                    }
                }
                if (action.equalsIgnoreCase("texturizebackground")) {
                    String value = AbstractElement.fixBackslashPath(sec.getValue("path"));
                    String pano = sec.getValue("wideformat");
                    if (value != null) {
                        MenuBackgroundBuilder<?> builder = MenuBackgroundRegistry.getBuilder("image");
                        if (builder != null) {
                            ImageMenuBackground b = new ImageMenuBackground((MenuBackgroundBuilder<ImageMenuBackground>)builder);
                            b.textureSupplier = SerializationUtils.deserializeImageResourceSupplier(value);
                            b.slideLeftRight = (pano != null) && pano.equalsIgnoreCase("true");
                            return b;
                        }
                    }
                }
                if (action.equalsIgnoreCase("animatebackground")) {
                    String value = sec.getValue("name");
                    String restartOnLoadString = sec.getValue("restart_on_load");
                    if (value != null) {
                        if (AnimationHandler.animationExists(value)) {
                            MenuBackgroundBuilder<?> builder = MenuBackgroundRegistry.getBuilder("animation");
                            if (builder != null) {
                                AnimationMenuBackground b = new AnimationMenuBackground((MenuBackgroundBuilder<AnimationMenuBackground>) builder);
                                b.animationName = value;
                                b.restartOnMenuLoad = (restartOnLoadString != null) && restartOnLoadString.equalsIgnoreCase("true");
                                return b;
                            }
                        }
                    }
                }

            }

        }

        return null;

    }

    @Legacy("This converts old button customization sections and should get removed in the future.")
    @NotNull
    protected static List<SerializedElement> convertLegacyVanillaButtonCustomizations(PropertyContainerSet layout) {

        Map<String, VanillaWidgetElement> elements = new HashMap<>();

        for (PropertyContainer sec : layout.getContainersOfType("customization")) {
            VanillaWidgetElement element = VanillaWidgetElementBuilder.INSTANCE.buildDefaultInstance();
            String action = sec.getValue("action");
            String identifier = sec.getValue("identifier");
            if ((identifier != null) && identifier.startsWith("%id=")) {
                identifier = identifier.replace("%id=", "").replace("button_compatibility_id:", "").replace("%", "").replace("vanillabtn:", "");
            } else {
                identifier = null;
            }
            if ((action != null) && (identifier != null)) {

                if (elements.containsKey(identifier)) {
                    element = elements.get(identifier);
                }

                element.setInstanceIdentifier(identifier);

                boolean addElement = false;

                if (action.equalsIgnoreCase("addhoversound")) {
                    element.hoverSound = SerializationUtils.deserializeAudioResourceSupplier(sec.getValue("path"));
                    if (element.hoverSound != null) {
                        addElement = true;
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

                if (action.equalsIgnoreCase("resizebutton")) {
                    element.baseWidth = SerializationUtils.deserializeNumber(Integer.class, 5, sec.getValue("width"));
                    element.baseHeight = SerializationUtils.deserializeNumber(Integer.class, 5, sec.getValue("height"));
                    element.advancedWidth = sec.getValue("advanced_width");
                    element.advancedHeight = sec.getValue("advanced_height");
                    addElement = true;
                }

                if (action.equalsIgnoreCase("movebutton")) {

                    String x = sec.getValue("x");
                    String y = sec.getValue("y");
                    if (x != null) {
                        x = PlaceholderParser.replacePlaceholders(x);
                        if (MathUtils.isInteger(x)) {
                            element.posOffsetX = Integer.parseInt(x);
                        }
                    }
                    if (y != null) {
                        y = PlaceholderParser.replacePlaceholders(y);
                        if (MathUtils.isInteger(y)) {
                            element.posOffsetY = Integer.parseInt(y);
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
                    element.backgroundTextureNormal = SerializationUtils.deserializeImageResourceSupplier(sec.getValue("backgroundnormal"));
                    element.backgroundTextureHover = SerializationUtils.deserializeImageResourceSupplier(sec.getValue("backgroundhovered"));
                    element.backgroundAnimationNormal = sec.getValue("backgroundanimationnormal");
                    element.backgroundAnimationHover = sec.getValue("backgroundanimationhovered");
                    addElement = true;
                }

                if (action.equalsIgnoreCase("setbuttonclicksound")) {
                    element.clickSound = SerializationUtils.deserializeAudioResourceSupplier(sec.getValue("path"));
                    if (element.clickSound != null) {
                        addElement = true;
                    }
                }

                if (action.equalsIgnoreCase("vanilla_button_visibility_requirements")) {
                    element.loadingRequirementContainer = LoadingRequirementContainer.deserializeToSingleContainer(sec);
                    addElement = true;
                }

                if (action.equalsIgnoreCase("clickbutton")) {
                    String clicks = sec.getValue("clicks");
                    if ((clicks != null) && (MathUtils.isInteger(clicks))) {
                        element.automatedButtonClicks = Integer.parseInt(clicks);
                        addElement = true;
                    }
                }

                if (addElement && !elements.containsKey(identifier)) {
                    elements.put(identifier, element);
                }

            }
        }

        List<SerializedElement> l = new ArrayList<>();
        for (VanillaWidgetElement e : elements.values()) {
            e.stayOnScreen = false;
            SerializedElement serialized = VanillaWidgetElementBuilder.INSTANCE.serializeElementInternal(e);
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

    public enum LayoutStatus implements LocalizedCycleEnum<LayoutStatus> {

        ENABLED("enabled", SUCCESS_TEXT_STYLE),
        DISABLED("disabled", ERROR_TEXT_STYLE);

        final String name;
        final Supplier<Style> style;

        LayoutStatus(String name, Supplier<Style> style) {
            this.name = name;
            this.style = style;
        }

        @Override
        public @NotNull String getLocalizationKeyBase() {
            return "fancymenu.layout.status";
        }

        @Override
        public @NotNull String getName() {
            return this.name;
        }

        @NotNull
        @Override
        public LayoutStatus[] getValues() {
            return LayoutStatus.values();
        }

        @Nullable
        @Override
        public LayoutStatus getByNameInternal(@NotNull String name) {
            return getByName(name);
        }

        @Nullable
        public static LayoutStatus getByName(@NotNull String name) {
            for (LayoutStatus e : LayoutStatus.values()) {
                if (e.getName().equals(name)) return e;
            }
            return null;
        }

        @Override
        public @NotNull Style getValueComponentStyle() {
            return this.style.get();
        }

    }

}
