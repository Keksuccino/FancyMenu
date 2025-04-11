package de.keksuccino.fancymenu.mixin.mixins.forge.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.client.gui.NotificationModUpdateScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import java.util.function.BiConsumer;

@Mixin(TitleScreen.class)
public class MixinForgeTitleScreen {

    @Nullable
    @Unique
    private AbstractWidget cachedForgeModsButtonFancyMenu = null;

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;renderMainMenu(Lnet/minecraft/client/gui/screens/TitleScreen;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;III)V", remap = false))
    private boolean cancelForgeWarningAboveLogoRenderingFancyMenu(TitleScreen line, PoseStack gui, Font poseStack, int font, int width, int height) {
        return false;
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/internal/BrandingControl;forEachLine(ZZLjava/util/function/BiConsumer;)V", remap = false))
    private boolean cancelForgeBrandingRenderingFancyMenu(boolean includeMC, boolean reverse, BiConsumer<Integer, String> lineConsumer) {
        return false;
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/internal/BrandingControl;forEachAboveCopyrightLine(Ljava/util/function/BiConsumer;)V", remap = false))
    private boolean cancelForgeBrandingAboveCopyrightRenderingFancyMenu(BiConsumer<Integer, String> lineConsumer) {
        return false;
    }

    @WrapOperation(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/gui/NotificationModUpdateScreen;init(Lnet/minecraft/client/gui/screens/TitleScreen;Lnet/minecraft/client/gui/components/Button;)Lnet/minecraftforge/client/gui/NotificationModUpdateScreen;", remap = false))
    private NotificationModUpdateScreen wrapForgeModUpdateIndicatorInitFancyMenu(TitleScreen guiMainMenu, Button modButton, Operation<NotificationModUpdateScreen> original) {
        this.cachedForgeModsButtonFancyMenu = modButton;
        return original.call(guiMainMenu, modButton);
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/gui/NotificationModUpdateScreen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"))
    private boolean wrapForgeModUpdateIndicatorRenderingFancyMenu(NotificationModUpdateScreen instance, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (this.cachedForgeModsButtonFancyMenu instanceof CustomizableWidget w) {
            //Don't render update indicator if Mods button is hidden
            if (w.isHiddenFancyMenu()) return false;
        }
        return true;
    }

}
