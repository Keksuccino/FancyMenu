package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

//TODO übernehmen
@Mixin(CreateWorldScreen.GameTab.class)
public class MixinCreateWorldScreen_GameTab {

    //Mixin plugin shows param error for original.call(), but should work (probably because of generic T)
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;Lnet/minecraft/client/gui/layouts/LayoutSettings;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    private <T extends LayoutElement> T wrapAddChild1_FancyMenu(GridLayout.RowHelper instance, T layoutElement, LayoutSettings settings, Operation<T> original) {
        this.makeGameTabWidgetsUnique_FancyMenu(layoutElement);
        return original.call(instance, layoutElement, settings);
    }

    //Mixin plugin shows param error for original.call(), but should work (probably because of generic T)
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    private <T extends LayoutElement> T wrapAddChild2_FancyMenu(GridLayout.RowHelper instance, T layoutElement, Operation<T> original) {
        this.makeGameTabWidgetsUnique_FancyMenu(layoutElement);
        return original.call(instance, layoutElement);
    }

    @Unique
    private void makeGameTabWidgetsUnique_FancyMenu(Object layoutElement) {

        //World Name text field
        if (layoutElement instanceof EditBox b) {
            ((UniqueWidget)b).setWidgetIdentifierFancyMenu("world_name_field");
        }

        //World Name label
        if (layoutElement instanceof StringWidget w) {
            ((UniqueWidget)w).setWidgetIdentifierFancyMenu("name_label");
        }

        if (layoutElement instanceof CycleButton<?> c) {

            //Game Mode button
            if (c.getValue() instanceof WorldCreationUiState.SelectedGameMode) {
                ((UniqueWidget)c).setWidgetIdentifierFancyMenu("gamemode_button");
            }

            //Difficulty button
            if (c.getValue() instanceof Difficulty) {
                ((UniqueWidget)c).setWidgetIdentifierFancyMenu("difficulty_button");
            }

            //Allow Cheats button
            if (c.getValue() instanceof Boolean) {
                ((UniqueWidget)c).setWidgetIdentifierFancyMenu("allow_cheats_button");
            }

        }

        //Unstable MC Version Experiments button
        if (layoutElement instanceof Button b) {
            if (b.getMessage() instanceof MutableComponent c) {
                if (c.getContents() instanceof TranslatableContents t) {
                    if ("selectWorld.experiments".equals(t.getKey())) {
                        ((UniqueWidget)b).setWidgetIdentifierFancyMenu("unstable_mc_experiments_button");
                    }
                }
            }
        }

    }

}
