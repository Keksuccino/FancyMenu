//TODO übernehmen
package de.keksuccino.fancymenu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.ScrollableMarkdownRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WelcomeScreen extends Screen {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int HEADER_HEIGHT = 40;
    private static final int FOOTER_HEIGHT = 50;
    private static final int BORDER = 40;

    private final Screen parent;
    private final Component headline = Component.translatable("fancymenu.welcome.screen.headline").withStyle(Style.EMPTY.withBold(true));
    private ScrollableMarkdownRenderer markdownRenderer;
    private final Font font = Minecraft.getInstance().font;

    public WelcomeScreen(@Nullable Screen parent) {
        super(Component.translatable("fancymenu.welcome.screen.title"));
        this.parent = parent;
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
        this.addRenderableWidget(this.markdownRenderer);

        this.addRenderableWidget(new ExtendedButton(centerX - 100, this.height - (FOOTER_HEIGHT / 2) - 10, 200, 20, Component.translatable("fancymenu.welcome.screen.open_docs"), button -> {
            try {
                WebUtils.openWebLink("https://docs.fancymenu.net");
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to open FancyMenu docs in WelcomeScreen!", ex);
            }
            FancyMenu.getOptions().showWelcomeScreen.setValue(false);
            this.onClose();
        })).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.welcome.screen.open_docs.tooltip")));

    }

    @Override
    public void render(@NotNull PoseStack graphics, int mouseX, int mouseY, float partial) {

        this.renderBackground(graphics, mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);

        drawCenteredString(graphics, this.font, this.headline, this.width / 2, (HEADER_HEIGHT / 2) - (this.font.lineHeight / 2), -1);

    }

    public void renderBackground(@NotNull PoseStack graphics, int mouseX, int mouseY, float partial) {

        this.renderBackground(graphics);

        //Render header and footer separators
        RenderSystem.enableBlend();
        RenderingUtils.resetShaderColor();
        fillGradient(graphics, 0, HEADER_HEIGHT, this.width, HEADER_HEIGHT + 4, -16777216, 0);
        fillGradient(graphics, 0, this.height - FOOTER_HEIGHT - 4, this.width, this.height - FOOTER_HEIGHT, 0, -16777216);
        RenderingUtils.resetShaderColor();

    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaY) {
        return this.markdownRenderer.mouseScrolled(mouseX, mouseY, scrollDeltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.markdownRenderer.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

}
