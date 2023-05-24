package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.event.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.event.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinScreen;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    private ContextMenu contextMenu;

    @EventListener(priority = -2000)
    public void onInit(InitOrResizeScreenCompletedEvent e) {
        if (contextMenu != null) {
            contextMenu.closeMenu();
        }
        contextMenu = new ContextMenu();
        contextMenu.addClickableEntry("entry_1", Component.literal("Entry 1"), ((menu, entry) -> LOGGER.info("CLICK")));
        contextMenu.addClickableEntry("entry_2", Component.literal("Entry 2 loooooong iuiuiuiuiuiuiu"), ((menu, entry) -> LOGGER.info("CLICK"))).setShortcutTextSupplier((menu, entry) -> Component.literal("Ctrl+Alt+C"));
        contextMenu.addClickableEntry("entry_3", Component.literal("Entry 3"), ((menu, entry) -> LOGGER.info("CLICK")));
        contextMenu.addSeparatorEntry("entry_4");
        ContextMenu subMenu1 = new ContextMenu();
        subMenu1.addClickableEntry("entry_1", Component.literal("Entry 1"), (menu, entry) -> LOGGER.info("CLICK"));
        subMenu1.addClickableEntry("entry_2", Component.literal("Entry 2"), (menu, entry) -> LOGGER.info("CLICK"));
        ContextMenu subMenu1SubMenu1 = new ContextMenu();
        subMenu1SubMenu1.addClickableEntry("entry_1", Component.literal("Entry 1"), (menu, entry) -> LOGGER.info("CLICK"));
        subMenu1SubMenu1.addClickableEntry("entry_2", Component.literal("Entry 2"), (menu, entry) -> LOGGER.info("CLICK"));
        subMenu1.addEntry(new ContextMenu.SubMenuContextMenuEntry("sub_1", subMenu1, Component.literal("Sub Menu 1"), subMenu1SubMenu1));
        subMenu1.addClickableEntry("entry_3", Component.literal("Entry 3"), (menu, entry) -> LOGGER.info("CLICK")).setShortcutTextSupplier((menu, entry) -> Component.literal("Ctrl+C"));
        contextMenu.addEntry(new ContextMenu.SubMenuContextMenuEntry("sub_1", contextMenu, Component.literal("Sub Menu 1"), subMenu1));
        contextMenu.addClickableEntry("entry_5", Component.literal("Entry 5"), ((menu, entry) -> LOGGER.info("CLICK"))).setShortcutTextSupplier((menu, entry) -> Component.literal("Alt+G")).setIsActiveSupplier((menu, entry) -> false);
        ContextMenu subMenu2 = new ContextMenu();
        subMenu2.addClickableEntry("entry_1", Component.literal("Entry 1"), (menu, entry) -> LOGGER.info("CLICK"));
        subMenu2.addClickableEntry("entry_2", Component.literal("Entry 2"), (menu, entry) -> LOGGER.info("CLICK"));
        subMenu2.addClickableEntry("entry_3", Component.literal("Entry 3"), (menu, entry) -> LOGGER.info("CLICK"));
        contextMenu.addEntry(new ContextMenu.SubMenuContextMenuEntry("sub_2", contextMenu, Component.literal("Sub Menu 2"), subMenu2));
        contextMenu.addClickableEntry("entry_6", Component.literal("Entry 6 i iu reiutiurtiru"), ((menu, entry) -> LOGGER.info("CLICK")));
        ((IMixinScreen)e.getScreen()).invokeAddWidgetFancyMenu(contextMenu);
    }

    @EventListener(priority = -100)
    public void onRenderPost(RenderScreenEvent.Post e) {

        if (e.getScreen() instanceof TitleScreen) {

            if (MouseInput.isRightMouseDown()) {
                this.contextMenu.openMenuAtMouse();
            }

            this.contextMenu.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartial());

        }

    }

}
