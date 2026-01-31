package de.keksuccino.fancymenu;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.ScrollableMarkdownRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class WelcomeWindowBody extends PiPWindowBody {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int HEADER_HEIGHT = 40;
    private static final int FOOTER_HEIGHT = 50;
    private static final int BORDER = 40;
    private static final Component TITLE = Component.translatable("fancymenu.welcome.screen.title");
    private static final Component HEADLINE = Component.translatable("fancymenu.welcome.screen.headline").withStyle(Style.EMPTY.withBold(true));

    public static final int PIP_WINDOW_WIDTH = 490;
    public static final int PIP_WINDOW_HEIGHT = 294;

    private ScrollableMarkdownRenderer markdownRenderer;

    public static void openInWindow() {
        PiPWindow window = new PiPWindow(new WelcomeWindowBody())
                .setForceFancyMenuUiScale(true)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setAlwaysOnTop(true)
                .setForceFocus(true)
                .setBlockMinecraftScreenInputs(true)
                .setTitle(TITLE);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
    }

    private WelcomeWindowBody() {
        super(TITLE);
    }

    protected void init() {

        int centerX = this.width / 2;
        int scrollWidth = this.width - BORDER * 2;
        int scrollHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT;
        if (this.markdownRenderer == null) {
            this.markdownRenderer = new ScrollableMarkdownRenderer((float)(centerX - scrollWidth / 2), (float)HEADER_HEIGHT, (float)scrollWidth, (float)scrollHeight);
        } else {
            this.markdownRenderer.rebuild((float)(centerX - scrollWidth / 2), (float)HEADER_HEIGHT, (float)scrollWidth, (float)scrollHeight);
        }
        this.markdownRenderer.getMarkdownRenderer().setText("^^^\n\n\n" + I18n.get("fancymenu.welcome.screen.text") + "\n^^^");
        this.markdownRenderer.getMarkdownRenderer().setAutoLineBreakingEnabled(true);
        this.markdownRenderer.getMarkdownRenderer().setTextBaseColor(UIBase.getUITheme().ui_interface_generic_text_color);
        this.markdownRenderer.getMarkdownRenderer().setTextShadow(false);
        this.markdownRenderer.getMarkdownRenderer().setUIFontRenderingEnabled(true);
        this.addRenderableWidget(this.markdownRenderer);

        ExtendedButton btn = this.addRenderableWidget(new ExtendedButton(centerX - 100, this.height - (FOOTER_HEIGHT / 2) - 10, 200, 20, Component.translatable("fancymenu.welcome.screen.open_docs"), button -> {
            this.onCloseWelcomeWindow();
            this.closeWindow();
        })).setUITooltip(UITooltip.of(Component.translatable("fancymenu.welcome.screen.open_docs.tooltip")));
        UIBase.applyDefaultWidgetSkinTo(btn, UIBase.shouldBlur());

    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();

        // Render headline
        float headlineW = UIBase.getUITextWidthNormal(HEADLINE, true);
        float headlineH = UIBase.getUITextHeightNormal(true);
        UIBase.renderText(graphics, HEADLINE, (this.width / 2F) - (headlineW / 2F), (HEADER_HEIGHT / 2F) - (headlineH / 2F), UIBase.getUITheme().ui_interface_generic_text_color.getColorInt(), UIBase.getUITextSizeNormal(), false);

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
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.markdownRenderer.mouseReleased(mouseX, mouseY, button);
    }

}
