package de.keksuccino.drippyloadingscreen.mixin.mixins.client;

import de.keksuccino.drippyloadingscreen.customization.DrippyOverlayScreen;
import de.keksuccino.drippyloadingscreen.customization.deepcustomization.overlay.background.OverlayBackgroundItem;
import de.keksuccino.drippyloadingscreen.customization.deepcustomization.overlay.background.OverlayBackgroundLayoutElement;
import de.keksuccino.drippyloadingscreen.mixin.MixinCache;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.awt.*;
import java.util.List;

@Mixin(LayoutEditorScreen.class)
public abstract class MixinLayoutEditorScreen {

    @Shadow public abstract List<LayoutElement> getContent();

    @Shadow @Final public Screen screen;

    @Redirect(method = "renderCreatorBackground", at = @At(value = "INVOKE", target = "Ljava/awt/Color;getRGB()I", remap = false), remap = false)
    private int overrideEditorBackgroundColor(Color instance) {
        //Make the editor background fit the loading screen background color when making layout for DrippyOverlayScreen
        if (this.screen instanceof DrippyOverlayScreen) {
            OverlayBackgroundItem i = getBackgroundItem();
            if ((i != null) && (i.hexColor != null)) {
                return i.hexColor.getRGB();
            }
            return IMixinLoadingOverlay.getBrandBackgroundDrippy().getAsInt();
        }
        return instance.getRGB();
    }

    @Nullable
    private OverlayBackgroundItem getBackgroundItem() {
        for (LayoutElement e : this.getContent()) {
            if (e instanceof OverlayBackgroundLayoutElement) {
                return (OverlayBackgroundItem) e.object;
            }
        }
        return null;
    }

}
