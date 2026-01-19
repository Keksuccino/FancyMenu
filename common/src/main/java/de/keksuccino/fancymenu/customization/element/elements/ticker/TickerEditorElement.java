package de.keksuccino.fancymenu.customization.element.elements.ticker;

import de.keksuccino.fancymenu.customization.action.ui.ActionScriptEditorWindowBody;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class TickerEditorElement extends AbstractEditorElement<TickerEditorElement, TickerElement> {

    public TickerEditorElement(@NotNull TickerElement element, @NotNull LayoutEditorScreen editor) {
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
            ActionScriptEditorWindowBody s = new ActionScriptEditorWindowBody(this.element.actionExecutor, (call) -> {
                if (call != null) {
                    this.editor.history.saveSnapshot();
                    this.element.actionExecutor = call;
                }
            });
            menu.closeMenuChain();
            ActionScriptEditorWindowBody.openInWindow(s);
        }).setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.ticker.manage_actions.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("script"))
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("ticker_separator_1");

        this.addLongInputContextMenuEntryTo(this.rightClickMenu, "tick_delay", TickerEditorElement.class,
                        consumes -> consumes.element.tickDelayMs,
                        (tickerEditorElement, aLong) -> tickerEditorElement.element.tickDelayMs = Math.max(0L, aLong),
                        Component.translatable("fancymenu.elements.ticker.tick_delay"), true, 0L, null, null)
                .setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("timer"))
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.ticker.tick_delay.desc")));

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "set_async",
                        TickerEditorElement.class,
                        element -> element.element.isAsync,
                        (element, aBoolean) -> element.element.isAsync = aBoolean,
                        "fancymenu.elements.ticker.async")
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.ticker.async.desc")));

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
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.ticker.tick_mode.desc")));

    }


}
