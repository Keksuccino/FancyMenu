package de.keksuccino.fancymenu.customization.requirement.ui;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.RequirementRegistry;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.InitialWidgetFocusScreen;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class BuildRequirementWindowBody extends PiPWindowBody implements InitialWidgetFocusScreen {

    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 420;

    protected RequirementContainer parent;
    protected final RequirementInstance instance;
    @Nullable
    protected final RequirementInstance editTargetInstance;
    protected boolean isEdit;
    protected Consumer<RequirementInstance> callback;

    protected ScrollArea requirementsListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ScrollArea descriptionScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedEditBox searchBar;

    private static final Comparator<Requirement> REQUIREMENT_DISPLAY_NAME_COMPARATOR = Comparator
            .comparing((Requirement requirement) -> requirement.getDisplayName().getString(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(requirement -> requirement.getDisplayName().getString())
            .thenComparing(Requirement::getIdentifier);

    public BuildRequirementWindowBody(@NotNull RequirementContainer parent, @Nullable RequirementInstance instanceToEdit, @NotNull Consumer<RequirementInstance> callback) {

        super((instanceToEdit != null) ? Component.translatable("fancymenu.requirements.screens.edit_requirement") : Component.translatable("fancymenu.requirements.screens.add_requirement"));

        this.parent = parent;
        this.isEdit = instanceToEdit != null;
        this.editTargetInstance = instanceToEdit;
        if (instanceToEdit != null) {
            this.instance = instanceToEdit.copy(false);
            this.instance.parent = parent;
            this.instance.group = instanceToEdit.group;
        } else {
            this.instance = new RequirementInstance(null, null, RequirementInstance.RequirementMode.IF, parent);
        }
        this.callback = callback;

    }

    @SuppressWarnings("all")
    @Override
    protected void init() {
        boolean blur = UIBase.shouldBlur();

        String oldSearchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        this.searchBar = new ExtendedEditBox(Minecraft.getInstance().font, 20 + 1, 50 + 15 + 1, (this.width / 2) - 40 - 2, 20 - 2, Component.empty());
        this.searchBar.setHintFancyMenu(consumes -> Component.translatable("fancymenu.requirements.build_requirement.screen.search_requirement"));
        this.searchBar.setValue(oldSearchValue);
        this.searchBar.setResponder(s -> this.updateRequirementsList());
        this.addRenderableWidget(this.searchBar);
        UIBase.applyDefaultWidgetSkinTo(this.searchBar, blur);
        this.setupInitialFocusWidget(this, this.searchBar);

        // Set positions for scroll areas
        this.requirementsListScrollArea.setSetupForBlurInterface(blur);
        this.requirementsListScrollArea.setWidth((this.width / 2) - 40, true);
        this.requirementsListScrollArea.setHeight(this.height - 85 - 25, true);
        this.requirementsListScrollArea.setX(20, true);
        this.requirementsListScrollArea.setY(50 + 15 + 25, true);
        this.addRenderableWidget(this.requirementsListScrollArea);

        this.descriptionScrollArea.setSetupForBlurInterface(blur);
        this.descriptionScrollArea.setWidth((this.width / 2) - 40, true);
        this.descriptionScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
        this.descriptionScrollArea.setX(this.width - 20 - this.descriptionScrollArea.getWidthWithBorder(), true);
        this.descriptionScrollArea.setY(50 + 15, true);
        this.descriptionScrollArea.horizontalScrollBar.active = false;
        this.addRenderableWidget(this.descriptionScrollArea);

        // Calculate button positions
        int doneButtonX = this.width - 20 - 150; // 150 is button width
        int doneButtonY = this.height - 20 - 20;
        int cancelButtonX = doneButtonX;
        int cancelButtonY = doneButtonY - 5 - 20;
        int requirementModeButtonX = doneButtonX;
        int requirementModeButtonY = cancelButtonY - 15 - 20;

        // Create buttons with positions in constructors
        ExtendedButton doneOrNextButton = new ExtendedButton(doneButtonX, doneButtonY, 150, 20, Component.empty(), (button) -> {
            this.onNextStep();
        }).setLabelSupplier(consumes -> this.needsValueFirst()
                ? Component.translatable("fancymenu.ui.generic.next_step")
                : Component.translatable("fancymenu.common_components.done"));
        this.addRenderableWidget(doneOrNextButton);
        UIBase.applyDefaultWidgetSkinTo(doneOrNextButton, blur);

        ExtendedButton cancelButton = new ExtendedButton(cancelButtonX, cancelButtonY, 150, 20, Component.translatable("fancymenu.common_components.cancel"), (button) -> {
            this.onCancelOrEscape();
        });
        this.addRenderableWidget(cancelButton);
        UIBase.applyDefaultWidgetSkinTo(cancelButton, blur);

        ExtendedButton requirementModeButton = new ExtendedButton(requirementModeButtonX, requirementModeButtonY, 150, 20, Component.empty(), (button) -> {
            if (this.instance.mode == RequirementInstance.RequirementMode.IF) {
                this.instance.mode = RequirementInstance.RequirementMode.IF_NOT;
            } else {
                this.instance.mode = RequirementInstance.RequirementMode.IF;
            }
        }).setUITooltipSupplier(consumes -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.requirements.screens.build_screen.requirement_mode.desc")))
                .setLabelSupplier(consumes -> {
                    if (this.instance.mode == RequirementInstance.RequirementMode.IF) {
                        return Component.translatable("fancymenu.requirements.screens.build_screen.requirement_mode.normal");
                    }
                    return Component.translatable("fancymenu.requirements.screens.build_screen.requirement_mode.opposite");
                });
        this.addRenderableWidget(requirementModeButton);
        UIBase.applyDefaultWidgetSkinTo(requirementModeButton, blur);

        this.updateRequirementsList();
        this.setDescription(this.instance.requirement);

    }

    @Override
    public void onWindowClosedExternally() {
        if (this.isEdit) {
            this.callback.accept(this.applyCurrentEditResultToTarget());
        } else {
            this.callback.accept(null);
        }
    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        int textColor = this.getGenericTextColor();
        float labelHeight = UIBase.getUITextHeightNormal();
        int availableAreaTop = (this.searchBar != null) ? this.searchBar.getY() : (50 + 15 + 1);
        float availableLabelY = availableAreaTop - labelHeight - UIBase.getAreaLabelVerticalPadding();
        UIBase.renderText(graphics, Component.translatable("fancymenu.requirements.screens.build_screen.available_requirements"), 20, availableLabelY, textColor);

        Component descLabel = Component.translatable("fancymenu.requirements.screens.build_screen.requirement_description");
        float descLabelWidth = UIBase.getUITextWidthNormal(descLabel);
        float descAreaTop = (this.descriptionScrollArea != null) ? this.descriptionScrollArea.getYWithBorder() : (50 + 15);
        float descLabelY = descAreaTop - labelHeight - UIBase.getAreaLabelVerticalPadding();
        UIBase.renderText(graphics, descLabel, this.width - 20 - descLabelWidth, descLabelY, textColor);

        this.performInitialWidgetFocusActionInRender();

    }

public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.isEdit && (keyCode == InputConstants.KEY_ESCAPE)) {
            this.onCancelOrEscape();
            return true;
        }

        if (keyCode == InputConstants.KEY_BACKSPACE) {
            if (this.searchBar != null) {
                if (!this.searchBar.isFocused()) {
                    this.focusSearchBar();
                }
                this.searchBar.keyPressed(keyCode, scanCode, modifiers);
                return true;
            }
        }

        if (keyCode == InputConstants.KEY_TAB) {
            return true;
        }

        if ((keyCode == InputConstants.KEY_UP) || (keyCode == InputConstants.KEY_DOWN)) {
            this.selectAdjacentRequirementsEntry(keyCode == InputConstants.KEY_DOWN);
            return true;
        }

        if ((keyCode == InputConstants.KEY_LEFT) || (keyCode == InputConstants.KEY_RIGHT)) {
            return true;
        }

        if ((keyCode == InputConstants.KEY_ENTER) || (keyCode == InputConstants.KEY_NUMPADENTER) || (keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            if (this.activateSelectedRequirementsListEntry()) {
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.shouldRouteTypedCharacterToSearchBar(codePoint) && (this.searchBar != null) && !this.searchBar.isFocused()) {
            this.focusSearchBar();
            if (this.searchBar.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    protected void onEditValue() {
        if (this.instance.requirement == null) {
            return;
        }
        PiPWindow parentWindow = this.getWindow();
        this.setWindowVisible(false);
        this.instance.requirement.editValueInternal(this.instance, (instance1, oldValue, newValue) -> {
            if (this.canClickDone()) {
                this.onDone();
            } else {
                this.setWindowVisible(true);
            }
        }, instance1 -> {
            this.setWindowVisible(true);
        }, parentWindow);
    }

    protected boolean hasValue() {
        return (this.instance.requirement != null) && this.instance.requirement.hasValue();
    }

    protected void onDone() {
        if (this.isEdit) {
            this.callback.accept(this.applyCurrentEditResultToTarget());
        } else {
            this.callback.accept((this.instance.requirement != null) ? this.instance : null);
        }
        this.closeWindow();
    }

    protected boolean canClickDone() {
        if (this.instance.requirement == null) {
            return false;
        }
        return (this.instance.value != null) || !this.instance.requirement.hasValue();
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

    protected void setDescription(@Nullable Requirement requirement) {

        this.descriptionScrollArea.clearEntries();

        this.descriptionScrollArea.addEntry(new CellScreen.SpacerScrollAreaEntry(this.descriptionScrollArea, 5));

        if (requirement != null) {
            var desc = requirement.getDescription();
            if (desc != null) this.addDescriptionLine(desc);
        }

        this.descriptionScrollArea.addEntry(new CellScreen.SpacerScrollAreaEntry(this.descriptionScrollArea, 5));

    }

    protected void addDescriptionLine(@NotNull Component line) {
        float maxWidth = this.descriptionScrollArea.getInnerWidth() - 15F;
        List<MutableComponent> lines = UIBase.lineWrapUIComponentsNormal(line, maxWidth);
        lines.forEach(component -> {
            TextScrollAreaEntry e = new TextScrollAreaEntry(this.descriptionScrollArea, component, (entry) -> {});
            e.setSelectable(false);
            e.setBackgroundColorHover(e.getBackgroundColorNormal());
            e.setPlayClickSound(false);
            e.setTextBaseColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt());
            this.descriptionScrollArea.addEntry(e);
        });
    }

    protected boolean requirementFitsSearchValue(@NotNull Requirement requirement, @Nullable String s) {
        if ((s == null) || s.isBlank()) return true;
        s = s.toLowerCase();
        if (requirement.getDisplayName().getString().toLowerCase().contains(s)) return true;
        return this.requirementDescriptionContains(requirement, s);
    }

    protected boolean requirementDescriptionContains(@NotNull Requirement requirement, @NotNull String s) {
        var desc = Objects.requireNonNullElse(requirement.getDescription(), Component.empty());
        return desc.getString().toLowerCase().contains(s);
    }

    protected void setContentOfRequirementsList(@Nullable String category) {
        int labelColor = this.getLabelTextColor();

        String searchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        if (searchValue.isBlank()) searchValue = null;

        this.requirementsListScrollArea.clearEntries();

        if (searchValue != null) {
            List<Requirement> requirements = RequirementRegistry.getRequirements();
            requirements.sort(REQUIREMENT_DISPLAY_NAME_COMPARATOR);
            for (Requirement r : requirements) {
                if ((LayoutEditorScreen.getCurrentInstance() != null) && !r.shouldShowUpInEditorRequirementMenu(LayoutEditorScreen.getCurrentInstance())) continue;
                if (!this.requirementFitsSearchValue(r, searchValue)) continue;
                Component label = r.getDisplayName();
                RequirementScrollEntry e = new RequirementScrollEntry(this.requirementsListScrollArea, label, UIBase.getUITheme().bullet_list_dot_color_1, (entry) -> {
                    this.selectRequirement(r);
                });
                e.setTextBaseColor(labelColor);
                e.requirement = r;
                this.requirementsListScrollArea.addEntry(e);
            }
            return;
        }

        LinkedHashMap<String, List<Requirement>> categories = RequirementRegistry.getRequirementsOrderedByCategories();
        categories.values().forEach(list -> list.sort(REQUIREMENT_DISPLAY_NAME_COMPARATOR));
        List<Map.Entry<String, List<Requirement>>> sortedCategories = new ArrayList<>(categories.entrySet());
        sortedCategories.sort(Comparator
                .comparing((Map.Entry<String, List<Requirement>> entry) -> entry.getKey(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Map.Entry::getKey));

        if (category == null) {

            //Add category entries
            for (Map.Entry<String, List<Requirement>> m : sortedCategories) {
                Component label = Component.literal(m.getKey());
                TextListScrollAreaEntry e = new TextListScrollAreaEntry(this.requirementsListScrollArea, label, UIBase.getUITheme().bullet_list_dot_color_2, (entry) -> {
                    BuildRequirementWindowBody.this.setContentOfRequirementsList(m.getKey());
                    BuildRequirementWindowBody.this.instance.requirement = null;
                    BuildRequirementWindowBody.this.instance.value = null;
                    this.setDescription(null);
                });
                e.setTextBaseColor(labelColor);
                this.requirementsListScrollArea.addEntry(e);
            }

            //Add requirement entries without category
            List<Requirement> uncategorized = RequirementRegistry.getRequirementsWithoutCategory();
            uncategorized.sort(REQUIREMENT_DISPLAY_NAME_COMPARATOR);
            for (Requirement r : uncategorized) {
                if ((LayoutEditorScreen.getCurrentInstance() != null) && !r.shouldShowUpInEditorRequirementMenu(LayoutEditorScreen.getCurrentInstance())) continue;
                Component label = r.getDisplayName();
                RequirementScrollEntry e = new RequirementScrollEntry(this.requirementsListScrollArea, label, UIBase.getUITheme().bullet_list_dot_color_1, (entry) -> {
                    this.selectRequirement(r);
                });
                e.setTextBaseColor(labelColor);
                e.requirement = r;
                this.requirementsListScrollArea.addEntry(e);
            }

        } else {

            //Add "Back" button
            Component backLabel = Component.translatable("fancymenu.requirements.screens.lists.back");
            TextListScrollAreaEntry backEntry = new TextListScrollAreaEntry(this.requirementsListScrollArea, backLabel, UIBase.getUITheme().bullet_list_dot_color_2, (entry) -> {
                BuildRequirementWindowBody.this.setContentOfRequirementsList(null);
                BuildRequirementWindowBody.this.instance.requirement = null;
                BuildRequirementWindowBody.this.instance.value = null;
                this.setDescription(null);
            });
            backEntry.setTextBaseColor(UIBase.getUITheme().warning_color.getColorInt());
            this.requirementsListScrollArea.addEntry(backEntry);

            //Add requirement entries of given category
            List<Requirement> l = categories.get(category);
            if (l != null) {
                for (Requirement r : l) {
                    if ((LayoutEditorScreen.getCurrentInstance() != null) && !r.shouldShowUpInEditorRequirementMenu(LayoutEditorScreen.getCurrentInstance())) continue;
                    Component label = r.getDisplayName();
                    RequirementScrollEntry e = new RequirementScrollEntry(this.requirementsListScrollArea, label, UIBase.getUITheme().bullet_list_dot_color_1, (entry) -> {
                        this.selectRequirement(r);
                    });
                    e.setTextBaseColor(labelColor);
                    e.requirement = r;
                    this.requirementsListScrollArea.addEntry(e);
                }
            }

        }

    }

    protected void updateRequirementsList() {

        this.setContentOfRequirementsList(null);

        //Select correct entry if instance has requirement
        if (this.instance.requirement != null) {
            this.setContentOfRequirementsList(this.instance.requirement.getCategory());
            for (ScrollAreaEntry e : this.requirementsListScrollArea.getEntries()) {
                if ((e instanceof RequirementScrollEntry) && (((RequirementScrollEntry) e).requirement == this.instance.requirement)) {
                    e.setSelected(true);
                    break;
                }
            }
        }

    }

    private void focusSearchBar() {
        if (this.searchBar == null) {
            return;
        }
        this.setFocused(this.searchBar);
        this.searchBar.setFocused(true);
    }

    private void defocusSearchBar() {
        if (this.searchBar == null) {
            return;
        }
        this.searchBar.setFocused(false);
        if (this.getFocused() == this.searchBar) {
            this.setFocused(null);
        }
    }

    private void clearSelectedRequirementsEntries() {
        for (ScrollAreaEntry entry : this.requirementsListScrollArea.getEntries()) {
            entry.setSelected(false);
        }
    }

    private boolean shouldRouteTypedCharacterToSearchBar(char codePoint) {
        if (hasControlDown() || hasAltDown()) {
            return false;
        }
        return !Character.isISOControl(codePoint);
    }

    private void selectRequirement(@NotNull Requirement requirement) {
        Requirement previousRequirement = this.instance.requirement;
        this.instance.requirement = requirement;
        if (previousRequirement != requirement) {
            this.instance.value = null;
        }
        this.setDescription(this.instance.requirement);
    }

    @Nullable
    private RequirementInstance applyCurrentEditResultToTarget() {
        if (!this.isEdit) {
            return this.instance;
        }
        if (this.editTargetInstance == null) {
            return null;
        }
        this.editTargetInstance.requirement = this.instance.requirement;
        this.editTargetInstance.value = this.instance.value;
        this.editTargetInstance.mode = this.instance.mode;
        this.editTargetInstance.parent = this.parent;
        this.editTargetInstance.group = this.instance.group;
        return this.editTargetInstance;
    }

    private void onCancelOrEscape() {
        this.callback.accept(null);
        this.closeWindow();
    }

    private boolean activateSelectedRequirementsListEntry() {
        ScrollAreaEntry selectedEntry = this.requirementsListScrollArea.getFocusedEntry();
        if (selectedEntry instanceof RequirementScrollEntry requirementEntry) {
            if (requirementEntry.requirement != null) {
                this.selectRequirement(requirementEntry.requirement);
            }
            this.onNextStep();
            return true;
        }
        if (selectedEntry instanceof TextListScrollAreaEntry textEntry) {
            textEntry.onClick(textEntry, this.getRenderMouseX(), this.getRenderMouseY(), 0);
            return true;
        }
        return false;
    }

    private boolean selectAdjacentRequirementsEntry(boolean moveDown) {
        List<ScrollAreaEntry> entries = this.requirementsListScrollArea.getEntries();
        boolean searchBarAvailable = this.searchBar != null;
        boolean searchBarFocused = searchBarAvailable && this.searchBar.isFocused();

        if (entries.isEmpty()) {
            if (searchBarAvailable && !searchBarFocused) {
                this.clearSelectedRequirementsEntries();
                this.focusSearchBar();
                return true;
            }
            return false;
        }

        if (searchBarFocused) {
            if (moveDown) {
                this.defocusSearchBar();
                ScrollAreaEntry firstEntry = entries.get(0);
                firstEntry.setSelected(true);
                this.scrollRequirementsEntryIntoView(firstEntry);
            } else {
                this.defocusSearchBar();
                ScrollAreaEntry lastEntry = entries.get(entries.size() - 1);
                lastEntry.setSelected(true);
                this.scrollRequirementsEntryIntoView(lastEntry);
            }
            return true;
        }

        ScrollAreaEntry selected = this.requirementsListScrollArea.getFocusedEntry();
        if (selected == null) {
            if (searchBarAvailable) {
                this.clearSelectedRequirementsEntries();
                this.focusSearchBar();
                return true;
            }
            ScrollAreaEntry fallbackTarget = moveDown ? entries.get(0) : entries.get(entries.size() - 1);
            fallbackTarget.setSelected(true);
            this.scrollRequirementsEntryIntoView(fallbackTarget);
            return true;
        }

        int currentIndex = entries.indexOf(selected);
        if (currentIndex <= 0 && !moveDown && searchBarAvailable) {
            this.clearSelectedRequirementsEntries();
            this.focusSearchBar();
            return true;
        }
        if (currentIndex == (entries.size() - 1) && moveDown && searchBarAvailable) {
            this.clearSelectedRequirementsEntries();
            this.focusSearchBar();
            return true;
        }

        int targetIndex;
        if (currentIndex == -1) {
            targetIndex = moveDown ? 0 : entries.size() - 1;
        } else {
            targetIndex = currentIndex + (moveDown ? 1 : -1);
            if (targetIndex < 0) {
                targetIndex = entries.size() - 1;
            } else if (targetIndex >= entries.size()) {
                targetIndex = 0;
            }
        }
        ScrollAreaEntry target = entries.get(targetIndex);
        this.defocusSearchBar();
        target.setSelected(true);
        this.scrollRequirementsEntryIntoView(target);
        return true;
    }

    private void scrollRequirementsEntryIntoView(@NotNull ScrollAreaEntry entry) {
        List<ScrollAreaEntry> entries = this.requirementsListScrollArea.getEntries();
        int totalHeight = 0;
        for (ScrollAreaEntry e : entries) {
            totalHeight += (int)e.getHeight();
        }
        int visibleHeight = (int)this.requirementsListScrollArea.getInnerHeight();
        if (totalHeight <= visibleHeight) {
            return;
        }
        int offset = 0;
        for (ScrollAreaEntry e : entries) {
            if (e == entry) {
                break;
            }
            offset += (int)e.getHeight();
        }
        int entryCenter = offset + ((int)entry.getHeight() / 2);
        int target = Math.max(0, entryCenter - (visibleHeight / 2));
        int maxScroll = Math.max(1, totalHeight - visibleHeight);
        float scroll = Math.min(1.0F, (float)target / (float)maxScroll);
        this.requirementsListScrollArea.verticalScrollBar.setScroll(Mth.clamp(scroll, 0.0F, 1.0F));
        this.requirementsListScrollArea.updateEntries(null);
    }

    public class RequirementScrollEntry extends TextListScrollAreaEntry {

        public Requirement requirement;
        protected long lastClickTime = 0;
        protected static final long DOUBLE_CLICK_TIME = 500; // milliseconds

        public RequirementScrollEntry(ScrollArea parent, @NotNull Component text, @NotNull DrawableColor listDotColor, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, text, listDotColor, onClick);
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
            long currentTime = System.currentTimeMillis();
            
            // Check if this is a double-click
            if (currentTime - this.lastClickTime < DOUBLE_CLICK_TIME) {
                // Double-click detected
                if ((this.requirement != null) && (BuildRequirementWindowBody.this.instance.requirement == this.requirement)) {
                    // Check if requirement has value or doesn't need value -> act as "Done"
                    BuildRequirementWindowBody.this.onNextStep();
                    this.lastClickTime = 0; // Reset to prevent triple clicks
                    return;
                }
            }
            
            this.lastClickTime = currentTime;
            
            // Normal single click behavior
            super.onClick(entry, mouseX, mouseY, button);
        }

    }

    private int getGenericTextColor() {
        return UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_generic_text_color.getColorInt()
                : UIBase.getUITheme().ui_interface_generic_text_color.getColorInt();
    }

    private int getLabelTextColor() {
        return UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_widget_label_color_normal.getColorInt()
                : UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt();
    }

    public static @NotNull PiPWindow openInWindow(@NotNull BuildRequirementWindowBody screen, @Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(screen.getTitle())
                .setScreen(screen)
                .setForceFancyMenuUiScale(true)
                .setAlwaysOnTop(false)
                .setForceFocus(false)
                .setBlockMinecraftScreenInputs(false)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        focusWindowAfterOpen(window);
        return window;
    }

    private static void focusWindowAfterOpen(@NotNull PiPWindow window) {
        if (PiPWindowHandler.INSTANCE.getOpenWindows().contains(window)) {
            PiPWindowHandler.INSTANCE.bringToFront(window);
        }
        MainThreadTaskExecutor.executeInMainThread(() ->
                        MainThreadTaskExecutor.executeInMainThread(() -> {
                                    if (PiPWindowHandler.INSTANCE.getOpenWindows().contains(window) && window.isVisible()) {
                                        PiPWindowHandler.INSTANCE.bringToFront(window);
                                    }
                                },
                                MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK),
                MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
    }

    public static @NotNull PiPWindow openInWindow(@NotNull BuildRequirementWindowBody screen) {
        return openInWindow(screen, null);
    }

}
