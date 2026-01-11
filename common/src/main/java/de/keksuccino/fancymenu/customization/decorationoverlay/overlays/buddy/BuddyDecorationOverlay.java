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
import java.util.function.Consumer;

public class BuddyDecorationOverlay extends AbstractDecorationOverlay<BuddyDecorationOverlay> {

    public final Property<ResourceSupplier<ITexture>> customAtlasTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_atlas_texture", null, "fancymenu.decoration_overlays.buddy.custom_atlas_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customThoughtBubbleTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_thought_texture", null, "fancymenu.decoration_overlays.buddy.custom_thought_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customPetIconTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_pet_icon_texture", null, "fancymenu.decoration_overlays.buddy.custom_pet_icon_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customPlayIconTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_play_icon_texture", null, "fancymenu.decoration_overlays.buddy.custom_play_icon_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customFoodTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_food_texture", null, "fancymenu.decoration_overlays.buddy.custom_food_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customBallTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_ball_texture", null, "fancymenu.decoration_overlays.buddy.custom_ball_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customPoopTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_poop_texture", null, "fancymenu.decoration_overlays.buddy.custom_poop_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customStatusBackgroundTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_status_background_texture", null, "fancymenu.decoration_overlays.buddy.custom_status_background_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customStatusBorderTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_status_border_texture", null, "fancymenu.decoration_overlays.buddy.custom_status_border_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customTabButtonTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_tab_button_texture", null, "fancymenu.decoration_overlays.buddy.custom_tab_button_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customTabButtonSelectedTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_tab_button_selected_texture", null, "fancymenu.decoration_overlays.buddy.custom_tab_button_selected_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customDefaultButtonTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_default_button_texture", null, "fancymenu.decoration_overlays.buddy.custom_default_button_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customDefaultButtonHoverTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_default_button_hover_texture", null, "fancymenu.decoration_overlays.buddy.custom_default_button_hover_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customDefaultButtonInactiveTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_default_button_inactive_texture", null, "fancymenu.decoration_overlays.buddy.custom_default_button_inactive_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customCloseButtonTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_close_button_texture", null, "fancymenu.decoration_overlays.buddy.custom_close_button_texture", true, true, true, null));
    public final Property<ResourceSupplier<ITexture>> customCloseButtonHoverTexture = putProperty(Property.resourceSupplierProperty(ITexture.class, "custom_buddy_close_button_hover_texture", null, "fancymenu.decoration_overlays.buddy.custom_close_button_hover_texture", true, true, true, null));

    private final BuddyWidget buddyWidget = new BuddyWidget(0, 0);
    private int lastGuiTick = -1;

