package de.keksuccino.fancymenu.util.rendering.ui.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class LogicExecutorScreen extends Screen {

    @NotNull
    protected Runnable task;

    @NotNull
    public static LogicExecutorScreen build(@NotNull Runnable taskToExecuteOnScreenInit) {
        return new LogicExecutorScreen(taskToExecuteOnScreenInit);
    }

    protected LogicExecutorScreen(@NotNull Runnable taskToExecuteOnScreenInit) {
        super(Component.empty());
        this.task = taskToExecuteOnScreenInit;
    }

    @Override
    protected void init() {
        this.task.run();
    }

}
