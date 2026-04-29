package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RealmsNotificationsScreen.class)
public interface IMixinRealmsNotificationsScreen {

    @Accessor("numberOfPendingInvites") int get_numberOfPendingInvites_FancyMenu();

    @Accessor("trialAvailable")
    static boolean get_trialAvailable_FancyMenu() {
        throw new AssertionError();
    }

    @Accessor("hasUnreadNews")
    static boolean get_hasUnreadNews_FancyMenu() {
        throw new AssertionError();
    }

    @Accessor("validClient")
    static boolean get_validClient_FancyMenu() {
        throw new AssertionError();
    }

}
