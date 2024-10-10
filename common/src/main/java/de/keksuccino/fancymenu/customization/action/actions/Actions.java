package de.keksuccino.fancymenu.customization.action.actions;

import de.keksuccino.fancymenu.customization.action.actions.audio.SetAudioElementVolumeAction;
import de.keksuccino.fancymenu.customization.action.actions.layout.DisableLayoutAction;
import de.keksuccino.fancymenu.customization.action.actions.layout.EnableLayoutAction;
import de.keksuccino.fancymenu.customization.action.actions.layout.ToggleLayoutAction;
import de.keksuccino.fancymenu.customization.action.actions.level.DisconnectAction;
import de.keksuccino.fancymenu.customization.action.actions.level.EnterWorldAction;
import de.keksuccino.fancymenu.customization.action.actions.level.JoinLastWorldServerAction;
import de.keksuccino.fancymenu.customization.action.actions.level.JoinServerAction;
import de.keksuccino.fancymenu.customization.action.actions.other.*;
import de.keksuccino.fancymenu.customization.action.actions.screen.CloseScreenAction;
import de.keksuccino.fancymenu.customization.action.actions.screen.OpenScreenAction;
import de.keksuccino.fancymenu.customization.action.actions.screen.UpdateScreenAction;
import de.keksuccino.fancymenu.customization.action.actions.variables.ClearVariablesAction;
import de.keksuccino.fancymenu.customization.action.actions.variables.SetVariableAction;
import de.keksuccino.fancymenu.customization.action.ActionRegistry;

public class Actions {

    public static final SetVariableAction SET_VARIABLE = new SetVariableAction();
    public static final ClearVariablesAction CLEAR_VARIABLES = new ClearVariablesAction();
    public static final PasteToChatAction PASTE_TO_CHAT = new PasteToChatAction();
    public static final ToggleLayoutAction TOGGLE_LAYOUT = new ToggleLayoutAction();
    public static final EnableLayoutAction ENABLE_LAYOUT = new EnableLayoutAction();
    public static final DisableLayoutAction DISABLE_LAYOUT = new DisableLayoutAction();
    public static final SendMessageAction SEND_MESSAGE = new SendMessageAction();
    public static final QuitGameAction QUIT_GAME = new QuitGameAction();
    public static final JoinServerAction JOIN_SERVER = new JoinServerAction();
    public static final EnterWorldAction ENTER_WORLD = new EnterWorldAction();
    public static final JoinLastWorldServerAction JOIN_LAST_WORLD_SERVER = new JoinLastWorldServerAction();
    public static final DisconnectAction DISCONNECT = new DisconnectAction();
    public static final OpenScreenAction OPEN_SCREEN = new OpenScreenAction();
    public static final CloseScreenAction CLOSE_SCREEN = new CloseScreenAction();
    public static final UpdateScreenAction UPDATE_SCREEN = new UpdateScreenAction();
    public static final OpenLinkAction OPEN_LINK = new OpenLinkAction();
    //TODO übernehmen (FMSIA)
//    public static final OpenFileAction OPEN_FILE = new OpenFileAction();
//    public static final MoveFileAction MOVE_FILE = new MoveFileAction();
//    public static final CopyFileAction COPY_FILE = new CopyFileAction();
//    public static final DeleteFileAction DELETE_FILE = new DeleteFileAction();
//    public static final RenameFileAction RENAME_FILE = new RenameFileAction();
//    public static final DownloadFileAction DOWNLOAD_FILE = new DownloadFileAction();
//    public static final UnpackZipAction UNPACK_ZIP = new UnpackZipAction();
    public static final ReloadFancyMenuAction RELOAD_FANCYMENU = new ReloadFancyMenuAction();
    public static final RunButtonScriptAction RUN_BUTTON_SCRIPT = new RunButtonScriptAction();
    //TODO übernehmen (FMSIA)
//    public static final ExecuteTerminalCommandAction EXECUTE_TERMINAL_COMMAND = new ExecuteTerminalCommandAction();
    public static final CopyToClipboardAction COPY_TO_CLIPBOARD = new CopyToClipboardAction();
    public static final MimicButtonAction MIMIC_BUTTON = new MimicButtonAction();
    public static final EditMinecraftOptionAction EDIT_MINECRAFT_OPTION = new EditMinecraftOptionAction();
    //TODO übernehmen
    public static final SetAudioElementVolumeAction SET_AUDIO_ELEMENT_VOLUME = new SetAudioElementVolumeAction();

    public static void registerAll() {

        ActionRegistry.register(SET_VARIABLE);
        ActionRegistry.register(CLEAR_VARIABLES);
        ActionRegistry.register(PASTE_TO_CHAT);
        ActionRegistry.register(TOGGLE_LAYOUT);
        ActionRegistry.register(ENABLE_LAYOUT);
        ActionRegistry.register(DISABLE_LAYOUT);
        ActionRegistry.register(SEND_MESSAGE);
        ActionRegistry.register(QUIT_GAME);
        ActionRegistry.register(JOIN_SERVER);
        ActionRegistry.register(ENTER_WORLD);
        ActionRegistry.register(JOIN_LAST_WORLD_SERVER);
        ActionRegistry.register(DISCONNECT);
        ActionRegistry.register(OPEN_SCREEN);
        ActionRegistry.register(CLOSE_SCREEN);
        ActionRegistry.register(UPDATE_SCREEN);
        ActionRegistry.register(OPEN_LINK);
        //TODO übernehmen (FMSIA)
//        ActionRegistry.register(OPEN_FILE);
//        ActionRegistry.register(MOVE_FILE);
//        ActionRegistry.register(COPY_FILE);
//        ActionRegistry.register(DELETE_FILE);
//        ActionRegistry.register(RENAME_FILE);
//        ActionRegistry.register(DOWNLOAD_FILE);
//        ActionRegistry.register(UNPACK_ZIP);
        ActionRegistry.register(RELOAD_FANCYMENU);
        ActionRegistry.register(RUN_BUTTON_SCRIPT);
        //TODO übernehmen (FMSIA)
//        ActionRegistry.register(EXECUTE_TERMINAL_COMMAND);
        ActionRegistry.register(COPY_TO_CLIPBOARD);
        ActionRegistry.register(MIMIC_BUTTON);
        ActionRegistry.register(EDIT_MINECRAFT_OPTION);
        //TODO übernehmen
        ActionRegistry.register(SET_AUDIO_ELEMENT_VOLUME);

    }

}
