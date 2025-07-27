package de.keksuccino.fancymenu.util.rendering.entity;

import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import it.crystalnest.fancy_entity_renderer.api.Rotation;
import it.crystalnest.fancy_entity_renderer.api.entity.RenderMode;
import it.crystalnest.fancy_entity_renderer.api.entity.player.FancyPlayerWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

public class WrappedFancyPlayerWidget extends AbstractWidget implements NavigatableWidget {

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
        wrapped.render(graphics, mouseX, mouseY, partial);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        wrapped.updateNarration(output);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        wrapped.setX(x);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        wrapped.setY(y);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        wrapped.setWidth(width);
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        wrapped.setHeight(height);
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        wrapped.setSize(width, height);
    }

    /**
     * Sets whether the player's model should follow the mouse cursor.<br>
     * Overrides any manual body rotation set previously if {@code true}, otherwise restores the previous body rotation, if any.
     *
     * @param followsMouse whether to follow the mouse cursor.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setBodyFollowsMouse(boolean followsMouse) {
        wrapped.setBodyFollowsMouse(followsMouse);
        return this;
    }

    /**
     * Sets whether the player's head should follow the mouse cursor.<br>
     * Overrides any manual head rotation set previously if {@code true}, otherwise restores the previous head rotation, if any.
     *
     * @param followsMouse whether to follow the mouse cursor.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setHeadFollowsMouse(boolean followsMouse) {
        wrapped.setHeadFollowsMouse(followsMouse);
        return this;
    }

    /**
     * Sets a manual head rotation.<br>
     * If you want to set the rotation based on degree values, it's suggested to use {@link #setHeadRotation(float, float, float)} instead.
     *
     * @param rotation {@link Rotation} to set.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setHeadRotation(Rotation rotation) {
        wrapped.setHeadRotation(rotation);
        return this;
    }

    /**
     * Sets a manual head rotation.<br>
     * The values passed as parameters are assumed in degrees. If you want to use radians, use {@link #setHeadRotation(Rotation)} instead.
     *
     * @param x rotation around the X axis (horizontal).
     * @param y rotation around the Y axis (vertical).
     * @param z rotation around the Z axis (depth).
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setHeadRotation(float x, float y, float z) {
        wrapped.setHeadRotation(x, y, z);
        return this;
    }

    /**
     * Sets a manual rotation of the whole model.<br>
     * If you want to set the rotation based on degree values, it's suggested to use {@link #setBodyRotation(float, float, float)} instead.
     *
     * @param rotation {@link Rotation} to set.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setBodyRotation(Rotation rotation) {
        wrapped.setBodyRotation(rotation);
        return this;
    }

    /**
     * Sets a manual rotation of the whole model.<br>
     * The values passed as parameters are assumed in degrees. If you want to use radians, use {@link #setBodyRotation(Rotation)} instead.
     *
     * @param x rotation around the X axis (horizontal).
     * @param y rotation around the Y axis (vertical).
     * @param z rotation around the Z axis (depth).
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setBodyRotation(float x, float y, float z) {
        wrapped.setBodyRotation(x, y, z);
        return this;
    }

    /**
     * Sets a manual left arm rotation.<br>
     * If you want to set the rotation based on degree values, it's suggested to use {@link #setLeftArmRotation(float, float, float)} instead.
     *
     * @param rotation {@link Rotation} to set.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setLeftArmRotation(Rotation rotation) {
        wrapped.setLeftArmRotation(rotation);
        return this;
    }

    /**
     * Sets a manual left arm rotation.<br>
     * The values passed as parameters are assumed in degrees. If you want to use radians, use {@link #setLeftArmRotation(Rotation)} instead.
     *
     * @param x rotation around the X axis (horizontal).
     * @param y rotation around the Y axis (vertical).
     * @param z rotation around the Z axis (depth).
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setLeftArmRotation(float x, float y, float z) {
        wrapped.setLeftArmRotation(x, y, z);
        return this;
    }

    /**
     * Sets a manual right arm rotation.<br>
     * If you want to set the rotation based on degree values, it's suggested to use {@link #setRightArmRotation(float, float, float)} instead.
     *
     * @param rotation {@link Rotation} to set.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setRightArmRotation(Rotation rotation) {
        wrapped.setRightArmRotation(rotation);
        return this;
    }

    /**
     * Sets a manual right arm rotation.<br>
     * The values passed as parameters are assumed in degrees. If you want to use radians, use {@link #setRightArmRotation(Rotation)} instead.
     *
     * @param x rotation around the X axis (horizontal).
     * @param y rotation around the Y axis (vertical).
     * @param z rotation around the Z axis (depth).
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setRightArmRotation(float x, float y, float z) {
        wrapped.setRightArmRotation(x, y, z);
        return this;
    }

    /**
     * Sets a manual left leg rotation.<br>
     * If you want to set the rotation based on degree values, it's suggested to use {@link #setLeftLegRotation(float, float, float)} instead.
     *
     * @param rotation {@link Rotation} to set.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setLeftLegRotation(Rotation rotation) {
        wrapped.setLeftLegRotation(rotation);
        return this;
    }

    /**
     * Sets a manual left leg rotation.<br>
     * The values passed as parameters are assumed in degrees. If you want to use radians, use {@link #setLeftLegRotation(Rotation)} instead.
     *
     * @param x rotation around the X axis (horizontal).
     * @param y rotation around the Y axis (vertical).
     * @param z rotation around the Z axis (depth).
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setLeftLegRotation(float x, float y, float z) {
        wrapped.setLeftLegRotation(x, y, z);
        return this;
    }

    /**
     * Sets a manual right leg rotation.<br>
     * If you want to set the rotation based on degree values, it's suggested to use {@link #setRightLegRotation(float, float, float)} instead.
     *
     * @param rotation {@link Rotation} to set.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setRightLegRotation(Rotation rotation) {
        wrapped.setRightLegRotation(rotation);
        return this;
    }

    /**
     * Sets a manual right leg rotation.<br>
     * The values passed as parameters are assumed in degrees. If you want to use radians, use {@link #setRightLegRotation(Rotation)} instead.
     *
     * @param x rotation around the X axis (horizontal).
     * @param y rotation around the Y axis (vertical).
     * @param z rotation around the Z axis (depth).
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setRightLegRotation(float x, float y, float z) {
        wrapped.setRightLegRotation(x, y, z);
        return this;
    }

    /**
     * Makes the player slim or wide.<br>
     * If no skin is set (either manually or by copying a player), a random base skin is selected.
     *
     * @param isSlim whether the player should be slim.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setSlim(boolean isSlim) {
        wrapped.setSlim(isSlim);
        return this;
    }

    /**
     * Sets a custom skin for the player.<br>
     * Overrides the slim property if a valid skin. If {@code null}, restores the previous value for the slim property.
     *
     * @param skin {@link PlayerSkin}.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setSkin(@Nullable PlayerSkin skin) {
        wrapped.setSkin(skin);
        return this;
    }

    /**
     * Makes the model copy the local player.<br>
     * If you want to undo the copy, use {@link #uncopyPlayer()}.<br>
     * Overrides the slim and the skin properties.
     *
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget copyLocalPlayer() {
        wrapped.copyLocalPlayer();
        return this;
    }

    /**
     * Sets the player's name.<br>
     * If you want to change the name's visibility, use {@link #setShowName(boolean)}.
     *
     * @param name player's name.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setName(String name) {
        wrapped.setName(name);
        return this;
    }

    /**
     * Sets whether to show the player's name.
     *
     * @param showName whether to show the player's name.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setShowName(boolean showName) {
        wrapped.setShowName(showName);
        return this;
    }

    /**
     * Sets whether the player is rendered upside-down.
     *
     * @param isUpsideDown whether to render the player's upside-down.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setUpsideDown(boolean isUpsideDown) {
        wrapped.setUpsideDown(isUpsideDown);
        return this;
    }

    public WrappedFancyPlayerWidget setPose(@NotNull Pose pose) {
        wrapped.setPose(pose);
        return this;
    }

    public WrappedFancyPlayerWidget setRenderMode(@NotNull RenderMode renderMode) {
        wrapped.setRenderMode(renderMode);
        return this;
    }

    /**
     * Sets whether the player should glow.<p>
     * <b>WARNING: Experimental!</b><br>
     * Currently, it has no effect.
     *
     * @param isGlowing whether the player should glow.
     * @return {@code this}.
     */
    @ApiStatus.Experimental
    public WrappedFancyPlayerWidget setGlowing(boolean isGlowing) {
        wrapped.setGlowing(isGlowing);
        return this;
    }

    /**
     * Sets whether the player should be moving.<p>
     * <b>WARNING: Experimental!</b><br>
     * Currently, it just makes the player's arms move idly and has not been tested with custom arm rotations.
     *
     * @param isMoving whether the player should be moving.
     * @return {@code this}.
     */
    @ApiStatus.Experimental
    public WrappedFancyPlayerWidget setMoving(boolean isMoving) {
        wrapped.setMoving(isMoving);
        return this;
    }

    /**
     * Sets whether the player is on fire.<br>
     * If Soul Fire'd is installed, you can use {@link #setOnFire(boolean, ResourceLocation)} to specify the kind of fire.
     *
     * @param onFire whether the player is on fire.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setOnFire(boolean onFire) {
        wrapped.setOnFire(onFire);
        return this;
    }

    /**
     * Sets whether the player is on fire and what kind of fire it is.<br>
     * Effective only when Soul Fire'd is installed too.
     *
     * @param onFire whether the player is on fire.
     * @param fireType Soul Fire'd fire type.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setOnFire(boolean onFire, ResourceLocation fireType) {
        wrapped.setOnFire(onFire, fireType);
        return this;
    }

    /**
     * Sets whether the player is a baby.<br>
     * Overrides the visibility of the left and right parrots. If {@code true}, the parrots will be hidden. If {@code false}, any previously hidden parrots will be restored.
     *
     * @param isBaby whether the player is a baby.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setBaby(boolean isBaby) {
        wrapped.setBaby(isBaby);
        return this;
    }

    public WrappedFancyPlayerWidget setParrots(@Nullable Parrot.Variant leftParrot, @Nullable Parrot.Variant rightParrot) {
        wrapped.setParrots(leftParrot, rightParrot);
        return this;
    }

    public WrappedFancyPlayerWidget setBodyMovement(boolean shouldMove) {
        wrapped.setMoving(shouldMove);
        return this;
    }

    /**
     * Sets the item the player is holding in its right hand.<br>
     * Pass a valid item to set it, pass {@code null} to empty the hand.
     *
     * @param item item to set or {@code null}.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setRightHandItem(@Nullable Item item) {
        wrapped.setRightHandItem(item);
        return this;
    }

    /**
     * Sets the item the player is holding in its left hand.<br>
     * Pass a valid item to set it, pass {@code null} to empty the hand.
     *
     * @param item item to set or {@code null}.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setLeftHandItem(@Nullable Item item) {
        wrapped.setLeftHandItem(item);
        return this;
    }

    /**
     * Sets the item the player is wearing on its head.<br>
     * Pass a valid item string to set it, pass {@code null} to remove it.
     *
     * @param item item string, in the same format as for the command {@code /give}.
     * @param provider {@link HolderLookup.Provider} for registry access, for example from {@link net.minecraft.world.level.Level#registryAccess()}.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setHeadWearable(@Nullable String item, HolderLookup.Provider provider) {
        wrapped.setHeadWearable(item, provider);
        return this;
    }

    /**
     * Sets the item the player is wearing on its chest.<br>
     * Pass a valid item to set it, pass {@code null} to remove it.
     *
     * @param item item string, in the same format as for the command {@code /give}.
     * @param provider {@link HolderLookup.Provider} for registry access, for example from {@link net.minecraft.world.level.Level#registryAccess()}.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setChestWearable(@Nullable String item, HolderLookup.Provider provider) {
        wrapped.setChestWearable(item, provider);
        return this;
    }

    /**
     * Sets the item the player is wearing on its legs.<br>
     * Pass a valid item to set it, pass {@code null} to remove it.
     *
     * @param item item string, in the same format as for the command {@code /give}.
     * @param provider {@link HolderLookup.Provider} for registry access, for example from {@link net.minecraft.world.level.Level#registryAccess()}.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setLegsWearable(@Nullable String item, HolderLookup.Provider provider) {
        wrapped.setLegsWearable(item, provider);
        return this;
    }

    /**
     * Sets the item the player is wearing on its feet.<br>
     * Pass a valid item to set it, pass {@code null} to remove it.
     *
     * @param item item string, in the same format as for the command {@code /give}.
     * @param provider {@link HolderLookup.Provider} for registry access, for example from {@link net.minecraft.world.level.Level#registryAccess()}.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setFeetWearable(@Nullable String item, HolderLookup.Provider provider) {
        wrapped.setFeetWearable(item, provider);
        return this;
    }

    /**
     * Sets the item the player is wearing on its head.<br>
     * Pass a valid item to set it, pass {@code null} to remove it.
     *
     * @param item item to wear on the head.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setHeadWearable(@Nullable Item item) {
        wrapped.setHeadWearable(item);
        return this;
    }

    /**
     * Sets the item the player is wearing on its chest.<br>
     * Pass a valid item to set it, pass {@code null} to remove it.
     *
     * @param item item to wear on the chest.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setChestWearable(@Nullable Item item) {
        wrapped.setChestWearable(item);
        return this;
    }

    /**
     * Sets the item the player is wearing on its legs.<br>
     * Pass a valid item to set it, pass {@code null} to remove it.
     *
     * @param item item to wear on the legs.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setLegsWearable(@Nullable Item item) {
        wrapped.setLegsWearable(item);
        return this;
    }

    /**
     * Sets the item the player is wearing on its feet.<br>
     * Pass a valid item to set it, pass {@code null} to remove it.
     *
     * @param item item to wear on the feet.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setFeetWearable(@Nullable Item item) {
        wrapped.setFeetWearable(item);
        return this;
    }

    /**
     * Sets the item the player is wearing on its head.<br>
     * Pass a valid item to set it, pass {@code null} to remove it.
     *
     * @param item item to wear on the head.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setHeadWearable(@Nullable ItemStack item) {
        wrapped.setHeadWearable(item);
        return this;
    }

    /**
     * Sets the item the player is wearing on its chest.<br>
     * Pass a valid item to set it, pass {@code null} to remove it.
     *
     * @param item item to wear on the chest.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setChestWearable(@Nullable ItemStack item) {
        wrapped.setChestWearable(item);
        return this;
    }

    /**
     * Sets the item the player is wearing on its legs.<br>
     * Pass a valid item to set it, pass {@code null} to remove it.
     *
     * @param item item to wear on the legs.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setLegsWearable(@Nullable ItemStack item) {
        wrapped.setLegsWearable(item);
        return this;
    }

    /**
     * Sets the item the player is wearing on its feet.<br>
     * Pass a valid item to set it, pass {@code null} to remove it.
     *
     * @param item item to wear on the feet.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget setFeetWearable(@Nullable ItemStack item) {
        wrapped.setFeetWearable(item);
        return this;
    }

    /**
     * Copies a player from its profile name.<br>
     * Verify that the copy was successful by calling {@link #isCopyingPlayer()}.
     *
     * @param profileName profile name.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget copyPlayer(String profileName) {
        wrapped.copyPlayer(profileName);
        return this;
    }

    /**
     * Copies a player from its UUID.<br>
     * Verify that the copy was successful by calling {@link #isCopyingPlayer()}.
     *
     * @param profileId profile UUID.
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget copyPlayer(UUID profileId) {
        wrapped.copyPlayer(profileId);
        return this;
    }

    /**
     * Stops the widget from currently copying a player.
     *
     * @return {@code this}.
     */
    public WrappedFancyPlayerWidget uncopyPlayer() {
        wrapped.uncopyPlayer();
        return this;
    }

    /**
     * Returns whether the widget is currently copying a player (either local or remote).
     *
     * @return whether the widget is copying a player.
     */
    public boolean isCopyingPlayer() {
        return wrapped.isCopyingPlayer();
    }

    @Override
    public boolean isFocusable() {
        return false;
    }

    @Override
    public void setFocusable(boolean focusable) {
    }

    @Override
    public boolean isNavigatable() {
        return false;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
    }

}
