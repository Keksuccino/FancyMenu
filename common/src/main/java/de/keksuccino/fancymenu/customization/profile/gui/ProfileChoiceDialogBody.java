package de.keksuccino.fancymenu.customization.profile.gui;

import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * A message dialog with custom choices, for questions and results a simple accept/cancel dialog can't present clearly.
 * Looks like FancyMenu's message dialogs.
 */
final class ProfileChoiceDialogBody extends PiPWindowBody {

    private static final int WIDTH = 440;
    private static final int HEIGHT = 190;
    private static final int ICON_SIZE = 32;
    private static final int ICON_GAP = 12;
    private static final int PADDING = 16;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;
    private static final int BUTTON_BOTTOM_PADDING = 20;
    private static final int LINE_SPACING = 14;
    /** Gets passed to the callback when the dialog got closed without choosing anything. */
    static final int CLOSED = -1;

    @NotNull
    private final MaterialIcon icon;
    private final int iconColor;
    @NotNull
    private final Component message;
    @NotNull
    private final List<Component> choices;
    @NotNull
    private final IntConsumer callback;
    @NotNull
    private final List<ExtendedButton> buttons = new ArrayList<>();
    @NotNull
    private List<MutableComponent> lines = new ArrayList<>();
    private boolean answered = false;

    /**
     * @param callback gets the index of the chosen choice, or {@link #CLOSED}.
     */
    static void open(@NotNull Component title, @NotNull MaterialIcon icon, int iconColor, @NotNull Component message, @NotNull List<Component> choices, @Nullable PiPWindow parentWindow, @NotNull IntConsumer callback) {
        ProfileChoiceDialogBody body = new ProfileChoiceDialogBody(icon, iconColor, message, choices, callback);
        PiPWindow window = new PiPWindow(title)
                .setIcon(icon)
                .setMinSize(WIDTH, HEIGHT)
                .setSize(WIDTH, HEIGHT)
                .setScreen(body)
                .setBlockMinecraftScreenInputs(true)
                .setForceFancyMenuUiScale(true)
                .setForceFocus(true);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
    }

    private ProfileChoiceDialogBody(@NotNull MaterialIcon icon, int iconColor, @NotNull Component message, @NotNull List<Component> choices, @NotNull IntConsumer callback) {
        super(Component.empty());
        this.icon = icon;
        this.iconColor = iconColor;
        this.message = message;
        this.choices = choices;
        this.callback = callback;
    }

    @Override
    protected void init() {
        this.buttons.clear();
        for (int i = 0; i < this.choices.size(); i++) {
            int index = i;
            ExtendedButton button = new ExtendedButton(0, 0, 100, BUTTON_HEIGHT, this.choices.get(i), b -> this.answer(index));
            button.setFocusable(false);
            button.setNavigatable(false);
            UIBase.applyDefaultWidgetSkinTo(button, UIBase.shouldBlur());
            this.buttons.add(this.addRenderableWidget(button));
        }
        int wrapWidth = Math.max(20, this.width - (PADDING * 2) - ICON_SIZE - ICON_GAP);
        this.lines = UIBase.lineWrapUIComponentsNormal(UIBase.lineWrapUIComponentsNormal(this.message, 100000.0F), wrapWidth);
    }

    @Override
    public void renderBody(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        int contentBottom = this.height - BUTTON_HEIGHT - BUTTON_BOTTOM_PADDING;
        int textHeight = this.lines.size() * LINE_SPACING;
        int blockHeight = Math.max(textHeight, ICON_SIZE);
        int contentY = PADDING + Math.max(0, (contentBottom - PADDING - blockHeight) / 2);
        ProfileUiRendering.renderMaterialIcon(graphics, this.icon, PADDING, contentY + ((blockHeight - ICON_SIZE) / 2.0F), ICON_SIZE, this.iconColor);
        int textX = PADDING + ICON_SIZE + ICON_GAP;
        int y = contentY + Math.max(0, (blockHeight - textHeight) / 2);
        for (Component line : this.lines) {
            UIBase.renderText(graphics, line, textX, y, ProfileUiRendering.getTextColor());
            y += LINE_SPACING;
        }
        int buttonWidth = Math.min(130, (this.width - (PADDING * 2) - (BUTTON_GAP * (this.buttons.size() - 1))) / Math.max(1, this.buttons.size()));
        int totalWidth = (buttonWidth * this.buttons.size()) + (BUTTON_GAP * (this.buttons.size() - 1));
        int x = (this.width - totalWidth) / 2;
        for (ExtendedButton button : this.buttons) {
            button.setWidth(buttonWidth);
            button.setX(x);
            button.setY(contentBottom);
            x += buttonWidth + BUTTON_GAP;
        }
    }

    private void answer(int choice) {
        if (this.answered) return;
        this.answered = true;
        this.callback.accept(choice);
        this.closeWindow();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            this.answer(CLOSED);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onWindowClosedExternally() {
        if (!this.answered) {
            this.answered = true;
            this.callback.accept(CLOSED);
        }
    }

}
