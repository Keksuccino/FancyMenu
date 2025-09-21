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
    public static final OnOpenScreenListener ON_OPEN_SCREEN = new OnOpenScreenListener();
    public static final OnCloseScreenListener ON_CLOSE_SCREEN = new OnCloseScreenListener();
    public static final OnVariableUpdatedListener ON_VARIABLE_UPDATED = new OnVariableUpdatedListener();
    public static final OnFileDownloadedListener ON_FILE_DOWNLOADED = new OnFileDownloadedListener();
    public static final OnChatMessageReceivedListener ON_CHAT_MESSAGE_RECEIVED = new OnChatMessageReceivedListener();
    public static final OnChatMessageSentListener ON_CHAT_MESSAGE_SENT = new OnChatMessageSentListener();
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
    public static final OnServerJoinedListener ON_SERVER_JOINED = new OnServerJoinedListener();
    public static final OnServerLeftListener ON_SERVER_LEFT = new OnServerLeftListener();
    public static final OnOtherPlayerJoinedWorldListener ON_OTHER_PLAYER_JOINED_WORLD = new OnOtherPlayerJoinedWorldListener();
    public static final OnOtherPlayerLeftWorldListener ON_OTHER_PLAYER_LEFT_WORLD = new OnOtherPlayerLeftWorldListener();

    public static void registerAll() {

        ListenerRegistry.register(ON_KEY_PRESSED);
        ListenerRegistry.register(ON_KEY_RELEASED);
        ListenerRegistry.register(ON_CHAR_TYPED);
        ListenerRegistry.register(ON_MOUSE_MOVED);
        ListenerRegistry.register(ON_MOUSE_BUTTON_CLICKED);
        ListenerRegistry.register(ON_MOUSE_BUTTON_RELEASED);
        ListenerRegistry.register(ON_MOUSE_SCROLLED);
        ListenerRegistry.register(ON_OPEN_SCREEN);
        ListenerRegistry.register(ON_CLOSE_SCREEN);
        ListenerRegistry.register(ON_VARIABLE_UPDATED);
        ListenerRegistry.register(ON_FILE_DOWNLOADED);
        ListenerRegistry.register(ON_CHAT_MESSAGE_RECEIVED);
        ListenerRegistry.register(ON_CHAT_MESSAGE_SENT);
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
        ListenerRegistry.register(ON_SERVER_JOINED);
        ListenerRegistry.register(ON_SERVER_LEFT);
        ListenerRegistry.register(ON_OTHER_PLAYER_JOINED_WORLD);
        ListenerRegistry.register(ON_OTHER_PLAYER_LEFT_WORLD);

    }

}