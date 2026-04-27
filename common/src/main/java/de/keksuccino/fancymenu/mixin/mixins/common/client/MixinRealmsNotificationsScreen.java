package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.accessor.client.IRealmsNotificationsScreenStatics;
import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RealmsNotificationsScreen.class)
public class MixinRealmsNotificationsScreen implements IRealmsNotificationsScreenStatics {

    @Shadow private static boolean trialAvailable;
    @Shadow private static boolean hasUnreadNews;
    @Shadow private static boolean hasUnseenNotifications;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstructFancyMenu(CallbackInfo info) {
        //This is to avoid crashes in the TitleScreen
        this.getRealmsScreenFancyMenu().init(0, 0);
    }

    @SuppressWarnings("all")
    private RealmsNotificationsScreen getRealmsScreenFancyMenu() {
        return (RealmsNotificationsScreen) ((Object)this);
    }

    @Override
    public boolean fancymenu$trialAvailable() {
        return trialAvailable;
    }

    @Override
    public boolean fancymenu$hasUnreadNews() {
        return hasUnreadNews;
    }

    @Override
    public boolean fancymenu$hasUnseenNotifications() {
        return hasUnseenNotifications;
    }

}
