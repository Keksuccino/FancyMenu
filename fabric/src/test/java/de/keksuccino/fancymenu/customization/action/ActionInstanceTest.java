package de.keksuccino.fancymenu.customization.action;

import de.keksuccino.fancymenu.customization.action.actions.audio.PlayAudioAction;
import de.keksuccino.fancymenu.customization.action.actions.file.CopyFileAction;
import de.keksuccino.fancymenu.customization.action.actions.file.CreateFileAction;
import de.keksuccino.fancymenu.customization.action.actions.file.DeleteFileAction;
import de.keksuccino.fancymenu.customization.action.actions.file.DownloadFileAction;
import de.keksuccino.fancymenu.customization.action.actions.file.ExtractZipFileAction;
import de.keksuccino.fancymenu.customization.action.actions.file.MoveFileAction;
import de.keksuccino.fancymenu.customization.action.actions.file.OpenFileFolderAction;
import de.keksuccino.fancymenu.customization.action.actions.file.RenameFileAction;
import de.keksuccino.fancymenu.customization.action.actions.file.SelectFileAction;
import de.keksuccino.fancymenu.customization.action.actions.file.WriteFileAction;
import de.keksuccino.fancymenu.customization.action.actions.level.EnterWorldAction;
import de.keksuccino.fancymenu.customization.action.actions.other.ConnectToRemoteServerAction;
import de.keksuccino.fancymenu.customization.action.actions.other.ExecuteCommandAsIntegratedServerAction;
import de.keksuccino.fancymenu.customization.action.actions.other.ManageResourcePackAction;
import de.keksuccino.fancymenu.customization.action.actions.other.OpenLinkAction;
import de.keksuccino.fancymenu.customization.action.actions.other.SendDataToRemoteServerAction;
import de.keksuccino.fancymenu.customization.action.actions.other.SendFmDataToServerAction;
import de.keksuccino.fancymenu.customization.action.actions.other.SendHttpRequestAction;
import de.keksuccino.fancymenu.customization.action.actions.other.SendMessageAction;
import de.keksuccino.fancymenu.customization.action.actions.variables.SetVariableAction;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.testing.TestPlaceholders;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("PlaceholderParser global state")
class ActionInstanceTest {

    private PlaceholderParser.PlaceholderCachingController originalCachingController;

    @BeforeEach
    void disableCaching() {
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
    }

    @AfterEach
    void restoreCaching() {
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
    }

    @Test
    void rawValueActionsReceiveFormattingCodeLookalikesIntact() {
        RecordingAction action = new RecordingAction(true);

        new ActionInstance(action, "https://example.com/?id=" + TestPlaceholders.echo("42") + "&embed=true&f=1").execute();

        assertEquals("https://example.com/?id=42&embed=true&f=1", action.lastValue);
    }

    @Test
    void formattedTextActionsStillConvertFormattingCodes() {
        RecordingAction action = new RecordingAction(false);

        new ActionInstance(action, TestPlaceholders.echo("Hello") + " &eWorld").execute();

        assertEquals("Hello §eWorld", action.lastValue);
    }

    @Test
    void urlPathAndPayloadActionsDeclareRawValues() {
        List<Action> rawValueActions = List.of(new CopyFileAction(), new CreateFileAction(), new DeleteFileAction(), new DownloadFileAction(), new ExtractZipFileAction(), new MoveFileAction(), new OpenFileFolderAction(), new RenameFileAction(), new SelectFileAction(), new WriteFileAction(), new SendHttpRequestAction(), new OpenLinkAction(), new PlayAudioAction(), new ConnectToRemoteServerAction(), new SendDataToRemoteServerAction(), new EnterWorldAction());
        // Their values can rely on "&" codes becoming "§": chat and commands, resource pack titles, server-side data and variables compared by requirements.
        List<Action> formattedValueActions = List.of(new SendMessageAction(), new ExecuteCommandAsIntegratedServerAction(), new ManageResourcePackAction(), new SendFmDataToServerAction(), new SetVariableAction());

        for (Action action : rawValueActions) assertTrue(action.hasRawValue(), action.getIdentifier());
        for (Action action : formattedValueActions) assertFalse(action.hasRawValue(), action.getIdentifier());
    }

    private static final class RecordingAction extends Action {

        private final boolean rawValue;
        @Nullable
        private String lastValue;

        private RecordingAction(boolean rawValue) {
            super("recording_test_action");
            this.rawValue = rawValue;
        }

        @Override
        public boolean hasValue() {
            return true;
        }

        @Override
        public boolean hasRawValue() {
            return this.rawValue;
        }

        @Override
        public void execute(@Nullable String value) {
            this.lastValue = value;
        }

        // The default check needs a running Minecraft client to compare threads.
        @Override
        public boolean checkAsync() {
            return true;
        }

        @Override
        public Component getDisplayName() {
            return Component.empty();
        }

        @Override
        public Component getDescription() {
            return Component.empty();
        }

        @Override
        public Component getValueDisplayName() {
            return Component.empty();
        }

        @Override
        public String getValuePreset() {
            return "";
        }

    }

}
