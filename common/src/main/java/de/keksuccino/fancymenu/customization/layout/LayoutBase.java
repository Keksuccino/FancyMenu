package de.keksuccino.fancymenu.customization.layout;

import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.SerializedMenuBackground;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import org.jetbrains.annotations.NotNull;
import java.util.Map;

/**
 * Basically everything of layouts that is stackable.
 */
public class LayoutBase {

    public MenuBackground menuBackground;
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
    //TODO übernehmen
    public boolean applyVanillaBackgroundBlur = false;

    @NotNull
    public static LayoutBase stackLayoutBases(LayoutBase... layouts) {
        LayoutBase layout = new LayoutBase();
        if (layouts != null) {
            for (LayoutBase l : layouts) {

                if (l.menuBackground != null) {
                    layout.menuBackground = l.menuBackground;
                }
                if (l.preserveBackgroundAspectRatio) {
                    layout.preserveBackgroundAspectRatio = true;
                }
                if (l.openAudio != null) {
                    layout.openAudio = l.openAudio;
                }
                if (l.closeAudio != null) {
                    layout.closeAudio = l.closeAudio;
                }
                if (l.forcedScale != 0) {
                    layout.forcedScale = l.forcedScale;
                }
                if (l.autoScalingWidth != 0) {
                    layout.autoScalingWidth = l.autoScalingWidth;
                }
                if (l.autoScalingHeight != 0) {
                    layout.autoScalingHeight = l.autoScalingHeight;
                }
                if (l.customMenuTitle != null) {
                    layout.customMenuTitle = l.customMenuTitle;
                }
                if (l.scrollListHeaderTexture != null) {
                    layout.scrollListHeaderTexture = l.scrollListHeaderTexture;
                }
                if (l.scrollListFooterTexture != null) {
                    layout.scrollListFooterTexture = l.scrollListFooterTexture;
                }
                if (!l.renderScrollListHeaderShadow) {
                    layout.renderScrollListHeaderShadow = false;
                }
                if (!l.renderScrollListFooterShadow) {
                    layout.renderScrollListFooterShadow = false;
                }
                if (!l.preserveScrollListHeaderFooterAspectRatio) {
                    layout.preserveScrollListHeaderFooterAspectRatio = false;
                }
                if (l.repeatScrollListHeaderTexture) {
                    layout.repeatScrollListHeaderTexture = true;
                }
                if (l.repeatScrollListFooterTexture) {
                    layout.repeatScrollListFooterTexture = true;
                }
                if (l.showScrollListHeaderFooterPreviewInEditor) {
                    layout.showScrollListHeaderFooterPreviewInEditor = true;
                }
                if (l.showScreenBackgroundOverlayOnCustomBackground) {
                    layout.showScreenBackgroundOverlayOnCustomBackground = true;
                }
                //TODO übernehmen
                if (l.applyVanillaBackgroundBlur) {
                    layout.applyVanillaBackgroundBlur = true;
                }

            }
        }
        return layout;
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
