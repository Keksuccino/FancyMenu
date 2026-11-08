package de.keksuccino.fancymenu.customization.profile.gui;

import de.keksuccino.fancymenu.customization.profile.ProfileLinkType;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Locale;

/**
 * Shared drawing and text helpers of the profile UIs. Everything that depends on Minecraft's rendering API lives here,
 * so the profile UIs stay the same across Minecraft versions.
 */
final class ProfileUiRendering {

    /** Muted colors for profiles without icon. They work on dark and light UI themes, because they always get white text. */
    private static final int[] AVATAR_COLORS = {
            0xFF5B6EE1, 0xFF8E5BD8, 0xFFC2508A, 0xFFD0603F, 0xFFC28A2E,
            0xFF4E9A4F, 0xFF2F9C8F, 0xFF3A87C6, 0xFF6A7487, 0xFF9B6B4A
    };
    private static final String ELLIPSIS = "…";

    private ProfileUiRendering() {
    }

    /**
     * Renders the icon of a profile. Profiles without icon get a colored tile with the first letter of their title.
     */
    static void renderProfileIcon(@NotNull GuiGraphicsExtractor graphics, @Nullable ITexture icon, @NotNull String title, @NotNull String id, float x, float y, float size) {
        float radius = Math.max(2.0F, size * 0.16F);
        Identifier location = ((icon != null) && icon.isReady()) ? icon.getResourceLocation() : null;
        if ((location != null) && (icon.getWidth() > 0) && (icon.getHeight() > 0)) {
            blitTexture(graphics, location, x, y, size, size, 0, 0, icon.getWidth(), icon.getHeight(), icon.getWidth(), icon.getHeight(), -1);
            UIBase.renderRoundedBorder(graphics, x, y, x + size, y + size, 1, 0, 0, 0, 0, getBorderColor());
            return;
        }
        int color = AVATAR_COLORS[Math.floorMod(id.hashCode(), AVATAR_COLORS.length)];
        UIBase.renderRoundedRect(graphics, x, y, size, size, radius, radius, radius, radius, color);
        String initial = getInitial(title);
        float textSize = Math.max(UIBase.getUITextSizeNormal(), size * 0.48F);
        Component initialComponent = Component.literal(initial);
        float textWidth = UIBase.getUITextWidth(initialComponent, textSize);
        float textHeight = UIBase.getUITextHeight(textSize);
        UIBase.renderText(graphics, initialComponent, x + ((size - textWidth) / 2.0F), y + ((size - textHeight) / 2.0F), 0xFFFFFFFF, textSize);
    }

    @NotNull
    private static String getInitial(@NotNull String title) {
        String trimmed = title.strip();
        if (trimmed.isEmpty()) return "?";
        int codePoint = trimmed.codePointAt(0);
        return new String(Character.toChars(codePoint)).toUpperCase(Locale.ROOT);
    }

    /**
     * Renders the centered square of a texture, like the crop a profile icon gets when it gets created from the image.
     */
    static void renderCenteredSquare(@NotNull GuiGraphicsExtractor graphics, @NotNull ITexture texture, float x, float y, float size) {
        Identifier location = texture.isReady() ? texture.getResourceLocation() : null;
        int width = texture.getWidth();
        int height = texture.getHeight();
        if ((location == null) || (width <= 0) || (height <= 0)) return;
        int side = Math.min(width, height);
        blitTexture(graphics, location, x, y, size, size, (width - side) / 2.0F, (height - side) / 2.0F, side, side, width, height, -1);
    }

    private static void blitTexture(@NotNull GuiGraphicsExtractor graphics, @NotNull Identifier location, float x, float y, float width, float height, float u, float v, float regionWidth, float regionHeight, int textureWidth, int textureHeight, int color) {
        RenderingUtils.blitF(graphics, RenderPipelines.GUI_TEXTURED, location, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight, color);
    }

    /**
     * Renders a small rounded label, like the "Active" badge.
     *
     * @return the width of the pill.
     */
    static float renderPill(@NotNull GuiGraphicsExtractor graphics, @NotNull Component text, float x, float y, float height, int textColor, int backgroundColor) {
        float textSize = UIBase.getUITextSizeSmall();
        float textWidth = UIBase.getUITextWidth(text, textSize);
        float width = textWidth + (height * 0.9F);
        float radius = height / 2.0F;
        UIBase.renderRoundedRect(graphics, x, y, width, height, radius, radius, radius, radius, backgroundColor);
        UIBase.renderText(graphics, text, x + ((width - textWidth) / 2.0F), y + ((height - UIBase.getUITextHeight(textSize)) / 2.0F), textColor, textSize);
        return width;
    }

    static float getPillWidth(@NotNull Component text, float height) {
        return UIBase.getUITextWidth(text, UIBase.getUITextSizeSmall()) + (height * 0.9F);
    }

