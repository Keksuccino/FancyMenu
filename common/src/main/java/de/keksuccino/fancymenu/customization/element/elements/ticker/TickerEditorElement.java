package de.keksuccino.fancymenu.customization.element.elements.ticker;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.actions.ManageActionsScreen;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class TickerEditorElement extends AbstractEditorElement {

    public TickerEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
        this.settings.setAdvancedSizingSupported(false);
        this.settings.setAdvancedPositioningSupported(false);
        this.settings.setFadeable(false);
        this.settings.setStretchable(false);
        this.settings.setInEditorColorSupported(true);
    }

    @Override
    public void init() {

        super.init();

        this.rightClickMenu.addClickableEntry("manage_actions", Component.translatable("fancymenu.actions.screens.manage_screen.manage"), (menu, entry) -> {
            ManageActionsScreen s = new ManageActionsScreen(this.getElement().actionExecutor, (call) -> {
                if (call != null) {
                    this.editor.history.saveSnapshot();
                    this.getElement().actionExecutor = call;
                }
                Minecraft.getInstance().setScreen(this.editor);
            });
            Minecraft.getInstance().setScreen(s);
        }).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.ticker.manage_actions.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("script"))
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("ticker_separator_1");

        this.addLongInputContextMenuEntryTo(this.rightClickMenu, "tick_delay", TickerEditorElement.class,
                        consumes -> consumes.getElement().tickDelayMs,
                        (tickerEditorElement, aLong) -> tickerEditorElement.getElement().tickDelayMs = Math.max(0L, aLong),
                        Component.translatable("fancymenu.elements.ticker.tick_delay"), true, 0L, null, null)
                .setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("timer"))
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.ticker.tick_delay.desc")));

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "set_async",
                        consumes -> (consumes instanceof TickerEditorElement),
                        consumes -> ((TickerElement)consumes.element).isAsync,
                        (element1, aBoolean) -> ((TickerElement)element1.element).isAsync = aBoolean,
                        "fancymenu.elements.ticker.async")
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.ticker.async.desc")));

        this.addGenericCycleContextMenuEntryTo(this.rightClickMenu, "set_tick_mode",
                        ListUtils.of(TickerElement.TickMode.NORMAL, TickerElement.TickMode.ONCE_PER_SESSION, TickerElement.TickMode.ON_MENU_LOAD),
                        consumes -> (consumes instanceof TickerEditorElement),
                        consumes -> ((TickerElement)consumes.element).tickMode,
                        (element, mode) -> ((TickerElement)element.element).tickMode = mode,
                        (menu, entry, switcherValue) -> {
                            if (switcherValue == TickerElement.TickMode.NORMAL) {
                                return Component.translatable("fancymenu.elements.ticker.tick_mode.normal");
                            }
                            if (switcherValue == TickerElement.TickMode.ONCE_PER_SESSION) {
                                return Component.translatable("fancymenu.elements.ticker.tick_mode.once_per_session");
                            }
                            return Component.translatable("fancymenu.elements.ticker.tick_mode.on_menu_load");
                        })
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.ticker.tick_mode.desc")));

    }

    protected TickerElement getElement() {
        return (TickerElement) this.element;
    }

}
