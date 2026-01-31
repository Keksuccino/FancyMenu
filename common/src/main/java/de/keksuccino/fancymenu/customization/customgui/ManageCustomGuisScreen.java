package de.keksuccino.fancymenu.customization.customgui;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPCellWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ManageCustomGuisScreen extends PiPCellWindowBody {

    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 420;
    private static final int LIST_ENTRY_TOP_DOWN_BORDER = 1;

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
        this.setSearchBarEnabled(true);
    }

    @Override
    protected void initCells() {

        this.addSpacerCell(5).setIgnoreSearch();

        for (CustomGui gui : this.guis) {
            this.addCell(new CustomGuiCell(gui)).setSelectable(true);
        }

        this.addSpacerCell(5).setIgnoreSearch();

    }

    @Override
    protected void init() {
        this.selected = null;
        super.init();
    }

    @Override
    protected void initRightSideWidgets() {

        this.addRightSideButton(20, Component.translatable("fancymenu.custom_guis.manage.add"), var1 -> {
            BuildCustomGuiScreen screen = new BuildCustomGuiScreen(null, customGui -> {
                if (customGui != null) {
                    this.guis.add(customGui);
                    this.rebuild();
                }
            });
            this.openChildWindow(parentWindow -> BuildCustomGuiScreen.openInWindow(screen, parentWindow));
        });

        this.addRightSideDefaultSpacer();

        this.addRightSideButton(20, Component.translatable("fancymenu.custom_guis.manage.open"), var1 -> {
            CustomGui selected = this.selected;
            if (selected != null) {
                CustomGuiHandler.CUSTOM_GUI_SCREENS.clear();
                for (CustomGui g : this.guis) {
                    if (!g.identifier.replace(" ", "").isEmpty()) CustomGuiHandler.CUSTOM_GUI_SCREENS.put(g.identifier, g);
                }
                CustomGuiHandler.saveChanges();
                Minecraft.getInstance().setScreen(CustomGuiHandler.constructInstance(selected, Minecraft.getInstance().screen, null));
            }
        }).setIsActiveSupplier(consumes -> this.selected != null);

        this.addRightSideButton(20, Component.translatable("fancymenu.custom_guis.manage.edit"), var1 -> {
            CustomGui selected = this.selected;
            if (selected != null) {
                BuildCustomGuiScreen screen = new BuildCustomGuiScreen(selected, customGui -> {
                    if (customGui != null) {
                        this.rebuild();
                    }
                });
                this.openChildWindow(parentWindow -> BuildCustomGuiScreen.openInWindow(screen, parentWindow));
            }
        }).setIsActiveSupplier(consumes -> this.selected != null);

        this.addRightSideButton(20, Component.translatable("fancymenu.custom_guis.manage.remove"), var1 -> {
            CustomGui selected = this.selected;
            if (selected != null) {
                Dialogs.openMessageWithCallback(Component.translatable("fancymenu.custom_guis.manage.remove.confirm"), MessageDialogStyle.WARNING, remove -> {
                    if (remove) {
                        this.guis.remove(selected);
                        this.selected = null;
                        this.rebuild();
                    }
                });
            }
        }).setIsActiveSupplier(consumes -> this.selected != null);

    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.selected = this.getSelectedGui();
    }

    @Override
    protected void onCancel() {
        this.onCloseRunnable.run();
        this.closeWindow();
    }

    @Override
    protected void onDone() {
        CustomGuiHandler.CUSTOM_GUI_SCREENS.clear();
        for (CustomGui g : this.guis) {
            if (!g.identifier.replace(" ", "").isEmpty()) CustomGuiHandler.CUSTOM_GUI_SCREENS.put(g.identifier, g);
        }
        CustomGuiHandler.saveChanges();
        this.onCloseRunnable.run();
        this.closeWindow();
    }

    @Override
    public void onWindowClosedExternally() {
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

        @Override
        public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            RenderingUtils.resetShaderColor(graphics);
            UIBase.renderText(graphics, this.text, this.getX(), this.getY() + LIST_ENTRY_TOP_DOWN_BORDER);
            RenderingUtils.resetShaderColor(graphics);
        }

        @Override
        protected void updateSize(@NotNull CellScrollEntry scrollEntry) {
            this.setWidth((int)UIBase.getUITextWidthNormal(this.text));
            this.setHeight((int)(UIBase.getUITextHeightNormal() + (LIST_ENTRY_TOP_DOWN_BORDER * 2)));
        }

        @Override
        protected void updatePosition(@NotNull CellScrollEntry scrollEntry) {
            this.setX((int)(scrollEntry.getX() + 5));
            this.setY((int)scrollEntry.getY());
        }

    }

    private void openChildWindow(@NotNull Function<PiPWindow, PiPWindow> opener) {
        PiPWindow parentWindow = this.getWindow();
        PiPWindow childWindow = opener.apply(parentWindow);
        if (parentWindow == null || childWindow == null) {
            return;
        }
        childWindow.setPosition(parentWindow.getX(), parentWindow.getY());
        parentWindow.setVisible(false);
        childWindow.addCloseCallback(() -> parentWindow.setVisible(true));
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ManageCustomGuisScreen screen, @Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(screen.getTitle())
                .setScreen(screen)
                .setForceFancyMenuUiScale(true)
                .setAlwaysOnTop(false)
                .setForceFocus(false)
                .setBlockMinecraftScreenInputs(false)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        return window;
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ManageCustomGuisScreen screen) {
        return openInWindow(screen, null);
    }

}
