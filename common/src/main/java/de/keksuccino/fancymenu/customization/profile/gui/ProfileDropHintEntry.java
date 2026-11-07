package de.keksuccino.fancymenu.customization.profile.gui;

import de.keksuccino.fancymenu.util.input.FileDragTracker;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * The last entry of the profile list, which tells users that they can drop profile files onto the window to add them.<br>
 * It can't get selected or clicked, and lights up while a file gets dragged over the game.
 */
class ProfileDropHintEntry extends ScrollAreaEntry {

    private static final int ICON_SLOT_SIZE = 28;
    private static final float ICON_SIZE = 16.0F;
    private static final int PADDING = 6;

    ProfileDropHintEntry(@NotNull ScrollArea parent) {
        super(parent, 100, ProfileListEntry.HEIGHT);
        this.setSelectable(false);
        this.setClickable(false);
        this.setPlayClickSound(false);
        this.setBackgroundColorNormal(null);
        this.setBackgroundColorHover(null);
    }

    @Override
    public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        boolean fileDragged = FileDragTracker.isDragging();
        int color = fileDragged ? ProfileUiRendering.getAccentColor() : ProfileUiRendering.getDimTextColor();

        // The icon sits in an empty slot where profiles show their icon, so the hint lines up with the profiles above it
        float slotX = this.x + PADDING;
        float slotY = this.y + ((this.height - ICON_SLOT_SIZE) / 2.0F);
        float radius = UIBase.getInterfaceCornerRoundingRadius();
        if (fileDragged) {
            UIBase.renderRoundedRect(graphics, slotX, slotY, ICON_SLOT_SIZE, ICON_SLOT_SIZE, radius, radius, radius, radius, ProfileUiRendering.withAlpha(color, 0.12F));
        }
        UIBase.renderRoundedBorder(graphics, slotX, slotY, slotX + ICON_SLOT_SIZE, slotY + ICON_SLOT_SIZE, 1, radius, radius, radius, radius, ProfileUiRendering.withAlpha(color, fileDragged ? 1.0F : 0.5F));
        ProfileUiRendering.renderMaterialIcon(graphics, MaterialIcons.DOWNLOAD, slotX + ((ICON_SLOT_SIZE - ICON_SIZE) / 2.0F), slotY + ((ICON_SLOT_SIZE - ICON_SIZE) / 2.0F), ICON_SIZE, color);

        float textX = slotX + ICON_SLOT_SIZE + 8;
        float textWidth = Math.max(10, (this.x + this.width - PADDING - 4) - textX);
        float titleSize = UIBase.getUITextSizeNormal();
        float descriptionSize = UIBase.getUITextSizeSmall();
        float titleHeight = UIBase.getUITextHeight(titleSize);
        float titleY = this.y + ((this.height - (titleHeight + 2 + UIBase.getUITextHeight(descriptionSize))) / 2.0F);
        UIBase.renderText(graphics, Component.literal(ProfileUiRendering.trimToWidth(I18n.get("fancymenu.profiles.drop.hint"), textWidth, titleSize)), textX, titleY, color, titleSize);
        UIBase.renderText(graphics, Component.literal(ProfileUiRendering.trimToWidth(I18n.get("fancymenu.profiles.drop.hint.desc"), textWidth, descriptionSize)), textX, titleY + titleHeight + 2, ProfileUiRendering.getDimTextColor(), descriptionSize);

    }

    @Override
    public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
    }

}
