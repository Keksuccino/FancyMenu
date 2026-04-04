package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.concurrent.CompletableFuture;

@Mixin(RealmsNotificationsScreen.class)
public interface IMixinRealmsNotificationsScreen {

    @Accessor("numberOfPendingInvites") int get_numberOfPendingInvites_FancyMenu();

    @Accessor("validClient") CompletableFuture<Boolean> get_validClient_FancyMenu();

}
