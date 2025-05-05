package de.keksuccino.fancymenu.mixin.mixins.forge.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.client.gui.TitleScreenModUpdateIndicator;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import java.util.function.ObjIntConsumer;

@Mixin(TitleScreen.class)
public class MixinForgeTitleScreen {

    @Nullable
    @Unique
    private AbstractWidget cachedForgeModsButtonFancyMenu = null;

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/internal/BrandingControl;forEachLine(ZZLjava/util/function/ObjIntConsumer;)V", remap = false))
    private boolean cancelForgeBrandingRenderingFancyMenu(boolean idx, boolean includeMC, ObjIntConsumer<String> reverse) {
        return false;
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/internal/BrandingControl;forEachAboveCopyrightLine(Ljava/util/function/ObjIntConsumer;)V", remap = false))
    private boolean cancelForgeBrandingAboveCopyrightRenderingFancyMenu(ObjIntConsumer<String> idx) {
        return false;
    }

    @WrapOperation(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/gui/TitleScreenModUpdateIndicator;init(Lnet/minecraft/client/gui/screens/TitleScreen;Lnet/minecraft/client/gui/components/Button;)Lnet/minecraftforge/client/gui/TitleScreenModUpdateIndicator;", remap = false))
    private TitleScreenModUpdateIndicator wrapForgeModUpdateIndicatorInitFancyMenu(TitleScreen guiMainMenu, Button modButton, Operation<TitleScreenModUpdateIndicator> original) {
        this.cachedForgeModsButtonFancyMenu = modButton;
        return original.call(guiMainMenu, modButton);
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/gui/TitleScreenModUpdateIndicator;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private boolean wrapForgeModUpdateIndicatorRenderingFancyMenu(TitleScreenModUpdateIndicator instance, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.cachedForgeModsButtonFancyMenu instanceof CustomizableWidget w) {
            //Don't render update indicator if Mods button is hidden
            if (w.isHiddenFancyMenu()) return false;
        }
        return true;
    }

}
