package de.keksuccino.fancymenu.customization.layout;

import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.SerializedMenuBackground;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.properties.PropertyContainer;
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

    @NotNull
    public static LayoutBase stackLayoutBases(LayoutBase... layouts) {
        LayoutBase layout = new LayoutBase();
        if (layouts != null) {
            for (LayoutBase l : layouts) {

                if (l.overrideMenuWith != null) {
                    layout.overrideMenuWith = l.overrideMenuWith;
                }
                if (l.menuBackground != null) {
                    layout.menuBackground = l.menuBackground;
                }
                if (l.keepBackgroundAspectRatio) {
                    layout.keepBackgroundAspectRatio = true;
                }
                if (l.openAudio != null) {
                    layout.openAudio = l.openAudio;
                }
                if (l.closeAudio != null) {
                    layout.closeAudio = l.closeAudio;
                }
                if (l.forcedScale != -1F) {
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

            }
        }
        return layout;
    }

    protected static SerializedElement convertSectionToElement(PropertyContainer sec) {
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
