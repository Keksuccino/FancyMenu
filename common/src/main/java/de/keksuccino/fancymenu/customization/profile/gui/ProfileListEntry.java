package de.keksuccino.fancymenu.customization.profile.gui;

import de.keksuccino.fancymenu.customization.profile.Profile;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

/**
 * A profile in the list of the profile manager, with icon, title, authors and a badge for the active profile.
 */
class ProfileListEntry extends ScrollAreaEntry {

    static final int HEIGHT = 40;
    private static final int ICON_SIZE = 28;
    private static final int PADDING = 6;
    private static final long DOUBLE_CLICK_MS = 400L;
    private static final float BADGE_HEIGHT = 12.0F;

    @NotNull
    final Profile profile;
    @NotNull
    private final ManageProfilesWindowBody manager;
    @NotNull
    private final Component activeBadge = Component.translatable("fancymenu.profiles.active");
    private long lastClickTime = 0L;

    ProfileListEntry(@NotNull ScrollArea parent, @NotNull Profile profile, @NotNull ManageProfilesWindowBody manager) {
        super(parent, 100, HEIGHT);
        this.profile = profile;
        this.manager = manager;
        this.playClickSound = false;
    }

    @Override
    public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        float iconX = this.x + PADDING;
        float iconY = this.y + ((this.height - ICON_SIZE) / 2.0F);
        ProfileUiRendering.renderProfileIcon(graphics, this.manager.getIcon(this.profile), this.profile.metadata().getTitle(), this.profile.getId(), iconX, iconY, ICON_SIZE);

        float textX = iconX + ICON_SIZE + 8;
        float rightEdge = this.x + this.width - PADDING - 4;
        if (this.profile.active()) {
            float badgeWidth = ProfileUiRendering.getPillWidth(this.activeBadge, BADGE_HEIGHT);
            rightEdge -= badgeWidth;
            ProfileUiRendering.renderPill(graphics, this.activeBadge, rightEdge, this.y + ((this.height - BADGE_HEIGHT) / 2.0F), BADGE_HEIGHT, ProfileUiRendering.getSuccessColor(), ProfileUiRendering.withAlpha(ProfileUiRendering.getSuccessColor(), 0.16F));
            rightEdge -= 6;
        }
        float textWidth = Math.max(10, rightEdge - textX);

        String title = this.manager.getDisplayTitle(this.profile);
        String subtitle = this.getSubtitle();
        float titleSize = UIBase.getUITextSizeNormal();
        float subtitleSize = UIBase.getUITextSizeSmall();
        float titleHeight = UIBase.getUITextHeight(titleSize);
        float subtitleHeight = UIBase.getUITextHeight(subtitleSize);
        float blockHeight = subtitle.isEmpty() ? titleHeight : (titleHeight + 2 + subtitleHeight);
        float titleY = this.y + ((this.height - blockHeight) / 2.0F);
        Component titleComponent = Component.literal(ProfileUiRendering.trimToWidth(title, textWidth, titleSize)).withStyle(Style.EMPTY.withBold(this.profile.active()));
        UIBase.renderText(graphics, titleComponent, textX, titleY, ProfileUiRendering.getTextColor(), titleSize);
        if (!subtitle.isEmpty()) {
            UIBase.renderText(graphics, Component.literal(ProfileUiRendering.trimToWidth(subtitle, textWidth, subtitleSize)), textX, titleY + titleHeight + 2, ProfileUiRendering.getDimTextColor(), subtitleSize);
        }

    }

    @NotNull
    private String getSubtitle() {
        String authors = this.profile.metadata().getAuthorsAsString();
        if (!authors.isEmpty()) return I18n.get("fancymenu.profiles.by_authors", authors);
        String version = this.profile.metadata().getVersion();
        if (!version.isEmpty()) return I18n.get("fancymenu.profiles.version", version);
        return "";
    }

    @Override
    public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            this.setSelected(true);
            this.manager.onEntrySelected(this.profile);
            this.manager.openProfileContextMenu(this.profile);
            return;
        }
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return;
        long now = System.currentTimeMillis();
        boolean doubleClick = (now - this.lastClickTime) < DOUBLE_CLICK_MS;
        this.lastClickTime = doubleClick ? 0L : now;
        this.manager.onEntrySelected(this.profile);
        if (doubleClick && !this.profile.active()) this.manager.loadProfile(this.profile);
    }

}
