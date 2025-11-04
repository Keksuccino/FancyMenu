package de.keksuccino.fancymenu.util.rendering.ui.screen.queueable;

import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.gui.ModernScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

public class QueueableScreen extends ModernScreen {

    private volatile Consumer<QueueableScreen> closeCallback;

    protected QueueableScreen(@NotNull Component title) {
        super(title);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        assertQueueableSetUpCorrectly();
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
    }

    @Override
    protected void init() {
        assertQueueableSetUpCorrectly();
        super.init();
    }

    @Override
    public void resize(@NotNull Minecraft mc, int width, int height) {
        assertQueueableSetUpCorrectly();
        super.resize(mc, width, height);
    }

    @Override
    protected void rebuildWidgets() {
        assertQueueableSetUpCorrectly();
        super.rebuildWidgets();
    }

    @Override
    public void onClose() {
        this.notifyHandlerOnClose();
    }

    public void setCloseCallback(@NotNull Consumer<QueueableScreen> closeCallback) {
        this.closeCallback = closeCallback;
        assertQueueableSetUpCorrectly();
    }

    protected void notifyHandlerOnClose() {
        assertQueueableSetUpCorrectly();
        this.closeCallback.accept(this);
    }

    public void assertQueueableSetUpCorrectly() {
        if (this.closeCallback == null) throw new RuntimeException("Close callback was NULL! QueueableScreens need to get opened via QueueableScreenHandler!");
    }

}
