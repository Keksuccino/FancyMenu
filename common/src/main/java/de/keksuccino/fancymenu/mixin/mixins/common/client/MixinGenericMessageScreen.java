package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import javax.annotation.Nullable;

@Mixin(GenericMessageScreen.class)
public class MixinGenericMessageScreen extends Screen {

    @Shadow @Nullable private FocusableTextWidget textWidget;

    //unused dummy constructor
    @SuppressWarnings("all")
    private MixinGenericMessageScreen() {
        super(null);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void return_init_FancyMenu(CallbackInfo info) {
        if (this.textWidget != null) ((UniqueWidget)this.textWidget).setWidgetIdentifierFancyMenu("message");
    }

}
