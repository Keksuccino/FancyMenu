package de.keksuccino.fancymenu.customization.layout.editor.actions;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionRegistry;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ScreenUtils;
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
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

public class BuildActionScreen extends ModernScreen {

    protected final ActionInstance instance;
    protected Consumer<ActionInstance> callback;
    protected Action originalAction = null;
    protected String originalActionValue = null;

    protected ScrollArea actionsListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ScrollArea actionDescriptionScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedEditBox searchBar;

    public BuildActionScreen(@Nullable ActionInstance instanceToEdit, @NotNull Consumer<ActionInstance> callback) {

        super((instanceToEdit != null) ? Components.translatable("fancymenu.editor.action.screens.edit_action") : Components.translatable("fancymenu.editor.action.screens.add_action"));

        if (instanceToEdit != null) {
            this.originalAction = instanceToEdit.action;
            this.originalActionValue = instanceToEdit.value;
        }
        this.instance = (instanceToEdit != null) ? instanceToEdit : new ActionInstance(Action.EMPTY, null);
        this.callback = callback;

    }

    @Override
    protected void init() {

        String oldSearchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        this.searchBar = new ExtendedEditBox(Minecraft.getInstance().font, 20 + 1, 50 + 15 + 1, (this.width / 2) - 40 - 2, 20 - 2, Components.empty()) {
            @Override
            public void renderButton(@NotNull PoseStack graphics, int mouseX, int mouseY, float partial) {
                super.renderButton(graphics, mouseX, mouseY, partial);
                if (this.getValue().isBlank() && !this.isFocused()) {
                    GuiGraphics.currentGraphics().drawString(this.font, Components.translatable("fancymenu.actions.build_action.screen.search_action"), this.getX() + 4, this.getY() + (this.getHeight() / 2) - (this.font.lineHeight / 2), UIBase.getUIColorTheme().edit_box_text_color_uneditable.getColorInt(), false);
                }
            }
        };
        this.searchBar.setValue(oldSearchValue);
        this.searchBar.setResponder(s -> this.updateActionsList());
        this.addRenderableWidget(this.searchBar);
        UIBase.applyDefaultWidgetSkinTo(this.searchBar);

        // Set positions for scroll areas
        this.actionsListScrollArea.setWidth((this.width / 2) - 40, true);
        this.actionsListScrollArea.setHeight(this.height - 85 - 25, true);
        this.actionsListScrollArea.setX(20, true);
        this.actionsListScrollArea.setY(50 + 15 + 25, true);

        this.actionDescriptionScrollArea.setWidth((this.width / 2) - 40, true);
        this.actionDescriptionScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
        this.actionDescriptionScrollArea.setX(this.width - 20 - this.actionDescriptionScrollArea.getWidthWithBorder(), true);
        this.actionDescriptionScrollArea.setY(50 + 15, true);

        // Calculate button positions
        int editValueButtonX = this.width - 20 - 150; // 150 is the button width
        int editValueButtonY = this.height - 20 - 20 - 5 - 20 - 15 - 20;
        int cancelButtonX = this.width - 20 - 150;
        int cancelButtonY = this.height - 20 - 20 - 5 - 20;
        int doneButtonX = this.width - 20 - 150;
        int doneButtonY = this.height - 20 - 20;

        // Create buttons with proper positions in constructors
        ExtendedButton editValueButton = new ExtendedButton(editValueButtonX, editValueButtonY, 150, 20, Components.translatable("fancymenu.editor.action.screens.build_screen.edit_value"), (button) -> {
            if (this.instance.action == Action.EMPTY) return;
            this.originalAction = null;
            this.originalActionValue = null;
            this.instance.action.editValue(this, this.instance);
        }).setIsActiveSupplier(consumes -> (this.instance.action != Action.EMPTY) && this.instance.action.hasValue())
                .setTooltipSupplier(consumes -> {
                    if ((this.instance.action != Action.EMPTY) && !this.instance.action.hasValue()) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.action.screens.build_screen.edit_value.desc.no_value"));
                    }
                    return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.action.screens.build_screen.edit_value.desc.normal"));
                });
        this.addRenderableWidget(editValueButton);
        UIBase.applyDefaultWidgetSkinTo(editValueButton);

        ExtendedButton doneButton = new ExtendedButton(doneButtonX, doneButtonY, 150, 20, Components.translatable("fancymenu.guicomponents.done"), (button) -> {
            this.callback.accept((this.instance.action != Action.EMPTY) ? this.instance : null);
        }).setIsActiveSupplier(consumes -> {
                    if (this.instance.action == Action.EMPTY) return false;
                    return (this.instance.value != null) || !this.instance.action.hasValue();
                })
                .setTooltipSupplier(consumes -> {
                    if (this.instance.action == Action.EMPTY) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.action.screens.finish.no_action_selected"));
                    } else if ((this.instance.value == null) && this.instance.action.hasValue()) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.action.screens.build_screen.finish.no_value_set"));
                    }
                    return null;
                });
        this.addRenderableWidget(doneButton);
        UIBase.applyDefaultWidgetSkinTo(doneButton);

        ExtendedButton cancelButton = new ExtendedButton(cancelButtonX, cancelButtonY, 150, 20, Components.translatable("fancymenu.guicomponents.cancel"), (button) -> {
            this.callback.accept(null);
        });
        this.addRenderableWidget(cancelButton);
        UIBase.applyDefaultWidgetSkinTo(cancelButton);

        this.updateActionsList();

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

        graphics.drawString(this.font, Components.translatable("fancymenu.editor.action.screens.build_screen.available_actions"), 20, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        Component descLabel = Components.translatable("fancymenu.editor.action.screens.build_screen.action_description");
        int descLabelWidth = this.font.width(descLabel);
        graphics.drawString(this.font, descLabel, this.width - 20 - descLabelWidth, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.actionsListScrollArea.render(graphics, mouseX, mouseY, partial);
        this.actionDescriptionScrollArea.render(graphics, mouseX, mouseY, partial);

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

    protected boolean actionFitsSearchValue(@NotNull Action action, @Nullable String s) {
        if ((s == null) || s.isBlank()) return true;
        s = s.toLowerCase();
        if (action.getActionDisplayName().getString().toLowerCase().contains(s)) return true;
        return this.actionDescriptionContains(action, s);
    }

    protected boolean actionDescriptionContains(@NotNull Action action, @NotNull String s) {
        Component[] desc = Objects.requireNonNullElse(action.getActionDescription(), new Component[0]);
        for (Component c : desc) {
            if (c.getString().toLowerCase().contains(s)) return true;
        }
        return false;
    }

    protected void setContentOfActionsList() {

        String searchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        if (searchValue.isBlank()) searchValue = null;

        this.actionsListScrollArea.clearEntries();
        for (Action action : ActionRegistry.getActions()) {
            if ((LayoutEditorScreen.getCurrentInstance() != null) && !action.shouldShowUpInEditorActionMenu(LayoutEditorScreen.getCurrentInstance())) continue;
            if (!this.actionFitsSearchValue(action, searchValue)) continue;
            ActionScrollEntry e = new ActionScrollEntry(this.actionsListScrollArea, action, (entry) -> {
                this.instance.action = action;
                if (this.originalAction == action) {
                    this.instance.value = this.originalActionValue;
                } else {
                    this.instance.value = null;
                }
                this.setDescription(action);
            });
            this.actionsListScrollArea.addEntry(e);
        }

    }

    protected void updateActionsList() {

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