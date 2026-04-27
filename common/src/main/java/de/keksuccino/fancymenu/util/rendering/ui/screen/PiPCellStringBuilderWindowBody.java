package de.keksuccino.fancymenu.util.rendering.ui.screen;

import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPCellWindowBody;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

public abstract class PiPCellStringBuilderWindowBody extends PiPCellWindowBody {

    protected final Consumer<String> callback;

    protected PiPCellStringBuilderWindowBody(@NotNull Component title, @NotNull Consumer<String> callback) {
        super(title);
        this.callback = callback;
    }

    @Override
    protected void onCancel() {
        this.callback.accept(null);
    }

    @Override
    protected void onDone() {
        this.callback.accept(this.buildString());
    }

    @NotNull
    public abstract String buildString();

}
