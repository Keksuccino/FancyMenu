package de.keksuccino.fancymenu.customization.scheduler.gui;

import de.keksuccino.fancymenu.customization.scheduler.SchedulerHandler;
import de.keksuccino.fancymenu.customization.scheduler.SchedulerInstance;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedEnumValueCycle;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPCellWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class ManageSchedulerSettingsScreen extends PiPCellWindowBody {

    public static final int PIP_WINDOW_WIDTH = 520;
    public static final int PIP_WINDOW_HEIGHT = 360;

    @NotNull
    private final SchedulerInstance instance;
    @NotNull
    private final Predicate<String> identifierAvailability;
    @NotNull
    private final Consumer<Boolean> callback;

    private TextInputCell identifierCell;
    private TextInputCell startDelayCell;
    private TextInputCell tickDelayCell;
    private TextInputCell ticksToRunCell;
    private LocalizedEnumValueCycle<CommonCycles.CycleEnabledDisabled> startOnLaunchCycle;

    public ManageSchedulerSettingsScreen(@NotNull SchedulerInstance instance, @NotNull Predicate<String> identifierAvailability, @NotNull Consumer<Boolean> callback) {
        super(Component.translatable("fancymenu.schedulers.settings.title"));
        this.instance = instance;
        this.identifierAvailability = identifierAvailability;
        this.callback = callback;
        this.setAllowCloseOnEsc(false);
    }

    @Override
    protected void initCells() {

        this.addStartEndSpacerCell();

        this.addLabelCell(Component.translatable("fancymenu.schedulers.settings.id"));
        this.identifierCell = this.addTextInputCell(CharacterFilter.buildResourceNameFilter(), false, false)
                .setText(this.instance.getIdentifier());
        this.identifierCell.editBox.setUITooltip(() -> UITooltip.of(Component.translatable("fancymenu.schedulers.settings.id.desc")));

        this.addCellGroupEndSpacerCell();

        this.addLabelCell(Component.translatable("fancymenu.schedulers.settings.start_delay"));
        this.startDelayCell = this.addTextInputCell(CharacterFilter.buildIntegerFilter(), false, false)
                .setText(String.valueOf(this.instance.getStartDelayMs()));
        this.startDelayCell.editBox.setUITooltip(() -> UITooltip.of(Component.translatable("fancymenu.schedulers.settings.start_delay.desc")));

        this.addCellGroupEndSpacerCell();

        this.addLabelCell(Component.translatable("fancymenu.schedulers.settings.tick_delay"));
        this.tickDelayCell = this.addTextInputCell(CharacterFilter.buildIntegerFilter(), false, false)
                .setText(String.valueOf(this.instance.getTickDelayMs()));
        this.tickDelayCell.editBox.setUITooltip(() -> UITooltip.of(Component.translatable("fancymenu.schedulers.settings.tick_delay.desc")));

        this.addCellGroupEndSpacerCell();

        this.addLabelCell(Component.translatable("fancymenu.schedulers.settings.ticks_to_run"));
        this.ticksToRunCell = this.addTextInputCell(CharacterFilter.buildIntegerFilter(), false, false)
                .setText(String.valueOf(this.instance.getTicksToRun()));
        this.ticksToRunCell.editBox.setUITooltip(() -> UITooltip.of(Component.translatable("fancymenu.schedulers.settings.ticks_to_run.desc")));

        this.addCellGroupEndSpacerCell();

        this.startOnLaunchCycle = CommonCycles.cycleEnabledDisabled("fancymenu.schedulers.settings.start_on_launch", this.instance.isStartOnLaunch());
        this.addCycleButtonCell(this.startOnLaunchCycle, true, (value, button) -> {
        });

    }

    @Override
    protected void onCancel() {
        this.callback.accept(false);
        this.closeWindow();
    }

    @Override
    protected void onDone() {
        if (!this.applyChanges()) {
            return;
        }
        this.callback.accept(true);
        this.closeWindow();
    }

    @Override
    public void onWindowClosedExternally() {
        this.callback.accept(false);
    }

    private boolean applyChanges() {

        String newIdentifier = (this.identifierCell != null) ? this.identifierCell.getText() : this.instance.getIdentifier();
        if (newIdentifier == null) newIdentifier = "";
        newIdentifier = newIdentifier.trim();

        if (!SchedulerHandler.isIdentifierValid(newIdentifier)) {
            Dialogs.openMessage(Component.translatable("fancymenu.schedulers.settings.invalid_id"), MessageDialogStyle.ERROR);
            return false;
        }

        if (!this.identifierAvailability.test(newIdentifier)) {
            Dialogs.openMessage(Component.translatable("fancymenu.schedulers.settings.id_in_use"), MessageDialogStyle.ERROR);
            return false;
        }

        Long startDelay = parseLongInput(this.startDelayCell);
        Long tickDelay = parseLongInput(this.tickDelayCell);
        Long ticksToRun = parseLongInput(this.ticksToRunCell);
        if (startDelay == null || tickDelay == null || ticksToRun == null) {
            Dialogs.openMessage(Component.translatable("fancymenu.schedulers.settings.invalid_number"), MessageDialogStyle.ERROR);
            return false;
        }

        this.instance.setIdentifier(newIdentifier);
        this.instance.setStartDelayMs(Math.max(0L, startDelay));
        this.instance.setTickDelayMs(Math.max(0L, tickDelay));
        this.instance.setTicksToRun(Math.max(0L, ticksToRun));
        this.instance.setStartOnLaunch(this.startOnLaunchCycle.current().getAsBoolean());

        return true;

    }

    @Nullable
    private static Long parseLongInput(@Nullable TextInputCell cell) {
        if (cell == null) return null;
        String value = cell.getText();
        if (value == null) return null;
        value = value.trim();
        if (!MathUtils.isLong(value)) return null;
        return Long.parseLong(value);
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ManageSchedulerSettingsScreen screen, @Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(screen.getTitle())
                .setScreen(screen)
                .setForceFancyMenuUiScale(true)
                .setBlockMinecraftScreenInputs(false)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        return window;
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ManageSchedulerSettingsScreen screen) {
        return openInWindow(screen, null);
    }

}
