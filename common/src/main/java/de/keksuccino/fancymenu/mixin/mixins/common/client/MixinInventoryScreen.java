package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InventoryScreen.class)
public class MixinInventoryScreen {

    @WrapOperation(method = "init", at = @At(value = "NEW", target = "(IIIILnet/minecraft/client/gui/components/WidgetSprites;Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/ImageButton;"))
    private ImageButton after_init_FancyMenu(int x, int y, int width, int height, WidgetSprites sprites, Button.OnPress onPress, Operation<ImageButton> original) {
        ImageButton b = original.call(x, y, width, height, sprites, onPress);
        if ((sprites == RecipeBookComponent.RECIPE_BUTTON_SPRITES) && (b instanceof UniqueWidget w)) {
            w.setWidgetIdentifierFancyMenu("inventory_screen_book_button");
        }
        return b;
    }

}
