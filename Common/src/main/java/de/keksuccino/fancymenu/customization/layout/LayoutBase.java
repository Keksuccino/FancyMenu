package de.keksuccino.fancymenu.customization.layout;

import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.SerializedMenuBackground;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.konkrete.properties.PropertiesSection;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Basically everything of layouts that is stackable.
 */
public class LayoutBase {

    public String overrideMenuWith;
    public MenuBackground menuBackground;
    public boolean keepBackgroundAspectRatio = false;
    public String openAudio;
    public String closeAudio;
    public float forcedScale = -1F;
    public int autoScalingWidth = 0;
    public int autoScalingHeight = 0;
    public String customMenuTitle;

    @SuppressWarnings("all")
    @NotNull
    public static LayoutBase stackLayoutBases(LayoutBase... layouts) {
        LayoutBase layout = new LayoutBase();
        if (layouts != null) {
            for (LayoutBase l : layouts) {
                layout.overrideMenuWith = (l.overrideMenuWith != null) ? l.overrideMenuWith : layout.overrideMenuWith;
                layout.menuBackground = (l.menuBackground != null) ? l.menuBackground : layout.menuBackground;
                layout.keepBackgroundAspectRatio = (l.keepBackgroundAspectRatio) ? l.keepBackgroundAspectRatio : layout.keepBackgroundAspectRatio;
                layout.openAudio = (l.openAudio != null) ? l.openAudio : layout.openAudio;
                layout.closeAudio = (l.closeAudio != null) ? l.closeAudio : layout.closeAudio;
                layout.forcedScale = (l.forcedScale != -1F) ? l.forcedScale : layout.forcedScale;
                layout.autoScalingWidth = (l.autoScalingWidth != 0) ? l.autoScalingWidth : layout.autoScalingWidth;
                layout.autoScalingHeight = (l.autoScalingHeight != 0) ? l.autoScalingHeight : layout.autoScalingHeight;
                layout.customMenuTitle = (l.customMenuTitle != null) ? l.customMenuTitle : layout.customMenuTitle;
            }
        }
        return layout;
    }

    protected static SerializedElement convertSectionToElement(PropertiesSection sec) {
        SerializedElement e = new SerializedElement();
        for (Map.Entry<String, String> m : sec.getEntries().entrySet()) {
            e.addEntry(m.getKey(), m.getValue());
        }
        return e;
    }

    public static SerializedMenuBackground convertSectionToBackground(PropertiesSection section) {
        SerializedMenuBackground b = new SerializedMenuBackground();
        for (Map.Entry<String, String> m : section.getEntries().entrySet()) {
            b.addEntry(m.getKey(), m.getValue());
        }
        return b;
    }

}
