package de.keksuccino.fancymenu.util.rendering.ui;

import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum UIScale {

    AUTO("auto", 0.0F),
    MICRO("micro", 1.0F),
    EXTRA_SMALL("extra_small", 1.25F),
    SMALL("small", 1.5F),
    MEDIUM("medium", 2.0F),
    MEDIUM_LARGE("medium_large", 2.25F),
    LARGE("large", 2.5F),
    EXTRA_LARGE("extra_large", 3.0F),
    HUGE("huge", 3.25F),
    GIGANTIC("gigantic", 3.5F),
    COLOSSAL("colossal", 4.0F),
    TITANIC("titanic", 4.5F),
    MASSIVE("massive", 5.0F),
    IMMENSE("immense", 5.5F),
    MAXIMUM("maximum", 6.0F);

    final String name;
    final float scale;

    UIScale(String name, float scale) {
        this.name = name;
        this.scale = scale;
    }

    public String getName() {
        return name;
    }

    public float getScale() {
        return scale;
    }

    @NotNull
    public Component getDisplayName() {
        if (this == AUTO) return Component.translatable("fancymenu.ui.scales." + this.getName());
        return Component.translatable("fancymenu.ui.scales." + this.getName(), this.getScale());
    }

    @Nullable
    public static UIScale getByName(String name) {
        if (name == null) {
            return null;
        }
        for (UIScale scale : UIScale.values()) {
            if (scale.name.equals(name)) {
                return scale;
            }
        }
        return null;
    }

    @NotNull
    public static UIScale getUIScale() {
        String uiScaleString = FancyMenu.getOptions().uiScale.getValue();
        UIScale resolved = getByName(uiScaleString);
        if (resolved == null) resolved = AUTO;
        return resolved;
    }

    /**
     * Returns the logical UI scale used for FancyMenu's UI elements, after applying
     * automatic adjustments (2K/4K auto-scale and Unicode font enforcement).
     */
    public static float getUIScaleFloat() {
        UIScale scale = getUIScale();
        float uiScale = scale.getScale();
        //Handle "Auto" scale (use SMALL for 2K-ish windows, MEDIUM for 4K-ish windows)
        if (scale == AUTO) {
            uiScale = EXTRA_SMALL.getScale();
            int windowWidth = Minecraft.getInstance().getWindow().getWidth();
            int windowHeight = Minecraft.getInstance().getWindow().getHeight();
            if ((windowWidth >= 2400) || (windowHeight >= 1300)) {
                uiScale = SMALL.getScale();
            }
            if ((windowWidth > 3000) || (windowHeight > 1700)) {
                uiScale = MEDIUM.getScale();
            }
        }
        //Force a scale of 2 or bigger if Unicode font is enabled
        if (UIBase.shouldUseMinecraftFontForUIRendering() && Minecraft.getInstance().isEnforceUnicode() && (uiScale < 2F)) {
            uiScale = MEDIUM.getScale();
        }
        return uiScale;
    }

}
