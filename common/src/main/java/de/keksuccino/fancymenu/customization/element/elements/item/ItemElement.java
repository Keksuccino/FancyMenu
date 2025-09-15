package de.keksuccino.fancymenu.customization.element.elements.item;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.SerializationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenRenderUtils;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
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
        this.supportsTilting = false;
    }

    protected void updateCachedItem() {

        String keyFinal = PlaceholderParser.replacePlaceholders(this.itemKey);
        String loreFinal = (this.lore == null) ? null : PlaceholderParser.replacePlaceholders(this.lore);
        String nameFinal = (this.itemName == null) ? null : PlaceholderParser.replacePlaceholders(this.itemName);
        String nbtFinal = (this.nbtData == null) ? null : PlaceholderParser.replacePlaceholders(this.nbtData);

        try {

            if ((this.cachedStack == null) || !keyFinal.equals(this.lastItemKey) || (this.enchanted != this.lastEnchanted) || !Objects.equals(loreFinal, this.lastLore) || !Objects.equals(nameFinal, this.lastItemName) || !Objects.equals(nbtFinal, this.lastNbtData)) {

                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(keyFinal));

                this.cachedStack = new ItemStack(item);
                this.cachedStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, this.enchanted);

                if ((loreFinal != null) && !loreFinal.isBlank()) {
                    List<Component> lines = new ArrayList<>();
                    for (String line : StringUtils.splitLines(loreFinal.replace("%n%", "\n"), "\n")) {
                        lines.add(buildComponent(line));
                    }
                    this.cachedStack.set(DataComponents.LORE, new ItemLore(lines));
                }

                if (nameFinal != null) {
                    this.cachedStack.set(DataComponents.ITEM_NAME, buildComponent(nameFinal));
                }

                if (nbtFinal != null) {
                    DataComponentPatch nbt = NBTBuilder.buildNbtFromString(this.cachedStack, nbtFinal);
                    if (nbt != null) {
                        this.cachedStack.applyComponentsAndValidate(nbt);
                    }
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

        this.renderScaledItem(graphics, itemStack, x, y, width, height);

        if (count > 1) {
            this.renderItemCount(graphics, this.font, x, y, Math.max(width, height), count);
        }

        if (!isEditor() && this.showTooltip && UIBase.isXYInArea(mouseX, mouseY, this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteWidth(), this.getAbsoluteHeight())) {
            ScreenRenderUtils.postPostRenderTask((graphics1, mouseX1, mouseY1, partial) -> this.renderItemTooltip(graphics1, mouseX, mouseY, itemStack));
        }

    }

    protected void renderScaledItem(@NotNull GuiGraphics graphics, @NotNull ItemStack stack, int x, int y, int width, int height) {
        // Save the current transformation state.
        PoseStack pose = graphics.pose();
        pose.pushPose();

        // Translate to the top-left of where you want the item to be drawn.
        pose.translate(x, y, 0);

        // Calculate a uniform scale factor based on the desired size.
        // (Items are rendered at a base size of 16x16.)
        float scale = Math.min(width, height) / 16.0F;
        pose.scale(scale, scale, 1.0F);

        // Now render the item at (0,0) because the translation has been applied.
        graphics.renderItem(stack, 0, 0);

        // Restore the previous transformation state.
        pose.popPose();
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
