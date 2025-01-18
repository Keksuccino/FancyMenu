package de.keksuccino.fancymenu.customization.element.elements.item;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGuiGraphics;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    public ResourceSupplier<ITexture> textureSupplier;
    @NotNull
    public DrawableColor imageTint = DrawableColor.of("#FFFFFF");
    public boolean repeat = false;
    public boolean nineSlice = false;
    public int nineSliceBorderX = 5;
    public int nineSliceBorderY = 5;
    protected final Font font = Minecraft.getInstance().font;

    public ItemElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();
            int w = this.getAbsoluteWidth();
            int h = this.getAbsoluteHeight();

            RenderSystem.enableBlend();

            ItemStack stack = new ItemStack(Items.POTATO);
            stack.setCount(20);

            this.renderItem(graphics, x, y, w, h, stack);

            RenderSystem.disableBlend();

        }

    }

    protected void renderItem(GuiGraphics graphics, int x, int y, int width, int height, @NotNull ItemStack itemStack) {

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 100.0F);

        int size = Math.max(width, height);
        int randomSeed = x + y * this.getAbsoluteWidth();
        this._renderItem(graphics, x, y, size, randomSeed, itemStack);
        this.renderItemCount(graphics, this.font, itemStack, x, y, size);

        graphics.pose().popPose();
        graphics.flush();

    }

    protected void _renderItem(@NotNull GuiGraphics graphics, int x, int y, int size, int seed, @NotNull ItemStack stack) {
        int guiOffset = 0;
        PoseStack pose = graphics.pose();
        ItemStackRenderState scratchItemStackRenderState = ((IMixinGuiGraphics)graphics).get_scratchItemStackRenderState_FancyMenu();
        if (!stack.isEmpty()) {
            // Calculate the scaling factor based on the requested size (maintaining aspect ratio)
            float scaleFactor = size / 16.0F;  // Convert from the original 16x16 base size
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(scratchItemStackRenderState, stack, ItemDisplayContext.GUI, false, null, null, seed);
            pose.pushPose();
            // Adjust translation to account for new size (center point needs to scale with size)
            pose.translate(
                    (float)(x + size/2),  // Center point X scales with size
                    (float)(y + size/2),  // Center point Y scales with size
                    (float)(150 + (scratchItemStackRenderState.isGui3d() ? guiOffset : 0))
            );
            try {
                // Apply scaled transformation while maintaining aspect ratio
                pose.scale(
                        16.0F * scaleFactor,    // Width scaling
                        -16.0F * scaleFactor,   // Height scaling (negative for Y-axis orientation)
                        16.0F                   // Keep Z scale constant
                );
                boolean bl = !scratchItemStackRenderState.usesBlockLight();
                if (bl) {
                    graphics.flush();
                    Lighting.setupForFlatItems();
                }
                scratchItemStackRenderState.render(pose, ((IMixinGuiGraphics)graphics).getBufferSource_FancyMenu(), 15728880, OverlayTexture.NO_OVERLAY);
                graphics.flush();
                if (bl) {
                    Lighting.setupFor3DItems();
                }
            } catch (Throwable var11) {
                CrashReport crashReport = CrashReport.forThrowable(var11, "Rendering item");
                CrashReportCategory crashReportCategory = crashReport.addCategory("Item being rendered");
                crashReportCategory.setDetail("Item Type", (CrashReportDetail<String>)(() -> String.valueOf(stack.getItem())));
                crashReportCategory.setDetail("Item Components", (CrashReportDetail<String>)(() -> String.valueOf(stack.getComponents())));
                crashReportCategory.setDetail("Item Foil", (CrashReportDetail<String>)(() -> String.valueOf(stack.hasFoil())));
                throw new ReportedException(crashReport);
            }
            pose.popPose();
        }
    }

    protected void renderItemCount(@NotNull GuiGraphics graphics, @NotNull Font font, @NotNull ItemStack stack, int x, int y, int size) {

        PoseStack pose = graphics.pose();
        String text = String.valueOf(stack.getCount());
        // Calculate scaling factor relative to original 16x16 size
        float scaleFactor = size / 16.0F;

        pose.pushPose();
        pose.translate(0.0F, 0.0F, 200.0F);
        pose.pushPose();

        // Scale text exactly proportionally to item size
        pose.scale(scaleFactor, scaleFactor, 1.0F);

        // Convert item-space coordinates to scaled text-space coordinates
        int scaledX = (int)((x / scaleFactor) + 19 - 2 - font.width(text));
        int scaledY = (int)((y / scaleFactor) + 6 + 3);

        graphics.drawString(font, text, scaledX, scaledY, -1, true);
        pose.popPose();
        pose.popPose();

    }

    @Nullable
    public ITexture getTextureResource() {
        if (this.textureSupplier != null) return this.textureSupplier.get();
        return null;
    }

    public void restoreAspectRatio() {
        ITexture t = this.getTextureResource();
        AspectRatio ratio = (t != null) ? t.getAspectRatio() : new AspectRatio(10, 10);
        this.baseWidth = ratio.getAspectRatioWidth(this.getAbsoluteHeight());
    }

}
