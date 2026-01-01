package de.keksuccino.fancymenu.customization.layout;

import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.SerializedMenuBackground;
import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.util.Pair;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Basically everything of layouts that is stackable.
 */
public class LayoutBase {

    @NotNull
    public final List<MenuBackground> menuBackgrounds = new ArrayList<>();
    @NotNull
    public final List<Pair<AbstractDecorationOverlayBuilder<?>, AbstractDecorationOverlay<?>>> decorationOverlays = new ArrayList<>();
    public boolean preserveBackgroundAspectRatio = false;
    public ResourceSupplier<IAudio> openAudio;
    public ResourceSupplier<IAudio> closeAudio;
    public float forcedScale = 0;
    public int autoScalingWidth = 0;
    public int autoScalingHeight = 0;
    public String customMenuTitle;
    public boolean preserveScrollListHeaderFooterAspectRatio = true;
    public boolean repeatScrollListHeaderTexture = false;
    public boolean repeatScrollListFooterTexture = false;
    public ResourceSupplier<ITexture> scrollListHeaderTexture;
    public ResourceSupplier<ITexture> scrollListFooterTexture;
    public boolean renderScrollListHeaderShadow = true;
    public boolean renderScrollListFooterShadow = true;
    public boolean showScrollListHeaderFooterPreviewInEditor = false;
    public boolean showScreenBackgroundOverlayOnCustomBackground = false;
    public boolean applyVanillaBackgroundBlur = false;
    @NotNull
    public final List<GenericExecutableBlock> openScreenExecutableBlocks = new ArrayList<>();
    @NotNull
    public final List<GenericExecutableBlock> closeScreenExecutableBlocks = new ArrayList<>();

    @NotNull
    public static LayoutBase stackLayoutBases(LayoutBase... layouts) {
        LayoutBase stacked = new LayoutBase();
        if (layouts != null) {
            for (LayoutBase layout : layouts) {

                if (!layout.menuBackgrounds.isEmpty()) {
                    stacked.menuBackgrounds.addAll(layout.menuBackgrounds);
                }
                if (layout.preserveBackgroundAspectRatio) {
                    stacked.preserveBackgroundAspectRatio = true;
                }
                if (layout.openAudio != null) {
                    stacked.openAudio = layout.openAudio;
                }
                if (layout.closeAudio != null) {
                    stacked.closeAudio = layout.closeAudio;
                }
                if (layout.forcedScale != 0) {
                    stacked.forcedScale = layout.forcedScale;
                }
                if (layout.autoScalingWidth != 0) {
                    stacked.autoScalingWidth = layout.autoScalingWidth;
                }
                if (layout.autoScalingHeight != 0) {
                    stacked.autoScalingHeight = layout.autoScalingHeight;
                }
                if (layout.customMenuTitle != null) {
                    stacked.customMenuTitle = layout.customMenuTitle;
                }
                if (layout.scrollListHeaderTexture != null) {
                    stacked.scrollListHeaderTexture = layout.scrollListHeaderTexture;
                }
                if (layout.scrollListFooterTexture != null) {
                    stacked.scrollListFooterTexture = layout.scrollListFooterTexture;
                }
                if (!layout.renderScrollListHeaderShadow) {
                    stacked.renderScrollListHeaderShadow = false;
                }
                if (!layout.renderScrollListFooterShadow) {
                    stacked.renderScrollListFooterShadow = false;
                }
                if (!layout.preserveScrollListHeaderFooterAspectRatio) {
                    stacked.preserveScrollListHeaderFooterAspectRatio = false;
                }
                if (layout.repeatScrollListHeaderTexture) {
                    stacked.repeatScrollListHeaderTexture = true;
                }
                if (layout.repeatScrollListFooterTexture) {
                    stacked.repeatScrollListFooterTexture = true;
                }
                if (layout.showScrollListHeaderFooterPreviewInEditor) {
                    stacked.showScrollListHeaderFooterPreviewInEditor = true;
                }
                if (layout.showScreenBackgroundOverlayOnCustomBackground) {
                    stacked.showScreenBackgroundOverlayOnCustomBackground = true;
                }
                if (layout.applyVanillaBackgroundBlur) {
                    stacked.applyVanillaBackgroundBlur = true;
                }
                if (!layout.openScreenExecutableBlocks.isEmpty()) {
                    stacked.openScreenExecutableBlocks.addAll(layout.openScreenExecutableBlocks);
                }
                if (!layout.closeScreenExecutableBlocks.isEmpty()) {
                    stacked.closeScreenExecutableBlocks.addAll(layout.closeScreenExecutableBlocks);
                }
                layout.decorationOverlays.forEach(pair -> {
                    if (pair.getValue().showOverlay.tryGetNonNullElse(false)) stacked.decorationOverlays.add(pair);
                });

            }
        }
        return stacked;
    }

    protected static SerializedElement convertContainerToSerializedElement(PropertyContainer sec) {
        SerializedElement e = new SerializedElement();
        for (Map.Entry<String, String> m : sec.getProperties().entrySet()) {
            e.putProperty(m.getKey(), m.getValue());
        }
        return e;
    }

    public static SerializedMenuBackground convertSectionToBackground(PropertyContainer section) {
        SerializedMenuBackground b = new SerializedMenuBackground();
        for (Map.Entry<String, String> m : section.getProperties().entrySet()) {
            b.putProperty(m.getKey(), m.getValue());
        }
        return b;
    }

}
