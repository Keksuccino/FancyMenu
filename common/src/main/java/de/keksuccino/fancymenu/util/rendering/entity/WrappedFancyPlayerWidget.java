package de.keksuccino.fancymenu.util.rendering.entity;

import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import it.crystalnest.fancy_entity_renderer.api.entity.player.FancyPlayerWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

/**
 * Adapts FER's optional player widget to FancyMenu's widget contracts.
 * {@link de.keksuccino.fancymenu.customization.element.elements.playerentity.PlayerEntityElement} only constructs this adapter after FER detection, so the typed references do not make FER mandatory.
 * Delegation must stay typed because loader remappers cannot rewrite Minecraft method names hidden in reflection strings.
 */
public class WrappedFancyPlayerWidget extends AbstractWidget implements NavigatableWidget {

    private boolean focusable = true;
    private boolean navigatable = true;

    @NotNull
    public static WrappedFancyPlayerWidget build(int x, int y, int width, int height) {
        return new WrappedFancyPlayerWidget(x, y, width, height);
    }

    @NotNull
    protected final FancyPlayerWidget wrapped;

    protected WrappedFancyPlayerWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.wrapped = new FancyPlayerWidget(x, y, width, height);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.wrapped.render(graphics, mouseX, mouseY, partial);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        this.wrapped.updateNarration(output);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.wrapped.setX(x);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.wrapped.setY(y);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        this.wrapped.setWidth(width);
    }

    public void setHeight(int height) {
        this.height = height;
        this.setWrappedHeight(height);
    }

    public void setSize(int width, int height) {
        this.setWidth(width);
        this.height = height;
        this.setWrappedHeight(height);
    }

    @Override
    public boolean isFocusable() {
        return this.focusable;
    }

    @Override
    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    @Override
    public boolean isNavigatable() {
        return this.navigatable;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
        this.navigatable = navigatable;
    }

    public WrappedFancyPlayerWidget setBodyFollowsMouse(boolean followsMouse) {
        this.wrapped.setBodyFollowsMouse(followsMouse);
        return this;
    }

    public WrappedFancyPlayerWidget setHeadFollowsMouse(boolean followsMouse) {
        this.wrapped.setHeadFollowsMouse(followsMouse);
        return this;
    }

    public WrappedFancyPlayerWidget setHeadRotation(float x, float y, float z) {
        this.wrapped.setHeadRotation(x, y, z);
        return this;
    }

    public WrappedFancyPlayerWidget setBodyRotation(float x, float y, float z) {
        this.wrapped.setBodyRotation(x, y, z);
        return this;
    }

    public WrappedFancyPlayerWidget setLeftArmRotation(float x, float y, float z) {
        this.wrapped.setLeftArmRotation(x, y, z);
        return this;
    }

    public WrappedFancyPlayerWidget setRightArmRotation(float x, float y, float z) {
        this.wrapped.setRightArmRotation(x, y, z);
        return this;
    }

    public WrappedFancyPlayerWidget setLeftLegRotation(float x, float y, float z) {
        this.wrapped.setLeftLegRotation(x, y, z);
        return this;
    }

    public WrappedFancyPlayerWidget setRightLegRotation(float x, float y, float z) {
        this.wrapped.setRightLegRotation(x, y, z);
        return this;
    }

    public WrappedFancyPlayerWidget setSlim(boolean isSlim) {
        this.wrapped.setSlim(isSlim);
        return this;
    }

    public WrappedFancyPlayerWidget setSkin(@NotNull ResourceLocation skin, @Nullable ResourceLocation cape, boolean slim) {
        this.wrapped.setSkin(skin, slim);
        this.setCape(cape);
        return this;
    }

    public WrappedFancyPlayerWidget copyLocalPlayer() {
        this.wrapped.copyLocalPlayer();
        return this;
    }

    public WrappedFancyPlayerWidget setName(String name) {
        this.wrapped.setName(name);
        return this;
    }

    public WrappedFancyPlayerWidget setPinName(boolean pin) {
        this.wrapped.setPinName(pin);
        return this;
    }

    public WrappedFancyPlayerWidget setShowName(boolean showName) {
        this.wrapped.setShowName(showName);
        return this;
    }

    public WrappedFancyPlayerWidget setShowCape(boolean showCape) {
        this.wrapped.setShowCape(showCape);
        return this;
    }

    public WrappedFancyPlayerWidget setShowLeftArm(boolean showLeftArm) {
        this.wrapped.setShowLeftArm(showLeftArm);
        return this;
    }

    public WrappedFancyPlayerWidget setShowLeftSleeve(boolean showLeftSleeve) {
        this.wrapped.setShowLeftSleeve(showLeftSleeve);
        return this;
    }

    public WrappedFancyPlayerWidget setShowRightArm(boolean showRightArm) {
        this.wrapped.setShowRightArm(showRightArm);
        return this;
    }

    public WrappedFancyPlayerWidget setShowRightSleeve(boolean showRightSleeve) {
        this.wrapped.setShowRightSleeve(showRightSleeve);
        return this;
    }

    public WrappedFancyPlayerWidget setShowLeftLeg(boolean showLeftLeg) {
        this.wrapped.setShowLeftLeg(showLeftLeg);
        return this;
    }

    public WrappedFancyPlayerWidget setShowLeftPants(boolean showLeftPants) {
        this.wrapped.setShowLeftPants(showLeftPants);
        return this;
    }

    public WrappedFancyPlayerWidget setShowRightLeg(boolean showRightLeg) {
        this.wrapped.setShowRightLeg(showRightLeg);
        return this;
    }

    public WrappedFancyPlayerWidget setShowRightPants(boolean showRightPants) {
        this.wrapped.setShowRightPants(showRightPants);
        return this;
    }

    public WrappedFancyPlayerWidget setShowHead(boolean showHead) {
        this.wrapped.setShowHead(showHead);
        return this;
    }

    public WrappedFancyPlayerWidget setShowHat(boolean showHat) {
        this.wrapped.setShowHat(showHat);
        return this;
    }

    public WrappedFancyPlayerWidget setShowBody(boolean showBody) {
        this.wrapped.setShowBody(showBody);
        return this;
    }

    public WrappedFancyPlayerWidget setShowJacket(boolean showJacket) {
        this.wrapped.setShowJacket(showJacket);
        return this;
    }

    public WrappedFancyPlayerWidget setPose(@NotNull Pose pose) {
        this.wrapped.setPose(pose);
        return this;
    }

    public WrappedFancyPlayerWidget setBaby(boolean isBaby) {
        this.wrapped.setBaby(isBaby);
        return this;
    }

    public WrappedFancyPlayerWidget setParrots(@Nullable Parrot.Variant leftParrot, @Nullable Parrot.Variant rightParrot) {
        this.wrapped.setParrots(leftParrot, rightParrot);
        return this;
    }

    public WrappedFancyPlayerWidget setBodyMovement(boolean shouldMove) {
        this.wrapped.setMoving(shouldMove);
        return this;
    }

    public WrappedFancyPlayerWidget setRightHandItem(@Nullable Item item) {
        this.wrapped.setRightHandItem(item);
        return this;
    }

    public WrappedFancyPlayerWidget setRightHandItem(@Nullable ItemStack item) {
        this.wrapped.setRightHandItem(item);
        return this;
    }

    public WrappedFancyPlayerWidget setLeftHandItem(@Nullable Item item) {
        this.wrapped.setLeftHandItem(item);
        return this;
    }

    public WrappedFancyPlayerWidget setLeftHandItem(@Nullable ItemStack item) {
        this.wrapped.setLeftHandItem(item);
        return this;
    }

    public WrappedFancyPlayerWidget setHeadWearable(@Nullable Item item) {
        this.wrapped.setHeadWearable(item);
        return this;
    }

    public WrappedFancyPlayerWidget setHeadWearable(@Nullable ItemStack item) {
        this.wrapped.setHeadWearable(item);
        return this;
    }

    public WrappedFancyPlayerWidget setChestWearable(@Nullable Item item) {
        this.wrapped.setChestWearable(item);
        return this;
    }

    public WrappedFancyPlayerWidget setChestWearable(@Nullable ItemStack item) {
        this.wrapped.setChestWearable(item);
        return this;
    }

    public WrappedFancyPlayerWidget setLegsWearable(@Nullable Item item) {
        this.wrapped.setLegsWearable(item);
        return this;
    }

    public WrappedFancyPlayerWidget setLegsWearable(@Nullable ItemStack item) {
        this.wrapped.setLegsWearable(item);
        return this;
    }

    public WrappedFancyPlayerWidget setFeetWearable(@Nullable Item item) {
        this.wrapped.setFeetWearable(item);
        return this;
    }

    public WrappedFancyPlayerWidget setFeetWearable(@Nullable ItemStack item) {
        this.wrapped.setFeetWearable(item);
        return this;
    }

    public WrappedFancyPlayerWidget copyPlayer(String profileName) {
        this.wrapped.copyPlayer(profileName);
        return this;
    }

    public WrappedFancyPlayerWidget copyPlayer(UUID profileId) {
        this.wrapped.copyPlayer(profileId);
        return this;
    }

    public WrappedFancyPlayerWidget uncopyPlayer() {
        this.wrapped.uncopyPlayer();
        return this;
    }

    private void setWrappedHeight(int height) {
        ((IMixinAbstractWidget) this.wrapped).setHeightFancyMenu(height);
    }

    private void setCape(@Nullable ResourceLocation cape) {
        try {
            ((FancyPlayerWidgetBridge) this.wrapped).setCape_FancyMenu(cape);
        } catch (Throwable ignored) {}
    }

}
