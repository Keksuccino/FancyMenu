package de.keksuccino.fancymenu.customization.customgui;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfiguratorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.NotificationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ManageCustomGuisScreen extends ConfiguratorScreen {

    protected Runnable onCloseRunnable;
    protected List<CustomGui> guis = new ArrayList<>();
    @Nullable
    protected CustomGui selected;

    public ManageCustomGuisScreen(@NotNull Runnable onClose) {
        super(Component.translatable("fancymenu.custom_guis.manage"));
        for (CustomGui g : CustomGuiHandler.getGuis()) {
            guis.add(g.copy());
        }
        this.onCloseRunnable = onClose;
    }

    @Override
    protected void initCells() {

        for (CustomGui gui : this.guis) {
            this.addCell(new CustomGuiCell(gui))
                    .setLabelYCentered(true)
                    .setSelectable(true);
        }

        this.addSpacerCell(20);

    }

    @Override
    protected void initWidgets() {

        int buttonX = this.doneButton.getX();
        int buttonYOrigin = this.cancelButton.getY() - 15 - 20;

        //Remove GUI
        UIBase.applyDefaultWidgetSkinTo(this.addRenderableWidget(new ExtendedButton(buttonX, buttonYOrigin, this.getRightSideButtonWidth(), 20, Component.translatable("fancymenu.custom_guis.manage.remove"), var1 -> {
            Screen s = Minecraft.getInstance().screen;
            CustomGui selected = this.selected;
            if (selected != null) {
                Minecraft.getInstance().setScreen(NotificationScreen.warning(remove -> {
                    if (remove) this.guis.remove(selected);
                    Minecraft.getInstance().setScreen(s);
                }, LocalizationUtils.splitLocalizedLines("fancymenu.custom_guis.manage.remove.confirm")));
            }
        }))).setIsActiveSupplier(consumes -> this.selected != null);

        //Edit GUI
        UIBase.applyDefaultWidgetSkinTo(this.addRenderableWidget(new ExtendedButton(buttonX, buttonYOrigin - 20 - 5, this.getRightSideButtonWidth(), 20, Component.translatable("fancymenu.custom_guis.manage.edit"), var1 -> {
            Screen s = Minecraft.getInstance().screen;
            CustomGui selected = this.selected;
            if (selected != null) {
                Minecraft.getInstance().setScreen(new BuildCustomGuiScreen(selected, customGui -> {
                    Minecraft.getInstance().setScreen(s);
                }));
            }
        }))).setIsActiveSupplier(consumes -> this.selected != null);

        //New GUI
        UIBase.applyDefaultWidgetSkinTo(this.addRenderableWidget(new ExtendedButton(buttonX, buttonYOrigin - 20 - 5 - 20 - 5, this.getRightSideButtonWidth(), 20, Component.translatable("fancymenu.custom_guis.manage.add"), var1 -> {
            Screen s = Minecraft.getInstance().screen;
            Minecraft.getInstance().setScreen(new BuildCustomGuiScreen(null, customGui -> {
                if (customGui != null) this.guis.add(customGui);
                Minecraft.getInstance().setScreen(s);
            }));
        })));

    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partial) {
        this.selected = this.getSelectedGui();
        super.render(pose, mouseX, mouseY, partial);
    }

    @Override
    protected void onCancel() {
        this.onCloseRunnable.run();
    }

    @Override
    protected void onDone() {
        CustomGuiHandler.CUSTOM_GUI_SCREENS.clear();
        for (CustomGui g : this.guis) {
            if (!g.identifier.replace(" ", "").isEmpty()) CustomGuiHandler.CUSTOM_GUI_SCREENS.put(g.identifier, g);
        }
        CustomGuiHandler.saveChanges();
        this.onCloseRunnable.run();
    }

    @Nullable
    protected CustomGui getSelectedGui() {
        RenderCell cell = this.getSelectedCell();
        if (cell instanceof CustomGuiCell c) {
            return c.gui;
        }
        return null;
    }

    public class CustomGuiCell extends LabelCell {

        protected CustomGui gui;

        public CustomGuiCell(@NotNull CustomGui gui) {
            super(Component.literal(gui.identifier));
            this.gui = gui;
        }

    }

}