    public BuddyDecorationOverlay() {
        this.buddyWidget.setInstanceIdentifier(getInstanceIdentifier());
        bindTextureProperty(this.customAtlasTexture, this.buddyWidget::setCustomAtlasTextureSupplier);
        bindTextureProperty(this.customThoughtBubbleTexture, supplier -> this.buddyWidget.getTextures().setCustomThoughtBubbleTextureSupplier(supplier));
        bindTextureProperty(this.customPetIconTexture, supplier -> this.buddyWidget.getTextures().setCustomPetIconTextureSupplier(supplier));
        bindTextureProperty(this.customPlayIconTexture, supplier -> this.buddyWidget.getTextures().setCustomPlayIconTextureSupplier(supplier));
        bindTextureProperty(this.customFoodTexture, supplier -> this.buddyWidget.getTextures().setCustomFoodTextureSupplier(supplier));
        bindTextureProperty(this.customBallTexture, supplier -> this.buddyWidget.getTextures().setCustomBallTextureSupplier(supplier));
        bindTextureProperty(this.customPoopTexture, supplier -> this.buddyWidget.getTextures().setCustomPoopTextureSupplier(supplier));
        bindTextureProperty(this.customStatusBackgroundTexture, supplier -> this.buddyWidget.getTextures().setCustomStatusBackgroundTextureSupplier(supplier));
        bindTextureProperty(this.customStatusBorderTexture, supplier -> this.buddyWidget.getTextures().setCustomStatusBorderTextureSupplier(supplier));
        bindTextureProperty(this.customTabButtonTexture, supplier -> this.buddyWidget.getTextures().setCustomTabButtonTextureSupplier(supplier));
        bindTextureProperty(this.customTabButtonSelectedTexture, supplier -> this.buddyWidget.getTextures().setCustomTabButtonSelectedTextureSupplier(supplier));
        bindTextureProperty(this.customDefaultButtonTexture, supplier -> this.buddyWidget.getTextures().setCustomDefaultButtonTextureSupplier(supplier));
        bindTextureProperty(this.customDefaultButtonHoverTexture, supplier -> this.buddyWidget.getTextures().setCustomDefaultButtonHoverTextureSupplier(supplier));
        bindTextureProperty(this.customDefaultButtonInactiveTexture, supplier -> this.buddyWidget.getTextures().setCustomDefaultButtonInactiveTextureSupplier(supplier));
        bindTextureProperty(this.customCloseButtonTexture, supplier -> this.buddyWidget.getTextures().setCustomCloseButtonTextureSupplier(supplier));
        bindTextureProperty(this.customCloseButtonHoverTexture, supplier -> this.buddyWidget.getTextures().setCustomCloseButtonHoverTextureSupplier(supplier));
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
        ContextMenu texturesMenu = new ContextMenu();
        menu.addSubMenuEntry("buddy_custom_textures", Component.translatable("fancymenu.decoration_overlays.buddy.custom_textures"), texturesMenu).setStackable(true);

        addTextureEntry(texturesMenu, this.customAtlasTexture, "fancymenu.decoration_overlays.buddy.custom_atlas_texture.desc");
        addTextureEntry(texturesMenu, this.customThoughtBubbleTexture, "fancymenu.decoration_overlays.buddy.custom_thought_texture.desc");
        addTextureEntry(texturesMenu, this.customPetIconTexture, "fancymenu.decoration_overlays.buddy.custom_pet_icon_texture.desc");
        addTextureEntry(texturesMenu, this.customPlayIconTexture, "fancymenu.decoration_overlays.buddy.custom_play_icon_texture.desc");
        addTextureEntry(texturesMenu, this.customFoodTexture, "fancymenu.decoration_overlays.buddy.custom_food_texture.desc");
        addTextureEntry(texturesMenu, this.customBallTexture, "fancymenu.decoration_overlays.buddy.custom_ball_texture.desc");
        addTextureEntry(texturesMenu, this.customPoopTexture, "fancymenu.decoration_overlays.buddy.custom_poop_texture.desc");
        addTextureEntry(texturesMenu, this.customStatusBackgroundTexture, "fancymenu.decoration_overlays.buddy.custom_status_background_texture.desc");
        addTextureEntry(texturesMenu, this.customStatusBorderTexture, "fancymenu.decoration_overlays.buddy.custom_status_border_texture.desc");
        addTextureEntry(texturesMenu, this.customTabButtonTexture, "fancymenu.decoration_overlays.buddy.custom_tab_button_texture.desc");
        addTextureEntry(texturesMenu, this.customTabButtonSelectedTexture, "fancymenu.decoration_overlays.buddy.custom_tab_button_selected_texture.desc");
        addTextureEntry(texturesMenu, this.customDefaultButtonTexture, "fancymenu.decoration_overlays.buddy.custom_default_button_texture.desc");
        addTextureEntry(texturesMenu, this.customDefaultButtonHoverTexture, "fancymenu.decoration_overlays.buddy.custom_default_button_hover_texture.desc");
        addTextureEntry(texturesMenu, this.customDefaultButtonInactiveTexture, "fancymenu.decoration_overlays.buddy.custom_default_button_inactive_texture.desc");
        addTextureEntry(texturesMenu, this.customCloseButtonTexture, "fancymenu.decoration_overlays.buddy.custom_close_button_texture.desc");
        addTextureEntry(texturesMenu, this.customCloseButtonHoverTexture, "fancymenu.decoration_overlays.buddy.custom_close_button_hover_texture.desc");
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
    public void setInstanceIdentifier(@NotNull String instanceIdentifier) {
        super.setInstanceIdentifier(instanceIdentifier);
        this.buddyWidget.setInstanceIdentifier(instanceIdentifier);
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

    private void bindTextureProperty(@NotNull Property<ResourceSupplier<ITexture>> property, @NotNull Consumer<ResourceSupplier<ITexture>> setter) {
        property.addValueSetListener((oldValue, newValue) -> setter.accept(newValue));
        setter.accept(property.get());
    }

    private void addTextureEntry(@NotNull ContextMenu menu, @NotNull Property<ResourceSupplier<ITexture>> property, @NotNull String descriptionKey) {
        property.buildContextMenuEntryAndAddTo(menu, this)
                .setTooltipSupplier((menu1, entry) -> Tooltip.of(Component.translatable(descriptionKey)));
    }
}
