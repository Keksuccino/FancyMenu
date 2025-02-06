package de.keksuccino.fancymenu.customization.layout.editor.actions;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionRegistry;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.gui.ModernScreen;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class BuildActionScreen extends ModernScreen {

    protected final ActionInstance instance;
    protected Consumer<ActionInstance> callback;
    protected Action originalAction = null;
    protected String originalActionValue = null;

    protected ScrollArea actionsListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ScrollArea actionDescriptionScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedButton editValueButton;
    protected ExtendedButton doneButton;
    protected ExtendedButton cancelButton;

    public BuildActionScreen(@Nullable ActionInstance instanceToEdit, @NotNull Consumer<ActionInstance> callback) {

        super((instanceToEdit != null) ? Components.translatable("fancymenu.editor.action.screens.edit_action") : Components.translatable("fancymenu.editor.action.screens.add_action"));

        if (instanceToEdit != null) {
            this.originalAction = instanceToEdit.action;
            this.originalActionValue = instanceToEdit.value;
        }
        this.instance = (instanceToEdit != null) ? instanceToEdit : new ActionInstance(Action.EMPTY, null);
        this.callback = callback;
        this.setContentOfActionsList();

        //Select correct entry
        if (this.instance.action != Action.EMPTY) {
            for (ScrollAreaEntry e : this.actionsListScrollArea.getEntries()) {
                if ((e instanceof ActionScrollEntry) && (((ActionScrollEntry)e).action == this.instance.action)) {
                    e.setSelected(true);
                    break;
                }
            }
        }

    }

    @Override
    protected void init() {

        this.editValueButton = new ExtendedButton(0, 0, 150, 20, Components.translatable("fancymenu.editor.action.screens.build_screen.edit_value"), (button) -> {
            if (this.instance.action == Action.EMPTY) return;
            this.originalAction = null;
            this.originalActionValue = null;
            this.instance.action.editValue(this, this.instance);
        }) {
            @Override
            public void render(@NotNull PoseStack graphics, int mouseX, int mouseY, float partial) {
                Action b = BuildActionScreen.this.instance.action;
                if ((b != Action.EMPTY) && !b.hasValue()) {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.build_screen.edit_value.desc.no_value")));
                } else {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.build_screen.edit_value.desc.normal")));
                }
                this.active = (b != Action.EMPTY) && b.hasValue();
                super.render(graphics, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.editValueButton);
        UIBase.applyDefaultWidgetSkinTo(this.editValueButton);

        this.doneButton = new ExtendedButton(0, 0, 150, 20, Components.translatable("fancymenu.guicomponents.done"), (button) -> {
            this.callback.accept((this.instance.action != Action.EMPTY) ? this.instance : null);
        }) {
            @Override
            public void renderButton(@NotNull PoseStack graphics, int mouseX, int mouseY, float partial) {
                if (BuildActionScreen.this.instance.action == Action.EMPTY) {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.finish.no_action_selected")));
                    this.active = false;
                } else if ((BuildActionScreen.this.instance.value == null) && BuildActionScreen.this.instance.action.hasValue()) {
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.action.screens.build_screen.finish.no_value_set")));
                    this.active = false;
                } else {
                    this.setTooltip((Tooltip)null);
                    this.active = true;
                }
                super.renderButton(graphics, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, Components.translatable("fancymenu.guicomponents.cancel"), (button) -> {
            this.callback.accept(null);
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

        this.setDescription(this.instance.action);

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

        graphics.drawString(this.font, I18n.get("fancymenu.editor.action.screens.build_screen.available_actions"), 20, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.actionsListScrollArea.setWidth((this.width / 2) - 40, true);
        this.actionsListScrollArea.setHeight(this.height - 85, true);
        this.actionsListScrollArea.setX(20, true);
        this.actionsListScrollArea.setY(50 + 15, true);
        this.actionsListScrollArea.render(graphics, mouseX, mouseY, partial);

        String descLabelString = I18n.get("fancymenu.editor.action.screens.build_screen.action_description");
        int descLabelWidth = this.font.width(descLabelString);
        graphics.drawString(this.font, descLabelString, this.width - 20 - descLabelWidth, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.actionDescriptionScrollArea.setWidth((this.width / 2) - 40, true);
        this.actionDescriptionScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
        this.actionDescriptionScrollArea.setX(this.width - 20 - this.actionDescriptionScrollArea.getWidthWithBorder(), true);
        this.actionDescriptionScrollArea.setY(50 + 15, true);
        this.actionDescriptionScrollArea.render(graphics, mouseX, mouseY, partial);

        this.doneButton.x = (this.width - 20 - this.doneButton.getWidth());
        this.doneButton.y = (this.height - 20 - 20);
        this.doneButton.render(graphics.pose(), mouseX, mouseY, partial);

        this.cancelButton.x = (this.width - 20 - this.cancelButton.getWidth());
        this.cancelButton.y = (this.doneButton.y - 5 - 20);
        this.cancelButton.render(graphics.pose(), mouseX, mouseY, partial);

        this.editValueButton.x = (this.width - 20 - this.editValueButton.getWidth());
        this.editValueButton.y = (this.cancelButton.y - 15 - 20);
        this.editValueButton.render(graphics.pose(), mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);

    }

    protected void setDescription(@Nullable Action action) {
        this.actionDescriptionScrollArea.clearEntries();
        if ((action != null) && (action.getActionDescription() != null)) {
            for (Component c : action.getActionDescription()) {
                TextScrollAreaEntry e = new TextScrollAreaEntry(this.actionDescriptionScrollArea, c, (entry) -> {});
                e.setSelectable(false);
                e.setBackgroundColorHover(e.getBackgroundColorIdle());
                e.setPlayClickSound(false);
                this.actionDescriptionScrollArea.addEntry(e);
            }
        }
    }

    protected void setContentOfActionsList() {
        this.actionsListScrollArea.clearEntries();
        for (Action c : ActionRegistry.getActions()) {
            if ((LayoutEditorScreen.getCurrentInstance() != null) && !c.shouldShowUpInEditorActionMenu(LayoutEditorScreen.getCurrentInstance())) continue;
            ActionScrollEntry e = new ActionScrollEntry(this.actionsListScrollArea, c, (entry) -> {
                this.instance.action = c;
                if (this.originalAction == c) {
                    this.instance.value = this.originalActionValue;
                } else {
                    this.instance.value = null;
                }
                this.setDescription(c);
            });
            this.actionsListScrollArea.addEntry(e);
        }
    }

    public static class ActionScrollEntry extends TextListScrollAreaEntry {

        public Action action;

        public ActionScrollEntry(ScrollArea parent, @NotNull Action action, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, buildLabel(action), UIBase.getUIColorTheme().listing_dot_color_1.getColor(), onClick);
            this.action = action;
        }

        @NotNull
        private static Component buildLabel(@NotNull Action action) {
            MutableComponent c = action.getActionDisplayName().copy().setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt()));
            if (action.isDeprecated()) {
                c = c.withStyle(Style.EMPTY.withStrikethrough(true));
                c = c.append(Components.literal(" ").setStyle(Style.EMPTY.withStrikethrough(false)));
                c = c.append(Components.translatable("fancymenu.editor.actions.deprecated").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt()).withStrikethrough(false)));
            }
            return c;
        }

    }

}
