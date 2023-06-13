
package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.actions;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.ConfirmationScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class ManageActionsScreen extends Screen {

    protected Screen parentScreen;
    protected List<ActionInstance> instances;

    protected ScrollArea actionsScrollArea = new ScrollArea(0, 0, 0, 0);
    protected Consumer<List<ActionInstance>> callback;
    protected AdvancedButton addActionButton;
    protected AdvancedButton moveUpButton;
    protected AdvancedButton moveDownButton;
    protected AdvancedButton editButton;
    protected AdvancedButton removeButton;
    protected AdvancedButton doneButton;

    public ManageActionsScreen(@Nullable Screen parentScreen, @NotNull List<ActionInstance> instances, @NotNull Consumer<List<ActionInstance>> callback) {

        super(Component.literal(Locals.localize("fancymenu.editor.action.screens.manage_screen.manage")));

        this.parentScreen = parentScreen;
        this.callback = callback;
        this.instances = instances;
        this.updateActionInstanceScrollArea(false);

        this.addActionButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.editor.action.screens.add_action"), true, (button) -> {
            BuildActionScreen s = new BuildActionScreen(this, null, (call) -> {
                if (call != null) {
                    this.instances.add(call);
                    this.updateActionInstanceScrollArea(false);
                }
            });
            Minecraft.getInstance().setScreen(s);
        });
        this.addActionButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.add_action.desc"), "%n%"));
        UIBase.applyDefaultButtonSkinTo(this.addActionButton);

        this.moveUpButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.editor.action.screens.move_action_up"), true, (button) -> {
            if (this.isInstanceSelected()) {
                ActionInstance selected = this.getSelectedInstance();
                int index = this.instances.indexOf(selected);
                if (index > 0) {
                    this.instances.remove(selected);
                    this.instances.add(index-1, selected);
                    this.updateActionInstanceScrollArea(true);
                    for (ScrollAreaEntry e : this.actionsScrollArea.getEntries()) {
                        if ((e instanceof ActionInstanceEntry) && (((ActionInstanceEntry)e).instance == selected)) {
                            e.setFocused(true);
                            break;
                        }
                    }
                }
            }
        }) {
            @Override
            public void render(GuiGraphics p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                ManageActionsScreen s = ManageActionsScreen.this;
                if (!s.isInstanceSelected()) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.finish.no_action_selected"), "%n%"));
                    this.active = false;
                } else {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.move_action_up.desc"), "%n%"));
                    this.active = true;
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.moveUpButton);

        this.moveDownButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.editor.action.screens.move_action_down"), true, (button) -> {
            if (this.isInstanceSelected()) {
                ActionInstance selected = this.getSelectedInstance();
                int index = this.instances.indexOf(selected);
                if ((index >= 0) && (index <= this.instances.size()-2)) {
                    this.instances.remove(selected);
                    this.instances.add(index+1, selected);
                    this.updateActionInstanceScrollArea(true);
                    for (ScrollAreaEntry e : this.actionsScrollArea.getEntries()) {
                        if ((e instanceof ActionInstanceEntry) && (((ActionInstanceEntry)e).instance == selected)) {
                            e.setFocused(true);
                            break;
                        }
                    }
                }
            }
        }) {
            @Override
            public void render(GuiGraphics p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                ManageActionsScreen s = ManageActionsScreen.this;
                if (!s.isInstanceSelected()) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.finish.no_action_selected"), "%n%"));
                    this.active = false;
                } else {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.move_action_down.desc"), "%n%"));
                    this.active = true;
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.moveDownButton);

        this.editButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.editor.action.screens.edit_action"), true, (button) -> {
            if (this.isInstanceSelected()) {
                BuildActionScreen s = new BuildActionScreen(this, this.getSelectedInstance(), (call) -> {
                    this.updateActionInstanceScrollArea(false);
                });
                Minecraft.getInstance().setScreen(s);
            }
        }) {
            @Override
            public void render(GuiGraphics p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                ManageActionsScreen s = ManageActionsScreen.this;
                if (!s.isInstanceSelected()) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.finish.no_action_selected"), "%n%"));
                    this.active = false;
                } else {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.edit_action.desc"), "%n%"));
                    this.active = true;
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.editButton);

        this.removeButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.editor.action.screens.remove_action"), true, (button) -> {
            if (this.isInstanceSelected()) {
                ActionInstance i = this.getSelectedInstance();
                Screen s = new ConfirmationScreen(this, (call) -> {
                    if (call) {
                        this.instances.remove(i);
                        this.updateActionInstanceScrollArea(false);
                    }
                }, StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.remove_action.confirm"), "%n%"));
                Minecraft.getInstance().setScreen(s);
            }
        }) {
            @Override
            public void render(GuiGraphics p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                ManageActionsScreen s = ManageActionsScreen.this;
                if (!s.isInstanceSelected()) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.finish.no_action_selected"), "%n%"));
                    this.active = false;
                } else {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.remove_action.desc"), "%n%"));
                    this.active = true;
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.removeButton);

        this.doneButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.guicomponents.done"), true, (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(this.instances);
        });
        UIBase.applyDefaultButtonSkinTo(this.doneButton);

    }

    @Override
    protected void init() {

        //Reset the GUI scale in case the layout editor changed it
        Minecraft.getInstance().getWindow().setGuiScale(Minecraft.getInstance().getWindow().calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode()));
        this.height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        this.width = Minecraft.getInstance().getWindow().getGuiScaledWidth();

        super.init();

    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parentScreen);
        this.callback.accept(this.instances);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        graphics.fill(0, 0, this.width, this.height, UIBase.SCREEN_BACKGROUND_COLOR.getRGB());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, -1, false);

        graphics.drawString(this.font, Locals.localize("fancymenu.editor.action.screens.manage_screen.actions"), 20, 50, -1, false);

        this.actionsScrollArea.setWidth(this.width - 20 - 150 - 20 - 20, true);
        this.actionsScrollArea.setHeight(this.height - 85, true);
        this.actionsScrollArea.setX(20, true);
        this.actionsScrollArea.setY(50 + 15, true);
        this.actionsScrollArea.render(graphics, mouseX, mouseY, partial);

        this.doneButton.setX(this.width - 20 - this.doneButton.getWidth());
        this.doneButton.setY(this.height - 20 - 20);
        this.doneButton.render(graphics, mouseX, mouseY, partial);

        this.removeButton.setX(this.width - 20 - this.removeButton.getWidth());
        this.removeButton.setY(this.doneButton.getY() - 15 - 20);
        this.removeButton.render(graphics, mouseX, mouseY, partial);

        this.editButton.setX(this.width - 20 - this.editButton.getWidth());
        this.editButton.setY(this.removeButton.getY() - 5 - 20);
        this.editButton.render(graphics, mouseX, mouseY, partial);

        this.moveDownButton.setX(this.width - 20 - this.moveDownButton.getWidth());
        this.moveDownButton.setY(this.editButton.getY() - 5 - 20);
        this.moveDownButton.render(graphics, mouseX, mouseY, partial);

        this.moveUpButton.setX(this.width - 20 - this.moveUpButton.getWidth());
        this.moveUpButton.setY(this.moveDownButton.getY() - 5 - 20);
        this.moveUpButton.render(graphics, mouseX, mouseY, partial);

        this.addActionButton.setX(this.width - 20 - this.addActionButton.getWidth());
        this.addActionButton.setY(this.moveUpButton.getY() - 5 - 20);
        this.addActionButton.render(graphics, mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);

    }

    @Nullable
    protected ActionInstance getSelectedInstance() {
        ScrollAreaEntry e = this.actionsScrollArea.getFocusedEntry();
        if (e instanceof ActionInstanceEntry) {
            return ((ActionInstanceEntry)e).instance;
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

        for (ActionInstance i : this.instances) {
            ActionInstanceEntry e = new ActionInstanceEntry(this.actionsScrollArea, i, 14);
            this.actionsScrollArea.addEntry(e);
        }

        if (keepScroll) {
            this.actionsScrollArea.verticalScrollBar.setScroll(oldScrollVertical);
            this.actionsScrollArea.horizontalScrollBar.setScroll(oldScrollHorizontal);
        }

    }

    public static class ActionInstanceEntry extends ScrollAreaEntry {

        public static final int HEADER_FOOTER_HEIGHT = 3;

        public ActionInstance instance;
        public final int lineHeight;
        public Font font = Minecraft.getInstance().font;

        private MutableComponent displayNameComponent;
        private MutableComponent valueComponent;

        public ActionInstanceEntry(ScrollArea parent, ActionInstance instance, int lineHeight) {

            super(parent, 100, 30);
            this.instance = instance;
            this.lineHeight = lineHeight;

            this.displayNameComponent = Component.literal(this.instance.action.getAction()).setStyle(Style.EMPTY.withColor(TEXT_COLOR_GRAY_1.getRGB()));
            String valueString = ((this.instance.value != null) && this.instance.action.hasValue()) ? this.instance.value : Locals.localize("fancymenu.editor.action.screens.manage_screen.info.value.none");
            this.valueComponent = Component.literal(Locals.localize("fancymenu.editor.action.screens.manage_screen.info.value") + " ").setStyle(Style.EMPTY.withColor(TEXT_COLOR_GRAY_1.getRGB())).append(Component.literal(valueString).setStyle(Style.EMPTY.withColor(TEXT_COLOR_GREY_4.getRGB())));

            this.setWidth(this.calculateWidth());
            this.setHeight((lineHeight * 2) + (HEADER_FOOTER_HEIGHT * 2));

        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            super.render(graphics, mouseX, mouseY, partial);

            int centerYLine1 = this.getY() + HEADER_FOOTER_HEIGHT + (this.lineHeight / 2);
            int centerYLine2 = this.getY() + HEADER_FOOTER_HEIGHT + ((this.lineHeight / 2) * 3);

            RenderSystem.enableBlend();

            renderListingDot(graphics, this.getX() + 5, centerYLine1 - 2, LISTING_DOT_RED);
            graphics.drawString(this.font, this.displayNameComponent, (this.getX() + 5 + 4 + 3), (centerYLine1 - (this.font.lineHeight / 2)), -1, false);

            renderListingDot(graphics, this.getX() + 5 + 4 + 3, centerYLine2 - 2, LISTING_DOT_BLUE);
            graphics.drawString(this.font, this.valueComponent, (this.getX() + 5 + 4 + 3 + 4 + 3), (centerYLine2 - (this.font.lineHeight / 2)), -1, false);

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

    public static class ActionInstance {

        public ButtonActionContainer action;
        public String value;

        public ActionInstance(ButtonActionContainer action, @Nullable String value) {
            this.action = action;
            this.value = value;
        }

    }

}
