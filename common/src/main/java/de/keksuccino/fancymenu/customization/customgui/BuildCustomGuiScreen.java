package de.keksuccino.fancymenu.customization.customgui;

import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPCellWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

public class BuildCustomGuiScreen extends PiPCellWindowBody {

    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 420;

    @NotNull
    protected CustomGui gui;
    @NotNull
    protected CustomGui guiTemp;
    protected Consumer<CustomGui> callback;
    protected boolean allSettingsValid = false;
    @Nullable
    protected String identifierOfEdit;
    protected LabelCell settingsFeedbackCell;

    protected BuildCustomGuiScreen(@Nullable CustomGui guiToEdit, @NotNull Consumer<CustomGui> callback) {
        super(Component.translatable("fancymenu.custom_guis.build"));
        this.gui = (guiToEdit != null) ? guiToEdit : new CustomGui();
        if (guiToEdit != null) this.identifierOfEdit = guiToEdit.identifier;
        this.guiTemp = (guiToEdit != null) ? this.gui.copy() : this.gui;
        this.callback = callback;
    }

    @Override
    protected void initCells() {

        this.addStartEndSpacerCell();

        this.addLabelCell(Component.translatable("fancymenu.custom_guis.build.identifier"));
        this.addTextInputCell(CharacterFilter.buildOnlyLowercaseFileNameFilter(), false, false)
                .setEditListener(s -> this.guiTemp.identifier = s)
                .setText(this.guiTemp.identifier);

        this.addCellGroupEndSpacerCell();

        this.addLabelCell(Component.translatable("fancymenu.custom_guis.build.title"));
        this.addTextInputCell(null, true, true)
                .setEditListener(s -> this.guiTemp.title = s)
                .setText(this.guiTemp.title);

        this.addCellGroupEndSpacerCell();

        this.addWidgetCell(new CycleButton<>(0, 0, 20, 20, CommonCycles.cycleEnabledDisabled("fancymenu.custom_guis.build.allow_esc", this.guiTemp.allowEsc), (value, button) -> {
            this.guiTemp.allowEsc = value.getAsBoolean();
        }), true);

        this.addWidgetCell(new CycleButton<>(0, 0, 20, 20, CommonCycles.cycleEnabledDisabled("fancymenu.custom_guis.build.pause_game", this.guiTemp.pauseGame), (value, button) -> {
            this.guiTemp.pauseGame = value.getAsBoolean();
        }), true);

        this.addWidgetCell(new CycleButton<>(0, 0, 20, 20, CommonCycles.cycleEnabledDisabled("fancymenu.custom_guis.build.world_background", this.guiTemp.worldBackground), (value, button) -> {
            this.guiTemp.worldBackground = value.getAsBoolean();
        }), true);

        this.addWidgetCell(new CycleButton<>(0, 0, 20, 20, CommonCycles.cycleEnabledDisabled("fancymenu.custom_guis.build.world_background_overlay", this.guiTemp.worldBackgroundOverlay), (value, button) -> {
            this.guiTemp.worldBackgroundOverlay = value.getAsBoolean();
        }).setUITooltip(UITooltip.of(Component.translatable("fancymenu.custom_guis.build.world_background_overlay.desc")))
                .setIsActiveSupplier(consumes -> this.guiTemp.worldBackground), true);

        this.addSpacerCell(10);

        this.addWidgetCell(new CycleButton<>(0, 0, 20, 20, CommonCycles.cycleEnabledDisabled("fancymenu.custom_guis.build.popup_mode", this.guiTemp.popupMode), (value, button) -> {
            this.guiTemp.popupMode = value.getAsBoolean();
        }).setUITooltip(UITooltip.of(Component.translatable("fancymenu.custom_guis.build.popup_mode.desc"))), true);

        this.addWidgetCell(new CycleButton<>(0, 0, 20, 20, CommonCycles.cycleEnabledDisabled("fancymenu.custom_guis.build.popup_mode_background_overlay", this.guiTemp.popupModeBackgroundOverlay), (value, button) -> {
            this.guiTemp.popupModeBackgroundOverlay = value.getAsBoolean();
        }).setUITooltip(UITooltip.of(Component.translatable("fancymenu.custom_guis.build.popup_mode_background_overlay.desc")))
                .setIsActiveSupplier(consumes -> this.guiTemp.popupMode), true);

        this.addSpacerCell(10);

        this.settingsFeedbackCell = this.addLabelCell(Component.empty());

        this.addStartEndSpacerCell();

    }

    @Override
    public boolean allowDone() {
        return this.allSettingsValid;
    }

    @Override
    public void renderBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.guiTemp.identifier.isEmpty()) {
            this.settingsFeedbackCell.setText(Component.translatable("fancymenu.custom_guis.build.identifier.invalid").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_text_color.getColorInt())));
            this.allSettingsValid = false;
        } else if (CustomGuiHandler.guiExists(this.guiTemp.identifier) && !Objects.equals(this.guiTemp.identifier, this.identifierOfEdit)) {
            this.settingsFeedbackCell.setText(Component.translatable("fancymenu.custom_guis.build.identifier.already_in_use").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_text_color.getColorInt())));
            this.allSettingsValid = false;
        } else {
            this.settingsFeedbackCell.setText(Component.empty());
            this.allSettingsValid = true;
        }
    }

    @Override
    protected void onCancel() {
        this.callback.accept(null);
        this.closeWindow();
    }

    @Override
    protected void onDone() {
        if (this.allSettingsValid) {
            if (this.gui != this.guiTemp) {
                this.gui.identifier = this.guiTemp.identifier;
                this.gui.title = this.guiTemp.title;
                this.gui.allowEsc = this.guiTemp.allowEsc;
                this.gui.pauseGame = this.guiTemp.pauseGame;
                this.gui.worldBackground = this.guiTemp.worldBackground;
                this.gui.worldBackgroundOverlay = this.guiTemp.worldBackgroundOverlay;
                this.gui.popupMode = this.guiTemp.popupMode;
                this.gui.popupModeBackgroundOverlay = this.guiTemp.popupModeBackgroundOverlay;
            }
            this.callback.accept(this.gui);
            this.closeWindow();
        }
    }

    @Override
    public void onWindowClosedExternally() {
        this.callback.accept(null);
    }

    public static @NotNull PiPWindow openInWindow(@NotNull BuildCustomGuiScreen screen, @Nullable PiPWindow parentWindow) {
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

    public static @NotNull PiPWindow openInWindow(@NotNull BuildCustomGuiScreen screen) {
        return openInWindow(screen, null);
    }

}
