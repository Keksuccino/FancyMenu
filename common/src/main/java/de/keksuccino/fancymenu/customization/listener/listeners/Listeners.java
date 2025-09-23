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
    public static final OnQuitMinecraftListener ON_QUIT_MINECRAFT = new OnQuitMinecraftListener();
    public static final OnDeathListener ON_DEATH = new OnDeathListener();
    public static final OnVariableUpdatedListener ON_VARIABLE_UPDATED = new OnVariableUpdatedListener();
    public static final OnFileDownloadedListener ON_FILE_DOWNLOADED = new OnFileDownloadedListener();
    public static final OnChatMessageReceivedListener ON_CHAT_MESSAGE_RECEIVED = new OnChatMessageReceivedListener();
    public static final OnChatMessageSentListener ON_CHAT_MESSAGE_SENT = new OnChatMessageSentListener();
    public static final OnLookingAtBlockListener ON_LOOKING_AT_BLOCK = new OnLookingAtBlockListener();
    public static final OnLookingAtEntityListener ON_LOOKING_AT_ENTITY = new OnLookingAtEntityListener();
    public static final OnEntitySpawnedListener ON_ENTITY_SPAWNED = new OnEntitySpawnedListener();
    public static final OnEntityDiedListener ON_ENTITY_DIED = new OnEntityDiedListener();
    public static final OnEntityInSightListener ON_ENTITY_IN_SIGHT = new OnEntityInSightListener();
    public static final OnInteractedWithEntityListener ON_INTERACTED_WITH_ENTITY = new OnInteractedWithEntityListener();
    public static final OnEntityMountedListener ON_ENTITY_MOUNTED = new OnEntityMountedListener();
    public static final OnEntityUnmountedListener ON_ENTITY_UNMOUNTED = new OnEntityUnmountedListener();
    public static final OnBlockBrokeListener ON_BLOCK_BROKE = new OnBlockBrokeListener();
    public static final OnBlockPlacedListener ON_BLOCK_PLACED = new OnBlockPlacedListener();
    public static final OnSteppingOnBlockListener ON_STEPPING_ON_BLOCK = new OnSteppingOnBlockListener();
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
    public static final OnOtherPlayerDiedListener ON_OTHER_PLAYER_DIED = new OnOtherPlayerDiedListener();

    public static final OnItemPickedUpListener ON_ITEM_PICKED_UP = new OnItemPickedUpListener();
    public static final OnItemDroppedListener ON_ITEM_DROPPED = new OnItemDroppedListener();
    public static final OnItemConsumedListener ON_ITEM_CONSUMED = new OnItemConsumedListener();

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
        ListenerRegistry.register(ON_QUIT_MINECRAFT);
        ListenerRegistry.register(ON_VARIABLE_UPDATED);
        ListenerRegistry.register(ON_FILE_DOWNLOADED);
        ListenerRegistry.register(ON_CHAT_MESSAGE_RECEIVED);
        ListenerRegistry.register(ON_CHAT_MESSAGE_SENT);
        ListenerRegistry.register(ON_LOOKING_AT_BLOCK);
        ListenerRegistry.register(ON_LOOKING_AT_ENTITY);
        ListenerRegistry.register(ON_ENTITY_SPAWNED);
        ListenerRegistry.register(ON_ENTITY_DIED);
        ListenerRegistry.register(ON_ENTITY_IN_SIGHT);
        ListenerRegistry.register(ON_INTERACTED_WITH_ENTITY);
        ListenerRegistry.register(ON_ENTITY_MOUNTED);
        ListenerRegistry.register(ON_ENTITY_UNMOUNTED);
        ListenerRegistry.register(ON_BLOCK_BROKE);
        ListenerRegistry.register(ON_BLOCK_PLACED);
        ListenerRegistry.register(ON_STEPPING_ON_BLOCK);
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
        ListenerRegistry.register(ON_DEATH);
        ListenerRegistry.register(ON_OTHER_PLAYER_DIED);
        ListenerRegistry.register(ON_ITEM_PICKED_UP);
        ListenerRegistry.register(ON_ITEM_DROPPED);
        ListenerRegistry.register(ON_ITEM_CONSUMED);

    }

}
