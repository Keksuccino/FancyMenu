package de.keksuccino.fancymenu.customization.layout;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.ValueCycle;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.ExtendedButton;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
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
        UIBase.applyDefaultButtonSkinTo(this.sortingButton);

        this.doneButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.guicomponents.done"), (button) -> {
            this.callback.accept(this.layouts);
        });
        this.addWidget(this.doneButton);
        UIBase.applyDefaultButtonSkinTo(this.doneButton);

        this.editButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.layout.manage.edit"), (button) -> {
            LayoutScrollEntry e = this.getSelectedEntry();
            if (e != null) {
                LayoutHandler.openLayoutEditor(e.layout, e.layout.isUniversalLayout() ? null : this.layoutTargetScreen);
            }
        }).setIsActiveSupplier(consumes -> (this.getSelectedEntry() != null));
        this.addWidget(this.editButton);
        UIBase.applyDefaultButtonSkinTo(this.editButton);

        this.deleteButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.layout.manage.delete"), (button) -> {
            LayoutScrollEntry e = this.getSelectedEntry();
            if (e != null) {
                ConfirmationScreen s = new ConfirmationScreen(call -> {
                    if (call) {
                        e.layout.delete(false);
                        this.layouts.remove(e.layout);
                        this.updateLayoutScrollArea();
                    }
                    Minecraft.getInstance().setScreen(this);
                }, LocalizationUtils.splitLocalizedStringLines("fancymenu.layout.manage.delete.confirm"));
                Minecraft.getInstance().setScreen(s);
            }
        }).setIsActiveSupplier(consumes -> (this.getSelectedEntry() != null));
        this.addWidget(this.deleteButton);
        UIBase.applyDefaultButtonSkinTo(this.deleteButton);

        this.openInTextEditorButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.layout.manage.open_in_text_editor"), (button) -> {
            LayoutScrollEntry e = this.getSelectedEntry();
            if ((e != null) && (e.layout.layoutFile != null)) {
                ScreenCustomization.openFile(e.layout.layoutFile);
            }
        }).setIsActiveSupplier(consumes -> (this.getSelectedEntry() != null));
        this.addWidget(this.openInTextEditorButton);
        UIBase.applyDefaultButtonSkinTo(this.openInTextEditorButton);

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
        UIBase.applyDefaultButtonSkinTo(this.toggleStatusButton);

    }

    @Override
    public void onClose() {
        this.callback.accept(this.layouts);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();

        fill(pose, 0, 0, this.width, this.height, UIBase.getUIColorScheme().screenBackgroundColor.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        this.font.draw(pose, titleComp, 20, 20, UIBase.getUIColorScheme().genericTextBaseColor.getColorInt());

        this.font.draw(pose, Component.translatable("fancymenu.layout.manage.layouts"), 20, 50, UIBase.getUIColorScheme().genericTextBaseColor.getColorInt());

        this.layoutListScrollArea.setWidth((this.width / 2) - 40, true);
        this.layoutListScrollArea.setHeight(this.height - 85, true);
        this.layoutListScrollArea.setX(20, true);
        this.layoutListScrollArea.setY(50 + 15, true);
        this.layoutListScrollArea.render(pose, mouseX, mouseY, partial);

//        Component previewLabel = Component.translatable("fancymenu.animation.choose.preview");
//        int previewLabelWidth = this.font.width(previewLabel);
//        this.font.draw(pose, previewLabel, this.width - 20 - previewLabelWidth, 50, UIBase.getUIColorScheme().genericTextBaseColor.getColorInt());
//
//        if (this.selectedAnimation != null) {
//            int aniW = (this.width / 2) - 40;
//            int aniH = this.height / 2;
//            AspectRatio ratio = new AspectRatio(this.selectedAnimation.getWidth(), this.selectedAnimation.getHeight());
//            int[] size = ratio.getAspectRatioSizeByMaximumSize(aniW, aniH);
//            aniW = size[0];
//            aniH = size[1];
//            int aniX = this.width - 20 - aniW;
//            int aniY = 50 + 15;
//            boolean cachedLooped = this.selectedAnimation.isGettingLooped();
//            fill(pose, aniX, aniY, aniX + aniW, aniY + aniH, UIBase.getUIColorScheme().areaBackgroundColor.getColorInt());
//            this.selectedAnimation.setLooped(false);
//            this.selectedAnimation.setPosX(aniX);
//            this.selectedAnimation.setPosY(aniY);
//            this.selectedAnimation.setWidth(aniW);
//            this.selectedAnimation.setHeight(aniH);
//            this.selectedAnimation.render(pose);
//            this.selectedAnimation.setLooped(cachedLooped);
//            UIBase.renderBorder(pose, aniX, aniY, aniX + aniW, aniY + aniH, UIBase.ELEMENT_BORDER_THICKNESS, UIBase.getUIColorScheme().elementBorderColorNormal.getColor(), true, true, true, true);
//        }

        this.sortingButton.setWidth(this.font.width(this.sortingButton.getMessage()) + 10);
        this.sortingButton.setX(this.layoutListScrollArea.getXWithBorder() + this.layoutListScrollArea.getWidthWithBorder() - this.sortingButton.getWidth());
        this.sortingButton.setY(this.layoutListScrollArea.getYWithBorder() - 5 - this.sortingButton.getHeight());
        this.sortingButton.render(pose, mouseX, mouseY, partial);

        this.doneButton.setX(this.width - 20 - this.doneButton.getWidth());
        this.doneButton.setY(this.height - 20 - 20);
        this.doneButton.render(pose, mouseX, mouseY, partial);

        this.openInTextEditorButton.setX(this.width - 20 - this.openInTextEditorButton.getWidth());
        this.openInTextEditorButton.setY(this.doneButton.getY() - 15 - 20);
        this.openInTextEditorButton.render(pose, mouseX, mouseY, partial);

        this.deleteButton.setX(this.width - 20 - this.deleteButton.getWidth());
        this.deleteButton.setY(this.openInTextEditorButton.getY() - 5 - 20);
        this.deleteButton.render(pose, mouseX, mouseY, partial);

        this.editButton.setX(this.width - 20 - this.editButton.getWidth());
        this.editButton.setY(this.deleteButton.getY() - 5 - 20);
        this.editButton.render(pose, mouseX, mouseY, partial);

        this.toggleStatusButton.setX(this.width - 20 - this.toggleStatusButton.getWidth());
        this.toggleStatusButton.setY(this.editButton.getY() - 5 - 20);
        this.toggleStatusButton.render(pose, mouseX, mouseY, partial);

        super.render(pose, mouseX, mouseY, partial);

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
            this.layoutListScrollArea.addEntry(new TextScrollAreaEntry(this.layoutListScrollArea, Component.translatable("fancymenu.layout.manage.no_layouts_found").setStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().errorTextColor.getColorInt())), (entry) -> {}));
        }
    }

    public static class LayoutScrollEntry extends TextListScrollAreaEntry {

        public Layout layout;

        public LayoutScrollEntry(ScrollArea parent, @NotNull Layout layout, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, Component.literal(""), UIBase.getUIColorScheme().listingDotColor1.getColor(), onClick);
            this.layout = layout;
            this.updateName();
        }

        protected void updateName() {
            Style style = this.layout.getStatus().getEntryComponentStyle();
            MutableComponent c = Component.literal(layout.getLayoutName()).setStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().descriptionAreaTextColor.getColorInt()));
            c.append(Component.literal(" (").setStyle(style));
            c.append(this.layout.getStatus().getEntryComponent());
            c.append(Component.literal(")").setStyle(style));
            this.setText(c);
        }

    }

    protected enum Sorting implements LocalizedCycleEnum {

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

    }

}
