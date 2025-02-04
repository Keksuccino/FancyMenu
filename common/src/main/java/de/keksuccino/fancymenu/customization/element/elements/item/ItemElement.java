package de.keksuccino.fancymenu.customization.element.elements.item;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinGuiGraphics;
import de.keksuccino.fancymenu.util.SerializationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenRenderUtils;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ItemElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    public ItemStack cachedStack = null;
    @NotNull
    public String itemKey = "" + BuiltInRegistries.ITEM.getKey(Items.BARRIER);
    public boolean enchanted = false;
    @NotNull
    public String itemCount = "1";
    @Nullable
    public String lore = null;
    @Nullable
    public String itemName = null;
    public boolean showTooltip = true;
    @Nullable
    public String nbtData = null;
    protected String lastItemKey = null;
    protected boolean lastEnchanted = false;
    protected String lastLore = null;
    protected String lastItemName = null;
    protected String lastNbtData = null;
    protected final Font font = Minecraft.getInstance().font;

    public ItemElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    protected void updateCachedItem() {

        String keyFinal = PlaceholderParser.replacePlaceholders(this.itemKey);
        String loreFinal = (this.lore == null) ? null : PlaceholderParser.replacePlaceholders(this.lore);
        String nameFinal = (this.itemName == null) ? null : PlaceholderParser.replacePlaceholders(this.itemName);
        String nbtFinal = (this.nbtData == null) ? null : PlaceholderParser.replacePlaceholders(this.nbtData);

        try {

            if ((this.cachedStack == null) || !keyFinal.equals(this.lastItemKey) || (this.enchanted != this.lastEnchanted) || !Objects.equals(loreFinal, this.lastLore) || !Objects.equals(nameFinal, this.lastItemName) || !Objects.equals(nbtFinal, this.lastNbtData)) {

                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(keyFinal));

                this.cachedStack = new ItemStack(item);

                if (nbtFinal != null) {
                    CompoundTag nbt = NBTBuilder.buildNbtFromString(this.cachedStack, nbtFinal);
                    if (nbt != null) {
                        this.cachedStack.setTag(nbt);
                    }
                }

                if (this.enchanted) this.cachedStack.enchant(Enchantments.AQUA_AFFINITY, 1);

                if ((loreFinal != null) && !loreFinal.isBlank()) {
                    List<Component> lines = new ArrayList<>();
                    for (String line : StringUtils.splitLines(loreFinal.replace("%n%", "\n"), "\n")) {
                        lines.add(buildComponent(line));
                    }
                    setLore(this.cachedStack, lines);
                }

                if (nameFinal != null) {
                    this.cachedStack.setHoverName(buildComponent(nameFinal));
                }

            }

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to create ItemStack instance for 'Item' element!", ex);
            this.cachedStack = new ItemStack(Items.BARRIER);
        }

        this.lastItemKey = keyFinal;
        this.lastEnchanted = this.enchanted;
        this.lastLore = loreFinal;
        this.lastItemName = nameFinal;
        this.lastNbtData = nbtFinal;

    }

    /**
     * Sets the lore on the given ItemStack.
     *
     * @param stack the ItemStack to modify
     * @param loreLines a list of Component objects representing each line of lore
     */
    protected static void setLore(@NotNull ItemStack stack, @NotNull List<Component> loreLines) {
        // Get or create the "display" compound tag on the item
        CompoundTag displayTag = stack.getOrCreateTagElement("display");

        // Create a new NbtList to hold the lore entries.
        ListTag loreList = new ListTag();

        // Each lore line must be stored as a JSON string.
        // Text.Serializer.toJson(text) converts the Text component into its JSON representation.
        for (Component line : loreLines) {
            loreList.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        }

        // Add the lore list to the "display" tag under the key "Lore"
        displayTag.put("Lore", loreList);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            this.updateCachedItem();

            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();
            int w = this.getAbsoluteWidth();
            int h = this.getAbsoluteHeight();

            RenderSystem.enableBlend();

            if (this.cachedStack != null) {
                this.renderItem(graphics, x, y, w, h, mouseX, mouseY, this.cachedStack);
            }

            RenderSystem.disableBlend();

        }

    }

    protected void renderItem(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY, @NotNull ItemStack itemStack) {

        int count = SerializationUtils.deserializeNumber(Integer.class, 1, PlaceholderParser.replacePlaceholders(this.itemCount));

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 100.0F);

        int size = Math.max(width, height);
        int randomSeed = x + y * this.getAbsoluteWidth();
        this._renderItem(graphics, x, y, size, randomSeed, itemStack);
        if (count > 1) {
            this.renderItemCount(graphics, this.font, x, y, size, count);
        }

        graphics.pose().popPose();
        graphics.flush();

        if (!isEditor() && this.showTooltip && UIBase.isXYInArea(mouseX, mouseY, this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteWidth(), this.getAbsoluteHeight())) {
            ScreenRenderUtils.postPostRenderTask((graphics1, mouseX1, mouseY1, partial) -> this.renderItemTooltip(graphics1, mouseX, mouseY, itemStack));
        }

    }

    protected void _renderItem(@NotNull GuiGraphics graphics, int x, int y, int size, int seed, @NotNull ItemStack itemStack) {
        int guiOffset = 0;
        PoseStack pose = graphics.pose();
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(itemStack, null, null, seed);
        if (!itemStack.isEmpty()) {
            // Calculate the scaling factor based on the requested size (maintaining aspect ratio)
            float scaleFactor = size / 16.0F;  // Convert from the original 16x16 base size
            pose.pushPose();
            // Adjust translation to account for new size (center point needs to scale with size)
            pose.translate(
                    (float)(x + size/2),  // Center point X scales with size
                    (float)(y + size/2),  // Center point Y scales with size
                    (float)(150 + (model.isGui3d() ? guiOffset : 0))
            );
            try {
                // Apply scaled transformation while maintaining aspect ratio
                pose.scale(
                        16.0F * scaleFactor,    // Width scaling
                        -16.0F * scaleFactor,   // Height scaling (negative for Y-axis orientation)
                        16.0F                   // Keep Z scale constant
                );
                boolean bl = !model.usesBlockLight();
                if (bl) {
                    graphics.flush();
                    Lighting.setupForFlatItems();
                }
                Minecraft.getInstance().getItemRenderer().render(itemStack, ItemDisplayContext.GUI, false, graphics.pose(), ((IMixinGuiGraphics)graphics).getBufferSource_FancyMenu(), 15728880, OverlayTexture.NO_OVERLAY, model);
                graphics.flush();
                if (bl) {
                    Lighting.setupFor3DItems();
                }
            } catch (Throwable throwable) {
                CrashReport crashReport = CrashReport.forThrowable(throwable, "[FANCYMENU] Rendering item in layout");
                CrashReportCategory crashReportCategory = crashReport.addCategory("Item being rendered");
                crashReportCategory.setDetail("Item Type", (() -> String.valueOf(itemStack.getItem())));
                crashReportCategory.setDetail("Item Foil", (() -> String.valueOf(itemStack.hasFoil())));
                throw new ReportedException(crashReport);
            }
            pose.popPose();
        }
    }

    protected void renderItemCount(@NotNull GuiGraphics graphics, @NotNull Font font, int x, int y, int size, int count) {

        PoseStack pose = graphics.pose();
        String text = String.valueOf(count);
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

    protected void renderItemTooltip(@NotNull GuiGraphics graphics, int mouseX, int mouseY, @NotNull ItemStack itemStack) {
        graphics.renderTooltip(this.font, Screen.getTooltipFromItem(Minecraft.getInstance(), itemStack), itemStack.getTooltipImage(), mouseX, mouseY);
    }

}
