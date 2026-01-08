package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy.buddy.BuddyWidget;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class BuddyDecorationOverlay extends AbstractDecorationOverlay<BuddyDecorationOverlay> {

    public final Property<ResourceSupplier<ITexture>> customAtlasTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_atlas_texture", null, "fancymenu.decoration_overlays.buddy.custom_atlas_texture", true, true, true, null));

    private final BuddyWidget buddyWidget = new BuddyWidget(0, 0);
    private int lastGuiTick = -1;

    public BuddyDecorationOverlay() {
        this.customAtlasTexture.addValueSetListener((oldValue, newValue) -> this.buddyWidget.setCustomAtlasTextureSupplier(newValue));
        this.buddyWidget.setCustomAtlasTextureSupplier(this.customAtlasTexture.get());
        this.showOverlay.addValueSetListener((oldValue, newValue) -> {
            if (Boolean.TRUE.equals(newValue)) {
                resetTickCounter();
            } else if (Boolean.TRUE.equals(oldValue)) {
                this.buddyWidget.cleanup();
            }
        });
    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {
        this.customAtlasTexture.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable("fancymenu.decoration_overlays.buddy.custom_atlas_texture.desc")));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        tickBuddyIfNeeded();
        this.buddyWidget.render(graphics, mouseX, mouseY, partial);
    }

    @Override
    public void onScreenInitializedOrResized(@NotNull Screen screen, @NotNull List<AbstractElement> elements) {
        this.buddyWidget.setScreenSize(screen.width, screen.height);
        resetTickCounter();
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        this.buddyWidget.cleanup();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.showOverlay.tryGetNonNullElse(false)) {
            return false;
        }
        return this.buddyWidget.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.showOverlay.tryGetNonNullElse(false)) {
            return false;
        }
        return this.buddyWidget.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.showOverlay.tryGetNonNullElse(false)) {
            return false;
        }
        return this.buddyWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (!this.showOverlay.tryGetNonNullElse(false)) {
            return false;
        }
        return this.buddyWidget.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
    }

    private void resetTickCounter() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.gui != null) {
            this.lastGuiTick = minecraft.gui.getGuiTicks();
        } else {
            this.lastGuiTick = -1;
        }
    }

    private void tickBuddyIfNeeded() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.gui == null) {
            return;
        }
        int guiTick = minecraft.gui.getGuiTicks();
        if (this.lastGuiTick < 0 || guiTick < this.lastGuiTick) {
            this.lastGuiTick = guiTick;
            return;
        }
        while (this.lastGuiTick < guiTick) {
            this.buddyWidget.tick();
            this.lastGuiTick++;
        }
    }

}
