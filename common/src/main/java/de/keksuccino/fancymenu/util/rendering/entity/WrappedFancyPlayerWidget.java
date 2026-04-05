package de.keksuccino.fancymenu.util.rendering.entity;

import de.keksuccino.fancymenu.util.ItemStackUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SuppressWarnings("unused")
public class WrappedFancyPlayerWidget extends AbstractWidget implements NavigatableWidget {

    private static final int BACKGROUND_COLOR = 0xAA101318;
    private static final int BORDER_COLOR = 0xFF3D4652;
    private static final int HEADER_COLOR = 0xFF7AA2FF;

    private boolean focusable;
    private boolean navigatable;
    @NotNull
    private String name = "Steve";
    private boolean showName = true;
    private boolean slim;
    private boolean baby;
    private boolean headFollowsMouse = true;
    private boolean bodyFollowsMouse = true;
    @NotNull
    private Pose pose = Pose.STANDING;
    @Nullable
    private PlayerSkin skin;
    @Nullable
    private ItemStack leftHandItem;
    @Nullable
    private ItemStack rightHandItem;
    @Nullable
    private ItemStack headWearable;
    @Nullable
    private ItemStack chestWearable;
    @Nullable
    private ItemStack legsWearable;
    @Nullable
    private ItemStack feetWearable;

    @NotNull
    public static WrappedFancyPlayerWidget build(int x, int y, int width, int height) {
        return new WrappedFancyPlayerWidget(x, y, width, height);
    }

    protected WrappedFancyPlayerWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    @Override
    protected void extractWidgetRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        Font font = Minecraft.getInstance().font;
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();

