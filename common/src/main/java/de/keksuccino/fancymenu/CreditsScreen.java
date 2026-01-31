package de.keksuccino.fancymenu;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.ScrollableMarkdownRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class CreditsScreen extends Screen {

    private static final ResourceSource CREDITS_SOURCE = ResourceSource.of("fancymenu:credits_and_copyright.md", ResourceSourceType.LOCATION);

    protected ScrollableMarkdownRenderer markdownRenderer;
    protected int headerHeight = 20;
    protected int footerHeight = 40;
    protected int border = 40;
    protected Screen parent;
    protected boolean textSet = false;
    protected ResourceSupplier<IText> creditsTextSupplier = ResourceSupplier.text(CREDITS_SOURCE.getSerializationSource());

    public CreditsScreen(@NotNull Screen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    @Override
    protected void init() {

        int centerX = this.width / 2;
        int scrollWidth = this.width - (this.border * 2);
        int scrollHeight = this.height - this.headerHeight - this.footerHeight;

        if (this.markdownRenderer == null) {
            this.markdownRenderer = new ScrollableMarkdownRenderer((float)(centerX - (scrollWidth / 2)), this.headerHeight, scrollWidth, scrollHeight);
        } else {
            this.markdownRenderer.rebuild((float)(centerX - (scrollWidth / 2)), this.headerHeight, scrollWidth, scrollHeight);
        }
        // Allow markdown to render once to measure its size before entry culling.
        this.markdownRenderer.getScrollArea().setRenderOnlyEntriesInArea(false);
        this.markdownRenderer.getMarkdownRenderer().setHeadlineLineColor(UIBase.getUITheme().ui_interface_background_color);
        this.markdownRenderer.getMarkdownRenderer().setTextBaseColor(UIBase.getUITheme().ui_interface_generic_text_color);
        this.markdownRenderer.getMarkdownRenderer().setTextShadow(false);
        this.addRenderableWidget(this.markdownRenderer);

        UIBase.applyDefaultWidgetSkinTo(this.addRenderableWidget(new ExtendedButton(centerX - 100, this.height - (this.footerHeight / 2) - 10, 200, 20, Component.translatable("fancymenu.common.close"), var1 -> this.onClose())));

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.textSet) {
            IText text = this.creditsTextSupplier.get();
            if (text != null) {
                List<String> lines = text.getTextLines();
                if (lines != null) {
                    StringBuilder lineString = new StringBuilder();
                    for (String s : lines) {
                        lineString.append(s).append("\n");
                    }
                    this.markdownRenderer.setText(lineString.toString());
                    this.textSet = true;
                }
            }
        }

        RenderSystem.enableBlend();

        //Background
        graphics.fill(0, 0, this.width, this.height, UIBase.getUITheme().ui_interface_background_color.getColorInt());
        RenderingUtils.resetShaderColor(graphics);

        //Footer
        graphics.fill(0, this.height - this.footerHeight, this.width, this.height, UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt());
        RenderingUtils.resetShaderColor(graphics);

        super.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int $$1, int $$2, float $$3) {
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        return this.markdownRenderer.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
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
        return true;
    }

}
