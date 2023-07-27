package de.keksuccino.fancymenu.customization.background.backgrounds.image;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.ChooseFileScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.ExtendedButton;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ImageMenuBackgroundConfigScreen extends Screen {

    protected Screen parent;
    protected ImageMenuBackground background;
    protected Consumer<ImageMenuBackground> callback;

    protected ExtendedButton chooseImageButton;
    protected ExtendedButton toggleSlideButton;
    protected ExtendedButton cancelButton;
    protected ExtendedButton doneButton;

    protected ImageMenuBackgroundConfigScreen(@Nullable Screen parent, @NotNull ImageMenuBackground background, @NotNull Consumer<ImageMenuBackground> callback) {

        super(Component.translatable("fancymenu.background.image.configure"));

        this.parent = parent;
        this.background = background;
        this.callback = callback;

    }

    @Override
    protected void init() {

        super.init();

        this.chooseImageButton = new ExtendedButton(0, 0, 300, 20, Component.translatable("fancymenu.background.image.configure.choose_image"), (press) -> {
            ChooseFileScreen s = new ChooseFileScreen(LayoutHandler.ASSETS_DIR, LayoutHandler.ASSETS_DIR, (call) -> {
                if (call != null) {
                    this.background.imagePath = ScreenCustomization.getPathWithoutGameDirectory(call.getAbsolutePath());
                }
                Minecraft.getInstance().setScreen(this);
            });
            s.setFileFilter(FileFilter.IMAGE_FILE_FILTER);
            Minecraft.getInstance().setScreen(s);
        });
        this.addWidget(this.chooseImageButton);
        UIBase.applyDefaultWidgetSkinTo(this.chooseImageButton);

        this.toggleSlideButton = new ExtendedButton(0, 0, 300, 20, Component.literal(""), (press) -> {
            this.background.slideLeftRight = !this.background.slideLeftRight;
        }) {
            @Override
            public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
                if (!ImageMenuBackgroundConfigScreen.this.background.slideLeftRight) {
                    this.setMessage(Component.translatable("fancymenu.background.image.configure.slide.off"));
                } else {
                    this.setMessage(Component.translatable("fancymenu.background.image.configure.slide.on"));
                }
                super.render(pose, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.toggleSlideButton);
        UIBase.applyDefaultWidgetSkinTo(this.toggleSlideButton);

        this.doneButton = new ExtendedButton(0, 0, 145, 20, Component.translatable("fancymenu.guicomponents.done"), (press) -> {
            Minecraft.getInstance().setScreen(this.parent);
            this.callback.accept(this.background);
        }) {
            @Override
            public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
                this.active = ImageMenuBackgroundConfigScreen.this.background.imagePath != null;
                if (!this.active) {
                    TooltipHandler.INSTANCE.addWidgetTooltip(this, Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.background.image.configure.no_image_chosen")).setDefaultStyle(), false, true);
                }
                super.render(pose, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);

        this.cancelButton = new ExtendedButton(0, 0, 145, 20, Component.translatable("fancymenu.guicomponents.cancel"), (press) -> {
            this.onClose();
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();

        fill(pose, 0, 0, this.width, this.height, UIBase.getUIColorScheme().screen_background_color.getColorInt());

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        MutableComponent title = this.title.copy().withStyle(ChatFormatting.BOLD);
        int titleWidth = this.font.width(title);
        this.font.draw(pose, title, (float)centerX - ((float)titleWidth / 2F), 20, UIBase.getUIColorScheme().generic_text_base_color.getColorInt());

        this.chooseImageButton.setX(centerX - (this.chooseImageButton.getWidth() / 2));
        this.chooseImageButton.setY(centerY - 20 - 3);
        this.chooseImageButton.render(pose, mouseX, mouseY, partial);

        this.toggleSlideButton.setX(centerX - (this.toggleSlideButton.getWidth() / 2));
        this.toggleSlideButton.setY(centerY + 2);
        this.toggleSlideButton.render(pose, mouseX, mouseY, partial);

        this.doneButton.setX((this.width / 2) - this.doneButton.getWidth() - 5);
        this.doneButton.setY(this.height - 40);
        this.doneButton.render(pose, mouseX, mouseY, partial);

        this.cancelButton.setX((this.width / 2) + 5);
        this.cancelButton.setY(this.height - 40);
        this.cancelButton.render(pose, mouseX, mouseY, partial);

    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
        this.callback.accept(null);
    }

}
