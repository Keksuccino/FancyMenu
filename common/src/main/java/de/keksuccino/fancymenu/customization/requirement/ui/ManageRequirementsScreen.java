package de.keksuccino.fancymenu.customization.requirement.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementGroup;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class ManageRequirementsScreen extends Screen {

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
            BuildRequirementScreen s = new BuildRequirementScreen(this, this.container, null, (call) -> {
                if (call != null) {
                    this.container.addInstance(call);
                    this.updateRequirementsScrollArea();
                }
            });
            Minecraft.getInstance().setScreen(s);
        });
        this.addWidget(this.addRequirementButton);
        this.addRequirementButton.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.manage_screen.add_requirement.desc")).setDefaultStyle());
        UIBase.applyDefaultWidgetSkinTo(this.addRequirementButton);

        this.addGroupButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.requirements.screens.add_group"), (button) -> {
            BuildRequirementGroupScreen s = new BuildRequirementGroupScreen(this, this.container, null, (call) -> {
                if (call != null) {
                    this.container.addGroup(call);
                    this.updateRequirementsScrollArea();
                }
            });
            Minecraft.getInstance().setScreen(s);
        });
        this.addWidget(this.addGroupButton);
        this.addGroupButton.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.manage_screen.add_group.desc")).setDefaultStyle());
        UIBase.applyDefaultWidgetSkinTo(this.addGroupButton);

        this.editButton = new ExtendedButton(0, 0, 150, 20, "", (button) -> {
            Screen s = null;
            if (this.isInstanceSelected()) {
                s = new BuildRequirementScreen(this, this.container, this.getSelectedInstance(), (call) -> {
                    if (call != null) {
                        this.updateRequirementsScrollArea();
                    }
                });
            } else if (this.isGroupSelected()) {
                s = new BuildRequirementGroupScreen(this, this.container, this.getSelectedGroup(), (call) -> {
                    if (call != null) {
                        this.updateRequirementsScrollArea();
                    }
                });
            }
            if (s != null) {
                Minecraft.getInstance().setScreen(s);
            }
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int p_93658_, int p_93659_, float p_93660_) {
                ManageRequirementsScreen s = ManageRequirementsScreen.this;
                if (!s.isInstanceSelected() && !s.isGroupSelected()) {
                    this.setLabel(I18n.get("fancymenu.requirements.screens.manage_screen.edit.generic"));
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.manage_screen.no_entry_selected")).setDefaultStyle());
                    this.active = false;
                } else {
                    if (s.isInstanceSelected()) {
                        this.setLabel(I18n.get("fancymenu.requirements.screens.edit_requirement"));
                    } else {
                        this.setLabel(I18n.get("fancymenu.requirements.screens.edit_group"));
                    }
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.manage_screen.edit.desc")).setDefaultStyle());
                    this.active = true;
                }
                super.render(graphics, p_93658_, p_93659_, p_93660_);
            }
        };
        this.addWidget(this.editButton);
        UIBase.applyDefaultWidgetSkinTo(this.editButton);

        this.removeButton = new ExtendedButton(0, 0, 150, 20, "", (button) -> {
            Screen s = null;
            if (this.isInstanceSelected()) {
                RequirementInstance i = this.getSelectedInstance();
                s = ConfirmationScreen.ofStrings((call) -> {
                    if (call) {
                        this.container.removeInstance(i);
                        this.updateRequirementsScrollArea();
                    }
                    Minecraft.getInstance().setScreen(this);
                }, LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.remove_requirement.confirm"));
            } else if (this.isGroupSelected()) {
                RequirementGroup g = this.getSelectedGroup();
                s = ConfirmationScreen.ofStrings((call) -> {
                    if (call) {
                        this.container.removeGroup(g);
                        this.updateRequirementsScrollArea();
                    }
                    Minecraft.getInstance().setScreen(this);
                }, LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.remove_group.confirm"));
            }
            if (s != null) {
                Minecraft.getInstance().setScreen(s);
            }
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int p_93658_, int p_93659_, float p_93660_) {
                ManageRequirementsScreen s = ManageRequirementsScreen.this;
                if (!s.isInstanceSelected() && !s.isGroupSelected()) {
                    this.setLabel(I18n.get("fancymenu.requirements.screens.manage_screen.remove.generic"));
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.manage_screen.no_entry_selected")).setDefaultStyle());
                    this.active = false;
                } else {
                    if (s.isInstanceSelected()) {
                        this.setLabel(I18n.get("fancymenu.requirements.screens.remove_requirement"));
                    } else {
                        this.setLabel(I18n.get("fancymenu.requirements.screens.remove_group"));
                    }
                    this.setTooltip(Tooltip.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.screens.manage_screen.remove.desc")).setDefaultStyle());
                    this.active = true;
                }
                super.render(graphics, p_93658_, p_93659_, p_93660_);
            }
        };
        this.addWidget(this.removeButton);
        UIBase.applyDefaultWidgetSkinTo(this.removeButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.common_components.cancel"), (button) -> {
            this.callback.accept(null);
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

        this.doneButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.common_components.done"), (button) -> {
            this.callback.accept(this.container);
        });
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);

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

        graphics.drawString(this.font, I18n.get("fancymenu.requirements.screens.manage_screen.requirements_and_groups"), 20, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.requirementsScrollArea.setWidth(this.width - 20 - 150 - 20 - 20, true);
        this.requirementsScrollArea.setHeight(this.height - 85, true);
        this.requirementsScrollArea.setX(20, true);
        this.requirementsScrollArea.setY(50 + 15, true);
        this.requirementsScrollArea.render(graphics, mouseX, mouseY, partial);

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

        super.render(graphics, mouseX, mouseY, partial);

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
            super(parent, Component.literal(group.identifier).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())).append(Component.literal(" (" + I18n.get("fancymenu.requirements.screens.manage_screen.group.info", "" + group.getInstances().size()) + ")").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt()))), UIBase.getUIColorTheme().listing_dot_color_3.getColor(), (entry) -> {});
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

            this.displayNameComponent = Component.literal(this.instance.requirement.getDisplayName()).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt()));
            String modeString = (this.instance.mode == RequirementInstance.RequirementMode.IF) ? I18n.get("fancymenu.requirements.screens.requirement.info.mode.normal") : I18n.get("fancymenu.requirements.screens.requirement.info.mode.opposite");
            this.modeComponent = Component.literal(I18n.get("fancymenu.requirements.screens.requirement.info.mode") + " ").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())).append(Component.literal(modeString).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt())));
            String valueString = (this.instance.value != null) ? this.instance.value : I18n.get("fancymenu.requirements.screens.requirement.info.value.none");
            this.valueComponent = Component.literal(I18n.get("fancymenu.requirements.screens.requirement.info.value") + " ").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())).append(Component.literal(valueString).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().element_label_color_normal.getColorInt())));

            this.setWidth(this.calculateWidth());
            this.setHeight((lineHeight * 3) + (HEADER_FOOTER_HEIGHT * 2));

        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            super.render(graphics, mouseX, mouseY, partial);

            int centerYLine1 = this.getY() + HEADER_FOOTER_HEIGHT + (this.lineHeight / 2);
            int centerYLine2 = this.getY() + HEADER_FOOTER_HEIGHT + ((this.lineHeight / 2) * 3);
            int centerYLine3 = this.getY() + HEADER_FOOTER_HEIGHT + ((this.lineHeight / 2) * 5);

            RenderSystem.enableBlend();

            renderListingDot(graphics, this.getX() + 5, centerYLine1 - 2, UIBase.getUIColorTheme().listing_dot_color_2.getColor());
            graphics.drawString(this.font, this.displayNameComponent, (this.getX() + 5 + 4 + 3), (centerYLine1 - (this.font.lineHeight / 2)), -1, false);

            renderListingDot(graphics, this.getX() + 5 + 4 + 3, centerYLine2 - 2, UIBase.getUIColorTheme().listing_dot_color_1.getColor());
            graphics.drawString(this.font, this.modeComponent, (this.getX() + 5 + 4 + 3 + 4 + 3), (centerYLine2 - (this.font.lineHeight / 2)), -1, false);

            renderListingDot(graphics, this.getX() + 5 + 4 + 3, centerYLine3 - 2, UIBase.getUIColorTheme().listing_dot_color_1.getColor());
            graphics.drawString(this.font, this.valueComponent, (this.getX() + 5 + 4 + 3 + 4 + 3), (centerYLine3 - (this.font.lineHeight / 2)), -1, false);

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
        public void onClick(ScrollAreaEntry entry) {}

    }

}