        graphics.fill(x, y, x + width, y + height, BACKGROUND_COLOR);
        graphics.fill(x, y, x + width, y + 1, BORDER_COLOR);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER_COLOR);
        graphics.fill(x, y, x + 1, y + height, BORDER_COLOR);
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER_COLOR);

        int titleY = y + 6;
        int centerX = x + (width / 2);
        graphics.centeredText(font, "Player Preview", centerX, titleY, HEADER_COLOR);

        int infoY = titleY + font.lineHeight + 6;
        if (this.showName) {
            graphics.centeredText(font, this.name, centerX, infoY, 0xFFFFFFFF);
            infoY += font.lineHeight + 4;
        }

        graphics.centeredText(font, "Pose: " + this.pose.name().toLowerCase(), centerX, infoY, 0xFFCDD6E0);
        infoY += font.lineHeight + 3;

        String behaviorLine = (this.headFollowsMouse ? "Head tracks cursor" : "Head fixed")
                + " | "
                + (this.bodyFollowsMouse ? "Body tracks cursor" : "Body fixed");
        graphics.centeredText(font, behaviorLine, centerX, infoY, 0xFF9EA9B8);
        infoY += font.lineHeight + 3;

        String bodyLine = (this.slim ? "Slim" : "Wide")
                + (this.baby ? " | Baby" : "")
                + ((this.skin != null) ? " | Custom skin" : "");
        graphics.centeredText(font, bodyLine, centerX, infoY, 0xFF9EA9B8);

        int rowY = y + height - 24;
        int slotY = rowY + 4;
        int[] slotXs = {
                x + 6,
                x + 26,
                x + 46,
                x + width - 62,
                x + width - 42,
                x + width - 22
        };
        ItemStack[] stacks = {
                this.leftHandItem,
                this.rightHandItem,
                this.headWearable,
                this.chestWearable,
                this.legsWearable,
                this.feetWearable
        };

        for (int i = 0; i < slotXs.length; i++) {
            int slotX = slotXs[i];
            graphics.fill(slotX - 1, rowY - 1, slotX + 17, rowY + 17, BORDER_COLOR);
            graphics.fill(slotX, rowY, slotX + 16, rowY + 16, 0x661A2029);
            if ((stacks[i] != null) && !stacks[i].isEmpty()) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(slotX, slotY);
                graphics.item(stacks[i], 0, 0);
                graphics.pose().popMatrix();
            }
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
    }

    public WrappedFancyPlayerWidget setBodyFollowsMouse(boolean followsMouse) {
        this.bodyFollowsMouse = followsMouse;
        return this;
    }

    public WrappedFancyPlayerWidget setHeadFollowsMouse(boolean followsMouse) {
        this.headFollowsMouse = followsMouse;
        return this;
    }

    public WrappedFancyPlayerWidget setHeadRotation(float x, float y, float z) {
        return this;
    }

    public WrappedFancyPlayerWidget setBodyRotation(float x, float y, float z) {
        return this;
    }

    public WrappedFancyPlayerWidget setLeftArmRotation(float x, float y, float z) {
        return this;
    }

    public WrappedFancyPlayerWidget setRightArmRotation(float x, float y, float z) {
        return this;
    }

    public WrappedFancyPlayerWidget setLeftLegRotation(float x, float y, float z) {
        return this;
    }

    public WrappedFancyPlayerWidget setRightLegRotation(float x, float y, float z) {
        return this;
    }

    public WrappedFancyPlayerWidget setSlim(boolean isSlim) {
        this.slim = isSlim;
        return this;
    }

    public WrappedFancyPlayerWidget setSkin(@Nullable PlayerSkin skin) {
        this.skin = skin;
        return this;
    }

    public WrappedFancyPlayerWidget copyLocalPlayer() {
        return this;
    }

    public WrappedFancyPlayerWidget setName(String name) {
        this.name = (name == null || name.isBlank()) ? "Steve" : name;
        return this;
    }

    public WrappedFancyPlayerWidget setShowName(boolean showName) {
        this.showName = showName;
        return this;
    }

    public WrappedFancyPlayerWidget setUpsideDown(boolean isUpsideDown) {
        return this;
    }

    public WrappedFancyPlayerWidget setPose(@NotNull Pose pose) {
        this.pose = pose;
        return this;
    }

    public WrappedFancyPlayerWidget setGlowing(int glowColor) {
        return this;
    }

    public WrappedFancyPlayerWidget setMoving(boolean isMoving) {
        return this;
    }

    public WrappedFancyPlayerWidget setOnFire(boolean onFire) {
        return this;
    }

    public WrappedFancyPlayerWidget setOnFire(boolean onFire, Identifier fireType) {
        return this;
    }

    public WrappedFancyPlayerWidget setBaby(boolean isBaby) {
        this.baby = isBaby;
        return this;
    }

    public WrappedFancyPlayerWidget setParrots(@Nullable Parrot.Variant leftParrot, @Nullable Parrot.Variant rightParrot) {
        return this;
    }

    public WrappedFancyPlayerWidget setBodyMovement(boolean shouldMove) {
        return this;
    }

    public WrappedFancyPlayerWidget setRightHandItem(@Nullable Item item) {
        this.rightHandItem = item == null ? null : ItemStackUtils.createDisplayStack(item);
        return this;
    }

    public WrappedFancyPlayerWidget setRightHandItem(@Nullable ItemStack item) {
        this.rightHandItem = copyItem(item);
        return this;
    }

    public WrappedFancyPlayerWidget setLeftHandItem(@Nullable Item item) {
        this.leftHandItem = item == null ? null : ItemStackUtils.createDisplayStack(item);
        return this;
    }

    public WrappedFancyPlayerWidget setLeftHandItem(@Nullable ItemStack item) {
        this.leftHandItem = copyItem(item);
        return this;
    }

    public WrappedFancyPlayerWidget setHeadWearable(@Nullable String item, HolderLookup.Provider provider) {
        this.headWearable = null;
        return this;
    }

    public WrappedFancyPlayerWidget setChestWearable(@Nullable String item, HolderLookup.Provider provider) {
        this.chestWearable = null;
        return this;
    }

    public WrappedFancyPlayerWidget setLegsWearable(@Nullable String item, HolderLookup.Provider provider) {
        this.legsWearable = null;
        return this;
    }

    public WrappedFancyPlayerWidget setFeetWearable(@Nullable String item, HolderLookup.Provider provider) {
        this.feetWearable = null;
        return this;
    }

    public WrappedFancyPlayerWidget setHeadWearable(@Nullable Item item) {
        this.headWearable = item == null ? null : ItemStackUtils.createDisplayStack(item);
        return this;
    }

    public WrappedFancyPlayerWidget setChestWearable(@Nullable Item item) {
        this.chestWearable = item == null ? null : ItemStackUtils.createDisplayStack(item);
        return this;
    }

    public WrappedFancyPlayerWidget setLegsWearable(@Nullable Item item) {
        this.legsWearable = item == null ? null : ItemStackUtils.createDisplayStack(item);
        return this;
    }

    public WrappedFancyPlayerWidget setFeetWearable(@Nullable Item item) {
        this.feetWearable = item == null ? null : ItemStackUtils.createDisplayStack(item);
        return this;
    }

    public WrappedFancyPlayerWidget setHeadWearable(@Nullable ItemStack item) {
        this.headWearable = copyItem(item);
        return this;
    }

    public WrappedFancyPlayerWidget setChestWearable(@Nullable ItemStack item) {
        this.chestWearable = copyItem(item);
        return this;
    }

    public WrappedFancyPlayerWidget setLegsWearable(@Nullable ItemStack item) {
        this.legsWearable = copyItem(item);
        return this;
    }

    public WrappedFancyPlayerWidget setFeetWearable(@Nullable ItemStack item) {
        this.feetWearable = copyItem(item);
        return this;
    }

    public WrappedFancyPlayerWidget copyPlayer(String profileName) {
        return this;
    }

    public WrappedFancyPlayerWidget copyPlayer(UUID profileId) {
        return this;
    }

    public WrappedFancyPlayerWidget uncopyPlayer() {
        return this;
    }

    public boolean isCopyingPlayer() {
        return false;
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

    @Nullable
    private static ItemStack copyItem(@Nullable ItemStack stack) {
        return stack == null ? null : stack.copy();
    }
}
