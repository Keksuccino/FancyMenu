package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.ListenerRegistry;

public class Listeners {

    public static final OnCharTypedListener ON_CHAR_TYPED = new OnCharTypedListener();
    public static final OnVariableUpdatedListener ON_VARIABLE_UPDATED = new OnVariableUpdatedListener();
    public static final OnFileDownloadedListener ON_FILE_DOWNLOADED = new OnFileDownloadedListener();
    public static final OnLookingAtBlockListener ON_LOOKING_AT_BLOCK = new OnLookingAtBlockListener();
    public static final OnEnterBiomeListener ON_ENTER_BIOME = new OnEnterBiomeListener();
    public static final OnLeaveBiomeListener ON_LEAVE_BIOME = new OnLeaveBiomeListener();
    public static final OnStartSwimmingListener ON_START_SWIMMING = new OnStartSwimmingListener();
    public static final OnStopSwimmingListener ON_STOP_SWIMMING = new OnStopSwimmingListener();
    public static final OnStartTouchingFluidListener ON_START_TOUCHING_FLUID = new OnStartTouchingFluidListener();
    public static final OnStopTouchingFluidListener ON_STOP_TOUCHING_FLUID = new OnStopTouchingFluidListener();

    public static void registerAll() {

        ListenerRegistry.register(ON_CHAR_TYPED);
        ListenerRegistry.register(ON_VARIABLE_UPDATED);
        ListenerRegistry.register(ON_FILE_DOWNLOADED);
        ListenerRegistry.register(ON_LOOKING_AT_BLOCK);
        ListenerRegistry.register(ON_ENTER_BIOME);
        ListenerRegistry.register(ON_LEAVE_BIOME);
        ListenerRegistry.register(ON_START_SWIMMING);
        ListenerRegistry.register(ON_STOP_SWIMMING);
        ListenerRegistry.register(ON_START_TOUCHING_FLUID);
        ListenerRegistry.register(ON_STOP_TOUCHING_FLUID);

    }

}
