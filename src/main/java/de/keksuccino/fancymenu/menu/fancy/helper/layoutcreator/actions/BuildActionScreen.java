//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.actions;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import de.keksuccino.fancymenu.api.buttonaction.ButtonActionRegistry;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorScreen;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class BuildActionScreen extends Screen {

    //TODO add new ManageActionsScreen
    //TODO add new ManageActionsScreen
    //TODO add new ManageActionsScreen
    //TODO add new ManageActionsScreen
    //TODO add new ManageActionsScreen

    protected Screen parentScreen;
    protected final ActionInstance instance;
    protected Consumer<ActionInstance> callback;

    protected ScrollArea actionsListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ScrollArea actionDescriptionScrollArea = new ScrollArea(0, 0, 0, 0);
    protected AdvancedButton editValueButton;
    protected AdvancedButton doneButton;
    protected AdvancedButton cancelButton;

    public BuildActionScreen(@Nullable Screen parentScreen, @Nullable Component title, @Nullable ActionInstance instanceToEdit, @NotNull Consumer<ActionInstance> callback) {

        super((title == null) ? Component.literal("") : title);

        this.parentScreen = parentScreen;
        this.instance = (instanceToEdit != null) ? instanceToEdit : new ActionInstance(null, null);
        this.callback = callback;
        this.setContentOfActionsList();

        //Select correct entry if instance has action
        if (this.instance.action != null) {
            for (ScrollAreaEntry e : this.actionsListScrollArea.getEntries()) {
                if ((e instanceof ActionScrollEntry) && (((ActionScrollEntry)e).action == this.instance.action)) {
                    e.setFocused(true);
                    break;
                }
            }
        }

        this.editValueButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.editor.action.screens.build_screen.edit_value"), true, (button) -> {
            TextEditorScreen s = new TextEditorScreen(Component.literal(this.instance.action.getValueDescription()), this, null, (call) -> {
                if (call != null) {
                    this.instance.value = call;
                }
            });
            if ((this.instance.action != null) && (this.instance.action.getValueFormattingRules() != null)) {
                s.formattingRules.addAll(this.instance.action.getValueFormattingRules());
            }
            s.multilineMode = false;
            if (this.instance.value != null) {
                s.setText(this.instance.value);
            } else if (this.instance.action != null) {
                s.setText(this.instance.action.getValueExample());
            }
            Minecraft.getInstance().setScreen(s);
        }) {
            @Override
            public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                ButtonActionContainer b = BuildActionScreen.this.instance.action;
                if ((b != null) && !b.hasValue()) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.build_screen.edit_value.desc.no_value"), "%n%"));
                } else {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.build_screen.edit_value.desc.normal"), "%n%"));
                }
                if ((b == null) || !b.hasValue()) {
                    this.active = false;
                } else {
                    this.active = true;
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.editValueButton);

        this.doneButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.guicomponents.done"), true, (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(this.instance);
        }) {
            @Override
            public void renderButton(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
                if (BuildActionScreen.this.instance.action == null) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.build_screen.finish.no_action_selected"), "%n%"));
                    this.active = false;
                } else if ((BuildActionScreen.this.instance.value == null) && BuildActionScreen.this.instance.action.hasValue()) {
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.action.screens.build_screen.finish.no_value_set"), "%n%"));
                    this.active = false;
                } else {
                    this.setDescription((String[])null);
                    this.active = true;
                }
                super.renderButton(matrix, mouseX, mouseY, partialTicks);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.doneButton);

        this.cancelButton = new AdvancedButton(0, 0, 150, 20, Locals.localize("fancymenu.guicomponents.cancel"), true, (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(null);
        });
        UIBase.applyDefaultButtonSkinTo(this.cancelButton);

    }

    @Override
    protected void init() {

        //Reset the GUI scale in case the layout editor changed it
        Minecraft.getInstance().getWindow().setGuiScale(Minecraft.getInstance().getWindow().calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode()));
        this.height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        this.width = Minecraft.getInstance().getWindow().getGuiScaledWidth();

        super.init();

        this.setDescription(this.instance.action);

    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parentScreen);
        this.callback.accept(null);
    }

    @Override
    public void render(@NotNull PoseStack matrix, int mouseX, int mouseY, float partial) {

        fill(matrix, 0, 0, this.width, this.height, UIBase.SCREEN_BACKGROUND_COLOR.getRGB());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        this.font.draw(matrix, titleComp, 20, 20, -1);

        this.font.draw(matrix, Locals.localize("fancymenu.editor.action.screens.build_screen.available_actions"), 20, 50, -1);

        this.actionsListScrollArea.setWidth((this.width / 2) - 40, true);
        this.actionsListScrollArea.setHeight(this.height - 85, true);
        this.actionsListScrollArea.setX(20, true);
        this.actionsListScrollArea.setY(50 + 15, true);
        this.actionsListScrollArea.render(matrix, mouseX, mouseY, partial);

        String descLabelString = Locals.localize("fancymenu.editor.action.screens.build_screen.action_description");
        int descLabelWidth = this.font.width(descLabelString);
        this.font.draw(matrix, descLabelString, this.width - 20 - descLabelWidth, 50, -1);

        this.actionDescriptionScrollArea.setWidth((this.width / 2) - 40, true);
        this.actionDescriptionScrollArea.setHeight(Math.max(40, (this.height / 2) - 50 - 25), true);
        this.actionDescriptionScrollArea.setX(this.width - 20 - this.actionDescriptionScrollArea.getWidthWithBorder(), true);
        this.actionDescriptionScrollArea.setY(50 + 15, true);
        this.actionDescriptionScrollArea.render(matrix, mouseX, mouseY, partial);

        this.doneButton.setX(this.width - 20 - this.doneButton.getWidth());
        this.doneButton.setY(this.height - 20 - 20);
        this.doneButton.render(matrix, mouseX, mouseY, partial);

        this.cancelButton.setX(this.width - 20 - this.cancelButton.getWidth());
        this.cancelButton.setY(this.doneButton.getY() - 5 - 20);
        this.cancelButton.render(matrix, mouseX, mouseY, partial);

        this.editValueButton.setX(this.width - 20 - this.editValueButton.getWidth());
        this.editValueButton.setY(this.cancelButton.getY() - 15 - 20);
        this.editValueButton.render(matrix, mouseX, mouseY, partial);

        super.render(matrix, mouseX, mouseY, partial);

    }

    protected void setDescription(@Nullable ButtonActionContainer action) {

        this.actionDescriptionScrollArea.clearEntries();

        if ((action != null) && (action.getActionDescription() != null)) {
            for (String s : StringUtils.splitLines(action.getActionDescription(), "%n%")) {
                TextScrollAreaEntry e = new TextScrollAreaEntry(this.actionDescriptionScrollArea, Component.literal(s), (entry) -> {});
                e.setFocusable(false);
                e.setBackgroundColorHover(e.getBackgroundColorIdle());
                e.setPlayClickSound(false);
                this.actionDescriptionScrollArea.addEntry(e);
            }
        }

    }

    protected void setContentOfActionsList() {

        this.actionsListScrollArea.clearEntries();

        for (ButtonActionContainer c : ButtonActionRegistry.getActions()) {
            ActionScrollEntry e = new ActionScrollEntry(this.actionsListScrollArea, c, (entry) -> {
                this.instance.action = c;
                this.setDescription(c);
            });
            this.actionsListScrollArea.addEntry(e);
        }

    }

    public static class ActionScrollEntry extends TextListScrollAreaEntry {

        public ButtonActionContainer action;

        public ActionScrollEntry(ScrollArea parent, @NotNull ButtonActionContainer action, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, Component.literal(action.getAction()).setStyle(Style.EMPTY.withColor(TEXT_COLOR_GRAY_1.getRGB())), LISTING_DOT_BLUE, onClick);
            this.action = action;
        }

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
