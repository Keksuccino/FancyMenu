
package de.keksuccino.fancymenu.customization.background;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.background.backgrounds.image.ImageMenuBackground;
import de.keksuccino.fancymenu.customization.background.backgrounds.image.ImageMenuBackgroundBuilder;
import de.keksuccino.fancymenu.misc.InputConstants;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.fancymenu.rendering.ui.scroll.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.rendering.ui.scroll.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.rendering.ui.scroll.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.rendering.ui.scroll.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.rendering.ui.widget.Button;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ChooseMenuBackgroundScreen extends Screen {

    protected static final MenuBackgroundBuilder<ImageMenuBackground> NO_BACKGROUND_TYPE = new ImageMenuBackgroundBuilder();
    public static final MenuBackground NO_BACKGROUND = new ImageMenuBackground(NO_BACKGROUND_TYPE);

    protected Screen parentScreen;
    protected MenuBackgroundBuilder<?> backgroundType;
    protected MenuBackground background;
    protected Consumer<MenuBackground> callback;

    protected ScrollArea backgroundTypeListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ScrollArea backgroundDescriptionScrollArea = new ScrollArea(0, 0, 0, 0);
    protected AdvancedButton configureButton;
    protected AdvancedButton doneButton;
    protected AdvancedButton cancelButton;

    public ChooseMenuBackgroundScreen(@Nullable Screen parentScreen, @Nullable MenuBackground backgroundToEdit, boolean addResetBackgroundEntry, @NotNull Consumer<MenuBackground> callback) {

        super(Component.translatable("fancymenu.menu_background.choose"));

        this.parentScreen = parentScreen;
        this.background = backgroundToEdit;
        this.callback = callback;
        this.setContentOfBackgroundTypeList(addResetBackgroundEntry);

        //Select correct entry if instance has action
        if (this.background != null) {
            for (ScrollAreaEntry e : this.backgroundTypeListScrollArea.getEntries()) {
                if ((e instanceof BackgroundTypeScrollEntry) && (((BackgroundTypeScrollEntry)e).backgroundType == this.background.builder)) {
                    e.setSelected(true);
                    this.backgroundType = this.background.builder;
                    this.setDescription(this.backgroundType);
                    break;
                }
            }
        }
        if (this.backgroundType == null) {
            this.background = null;
            if (addResetBackgroundEntry) {
                this.backgroundTypeListScrollArea.getEntries().get(0).setSelected(true);
                this.backgroundType = NO_BACKGROUND_TYPE;
                this.background = NO_BACKGROUND;
                this.setDescription(NO_BACKGROUND_TYPE);
            }
        }

        this.configureButton = new Button(0, 0, 150, 20, Component.translatable("fancymenu.menu_background.choose.configure_background"), true, (button) -> {
            if (this.backgroundType != null) {
                this.backgroundType.buildNewOrEditInstanceInternal(this.parentScreen, this.background, (back) -> {
                    if (back != null) {
                        this.background = back;
                    }
                });
            }
        }) {
            @Override
            public void render(@NotNull PoseStack pose, int p_93658_, int p_93659_, float p_93660_) {
                if (ChooseMenuBackgroundScreen.this.backgroundType == null) {
                    TooltipHandler.INSTANCE.addWidgetTooltip(this, Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.menu_background.choose.not_background_selected")).setDefaultBackgroundColor(), false, true);
                    this.active = false;
                } else {
                    this.active = ChooseMenuBackgroundScreen.this.backgroundType != NO_BACKGROUND_TYPE;
                }
                super.render(pose, p_93658_, p_93659_, p_93660_);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.configureButton);

        this.doneButton = new Button(0, 0, 150, 20, Component.translatable("fancymenu.guicomponents.done"), true, (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(this.background);
        }) {
            @Override
            public void renderWidget(PoseStack pose, int mouseX, int mouseY, float partial) {
                if (ChooseMenuBackgroundScreen.this.backgroundType == null) {
                    TooltipHandler.INSTANCE.addWidgetTooltip(this, Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.menu_background.choose.not_background_selected")).setDefaultBackgroundColor(), false, true);
                    this.active = false;
                } else if (ChooseMenuBackgroundScreen.this.background == null) {
                    TooltipHandler.INSTANCE.addWidgetTooltip(this, Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.menu_background.choose.not_configured")).setDefaultBackgroundColor(), false, true);
                    this.active = false;
                } else {
                    this.active = true;
                }
                super.renderWidget(pose, mouseX, mouseY, partial);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.doneButton);

        this.cancelButton = new Button(0, 0, 150, 20, Component.translatable("fancymenu.guicomponents.cancel"), true, (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(null);
        });
        UIBase.applyDefaultButtonSkinTo(this.cancelButton);

    }

    @Override
    protected void init() {

        super.init();

        this.setDescription(this.backgroundType);

    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parentScreen);
        this.callback.accept(null);
    }

    @Override
    public void render(@NotNull PoseStack matrix, int mouseX, int mouseY, float partial) {

        fill(matrix, 0, 0, this.width, this.height, UIBase.SCREEN_BACKGROUND_COLOR.getRGB());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        this.font.draw(matrix, titleComp, 20, 20, -1);

        this.font.draw(matrix, Component.translatable("fancymenu.menu_background.choose.available_types"), 20, 50, -1);

        this.backgroundTypeListScrollArea.setWidth((this.width / 2) - 40, true);
        this.backgroundTypeListScrollArea.setHeight(this.height - 85, true);
        this.backgroundTypeListScrollArea.setX(20, true);
        this.backgroundTypeListScrollArea.setY(50 + 15, true);
        this.backgroundTypeListScrollArea.render(matrix, mouseX, mouseY, partial);

        Component descLabel = Component.translatable("fancymenu.menu_background.choose.type_description");
        int descLabelWidth = this.font.width(descLabel);
        this.font.draw(matrix, descLabel, this.width - 20 - descLabelWidth, 50, -1);

        this.backgroundDescriptionScrollArea.setWidth((this.width / 2) - 40, true);
        this.backgroundDescriptionScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
        this.backgroundDescriptionScrollArea.setX(this.width - 20 - this.backgroundDescriptionScrollArea.getWidthWithBorder(), true);
        this.backgroundDescriptionScrollArea.setY(50 + 15, true);
        this.backgroundDescriptionScrollArea.render(matrix, mouseX, mouseY, partial);

        this.doneButton.setX(this.width - 20 - this.doneButton.getWidth());
        this.doneButton.setY(this.height - 20 - 20);
        this.doneButton.render(matrix, mouseX, mouseY, partial);

        this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
        this.cancelButton.setY(this.doneButton.getY() - 5 - 20);
        this.cancelButton.render(matrix, mouseX, mouseY, partial);

        this.configureButton.setX(this.width - 20 - this.configureButton.getWidth());
        this.configureButton.setY(this.cancelButton.getY() - 15 - 20);
        this.configureButton.render(matrix, mouseX, mouseY, partial);

        super.render(matrix, mouseX, mouseY, partial);

    }

    protected void setDescription(@Nullable MenuBackgroundBuilder<?> builder) {

        this.backgroundDescriptionScrollArea.clearEntries();

        if (builder == NO_BACKGROUND_TYPE) return;

        if ((builder != null) && (builder.getDescription() != null)) {
            for (Component c : builder.getDescription()) {
                TextScrollAreaEntry e = new TextScrollAreaEntry(this.backgroundDescriptionScrollArea, c.copy(), (entry) -> {});
                e.setSelectable(false);
                e.setBackgroundColorHover(e.getBackgroundColorIdle());
                e.setPlayClickSound(false);
                this.backgroundDescriptionScrollArea.addEntry(e);
            }
        }

    }

    protected void setContentOfBackgroundTypeList(boolean addResetBackgroundEntry) {

        this.backgroundTypeListScrollArea.clearEntries();

        if (addResetBackgroundEntry) {
            BackgroundTypeScrollEntry e = new BackgroundTypeScrollEntry(this.backgroundTypeListScrollArea, NO_BACKGROUND_TYPE, (entry) -> {
                if (this.backgroundType != NO_BACKGROUND_TYPE) {
                    this.backgroundType = NO_BACKGROUND_TYPE;
                    this.background = NO_BACKGROUND;
                    this.setDescription(NO_BACKGROUND_TYPE);
                }
            });
            this.backgroundTypeListScrollArea.addEntry(e);
        }

        for (MenuBackgroundBuilder<?> b : MenuBackgroundRegistry.getBuilders()) {
            BackgroundTypeScrollEntry e = new BackgroundTypeScrollEntry(this.backgroundTypeListScrollArea, b, (entry) -> {
                if (this.backgroundType != b) {
                    this.backgroundType = b;
                    this.background = null;
                    this.setDescription(b);
                }
            });
            this.backgroundTypeListScrollArea.addEntry(e);
        }

    }

    @Override
    public boolean keyPressed(int button, int $$1, int $$2) {

        if (button == InputConstants.KEY_ENTER) {
            if (this.background != null) {
                Minecraft.getInstance().setScreen(this.parentScreen);
                this.callback.accept(this.background);
                return true;
            }
        }

        return super.keyPressed(button, $$1, $$2);

    }

    public static class BackgroundTypeScrollEntry extends TextListScrollAreaEntry {

        public MenuBackgroundBuilder<?> backgroundType;

        public BackgroundTypeScrollEntry(ScrollArea parent, @NotNull MenuBackgroundBuilder<?> backgroundType, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, getText(backgroundType), LISTING_DOT_BLUE, onClick);
            this.backgroundType = backgroundType;
        }

        private static Component getText(MenuBackgroundBuilder<?> backgroundType) {
            if (backgroundType == NO_BACKGROUND_TYPE) {
                return Component.translatable("fancymenu.menu_background.choose.entry.no_background").withStyle(ChatFormatting.RED);
            }
            return backgroundType.getDisplayName().copy().setStyle(Style.EMPTY.withColor(TEXT_COLOR_GRAY_1.getRGB()));
        }

    }

}
