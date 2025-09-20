package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.ListenerRegistry;

public class Listeners {

    public static final OnKeyPressedListener ON_KEY_PRESSED = new OnKeyPressedListener();
    public static final OnKeyReleasedListener ON_KEY_RELEASED = new OnKeyReleasedListener();
    public static final OnCharTypedListener ON_CHAR_TYPED = new OnCharTypedListener();
    public static final OnMouseMovedListener ON_MOUSE_MOVED = new OnMouseMovedListener();
    public static final OnMouseButtonClickedListener ON_MOUSE_BUTTON_CLICKED = new OnMouseButtonClickedListener();
    public static final OnMouseButtonReleasedListener ON_MOUSE_BUTTON_RELEASED = new OnMouseButtonReleasedListener();
    public static final OnMouseScrolledListener ON_MOUSE_SCROLLED = new OnMouseScrolledListener();
    public static final OnVariableUpdatedListener ON_VARIABLE_UPDATED = new OnVariableUpdatedListener();
    public static final OnFileDownloadedListener ON_FILE_DOWNLOADED = new OnFileDownloadedListener();
    public static final OnLookingAtBlockListener ON_LOOKING_AT_BLOCK = new OnLookingAtBlockListener();
    public static final OnEnterBiomeListener ON_ENTER_BIOME = new OnEnterBiomeListener();
    public static final OnLeaveBiomeListener ON_LEAVE_BIOME = new OnLeaveBiomeListener();
    public static final OnEnterStructureListener ON_ENTER_STRUCTURE = new OnEnterStructureListener();
    public static final OnLeaveStructureListener ON_LEAVE_STRUCTURE = new OnLeaveStructureListener();
    public static final OnEnterStructureHighPrecisionListener ON_ENTER_STRUCTURE_HIGH_PRECISION = new OnEnterStructureHighPrecisionListener();
    public static final OnLeaveStructureHighPrecisionListener ON_LEAVE_STRUCTURE_HIGH_PRECISION = new OnLeaveStructureHighPrecisionListener();
    public static final OnStartSwimmingListener ON_START_SWIMMING = new OnStartSwimmingListener();
    public static final OnStopSwimmingListener ON_STOP_SWIMMING = new OnStopSwimmingListener();
    public static final OnStartTouchingFluidListener ON_START_TOUCHING_FLUID = new OnStartTouchingFluidListener();
    public static final OnStopTouchingFluidListener ON_STOP_TOUCHING_FLUID = new OnStopTouchingFluidListener();

    public static void registerAll() {

        ListenerRegistry.register(ON_KEY_PRESSED);
        ListenerRegistry.register(ON_KEY_RELEASED);
        ListenerRegistry.register(ON_CHAR_TYPED);
        ListenerRegistry.register(ON_MOUSE_MOVED);
        ListenerRegistry.register(ON_MOUSE_BUTTON_CLICKED);
        ListenerRegistry.register(ON_MOUSE_BUTTON_RELEASED);
        ListenerRegistry.register(ON_MOUSE_SCROLLED);
        ListenerRegistry.register(ON_VARIABLE_UPDATED);
        ListenerRegistry.register(ON_FILE_DOWNLOADED);
        ListenerRegistry.register(ON_LOOKING_AT_BLOCK);
        ListenerRegistry.register(ON_ENTER_BIOME);
        ListenerRegistry.register(ON_LEAVE_BIOME);
        ListenerRegistry.register(ON_ENTER_STRUCTURE);
        ListenerRegistry.register(ON_LEAVE_STRUCTURE);
        ListenerRegistry.register(ON_ENTER_STRUCTURE_HIGH_PRECISION);
        ListenerRegistry.register(ON_LEAVE_STRUCTURE_HIGH_PRECISION);
        ListenerRegistry.register(ON_START_SWIMMING);
        ListenerRegistry.register(ON_STOP_SWIMMING);
        ListenerRegistry.register(ON_START_TOUCHING_FLUID);
        ListenerRegistry.register(ON_STOP_TOUCHING_FLUID);

    }

}
