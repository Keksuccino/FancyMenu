package de.keksuccino.fancymenu.util.rendering.ui.screen;

import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPCellWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class StringListChooserScreen extends PiPCellWindowBody {

    public static final int PIP_WINDOW_WIDTH = 520;
    public static final int PIP_WINDOW_HEIGHT = 360;

    protected Consumer<String> callback;
    protected List<String> list;

    public StringListChooserScreen(@NotNull Component title, @NotNull List<String> stringList, @NotNull Consumer<String> callback) {
        super(title);
        this.list = stringList;
        this.callback = callback;
    }

    @Override
    protected void initCells() {

        for (String s : this.list) {
            this.addCell(new StringCell(s)).setSelectable(true);
        }

        this.addSpacerCell(20);

    }

    @Override
    public boolean allowDone() {
        return (this.getSelectedCell() != null);
    }

    @Override
    protected void onCancel() {
        this.callback.accept(null);
        this.closeWindow();
    }

    @Override
    protected void onDone() {
        RenderCell cell = this.getSelectedCell();
        if (cell instanceof StringCell s) {
            this.callback.accept(s.string);
        }
        this.closeWindow();
    }

    @Override
    public void onWindowClosedExternally() {
        this.callback.accept(null);
    }

    public static @NotNull PiPWindow openInWindow(@NotNull StringListChooserScreen screen, @Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(screen.getTitle())
                .setScreen(screen)
                .setForceFancyMenuUiScale(true)
                .setAlwaysOnTop(false)
                .setForceFocus(false)
                .setBlockMinecraftScreenInputs(false)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        return window;
    }

    public static @NotNull PiPWindow openInWindow(@NotNull StringListChooserScreen screen) {
        return openInWindow(screen, null);
    }

    public class StringCell extends LabelCell {

        public String string;

        public StringCell(@NotNull String string) {
            super(Component.literal(string));
            this.string = string;
        }

    }

}
