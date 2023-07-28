package de.keksuccino.fancymenu.customization.layout.editor.actions;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.action.ExecutableAction;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.ExtendedButton;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ManageActionsScreen extends Screen {

    protected List<ExecutableAction> actions = new ArrayList<>();
    protected Consumer<List<ExecutableAction>> callback;

    protected ScrollArea actionsScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedButton addActionButton;
    protected ExtendedButton moveUpButton;
    protected ExtendedButton moveDownButton;
    protected ExtendedButton editButton;
    protected ExtendedButton removeButton;
    protected ExtendedButton doneButton;
    protected ExtendedButton cancelButton;

    public ManageActionsScreen(@NotNull List<ExecutableAction> actions, @NotNull Consumer<List<ExecutableAction>> callback) {

        super(Component.translatable("fancymenu.editor.action.screens.manage_screen.manage"));

        for (ExecutableAction a : actions) {
            this.actions.add(a.copy());
        }
        this.callback = callback;
        this.updateActionInstanceScrollArea(false);

    }

    @Override
    protected void init() {

        //Reset the GUI scale in case the layout editor changed it
        Minecraft.getInstance().getWindow().setGuiScale(Minecraft.getInstance().getWindow().calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode()));
        this.height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        this.width = Minecraft.getInstance().getWindow().getGuiScaledWidth();

        super.init();

        this.addActionButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.editor.action.screens.add_action"), (button) -> {
            BuildActionScreen s = new BuildActionScreen(null, (call) -> {
                if (call != null) {
                    this.actions.add(call);
                    this.updateActionInstanceScrollArea(false);
                }
                Minecraft.getInstance().setScreen(this);
            });
            Minecraft.getInstance().setScreen(s);
        });
        this.addWidget(this.addActionButton);
        this.addActionButton.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.add_action.desc")));
        UIBase.applyDefaultWidgetSkinTo(this.addActionButton);

        this.moveUpButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.editor.action.screens.move_action_up"), (button) -> {
            if (this.isInstanceSelected()) {
                ExecutableAction selected = this.getSelectedInstance();
                int index = this.actions.indexOf(selected);
                if (index > 0) {
                    this.actions.remove(selected);
                    this.actions.add(index-1, selected);
                    this.updateActionInstanceScrollArea(true);
                    for (ScrollAreaEntry e : this.actionsScrollArea.getEntries()) {
                        if ((e instanceof ExecutableActionEntry) && (((ExecutableActionEntry)e).action == selected)) {
                            e.setSelected(true);
                            break;
                        }
                    }
                }
            }
        }) {
            @Override
            public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                ManageActionsScreen s = ManageActionsScreen.this;
                if (!s.isInstanceSelected()) {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.finish.no_action_selected")));
                    this.active = false;
                } else {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.move_action_up.desc")));
                    this.active = true;
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        this.addWidget(this.moveUpButton);
        UIBase.applyDefaultWidgetSkinTo(this.moveUpButton);

        this.moveDownButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.editor.action.screens.move_action_down"), (button) -> {
            if (this.isInstanceSelected()) {
                ExecutableAction selected = this.getSelectedInstance();
                int index = this.actions.indexOf(selected);
                if ((index >= 0) && (index <= this.actions.size()-2)) {
                    this.actions.remove(selected);
                    this.actions.add(index+1, selected);
                    this.updateActionInstanceScrollArea(true);
                    for (ScrollAreaEntry e : this.actionsScrollArea.getEntries()) {
                        if ((e instanceof ExecutableActionEntry) && (((ExecutableActionEntry)e).action == selected)) {
                            e.setSelected(true);
                            break;
                        }
                    }
                }
            }
        }) {
            @Override
            public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                ManageActionsScreen s = ManageActionsScreen.this;
                if (!s.isInstanceSelected()) {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.finish.no_action_selected")));
                    this.active = false;
                } else {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.move_action_down.desc")));
                    this.active = true;
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        this.addWidget(this.moveDownButton);
        UIBase.applyDefaultWidgetSkinTo(this.moveDownButton);

        this.editButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.editor.action.screens.edit_action"), (button) -> {
            ExecutableAction a = this.getSelectedInstance();
            if (a != null) {
                BuildActionScreen s = new BuildActionScreen(a.copy(), (call) -> {
                    if (call != null) {
                        int index = this.actions.indexOf(a);
                        this.actions.remove(a);
                        if (index != -1) {
                            this.actions.add(index, call);
                        } else {
                            this.actions.add(call);
                        }
                        this.updateActionInstanceScrollArea(false);
                    }
                    Minecraft.getInstance().setScreen(this);
                });
                Minecraft.getInstance().setScreen(s);
            }
        }) {
            @Override
            public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                ManageActionsScreen s = ManageActionsScreen.this;
                if (!s.isInstanceSelected()) {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.finish.no_action_selected")));
                    this.active = false;
                } else {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.edit_action.desc")));
                    this.active = true;
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        this.addWidget(this.editButton);
        UIBase.applyDefaultWidgetSkinTo(this.editButton);

        this.removeButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.editor.action.screens.remove_action"), (button) -> {
            if (this.isInstanceSelected()) {
                ExecutableAction i = this.getSelectedInstance();
                Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings((call) -> {
                    if (call) {
                        this.actions.remove(i);
                        this.updateActionInstanceScrollArea(false);
                    }
                    Minecraft.getInstance().setScreen(this);
                }, LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.remove_action.confirm")));
            }
        }) {
            @Override
            public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                ManageActionsScreen s = ManageActionsScreen.this;
                if (!s.isInstanceSelected()) {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.finish.no_action_selected")));
                    this.active = false;
                } else {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.remove_action.desc")));
                    this.active = true;
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        this.addWidget(this.removeButton);
        UIBase.applyDefaultWidgetSkinTo(this.removeButton);

        this.doneButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.guicomponents.done"), (button) -> {
            this.callback.accept(this.actions);
        });
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, Component.translatable("fancymenu.guicomponents.cancel"), (button) -> {
            this.callback.accept(null);
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

    }

    @Override
    public void onClose() {
        this.callback.accept(null);
    }

    @Override
    public void render(@NotNull PoseStack matrix, int mouseX, int mouseY, float partial) {

        fill(matrix, 0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        this.font.draw(matrix, titleComp, 20, 20, UIBase.getUIColorTheme().generic_text_base_color.getColorInt());

        this.font.draw(matrix, I18n.get("fancymenu.editor.action.screens.manage_screen.actions"), 20, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt());

        this.actionsScrollArea.setWidth(this.width - 20 - 150 - 20 - 20, true);
        this.actionsScrollArea.setHeight(this.height - 85, true);
        this.actionsScrollArea.setX(20, true);
        this.actionsScrollArea.setY(50 + 15, true);
        this.actionsScrollArea.render(matrix, mouseX, mouseY, partial);

        this.doneButton.setX(this.width - 20 - this.doneButton.getWidth());
        this.doneButton.setY(this.height - 20 - 20);
        this.doneButton.render(matrix, mouseX, mouseY, partial);

        this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
        this.cancelButton.setY(this.doneButton.getY() - 5 - 20);
        this.cancelButton.render(matrix, mouseX, mouseY, partial);

        this.removeButton.setX(this.width - 20 - this.removeButton.getWidth());
        this.removeButton.setY(this.cancelButton.getY() - 15 - 20);
        this.removeButton.render(matrix, mouseX, mouseY, partial);

        this.editButton.setX(this.width - 20 - this.editButton.getWidth());
        this.editButton.setY(this.removeButton.getY() - 5 - 20);
        this.editButton.render(matrix, mouseX, mouseY, partial);

        this.moveDownButton.setX(this.width - 20 - this.moveDownButton.getWidth());
        this.moveDownButton.setY(this.editButton.getY() - 5 - 20);
        this.moveDownButton.render(matrix, mouseX, mouseY, partial);

        this.moveUpButton.setX(this.width - 20 - this.moveUpButton.getWidth());
        this.moveUpButton.setY(this.moveDownButton.getY() - 5 - 20);
        this.moveUpButton.render(matrix, mouseX, mouseY, partial);

        this.addActionButton.setX(this.width - 20 - this.addActionButton.getWidth());
        this.addActionButton.setY(this.moveUpButton.getY() - 5 - 20);
        this.addActionButton.render(matrix, mouseX, mouseY, partial);

        super.render(matrix, mouseX, mouseY, partial);

    }

    @Nullable
    protected ExecutableAction getSelectedInstance() {
        ScrollAreaEntry e = this.actionsScrollArea.getFocusedEntry();
        if (e instanceof ExecutableActionEntry) {
            return ((ExecutableActionEntry)e).action;
        }
        return null;
    }

    protected boolean isInstanceSelected() {
        return this.getSelectedInstance() != null;
    }

    protected void updateActionInstanceScrollArea(boolean keepScroll) {

        float oldScrollVertical = this.actionsScrollArea.verticalScrollBar.getScroll();
        float oldScrollHorizontal = this.actionsScrollArea.horizontalScrollBar.getScroll();

        this.actionsScrollArea.clearEntries();

        for (ExecutableAction i : this.actions) {
            ExecutableActionEntry e = new ExecutableActionEntry(this.actionsScrollArea, i, 14);
            this.actionsScrollArea.addEntry(e);
        }

        if (keepScroll) {
            this.actionsScrollArea.verticalScrollBar.setScroll(oldScrollVertical);
            this.actionsScrollArea.horizontalScrollBar.setScroll(oldScrollHorizontal);
        }

    }

    public static class ExecutableActionEntry extends ScrollAreaEntry {

        public static final int HEADER_FOOTER_HEIGHT = 3;

        public ExecutableAction action;
        public final int lineHeight;
        public Font font = Minecraft.getInstance().font;

        private final MutableComponent displayNameComponent;
        private final MutableComponent valueComponent;

        public ExecutableActionEntry(ScrollArea parent, ExecutableAction action, int lineHeight) {

            super(parent, 100, 30);
            this.action = action;
            this.lineHeight = lineHeight;

            this.displayNameComponent = this.action.action.getActionDisplayName().copy().setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt()));
            String valueString = ((this.action.value != null) && this.action.action.hasValue()) ? this.action.value : I18n.get("fancymenu.editor.action.screens.manage_screen.info.value.none");
            this.valueComponent = Component.literal(I18n.get("fancymenu.editor.action.screens.manage_screen.info.value") + " ").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())).append(Component.literal(valueString).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt())));

            this.setWidth(this.calculateWidth());
            this.setHeight((lineHeight * 2) + (HEADER_FOOTER_HEIGHT * 2));

        }

        @Override
        public void render(PoseStack matrix, int mouseX, int mouseY, float partial) {

            super.render(matrix, mouseX, mouseY, partial);

            int centerYLine1 = this.getY() + HEADER_FOOTER_HEIGHT + (this.lineHeight / 2);
            int centerYLine2 = this.getY() + HEADER_FOOTER_HEIGHT + ((this.lineHeight / 2) * 3);

            RenderSystem.enableBlend();

            renderListingDot(matrix, this.getX() + 5, centerYLine1 - 2, UIBase.getUIColorTheme().listing_dot_color_2.getColor());
            this.font.draw(matrix, this.displayNameComponent, (float)(this.getX() + 5 + 4 + 3), (float)(centerYLine1 - (this.font.lineHeight / 2)), -1);

            renderListingDot(matrix, this.getX() + 5 + 4 + 3, centerYLine2 - 2, UIBase.getUIColorTheme().listing_dot_color_1.getColor());
            this.font.draw(matrix, this.valueComponent, (float)(this.getX() + 5 + 4 + 3 + 4 + 3), (float)(centerYLine2 - (this.font.lineHeight / 2)), -1);

        }

        private int calculateWidth() {
            int w = 5 + 4 + 3 + this.font.width(this.displayNameComponent) + 5;
            int w2 = 5 + 4 + 3 + 4 + 3 + this.font.width(this.valueComponent) + 5;
            if (w2 > w) {
                w = w2;
            }
            return w;
        }

        @Override
        public void onClick(ScrollAreaEntry entry) {}

    }

}
