package de.keksuccino.fancymenu;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import de.keksuccino.fancymenu.util.rendering.text.markdown.ScrollableMarkdownRenderer;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class WelcomeWindowBody extends PiPWindowBody {

    private static final Logger LOGGER = LogManager.getLogger();
    /** The space above the text, between the text and the button, and below the button. */
    private static final int MARGIN = 14;
    private static final int BUTTON_HEIGHT = 20;
    /** The space left and right of the text. */
    private static final int BORDER = 40;
    private static final Component TITLE = Component.translatable("fancymenu.welcome.screen.title");

    public static final int PIP_WINDOW_WIDTH = 490;
    /** The window is as tall as its text needs it, but never taller than this. A longer text scrolls. */
    public static final int MAX_PIP_WINDOW_HEIGHT = 340;

    private ScrollableMarkdownRenderer markdownRenderer;

    public static void openInWindow() {
        int height = getFittingWindowHeight();
        PiPWindow window = new PiPWindow(new WelcomeWindowBody())
                .setForceFancyMenuUiScale(true)
                .setMinSize(PIP_WINDOW_WIDTH, height)
                .setSize(PIP_WINDOW_WIDTH, height)
                .setAlwaysOnTop(true)
                .setForceFocus(true)
                .setBlockMinecraftScreenInputs(true)
                .setTitle(TITLE);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
    }

    /**
     * Returns the height the window needs to show its whole text, which is longer in some languages than in others.
     */
    private static int getFittingWindowHeight() {
        int bodyWidth = PIP_WINDOW_WIDTH - (PiPWindow.DEFAULT_BORDER_THICKNESS * 2);
        ScrollableMarkdownRenderer text = new ScrollableMarkdownRenderer(0.0F, 0.0F, getTextAreaWidth(bodyWidth), 0.0F);
        int bodyHeight = MARGIN + (int) setUpText(text) + MARGIN + BUTTON_HEIGHT + MARGIN;
        return Math.min(MAX_PIP_WINDOW_HEIGHT, bodyHeight + PiPWindow.DEFAULT_TITLE_BAR_HEIGHT + (PiPWindow.DEFAULT_BORDER_THICKNESS * 2));
    }

    private static int getTextAreaWidth(int bodyWidth) {
        return bodyWidth - (BORDER * 2);
    }

    /**
     * Sets up the text of the given area and returns the height the area has to have to show all of it.
     */
    private static float setUpText(@NotNull ScrollableMarkdownRenderer text) {
        MarkdownRenderer renderer = text.getMarkdownRenderer();
        // The headline is the first paragraph of the text, so it has the same space below it as every other paragraph
        renderer.setText("^^^\n**" + I18n.get("fancymenu.welcome.screen.headline") + "**\n\n" + I18n.get("fancymenu.welcome.screen.text") + "\n^^^");
        renderer.setAutoLineBreakingEnabled(true);
        renderer.getStyle().applyLook(MarkdownLook.NEUTRAL);
        renderer.setUIFontRenderingEnabled(true);
        renderer.setOptimalWidth(text.getScrollArea().getInnerWidth());
        // Whole pixels, so the area is never a fraction of a pixel too short for its text, which would make it scroll
        return (float) Math.ceil(renderer.getRealHeight()) + (text.getScrollArea().getBorderThickness() * 2);
    }

    private WelcomeWindowBody() {
        super(TITLE);
    }

    protected void init() {

        int centerX = this.width / 2;
        int textWidth = getTextAreaWidth(this.width);
        float textX = centerX - (textWidth / 2.0F);
        float availableHeight = Math.max(0, this.height - MARGIN - MARGIN - BUTTON_HEIGHT - MARGIN);
        this.markdownRenderer = new ScrollableMarkdownRenderer(textX, MARGIN, textWidth, availableHeight);
        float textHeight = setUpText(this.markdownRenderer);
        if (textHeight < availableHeight) {
            // A text that needs less than the space above the button stands in the middle of that space
            this.markdownRenderer = new ScrollableMarkdownRenderer(textX, MARGIN + ((availableHeight - textHeight) / 2.0F), textWidth, textHeight);
            setUpText(this.markdownRenderer);
        }
        this.addRenderableWidget(this.markdownRenderer);

        ExtendedButton btn = this.addRenderableWidget(new ExtendedButton(centerX - 100, this.height - MARGIN - BUTTON_HEIGHT, 200, BUTTON_HEIGHT, Component.translatable("fancymenu.welcome.screen.open_docs"), button -> {
            this.onCloseWelcomeWindow();
            this.closeWindow();
        })).setUITooltip(UITooltip.of(Component.translatable("fancymenu.welcome.screen.open_docs.tooltip")));
        UIBase.applyDefaultWidgetSkinTo(btn, UIBase.shouldBlur());

    }

    @Override
    public void onWindowClosedExternally() {
        this.onCloseWelcomeWindow();
    }

    private void onCloseWelcomeWindow() {
        try {
            WebUtils.openWebLink("https://docs.fancymenu.net");
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to open FancyMenu docs in WelcomeScreen!", ex);
        }
        FancyMenu.getOptions().showWelcomeScreen.setValue(false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        return this.markdownRenderer.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        return this.mouseReleased(event.x(), event.y(), event.button());
    }
    
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.markdownRenderer.mouseReleased(mouseX, mouseY, button);
    }

}
