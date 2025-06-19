package de.keksuccino.fancymenu.customization.element.elements.item;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.SerializationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
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
import java.util.Optional;

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

    /**
     * This method handles data and is not directly affected by rendering changes.
     * It remains the same.
     */
    protected void updateCachedItem() {
        String keyFinal = PlaceholderParser.replacePlaceholders(this.itemKey);
        String loreFinal = (this.lore == null) ? null : PlaceholderParser.replacePlaceholders(this.lore);
        String nameFinal = (this.itemName == null) ? null : PlaceholderParser.replacePlaceholders(this.itemName);
        String nbtFinal = (this.nbtData == null) ? null : PlaceholderParser.replacePlaceholders(this.nbtData);

        try {
            if ((this.cachedStack == null) || !keyFinal.equals(this.lastItemKey) || (this.enchanted != this.lastEnchanted) || !Objects.equals(loreFinal, this.lastLore) || !Objects.equals(nameFinal, this.lastItemName) || !Objects.equals(nbtFinal, this.lastNbtData)) {
                Optional<Holder.Reference<Item>> optional = BuiltInRegistries.ITEM.get(ResourceLocation.parse(keyFinal));
                Item item = optional.isPresent() ? Objects.requireNonNullElse(optional.get().value(), Items.AIR) : Items.AIR;
                this.cachedStack = new ItemStack(item);
                this.cachedStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, this.enchanted);

                if ((loreFinal != null) && !loreFinal.isBlank()) {
                    List<Component> lines = new ArrayList<>();
                    for (String line : StringUtils.splitLines(loreFinal.replace("%n%", "\n"), "\n")) {
                        lines.add(Component.literal(line)); // Assuming buildComponent is similar to literal()
                    }
                    this.cachedStack.set(DataComponents.LORE, new ItemLore(lines));
                }

                if (nameFinal != null) {
                    this.cachedStack.set(DataComponents.CUSTOM_NAME, Component.literal(nameFinal));
                }

                if (nbtFinal != null) {
                    // Assuming NBTBuilder is updated for modern DataComponentPatch logic
                    DataComponentPatch nbt = NBTBuilder.buildNbtFromString(this.cachedStack, nbtFinal);
                    if (nbt != null) {
                        this.cachedStack.applyComponents(nbt);
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

            if (this.cachedStack != null) {
                int x = this.getAbsoluteX();
                int y = this.getAbsoluteY();
                int w = this.getAbsoluteWidth();
                int h = this.getAbsoluteHeight();
                this.renderItem(graphics, x, y, w, h, mouseX, mouseY, this.cachedStack);
            }
        }
    }

    /**
     * Main rendering logic for the item, its count, and its tooltip.
     * The tooltip logic is now handled by the modern deferred tooltip system.
     */
    protected void renderItem(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY, @NotNull ItemStack itemStack) {
        int count = SerializationUtils.deserializeNumber(Integer.class, 1, PlaceholderParser.replacePlaceholders(this.itemCount));

        this.renderScaledItem(graphics, itemStack, x, y, width, height);

        if (count > 1) {
            this.renderItemCount(graphics, this.font, x, y, Math.max(width, height), count);
        }

        // NEW: Use the built-in deferred tooltip system. It's cleaner and safer.
        if (!isEditor() && this.showTooltip && UIBase.isXYInArea(mouseX, mouseY, this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteWidth(), this.getAbsoluteHeight())) {
            graphics.setTooltipForNextFrame(this.font, itemStack, mouseX, mouseY);
        }
    }

    /**
     * Renders a scaled item using the modern GuiGraphics transformation stack.
     */
    protected void renderScaledItem(@NotNull GuiGraphics graphics, @NotNull ItemStack stack, int x, int y, int width, int height) {
        graphics.pose().pushMatrix();

        // Translate to the item's position.
        graphics.pose().translate(x, y);

        // Calculate a uniform scale factor based on the desired size.
        // Items are rendered at a base size of 16x16.
        float scale = (float) Math.min(width, height) / 16.0F;
        graphics.pose().scale(scale, scale);

        // Render the item at (0,0) in the new transformed and scaled space.
        graphics.renderItem(stack, 0, 0);

        graphics.pose().popMatrix();
    }

    /**
     * Renders the item count, scaled proportionally to the item itself.
     * This implementation is now much cleaner using transformations.
     */
    protected void renderItemCount(@NotNull GuiGraphics graphics, @NotNull Font font, int x, int y, int size, int count) {
        String text = String.valueOf(count);
        // The scale factor is relative to the standard 16x16 item size.
        float scaleFactor = (float) size / 16.0F;

        graphics.pose().pushMatrix();

        // Calculate the unscaled position of the text relative to a 16x16 item box.
        // This is the standard position for item counts.
        float textX = (float) (19 - 2 - font.width(text));
        float textY = (float) (6 + 3);

        // 1. Translate to the item's top-left corner.
        graphics.pose().translate(x, y);
        // 2. Scale the context from this corner.
        graphics.pose().scale(scaleFactor, scaleFactor);

        // 3. Draw the string at the pre-calculated *unscaled* position.
        // The transformations will handle placing and scaling it correctly on screen.
        graphics.drawString(font, text, (int) textX, (int) textY, -1, true);

        graphics.pose().popMatrix();
    }

}