package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RealmsNotificationsScreen.class)
public interface IMixinRealmsNotificationsScreen {

    @Accessor("numberOfPendingInvites") int get_numberOfPendingInvites_FancyMenu();

    @Accessor("trialAvailable") boolean get_trialAvailable_FancyMenu();

    @Accessor("hasUnreadNews") boolean get_hasUnreadNews_FancyMenu();

    @Accessor("validClient") boolean get_validClient_FancyMenu();

}
