package de.keksuccino.fancymenu.customization.profile.gui;

import de.keksuccino.fancymenu.customization.profile.Profile;
import de.keksuccino.fancymenu.customization.profile.ProfileMetadata;
import de.keksuccino.fancymenu.customization.profile.ProfileTasks;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The header of the profile manager's detail view: icon, title, version, authors, stats and the clickable link chips.
 */
final class ProfileDetailsPanel {

    static final int ICON_SIZE = 64;
    private static final float CHIP_HEIGHT = 18.0F;
    private static final float CHIP_GAP = 5.0F;
    private static final float CHIP_ICON_SIZE = 11.0F;
    private static final int MAX_CHIP_ROWS = 3;

    @NotNull
    private final ManageProfilesWindowBody manager;
    @NotNull
    private final List<Chip> chips = new ArrayList<>();

    ProfileDetailsPanel(@NotNull ManageProfilesWindowBody manager) {
        this.manager = manager;
    }

    /**
     * @return the Y coordinate below the rendered header and link chips.
     */
    float render(@NotNull GuiGraphics graphics, @NotNull Profile profile, @Nullable ProfileTasks.ProfileStats stats, float x, float y, float width, int mouseX, int mouseY) {

        ProfileMetadata metadata = profile.metadata();
        ProfileUiRendering.renderProfileIcon(graphics, this.manager.getIcon(profile), metadata.getTitle(), profile.getId(), x, y, ICON_SIZE);

        float textX = x + ICON_SIZE + 12;
        float textWidth = Math.max(20, (x + width) - textX);
        float largeSize = UIBase.getUITextSizeLarge();
        float normalSize = UIBase.getUITextSizeNormal();
        float smallSize = UIBase.getUITextSizeSmall();
        float largeHeight = UIBase.getUITextHeight(largeSize);
        float normalHeight = UIBase.getUITextHeight(normalSize);
        float smallHeight = UIBase.getUITextHeight(smallSize);

        // Title with version pill
        Component versionText = metadata.getVersion().isEmpty() ? null : Component.literal(I18n.get("fancymenu.profiles.version", metadata.getVersion()));
        float pillHeight = smallHeight + 4;
        float versionWidth = (versionText != null) ? (ProfileUiRendering.getPillWidth(versionText, pillHeight) + 8) : 0;
        String title = ProfileUiRendering.trimToWidth(this.manager.getDisplayTitle(profile), Math.max(10, textWidth - versionWidth), largeSize);
        Component titleComponent = Component.literal(title).withStyle(Style.EMPTY.withBold(true));
        float titleY = y + 2;
        UIBase.renderText(graphics, titleComponent, textX, titleY, ProfileUiRendering.getTextColor(), largeSize);
        if (versionText != null) {
            float pillX = textX + UIBase.getUITextWidth(titleComponent, largeSize) + 8;
            ProfileUiRendering.renderPill(graphics, versionText, pillX, titleY + ((largeHeight - pillHeight) / 2.0F), pillHeight, ProfileUiRendering.getDimTextColor(), ProfileUiRendering.getSecondaryAreaColor());
        }

        // Authors
        float lineY = titleY + largeHeight + 5;
        String authors = metadata.getAuthorsAsString();
        if (!authors.isEmpty()) {
            String authorsLine = ProfileUiRendering.trimToWidth(I18n.get("fancymenu.profiles.by_authors", authors), textWidth, normalSize);
            UIBase.renderText(graphics, Component.literal(authorsLine), textX, lineY, ProfileUiRendering.getDimTextColor(), normalSize);
            lineY += normalHeight + 4;
        }

        // Stats
        if (stats != null) {
            String statsText = I18n.get("fancymenu.profiles.stats", stats.layoutCount(), ProfileUiRendering.formatBytes(stats.sizeBytes()));
            UIBase.renderText(graphics, Component.literal(ProfileUiRendering.trimToWidth(statsText, textWidth, smallSize)), textX, lineY, ProfileUiRendering.getDimTextColor(), smallSize);
        }

        float bottom = y + ICON_SIZE;
        this.layoutChips(metadata, x, bottom + 12, width);
        boolean hasChips = !this.chips.isEmpty();
        for (Chip chip : this.chips) {
            this.renderChip(graphics, chip, mouseX, mouseY);
        }
        if (hasChips) {
            Chip last = this.chips.get(this.chips.size() - 1);
            bottom = last.y + CHIP_HEIGHT;
        }
        return bottom;

    }

