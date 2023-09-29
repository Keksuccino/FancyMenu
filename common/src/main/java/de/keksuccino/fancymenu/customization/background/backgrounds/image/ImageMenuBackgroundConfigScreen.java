package de.keksuccino.fancymenu.customization.background.backgrounds.image;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfiguratorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.ChooseFileScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

public class ImageMenuBackgroundConfigScreen extends ConfiguratorScreen {

    @NotNull
    protected Consumer<ImageMenuBackground> callback;
    @NotNull ImageMenuBackground background;

    protected ImageMenuBackgroundConfigScreen(@NotNull ImageMenuBackground background, @NotNull Consumer<ImageMenuBackground> callback) {
        super(Component.translatable("fancymenu.background.image.configure"));
        this.background = background;
        this.callback = callback;
    }

    @Override
    protected void initCells() {

        this.addStartEndSpacerCell();

        this.addCycleButtonCell(ImageMenuBackground.BackgroundImageType.LOCAL.cycle(this.background.type), true, (value, button) -> {
            this.background.imagePathOrUrl = null;
            this.background.type = value;
            this.init();
        });

        this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.empty(), var1 -> {
            if (this.background.type == ImageMenuBackground.BackgroundImageType.WEB) {
                TextEditorScreen s = new TextEditorScreen(null, s1 -> {
                    if (s1 != null) {
                        this.background.imagePathOrUrl = s1;
                    }
                    Minecraft.getInstance().setScreen(this);
                });
                s.setMultilineMode(false);
                s.setText(this.background.imagePathOrUrl);
                Minecraft.getInstance().setScreen(s);
            } else {
                ChooseFileScreen s = new ChooseFileScreen(LayoutHandler.ASSETS_DIR, LayoutHandler.ASSETS_DIR, (call) -> {
                    if (call != null) {
                        this.background.imagePathOrUrl = ScreenCustomization.getPathWithoutGameDirectory(call.getAbsolutePath());
                    }
                    Minecraft.getInstance().setScreen(this);
                });
                s.setFileFilter(FileFilter.IMAGE_AND_GIF_FILE_FILTER);
                Minecraft.getInstance().setScreen(s);
            }
        }).setLabelSupplier(consumes -> {
            if (this.background.type == ImageMenuBackground.BackgroundImageType.WEB) return Component.translatable("fancymenu.background.image.configure.choose_image.web");
            return Component.translatable("fancymenu.background.image.configure.choose_image.local");
        }), true);

        if (this.background.type == ImageMenuBackground.BackgroundImageType.WEB) {

            this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.background.image.type.web.fallback"), var1 -> {
                ChooseFileScreen s = new ChooseFileScreen(LayoutHandler.ASSETS_DIR, LayoutHandler.ASSETS_DIR, (call) -> {
                    if (call != null) {
                        this.background.webImageFallbackPath = ScreenCustomization.getPathWithoutGameDirectory(call.getAbsolutePath());
                    }
                    Minecraft.getInstance().setScreen(this);
                });
                s.setFileFilter(FileFilter.IMAGE_AND_GIF_FILE_FILTER);
                Minecraft.getInstance().setScreen(s);
            }).setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.background.image.type.web.fallback.desc"))), true);

            this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.background.image.type.web.fallback.reset").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt())), var1 -> {
                this.background.webImageFallbackPath = null;
            }), true);

        }

        this.addCellGroupEndSpacerCell();

        this.addCycleButtonCell(CommonCycles.cycleEnabledDisabled("fancymenu.background.image.configure.slide", this.background.slideLeftRight), true, (value, button) -> {
           this.background.slideLeftRight = value.getAsBoolean();
        });

        this.addStartEndSpacerCell();

    }

    @Override
    protected void init() {

        super.init();

        if (this.doneButton != null) {
            this.doneButton.setTooltipSupplier(consumes -> {
                if (this.background.imagePathOrUrl == null) return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.background.image.configure.no_image_chosen"));
                return null;
            });
        }

    }

    @Override
    public boolean allowDone() {
        return (this.background.imagePathOrUrl != null);
    }

    @Override
    protected void onCancel() {
        this.callback.accept(null);
    }

    @Override
    protected void onDone() {
        this.callback.accept(this.background);
    }

}
