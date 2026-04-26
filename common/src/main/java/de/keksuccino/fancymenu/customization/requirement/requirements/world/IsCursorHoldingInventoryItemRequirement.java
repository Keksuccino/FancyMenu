package de.keksuccino.fancymenu.customization.requirement.requirements.world;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractContainerScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.network.chat.Component;

public class IsCursorHoldingInventoryItemRequirement extends Requirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsCursorHoldingInventoryItemRequirement() {
        super("is_cursor_holding_inventory_item");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        try {
            Screen screen = Minecraft.getInstance().screen;
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return false;

            ItemStack carried = containerScreen.getMenu().getCarried();
            if (!carried.isEmpty()) return true;

            ItemStack dragging = ((IMixinAbstractContainerScreen) containerScreen).get_draggingItem_FancyMenu();
            return !dragging.isEmpty();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle '" + this.getIdentifier() + "' loading requirement!", ex);
            return false;
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.is_cursor_holding_inventory_item");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.is_cursor_holding_inventory_item.desc");
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.world");
    }

    @Override
    public Component getValueDisplayName() {
        return null;
    }

    @Override
    public String getValuePreset() {
        return null;
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
