package de.keksuccino.fancymenu.customization.background;

import de.keksuccino.fancymenu.customization.background.backgrounds.image.ImageMenuBackground;
import de.keksuccino.fancymenu.customization.background.backgrounds.image.ImageMenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ChooseMenuBackgroundScreen extends Screen {

    protected static final MenuBackgroundBuilder<ImageMenuBackground> NO_BACKGROUND_TYPE = new ImageMenuBackgroundBuilder();
    public static final MenuBackground NO_BACKGROUND = new ImageMenuBackground(NO_BACKGROUND_TYPE);

    protected MenuBackgroundBuilder<?> backgroundType;
    protected MenuBackground background;
    protected Consumer<MenuBackground> callback;

    protected ScrollArea backgroundTypeListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ScrollArea backgroundDescriptionScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedButton configureButton;
    protected ExtendedButton doneButton;
    protected ExtendedButton cancelButton;

    public ChooseMenuBackgroundScreen(@Nullable MenuBackground backgroundToEdit, boolean addResetBackgroundEntry, @NotNull Consumer<MenuBackground> callback) {

        super(Component.translatable("fancymenu.menu_background.choose"));

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

    }

    @Override
    protected void init() {

        super.init();

        this.configureButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.menu_background.choose.configure_background"), (button) -> {
            if (this.backgroundType != null) {
                this.backgroundType.buildNewOrEditInstanceInternal(this, this.background, (back) -> {
                    if (back != null) {
                        this.background = back;
                    }
                });
            }
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                if (ChooseMenuBackgroundScreen.this.backgroundType == null) {
                    TooltipHandler.INSTANCE.addWidgetTooltip(this, Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.menu_background.choose.not_background_selected")).setDefaultStyle(), false, true);
                    this.active = false;
                } else {
                    this.active = ChooseMenuBackgroundScreen.this.backgroundType != NO_BACKGROUND_TYPE;
                }
                super.render(graphics, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.configureButton);
        UIBase.applyDefaultWidgetSkinTo(this.configureButton);

        this.doneButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.done"), (button) -> {
            this.callback.accept(this.background);
        }) {
            @Override
            public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                if (ChooseMenuBackgroundScreen.this.backgroundType == null) {
                    TooltipHandler.INSTANCE.addWidgetTooltip(this, Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.menu_background.choose.not_background_selected")).setDefaultStyle(), false, true);
                    this.active = false;
                } else if (ChooseMenuBackgroundScreen.this.background == null) {
                    TooltipHandler.INSTANCE.addWidgetTooltip(this, Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.menu_background.choose.not_configured")).setDefaultStyle(), false, true);
                    this.active = false;
                } else {
                    this.active = true;
                }
                super.renderWidget(graphics, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
            this.callback.accept(null);
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

        this.setDescription(this.backgroundType);

    }

    @Override
    public void onClose() {
        this.callback.accept(null);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        graphics.drawString(this.font, Component.translatable("fancymenu.menu_background.choose.available_types"), 20, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.backgroundTypeListScrollArea.setWidth((this.width / 2) - 40, true);
        this.backgroundTypeListScrollArea.setHeight(this.height - 85, true);
        this.backgroundTypeListScrollArea.setX(20, true);
        this.backgroundTypeListScrollArea.setY(50 + 15, true);
        this.backgroundTypeListScrollArea.render(graphics, mouseX, mouseY, partial);

        Component descLabel = Component.translatable("fancymenu.menu_background.choose.type_description");
        int descLabelWidth = this.font.width(descLabel);
        graphics.drawString(this.font, descLabel, this.width - 20 - descLabelWidth, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.backgroundDescriptionScrollArea.setWidth((this.width / 2) - 40, true);
        this.backgroundDescriptionScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
        this.backgroundDescriptionScrollArea.setX(this.width - 20 - this.backgroundDescriptionScrollArea.getWidthWithBorder(), true);
        this.backgroundDescriptionScrollArea.setY(50 + 15, true);
        this.backgroundDescriptionScrollArea.render(graphics, mouseX, mouseY, partial);

        this.doneButton.setX(this.width - 20 - this.doneButton.getWidth());
        this.doneButton.setY(this.height - 20 - 20);
        this.doneButton.render(graphics, mouseX, mouseY, partial);

        this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
        this.cancelButton.setY(this.doneButton.getY() - 5 - 20);
        this.cancelButton.render(graphics, mouseX, mouseY, partial);

        this.configureButton.setX(this.width - 20 - this.configureButton.getWidth());
        this.configureButton.setY(this.cancelButton.getY() - 15 - 20);
        this.configureButton.render(graphics, mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);

    }

    protected void setDescription(@Nullable MenuBackgroundBuilder<?> builder) {

        this.backgroundDescriptionScrollArea.clearEntries();

        if (builder == NO_BACKGROUND_TYPE) return;

        if ((builder != null) && (builder.getDescription() != null)) {
            for (Component c : builder.getDescription()) {
                TextScrollAreaEntry e = new TextScrollAreaEntry(this.backgroundDescriptionScrollArea, c.copy().withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())), (entry) -> {});
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
            if ((LayoutEditorScreen.getCurrentInstance() != null) && !b.shouldShowUpInEditorBackgroundMenu(LayoutEditorScreen.getCurrentInstance())) continue;
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
                this.callback.accept(this.background);
                return true;
            }
        }

        return super.keyPressed(button, $$1, $$2);

    }

    public static class BackgroundTypeScrollEntry extends TextListScrollAreaEntry {

        public MenuBackgroundBuilder<?> backgroundType;
        @Nullable
        public Supplier<Tooltip> tooltipSupplier = null;

        public BackgroundTypeScrollEntry(ScrollArea parent, @NotNull MenuBackgroundBuilder<?> backgroundType, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, getText(backgroundType), UIBase.getUIColorTheme().listing_dot_color_1.getColor(), onClick);
            this.backgroundType = backgroundType;
            if (this.backgroundType.isDeprecated()) {
                this.tooltipSupplier = () -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.menu_background.deprecated.details"))
                        .setDefaultStyle()
                        .setTextBaseColor(UIBase.getUIColorTheme().warning_text_color);
            }
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            if (this.tooltipSupplier != null) {
                Tooltip t = this.tooltipSupplier.get();
                if (t != null) TooltipHandler.INSTANCE.addTooltip(t, this::isHovered, false, true);
            }
            super.render(graphics, mouseX, mouseY, partial);
        }

        private static Component getText(MenuBackgroundBuilder<?> backgroundType) {
            if (backgroundType == NO_BACKGROUND_TYPE) {
                return Component.translatable("fancymenu.menu_background.choose.entry.no_background").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt()));
            }
            MutableComponent c = backgroundType.getDisplayName().copy().setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt()));
            if (backgroundType.isDeprecated()) c.append(Component.translatable("fancymenu.menu_background.deprecated").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt())));
            return c;
        }

    }

}