    /**
     * Cuts text to fit into the given width and adds an ellipsis if it got cut.
     */
    @NotNull
    static String trimToWidth(@NotNull String text, float maxWidth, float textSize) {
        if (UIBase.getUITextWidth(text, textSize) <= maxWidth) return text;
        float ellipsisWidth = UIBase.getUITextWidth(ELLIPSIS, textSize);
        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if ((UIBase.getUITextWidth(text.substring(0, mid), textSize) + ellipsisWidth) <= maxWidth) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        if ((low > 0) && Character.isHighSurrogate(text.charAt(low - 1))) low--;
        return text.substring(0, low).stripTrailing() + ELLIPSIS;
    }

    /**
     * Formats a file size in a short, human-readable way, like "12.4 MB".
     */
    @NotNull
    static String formatBytes(long bytes) {
        if (bytes < 1024L) return I18n.get("fancymenu.profiles.size.bytes", bytes);
        double value = bytes / 1024.0D;
        String[] unitKeys = {"fancymenu.profiles.size.kb", "fancymenu.profiles.size.mb", "fancymenu.profiles.size.gb"};
        int unit = 0;
        while ((value >= 1024.0D) && (unit < unitKeys.length - 1)) {
            value /= 1024.0D;
            unit++;
        }
        String number = (value >= 100.0D) ? String.format(Locale.ROOT, "%.0f", value) : String.format(Locale.ROOT, "%.1f", value);
        return I18n.get(unitKeys[unit], number);
    }

    /**
     * Formats a remaining time, like "About 2 min left".
     */
    @NotNull
    static String formatRemainingTime(long remainingMs) {
        long seconds = Math.max(1L, (remainingMs + 999L) / 1000L);
        if (seconds < 60L) return I18n.get("fancymenu.profiles.task.remaining.seconds", seconds);
        long minutes = (seconds + 59L) / 60L;
        return I18n.get("fancymenu.profiles.task.remaining.minutes", minutes);
    }

    @NotNull
    static MaterialIcon getLinkIcon(@NotNull ProfileLinkType type) {
        return switch (type) {
            case WEBSITE -> MaterialIcons.PUBLIC;
            case DISCORD -> MaterialIcons.FORUM;
            case YOUTUBE -> MaterialIcons.SMART_DISPLAY;
            case X -> MaterialIcons.TAG;
            case TWITCH -> MaterialIcons.LIVE_TV;
            case BLUESKY -> MaterialIcons.CLOUD;
            case GITHUB -> MaterialIcons.CODE;
            case MODRINTH, CURSEFORGE -> MaterialIcons.EXTENSION;
            case PATREON -> MaterialIcons.VOLUNTEER_ACTIVISM;
            case KOFI -> MaterialIcons.LOCAL_CAFE;
            case REDDIT -> MaterialIcons.CHAT;
            case INSTAGRAM -> MaterialIcons.PHOTO_CAMERA;
            case TIKTOK -> MaterialIcons.MUSIC_NOTE;
            case OTHER -> MaterialIcons.LINK;
        };
    }

    @NotNull
    static Component getLinkTypeName(@NotNull ProfileLinkType type) {
        String brand = type.getBrandName();
        if (brand != null) return Component.literal(brand);
        return Component.translatable("fancymenu.profiles.links.type." + type.getSerializedName());
    }

    static int getTextColor() {
        return UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_interface_generic_text_color.getColorInt() : UIBase.getUITheme().ui_interface_generic_text_color.getColorInt();
    }

    static int getDimTextColor() {
        return UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_interface_widget_label_color_inactive.getColorInt() : UIBase.getUITheme().ui_interface_widget_label_color_inactive.getColorInt();
    }

    static int getAreaColor() {
        return UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_1.getColorInt() : UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt();
    }

    static int getSecondaryAreaColor() {
        return UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_2.getColorInt() : UIBase.getUITheme().ui_interface_area_background_color_type_2.getColorInt();
    }

    static int getBorderColor() {
        return UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_interface_area_border_color.getColorInt() : UIBase.getUITheme().ui_interface_area_border_color.getColorInt();
    }

    static int getHoverColor() {
        return UIBase.shouldBlur() ? UIBase.getUITheme().ui_blur_interface_area_entry_selected_color.getColorInt() : UIBase.getUITheme().ui_interface_area_entry_selected_color.getColorInt();
    }

    static int getSuccessColor() {
        return UIBase.getUITheme().success_color.getColorInt();
    }

    static int getErrorColor() {
        return UIBase.getUITheme().error_color.getColorInt();
    }

    static int getAccentColor() {
        return UIBase.getUITheme().info_color.getColorInt();
    }

    static int withAlpha(int color, float alpha) {
        return RenderingUtils.replaceAlphaInColor(color, Math.max(0.0F, Math.min(1.0F, alpha)));
    }

}
