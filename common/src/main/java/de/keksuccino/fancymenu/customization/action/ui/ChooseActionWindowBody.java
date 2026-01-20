package de.keksuccino.fancymenu.customization.action.ui;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionRegistry;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.InitialWidgetFocusScreen;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ChooseActionWindowBody extends PiPWindowBody implements InitialWidgetFocusScreen {

    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 420;

    protected final ActionInstance instance;
    protected Consumer<ActionInstance> callback;
    protected Action originalAction = null;
    protected String originalActionValue = null;

    protected ScrollArea actionsListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ScrollArea descriptionScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedEditBox searchBar;

    public boolean isEdit;

    public ChooseActionWindowBody(@Nullable ActionInstance instanceToEdit, @NotNull Consumer<ActionInstance> callback) {

        super((instanceToEdit != null) ? Component.translatable("fancymenu.actions.screens.edit_action") : Component.translatable("fancymenu.actions.screens.add_action"));

        this.isEdit = (instanceToEdit != null);

        if (this.isEdit) {
            this.originalAction = instanceToEdit.action;
            this.originalActionValue = instanceToEdit.value;
        }
        this.instance = this.isEdit ? instanceToEdit : new ActionInstance(Action.EMPTY, null);
        this.callback = callback;

    }

    @Override
    protected void init() {

        String oldSearchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        this.searchBar = new ExtendedEditBox(Minecraft.getInstance().font, 20 + 1, 50 + 15 + 1, (this.width / 2) - 40 - 2, 20 - 2, Component.empty());
        this.searchBar.setHintFancyMenu(consumes -> Component.translatable("fancymenu.actions.build_action.screen.search_action"));
        this.searchBar.setValue(oldSearchValue);
        this.searchBar.setResponder(s -> this.updateActionsList());
        this.addRenderableWidget(this.searchBar);
        UIBase.applyDefaultWidgetSkinTo(this.searchBar);
        this.setupInitialFocusWidget(this, this.searchBar);

        // Set positions for scroll areas
        this.actionsListScrollArea.setWidth(((float) this.width / 2) - 40, true);
        this.actionsListScrollArea.setHeight(this.height - 85 - 25, true);
        this.actionsListScrollArea.setX(20, true);
        this.actionsListScrollArea.setY(50 + 15 + 25, true);
        this.addRenderableWidget(this.actionsListScrollArea);

        this.descriptionScrollArea.setWidth(((float) this.width / 2) - 40, true);
        this.descriptionScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
        this.descriptionScrollArea.setX(this.width - 20 - this.descriptionScrollArea.getWidthWithBorder(), true);
        this.descriptionScrollArea.setY(50 + 15, true);
        this.descriptionScrollArea.horizontalScrollBar.active = false;
        this.addRenderableWidget(this.descriptionScrollArea);

        // Calculate button positions
        int cancelButtonX = this.width - 20 - 150;
        int cancelButtonY = this.height - 20 - 20 - 5 - 20;
        int doneButtonX = this.width - 20 - 150;
        int doneButtonY = this.height - 20 - 20;

        // Create buttons with proper positions in constructors
        ExtendedButton doneOrNextButton = new ExtendedButton(doneButtonX, doneButtonY, 150, 20, Component.empty(), (button) -> {
            this.onNextStep();
        }).setLabelSupplier(consumes -> this.needsValueFirst() ? Component.translatable("fancymenu.ui.generic.next_step") : Component.translatable("fancymenu.common_components.done"));
        this.addRenderableWidget(doneOrNextButton);
        UIBase.applyDefaultWidgetSkinTo(doneOrNextButton);

        ExtendedButton cancelButton = new ExtendedButton(cancelButtonX, cancelButtonY, 150, 20, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
            this.callback.accept(null);
            this.closeWindow();
        });
        this.addRenderableWidget(cancelButton);
        UIBase.applyDefaultWidgetSkinTo(cancelButton);

        this.updateActionsList();
        this.setDescription(this.instance.action);

    }

    protected void onEditValue() {
        if (this.instance.action == Action.EMPTY) return;
        this.originalAction = null;
        this.originalActionValue = null;
        this.setWindowVisible(false);
        this.instance.action.editValueInternal(this.instance, (instance1, oldValue, newValue) -> {
            this.closeWindow();
        }, instance1 -> {
            this.setWindowVisible(true);
        });
    }

    protected boolean hasValue() {
        return (this.instance.action != Action.EMPTY) && this.instance.action.hasValue();
    }

    protected void onDone() {
        this.callback.accept((this.instance.action != Action.EMPTY) ? this.instance : null);
        this.closeWindow();
    }

    protected boolean canClickDone() {
        if (this.instance.action == Action.EMPTY) return false;
        return (this.instance.value != null) || !this.instance.action.hasValue();
    }

    protected boolean needsValueFirst() {
        return this.hasValue() && !this.canClickDone();
    }

    protected void onNextStep() {
        if (this.hasValue()) {
            this.onEditValue();
        } else if (this.canClickDone()) {
            this.onDone();
        }
    }

    @Override
    public void onWindowClosedExternally() {
        this.callback.accept(null);
    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        graphics.fill(0, 0, this.width, this.height, UIBase.getUITheme().ui_interface_background_color.getColorInt());

        graphics.drawString(this.font, Component.translatable("fancymenu.actions.screens.build_screen.available_actions"), 20, 50, UIBase.getUITheme().ui_interface_generic_text_color.getColorInt(), false);

        Component descLabel = Component.translatable("fancymenu.actions.screens.build_screen.action_description");
        int descLabelWidth = this.font.width(descLabel);
        graphics.drawString(this.font, descLabel, this.width - 20 - descLabelWidth, 50, UIBase.getUITheme().ui_interface_generic_text_color.getColorInt(), false);

        this.performInitialWidgetFocusActionInRender();

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    private void restoreWindowAfterValueEdit() {
        PiPWindow window = this.getWindow();
        if (window != null && !window.isVisible()) {
            window.setVisible(true);
            PiPWindowHandler.INSTANCE.bringToFront(window);
        }
    }

    protected void setDescription(@Nullable Action action) {

        this.descriptionScrollArea.clearEntries();

        this.descriptionScrollArea.addEntry(new CellScreen.SpacerScrollAreaEntry(this.descriptionScrollArea, 5));

        if ((action != null) && (action.getActionDescription() != null)) {
            for (Component c : action.getActionDescription()) {
                this.addDescriptionLine(c);
            }
        }

        this.descriptionScrollArea.addEntry(new CellScreen.SpacerScrollAreaEntry(this.descriptionScrollArea, 5));

    }

    protected void addDescriptionLine(@NotNull Component line) {
        List<Component> lines = new ArrayList<>();
        int maxWidth = (int)(this.descriptionScrollArea.getInnerWidth() - 15F);
        if (this.font.width(line) > maxWidth) {
            this.font.getSplitter().splitLines(line, maxWidth, Style.EMPTY).forEach(formatted -> {
                lines.add(TextFormattingUtils.convertFormattedTextToComponent(formatted));
            });
        } else {
            lines.add(line);
        }
        lines.forEach(component -> {
            TextScrollAreaEntry e = new TextScrollAreaEntry(this.descriptionScrollArea, component, (entry) -> {});
            e.setSelectable(false);
            e.setBackgroundColorHover(e.getBackgroundColorNormal());
            e.setPlayClickSound(false);
            e.setTextBaseColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt());
            this.descriptionScrollArea.addEntry(e);
        });
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
        List<Action> actions = ActionRegistry.getActions();
        actions.sort(Comparator
                .comparing((Action action) -> action.getActionDisplayName().getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(action -> action.getActionDisplayName().getString())
                .thenComparing(Action::getIdentifier));
        for (Action action : actions) {
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

    public class ActionScrollEntry extends TextListScrollAreaEntry {

        public Action action;
        protected long lastClickTime = 0;
        protected static final long DOUBLE_CLICK_TIME = 500; // milliseconds

        public ActionScrollEntry(ScrollArea parent, @NotNull Action action, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, buildLabel(action), UIBase.getUITheme().bullet_list_dot_color_1, onClick);
            this.action = action;
        }

        @NotNull
        private static Component buildLabel(@NotNull Action action) {
            MutableComponent c = action.getActionDisplayName().copy().setStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt()));
            if (action.isDeprecated()) {
                c = c.withStyle(Style.EMPTY.withStrikethrough(true));
                c = c.append(Component.literal(" ").setStyle(Style.EMPTY.withStrikethrough(false)));
                c = c.append(Component.translatable("fancymenu.actions.deprecated").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_text_color.getColorInt()).withStrikethrough(false)));
            }
            return c;
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            long currentTime = System.currentTimeMillis();
            
            // Check if this is a double-click
            if (currentTime - this.lastClickTime < DOUBLE_CLICK_TIME) {
                // Double-click detected
                if (ChooseActionWindowBody.this.instance.action == this.action) {
                    ChooseActionWindowBody.this.onNextStep();
                    this.lastClickTime = 0; // Reset to prevent triple clicks
                    return;
                }
            }
            
            this.lastClickTime = currentTime;
            
            // Normal single click behavior
            super.onClick(entry, mouseX, mouseY, button);
        }

    }

}