    private void layoutChips(@NotNull ProfileMetadata metadata, float x, float y, float width) {
        this.chips.clear();
        List<Chip> candidates = new ArrayList<>();
        for (ProfileMetadata.Link link : metadata.getLinks()) {
            Component label = link.label().isEmpty() ? ProfileUiRendering.getLinkTypeName(link.type()) : Component.literal(link.label());
            candidates.add(new Chip(ProfileUiRendering.getLinkIcon(link.type()), label, link.url()));
        }
        if (metadata.getIssueTrackerUrl() != null) {
            candidates.add(new Chip(MaterialIcons.BUG_REPORT, Component.translatable("fancymenu.profiles.links.report_issue"), metadata.getIssueTrackerUrl()));
        }
        float chipX = x;
        float chipY = y;
        int row = 0;
        float smallSize = UIBase.getUITextSizeSmall();
        for (Chip chip : candidates) {
            // Trimming happens here and not while rendering, so a label that fits never gets cut by rounding differences
            chip.displayLabel = ProfileUiRendering.trimToWidth(chip.label.getString(), width - 40, smallSize);
            float labelWidth = UIBase.getUITextWidth(chip.displayLabel, smallSize);
            chip.width = 8 + CHIP_ICON_SIZE + 5 + labelWidth + 9;
            if ((chipX > x) && ((chipX + chip.width) > (x + width))) {
                row++;
                if (row >= MAX_CHIP_ROWS) break;
                chipX = x;
                chipY += CHIP_HEIGHT + CHIP_GAP;
            }
            chip.x = chipX;
            chip.y = chipY;
            this.chips.add(chip);
            chipX += chip.width + CHIP_GAP;
        }
    }

    private void renderChip(@NotNull GuiGraphics graphics, @NotNull Chip chip, int mouseX, int mouseY) {
        boolean hovered = chip.isHovered(mouseX, mouseY);
        float radius = CHIP_HEIGHT / 2.0F;
        UIBase.renderRoundedRect(graphics, chip.x, chip.y, chip.width, CHIP_HEIGHT, radius, radius, radius, radius, hovered ? ProfileUiRendering.getHoverColor() : ProfileUiRendering.getSecondaryAreaColor());
        int contentColor = hovered ? ProfileUiRendering.getTextColor() : ProfileUiRendering.getDimTextColor();
        chip.icon.render(graphics, chip.x + 8, chip.y + ((CHIP_HEIGHT - CHIP_ICON_SIZE) / 2.0F), CHIP_ICON_SIZE, CHIP_ICON_SIZE, contentColor);
        float smallSize = UIBase.getUITextSizeSmall();
        float labelX = chip.x + 8 + CHIP_ICON_SIZE + 5;
        UIBase.renderText(graphics, Component.literal(chip.displayLabel), labelX, chip.y + ((CHIP_HEIGHT - UIBase.getUITextHeight(smallSize)) / 2.0F), contentColor, smallSize);
        if (hovered) {
            CursorHandler.setClientTickCursor(CursorHandler.CURSOR_POINTING_HAND);
            TooltipHandler.INSTANCE.addRenderTickTooltip(UITooltip.of(Component.literal(chip.url)), () -> true);
        }
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false;
        for (Chip chip : this.chips) {
            if (chip.isHovered(mouseX, mouseY)) {
                // Only http(s) links ever get here, because the metadata drops everything else
                WebUtils.openWebLink(chip.url);
                return true;
            }
        }
        return false;
    }

    private static final class Chip {

        @NotNull
        private final MaterialIcon icon;
        @NotNull
        private final Component label;
        @NotNull
        private final String url;
        @NotNull
        private String displayLabel = "";
        private float x;
        private float y;
        private float width;

        private Chip(@NotNull MaterialIcon icon, @NotNull Component label, @NotNull String url) {
            this.icon = icon;
            this.label = label;
            this.url = url;
        }

        private boolean isHovered(double mouseX, double mouseY) {
            return UIBase.isXYInArea(mouseX, mouseY, this.x, this.y, this.width, CHIP_HEIGHT);
        }

    }

}
