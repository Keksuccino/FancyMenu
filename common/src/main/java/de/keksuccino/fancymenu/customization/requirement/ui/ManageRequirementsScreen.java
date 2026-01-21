package de.keksuccino.fancymenu.customization.requirement.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementGroup;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class ManageRequirementsScreen extends PiPWindowBody {

    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 420;

    protected RequirementContainer container;
    protected Consumer<RequirementContainer> callback;

    protected ScrollArea requirementsScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedButton addRequirementButton;
    protected ExtendedButton addGroupButton;
    protected ExtendedButton editButton;
    protected ExtendedButton removeButton;
    protected ExtendedButton doneButton;
    protected ExtendedButton cancelButton;

    public ManageRequirementsScreen(@NotNull RequirementContainer container, @NotNull Consumer<RequirementContainer> callback) {
        super(Component.literal(I18n.get("fancymenu.requirements.screens.manage_screen.manage")));
        this.container = container;
        this.callback = callback;
        this.updateRequirementsScrollArea();
    }

    @Override
    protected void init() {

        this.addRequirementButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.requirements.screens.add_requirement"), (button) -> {
            BuildRequirementScreen s = new BuildRequirementScreen(this.container, null, (call) -> {
                if (call != null) {
                    this.container.addInstance(call);
                    this.updateRequirementsScrollArea();
                }
            });
            BuildRequirementScreen.openInWindow(s, this.getWindow());
        });
        this.addWidget(this.addRequirementButton);
        this.addRequirementButton.setUITooltip(UITooltip.of(Component.translatable("fancymenu.requirements.screens.manage_screen.add_requirement.desc")));
        UIBase.applyDefaultWidgetSkinTo(this.addRequirementButton);

        this.addGroupButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.requirements.screens.add_group"), (button) -> {
            BuildRequirementGroupScreen s = new BuildRequirementGroupScreen(this.container, null, (call) -> {
                if (call != null) {
                    this.container.addGroup(call);
                    this.updateRequirementsScrollArea();
                }
            });
            BuildRequirementGroupScreen.openInWindow(s, this.getWindow());
        });
        this.addWidget(this.addGroupButton);
        this.addGroupButton.setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.manage_screen.add_group.desc")));
        UIBase.applyDefaultWidgetSkinTo(this.addGroupButton);

        this.editButton = new ExtendedButton(0, 0, 150, 20, "", (button) -> {
            BuildRequirementScreen requirementScreen = null;
            BuildRequirementGroupScreen groupScreen = null;
            if (this.isInstanceSelected()) {
                requirementScreen = new BuildRequirementScreen(this.container, this.getSelectedInstance(), (call) -> {
                    if (call != null) {
                        this.updateRequirementsScrollArea();
                    }
                });
            } else if (this.isGroupSelected()) {
                groupScreen = new BuildRequirementGroupScreen(this.container, this.getSelectedGroup(), (call) -> {
                    if (call != null) {
                        this.updateRequirementsScrollArea();
                    }
                });
            }
            if (requirementScreen != null) {
                BuildRequirementScreen.openInWindow(requirementScreen, this.getWindow());
            } else if (groupScreen != null) {
                BuildRequirementGroupScreen.openInWindow(groupScreen, this.getWindow());
            }
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int p_93658_, int p_93659_, float p_93660_) {
                ManageRequirementsScreen s = ManageRequirementsScreen.this;
                if (!s.isInstanceSelected() && !s.isGroupSelected()) {
                    this.setLabel(I18n.get("fancymenu.requirements.screens.manage_screen.edit.generic"));
                    this.setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.manage_screen.no_entry_selected")));
                    this.active = false;
                } else {
                    if (s.isInstanceSelected()) {
                        this.setLabel(I18n.get("fancymenu.requirements.screens.edit_requirement"));
                    } else {
                        this.setLabel(I18n.get("fancymenu.requirements.screens.edit_group"));
                    }
                    this.setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.manage_screen.edit.desc")));
                    this.active = true;
                }
                super.render(graphics, p_93658_, p_93659_, p_93660_);
            }
        };
        this.addWidget(this.editButton);
        UIBase.applyDefaultWidgetSkinTo(this.editButton);

        this.removeButton = new ExtendedButton(0, 0, 150, 20, "", (button) -> {
            if (this.isInstanceSelected()) {
                RequirementInstance i = this.getSelectedInstance();
                Dialogs.openMessageWithCallback(Component.translatable("fancymenu.requirements.screens.remove_requirement.confirm"), MessageDialogStyle.WARNING, call -> {
                    if (call) {
                        this.container.removeInstance(i);
                        this.updateRequirementsScrollArea();
                    }
                });
            } else if (this.isGroupSelected()) {
                RequirementGroup g = this.getSelectedGroup();
                Dialogs.openMessageWithCallback(Component.translatable("fancymenu.requirements.screens.remove_group.confirm"), MessageDialogStyle.WARNING, call -> {
                    if (call) {
                        this.container.removeGroup(g);
                        this.updateRequirementsScrollArea();
                    }
                });
            }
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int p_93658_, int p_93659_, float p_93660_) {
                ManageRequirementsScreen s = ManageRequirementsScreen.this;
                if (!s.isInstanceSelected() && !s.isGroupSelected()) {
                    this.setLabel(I18n.get("fancymenu.requirements.screens.manage_screen.remove.generic"));
                    this.setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.manage_screen.no_entry_selected")));
                    this.active = false;
                } else {
                    if (s.isInstanceSelected()) {
                        this.setLabel(I18n.get("fancymenu.requirements.screens.remove_requirement"));
                    } else {
                        this.setLabel(I18n.get("fancymenu.requirements.screens.remove_group"));
                    }
                    this.setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.manage_screen.remove.desc")));
                    this.active = true;
                }
                super.render(graphics, p_93658_, p_93659_, p_93660_);
            }
        };
        this.addWidget(this.removeButton);
        UIBase.applyDefaultWidgetSkinTo(this.removeButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.common_components.cancel"), (button) -> {
            this.callback.accept(null);
            this.closeWindow();
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

        this.doneButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.common_components.done"), (button) -> {
            this.callback.accept(this.container);
            this.closeWindow();
        });
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);

        this.addRenderableWidget(this.requirementsScrollArea);

    }

    @Override
    public void onWindowClosedExternally() {
        this.callback.accept(null);
    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        graphics.fill(0, 0, this.width, this.height, UIBase.getUITheme().ui_interface_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, UIBase.getUITheme().ui_interface_generic_text_color.getColorInt(), false);

        graphics.drawString(this.font, I18n.get("fancymenu.requirements.screens.manage_screen.requirements_and_groups"), 20, 50, UIBase.getUITheme().ui_interface_generic_text_color.getColorInt(), false);

        this.requirementsScrollArea.setWidth(this.width - 20 - 150 - 20 - 20, true);
        this.requirementsScrollArea.setHeight(this.height - 85, true);
        this.requirementsScrollArea.setX(20, true);
        this.requirementsScrollArea.setY(50 + 15, true);

        this.doneButton.setX(this.width - 20 - this.doneButton.getWidth());
        this.doneButton.setY(this.height - 20 - 20);
        this.doneButton.render(graphics, mouseX, mouseY, partial);

        this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
        this.cancelButton.setY(this.doneButton.getY() - 5 - 20);
        this.cancelButton.render(graphics, mouseX, mouseY, partial);

        this.removeButton.setX(this.width - 20 - this.removeButton.getWidth());
        this.removeButton.setY(this.cancelButton.getY() - 15 - 20);
        this.removeButton.render(graphics, mouseX, mouseY, partial);

        this.editButton.setX(this.width - 20 - this.editButton.getWidth());
        this.editButton.setY(this.removeButton.getY() - 5 - 20);
        this.editButton.render(graphics, mouseX, mouseY, partial);

        this.addGroupButton.setX(this.width - 20 - this.addGroupButton.getWidth());
        this.addGroupButton.setY(this.editButton.getY() - 5 - 20);
        this.addGroupButton.render(graphics, mouseX, mouseY, partial);

        this.addRequirementButton.setX(this.width - 20 - this.addRequirementButton.getWidth());
        this.addRequirementButton.setY(this.addGroupButton.getY() - 5 - 20);
        this.addRequirementButton.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void renderBackground(@NotNull GuiGraphics $$0, int $$1, int $$2, float $$3) {
    }

    @Nullable
    protected RequirementInstance getSelectedInstance() {
        ScrollAreaEntry e = this.requirementsScrollArea.getFocusedEntry();
        if (e instanceof RequirementInstanceEntry) {
            return ((RequirementInstanceEntry)e).instance;
        }
        return null;
    }

    protected boolean isInstanceSelected() {
        return this.getSelectedInstance() != null;
    }

    @Nullable
    protected RequirementGroup getSelectedGroup() {
        ScrollAreaEntry e = this.requirementsScrollArea.getFocusedEntry();
        if (e instanceof RequirementGroupEntry) {
            return ((RequirementGroupEntry)e).group;
        }
        return null;
    }

    protected boolean isGroupSelected() {
        return this.getSelectedGroup() != null;
    }

    protected void updateRequirementsScrollArea() {

        this.requirementsScrollArea.clearEntries();

        for (RequirementGroup g : this.container.getGroups()) {
            RequirementGroupEntry e = new RequirementGroupEntry(this.requirementsScrollArea, g);
            this.requirementsScrollArea.addEntry(e);
        }

        for (RequirementInstance i : this.container.getInstances()) {
            RequirementInstanceEntry e = new RequirementInstanceEntry(this.requirementsScrollArea, i, 14);
            this.requirementsScrollArea.addEntry(e);
        }

    }

    public static class RequirementGroupEntry extends TextListScrollAreaEntry {

        public static final int HEADER_FOOTER_HEIGHT = 3;

        public RequirementGroup group;

        public RequirementGroupEntry(ScrollArea parent, RequirementGroup group) {
            super(parent, Component.literal(group.identifier).setStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt())).append(Component.literal(" (" + I18n.get("fancymenu.requirements.screens.manage_screen.group.info", "" + group.getInstances().size()) + ")").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt()))), UIBase.getUITheme().bullet_list_dot_color_3, (entry) -> {});
            this.group = group;
            this.setHeight(this.getHeight() + (HEADER_FOOTER_HEIGHT * 2));
        }

    }

    public static class RequirementInstanceEntry extends ScrollAreaEntry {

        public static final int HEADER_FOOTER_HEIGHT = 3;

        public RequirementInstance instance;
        public final int lineHeight;
        public Font font = Minecraft.getInstance().font;

        private final MutableComponent displayNameComponent;
        private final MutableComponent modeComponent;
        private final MutableComponent valueComponent;

        public RequirementInstanceEntry(ScrollArea parent, RequirementInstance instance, int lineHeight) {

            super(parent, 100, 30);
            this.instance = instance;
            this.lineHeight = lineHeight;

            this.displayNameComponent = Component.literal(this.instance.requirement.getDisplayName()).setStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt()));
            String modeString = (this.instance.mode == RequirementInstance.RequirementMode.IF) ? I18n.get("fancymenu.requirements.screens.requirement.info.mode.normal") : I18n.get("fancymenu.requirements.screens.requirement.info.mode.opposite");
            this.modeComponent = Component.literal(I18n.get("fancymenu.requirements.screens.requirement.info.mode") + " ").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt())).append(Component.literal(modeString).setStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt())));
            String valueString = (this.instance.value != null) ? this.instance.value : I18n.get("fancymenu.requirements.screens.requirement.info.value.none");
            this.valueComponent = Component.literal(I18n.get("fancymenu.requirements.screens.requirement.info.value") + " ").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt())).append(Component.literal(valueString).setStyle(Style.EMPTY.withColor(UIBase.getUITheme().ui_interface_widget_label_color_normal.getColorInt())));

            this.setWidth(this.calculateWidth());
            this.setHeight((lineHeight * 3) + (HEADER_FOOTER_HEIGHT * 2));

        }

        @Override
        public void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            int baseX = (int)this.getX();
            int baseY = (int)this.getY();
            int centerYLine1 = baseY + HEADER_FOOTER_HEIGHT + (this.lineHeight / 2);
            int centerYLine2 = baseY + HEADER_FOOTER_HEIGHT + ((this.lineHeight / 2) * 3);
            int centerYLine3 = baseY + HEADER_FOOTER_HEIGHT + ((this.lineHeight / 2) * 5);

            RenderSystem.enableBlend();

            renderListingDot(graphics, baseX + 5, centerYLine1 - 2, UIBase.getUITheme().bullet_list_dot_color_2.getColorInt());
            graphics.drawString(this.font, this.displayNameComponent, (baseX + 5 + 4 + 3), (centerYLine1 - (this.font.lineHeight / 2)), -1, false);

            renderListingDot(graphics, baseX + 5 + 4 + 3, centerYLine2 - 2, UIBase.getUITheme().bullet_list_dot_color_1.getColorInt());
            graphics.drawString(this.font, this.modeComponent, (baseX + 5 + 4 + 3 + 4 + 3), (centerYLine2 - (this.font.lineHeight / 2)), -1, false);

            renderListingDot(graphics, baseX + 5 + 4 + 3, centerYLine3 - 2, UIBase.getUITheme().bullet_list_dot_color_1.getColorInt());
            graphics.drawString(this.font, this.valueComponent, (baseX + 5 + 4 + 3 + 4 + 3), (centerYLine3 - (this.font.lineHeight / 2)), -1, false);

        }

        private int calculateWidth() {
            int w = 5 + 4 + 3 + this.font.width(this.displayNameComponent) + 5;
            int w2 = 5 + 4 + 3 + 4 + 3 + this.font.width(this.modeComponent) + 5;
            int w3 = 5 + 4 + 3 + 4 + 3 + this.font.width(this.valueComponent) + 5;
            if (w2 > w) {
                w = w2;
            }
            if (w3 > w) {
                w = w3;
            }
            return w;
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {}

    }

    public static @NotNull PiPWindow openInWindow(@NotNull ManageRequirementsScreen screen, @Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(screen.getTitle())
                .setScreen(screen)
                .setForceFancyMenuUiScale(true)
                .setAlwaysOnTop(true)
                .setBlockMinecraftScreenInputs(true)
                .setForceFocus(true)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        return window;
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ManageRequirementsScreen screen) {
        return openInWindow(screen, null);
    }

}
