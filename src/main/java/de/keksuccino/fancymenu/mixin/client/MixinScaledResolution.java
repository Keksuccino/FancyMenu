package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.mainwindow.MainWindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScaledResolution.class)
public class MixinScaledResolution {

    @Shadow @Mutable @Final private double scaledWidthD;
    @Shadow @Mutable @Final private double scaledHeightD;
    @Shadow private int scaledWidth;
    @Shadow private int scaledHeight;
    @Shadow private int scaleFactor;

    @Inject(at = @At("TAIL"), method = "<init>")
    protected void onNewInstanceTail(Minecraft mc, CallbackInfo info) {

        if (MainWindowHandler.isGuiScaleSet()) {

            this.scaledWidth = mc.displayWidth;
            this.scaledHeight = mc.displayHeight;
            this.scaleFactor = 1;
            boolean flag = mc.isUnicode();
            int i = MainWindowHandler.getGuiScale();

            if (i == 0)
            {
                i = 1000;
            }

            while (this.scaleFactor < i && this.scaledWidth / (this.scaleFactor + 1) >= 320 && this.scaledHeight / (this.scaleFactor + 1) >= 240)
            {
                ++this.scaleFactor;
            }

            if (flag && this.scaleFactor % 2 != 0 && this.scaleFactor != 1)
            {
                --this.scaleFactor;
            }

            this.scaledWidthD = (double)this.scaledWidth / (double)this.scaleFactor;
            this.scaledHeightD = (double)this.scaledHeight / (double)this.scaleFactor;
            this.scaledWidth = MathHelper.ceil(this.scaledWidthD);
            this.scaledHeight = MathHelper.ceil(this.scaledHeightD);

        }

    }

}
