package de.keksuccino.fancymenu.customization.layout;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.cycle.ValueCycle;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class ManageLayoutsScreen extends Screen {

    protected Consumer<List<Layout>> callback;
    protected List<Layout> layouts;
    @Nullable
    protected Screen layoutTargetScreen;

    protected ValueCycle<Sorting> sortBy = ValueCycle.fromArray(Sorting.LAST_EDITED, Sorting.NAME, Sorting.STATUS);
    protected ScrollArea layoutListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedButton sortingButton;
    protected ExtendedButton doneButton;
    protected ExtendedButton editButton;
    protected ExtendedButton deleteButton;
    protected ExtendedButton openInTextEditorButton;
    protected ExtendedButton toggleStatusButton;

    public ManageLayoutsScreen(@NotNull List<Layout> layouts, @Nullable Screen layoutTargetScreen, @NotNull Consumer<List<Layout>> callback) {

        super(Component.translatable("fancymenu.layout.manage"));

        this.layouts = layouts;
        this.layoutTargetScreen = layoutTargetScreen;
        this.callback = callback;
        this.updateLayoutScrollArea();

    }

    @Override
    protected void init() {

        super.init();

        this.sortingButton = new ExtendedButton(0, 0, 150, this.font.lineHeight + 4, Component.literal(""), (button) -> {
            this.sortBy.next();
            this.updateLayoutScrollArea();
        }).setLabelSupplier(consumes -> this.sortBy.current().getCycleComponent());
        this.addWidget(this.sortingButton);
        UIBase.applyDefaultWidgetSkinTo(this.sortingButton);

        this.doneButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.guicomponents.done"), (button) -> {
            this.callback.accept(this.layouts);
        });
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);

        this.editButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.layout.manage.edit"), (button) -> {
            LayoutScrollEntry e = this.getSelectedEntry();
            if (e != null) {
                LayoutHandler.openLayoutEditor(e.layout, e.layout.isUniversalLayout() ? null : this.layoutTargetScreen);
            }
        }).setIsActiveSupplier(consumes -> (this.getSelectedEntry() != null));
        this.addWidget(this.editButton);
        UIBase.applyDefaultWidgetSkinTo(this.editButton);

        this.deleteButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.layout.manage.delete"), (button) -> {
            LayoutScrollEntry e = this.getSelectedEntry();
            if (e != null) {
                Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings(call -> {
                    if (call) {
                        e.layout.delete(false);
                        this.layouts.remove(e.layout);
                        this.updateLayoutScrollArea();
                    }
                    Minecraft.getInstance().setScreen(this);
                }, LocalizationUtils.splitLocalizedStringLines("fancymenu.layout.manage.delete.confirm")));
            }
        }).setIsActiveSupplier(consumes -> (this.getSelectedEntry() != null));
        this.addWidget(this.deleteButton);
        UIBase.applyDefaultWidgetSkinTo(this.deleteButton);

        this.openInTextEditorButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.layout.manage.open_in_text_editor"), (button) -> {
            LayoutScrollEntry e = this.getSelectedEntry();
            if ((e != null) && (e.layout.layoutFile != null)) {
                FileUtils.openFile(e.layout.layoutFile);
            }
        }).setIsActiveSupplier(consumes -> (this.getSelectedEntry() != null));
        this.addWidget(this.openInTextEditorButton);
        UIBase.applyDefaultWidgetSkinTo(this.openInTextEditorButton);

        this.toggleStatusButton = new ExtendedButton(0, 0, 150, 20, Component.literal(""), (button) -> {
            LayoutScrollEntry e = this.getSelectedEntry();
            if (e != null) {
                e.layout.setEnabled(!e.layout.isEnabled(), false);
                e.updateName();
            }
        }).setIsActiveSupplier(consumes -> (this.getSelectedEntry() != null))
                .setLabelSupplier(consumes -> {
                    LayoutScrollEntry e = this.getSelectedEntry();
                    if (e != null) return e.layout.getStatus().getCycleComponent();
                    return Layout.LayoutStatus.DISABLED.getCycleComponent();
                });
        this.addWidget(this.toggleStatusButton);
        UIBase.applyDefaultWidgetSkinTo(this.toggleStatusButton);

    }

    @Override
    public void onClose() {
        this.callback.accept(this.layouts);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();

        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        graphics.drawString(this.font, Component.translatable("fancymenu.layout.manage.layouts"), 20, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.layoutListScrollArea.setWidth((this.width / 2) - 40, true);
        this.layoutListScrollArea.setHeight(this.height - 85, true);
        this.layoutListScrollArea.setX(20, true);
        this.layoutListScrollArea.setY(50 + 15, true);
        this.layoutListScrollArea.render(graphics, mouseX, mouseY, partial);

        this.sortingButton.setWidth(this.font.width(this.sortingButton.getMessage()) + 10);
        this.sortingButton.setX(this.layoutListScrollArea.getXWithBorder() + this.layoutListScrollArea.getWidthWithBorder() - this.sortingButton.getWidth());
        this.sortingButton.setY(this.layoutListScrollArea.getYWithBorder() - 5 - this.sortingButton.getHeight());
        this.sortingButton.render(graphics, mouseX, mouseY, partial);

        this.doneButton.setX(this.width - 20 - this.doneButton.getWidth());
        this.doneButton.setY(this.height - 20 - 20);
        this.doneButton.render(graphics, mouseX, mouseY, partial);

        this.openInTextEditorButton.setX(this.width - 20 - this.openInTextEditorButton.getWidth());
        this.openInTextEditorButton.setY(this.doneButton.getY() - 15 - 20);
        this.openInTextEditorButton.render(graphics, mouseX, mouseY, partial);

        this.deleteButton.setX(this.width - 20 - this.deleteButton.getWidth());
        this.deleteButton.setY(this.openInTextEditorButton.getY() - 5 - 20);
        this.deleteButton.render(graphics, mouseX, mouseY, partial);

        this.editButton.setX(this.width - 20 - this.editButton.getWidth());
        this.editButton.setY(this.deleteButton.getY() - 5 - 20);
        this.editButton.render(graphics, mouseX, mouseY, partial);

        this.toggleStatusButton.setX(this.width - 20 - this.toggleStatusButton.getWidth());
        this.toggleStatusButton.setY(this.editButton.getY() - 5 - 20);
        this.toggleStatusButton.render(graphics, mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);

    }

    @Nullable
    protected LayoutScrollEntry getSelectedEntry() {
        for (ScrollAreaEntry e : this.layoutListScrollArea.getEntries()) {
            if (e instanceof LayoutScrollEntry s) {
                if (s.isSelected()) return s;
            }
        }
        return null;
    }

    protected void updateLayoutScrollArea() {
        this.layoutListScrollArea.clearEntries();
        if (this.sortBy.current() == Sorting.STATUS) {
            LayoutHandler.sortLayoutListByStatus(this.layouts, false);
        } else if (this.sortBy.current() == Sorting.NAME) {
            LayoutHandler.sortLayoutListByName(this.layouts);
        } else if (this.sortBy.current() == Sorting.LAST_EDITED) {
            LayoutHandler.sortLayoutListByLastEdited(this.layouts, false);
        }
        for (Layout l : this.layouts) {
            LayoutScrollEntry e = new LayoutScrollEntry(this.layoutListScrollArea, l, (entry) -> {
            });
            this.layoutListScrollArea.addEntry(e);
        }
        if (this.layoutListScrollArea.getEntries().isEmpty()) {
            this.layoutListScrollArea.addEntry(new TextScrollAreaEntry(this.layoutListScrollArea, Component.translatable("fancymenu.layout.manage.no_layouts_found").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt())), (entry) -> {}));
        }
    }

    public static class LayoutScrollEntry extends TextListScrollAreaEntry {

        public Layout layout;

        public LayoutScrollEntry(ScrollArea parent, @NotNull Layout layout, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, Component.literal(""), UIBase.getUIColorTheme().listing_dot_color_1.getColor(), onClick);
            this.layout = layout;
            this.updateName();
        }

        protected void updateName() {
            Style style = this.layout.getStatus().getValueComponentStyle();
            MutableComponent c = Component.literal(layout.getLayoutName()).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt()));
            c.append(Component.literal(" (").setStyle(style));
            c.append(this.layout.getStatus().getValueComponent());
            c.append(Component.literal(")").setStyle(style));
            this.setText(c);
        }

    }

    protected enum Sorting implements LocalizedCycleEnum<Sorting> {

        NAME("name"),
        LAST_EDITED("last_edited"),
        STATUS("status");

        final String name;

        Sorting(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getLocalizationKeyBase() {
            return "fancymenu.layout.manage.layouts.sort_by";
        }

        @Override
        public @NotNull String getName() {
            return this.name;
        }

        @Override
        public @NotNull Sorting[] getValues() {
            return Sorting.values();
        }

        @Override
        public @Nullable Sorting getByNameInternal(@NotNull String name) {
            return getByName(name);
        }

        @Nullable
        public static Sorting getByName(@NotNull String name) {
            for (Sorting e : Sorting.values()) {
                if (e.getName().equals(name)) return e;
            }
            return null;
        }

    }

}
