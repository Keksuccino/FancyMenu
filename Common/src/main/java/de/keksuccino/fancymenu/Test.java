package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.event.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.rendering.ui.menubar.v2.MenuBar;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    @EventListener(priority = -2000)
    public void onInit(InitOrResizeScreenCompletedEvent e) {

        if (!(e.getScreen() instanceof TitleScreen)) return;

        MenuBar menuBar = new MenuBar();

        ContextMenu menu1 = new ContextMenu();
        menu1.addClickableEntry("menu_entry_1", Component.literal("Menu Entry 1"), (menu, entry) -> {
            LOGGER.info("CLICK");
        });
        menu1.addClickableEntry("menu_entry_2", Component.literal("Menu Entry 2"), (menu, entry) -> {
            LOGGER.info("CLICK");
        });
        menu1.addClickableEntry("menu_entry_3", Component.literal("Menu Entry 3"), (menu, entry) -> {
            LOGGER.info("CLICK");
        });
        menu1.addClickableEntry("menu_entry_4", Component.literal("Menu Entry 4"), (menu, entry) -> {
            LOGGER.info("CLICK");
        });
        menuBar.addContextMenuEntry(MenuBar.Side.LEFT, "entry_1", Component.literal("Context Entry 1"), menu1);

        ContextMenu subMenu1 = new ContextMenu();
        subMenu1.addClickableEntry("menu_entry_1", Component.literal("Menu Entry 1"), (menu, entry) -> {
            LOGGER.info("CLICK");
        });
        subMenu1.addClickableEntry("menu_entry_2", Component.literal("Menu Entry 2"), (menu, entry) -> {
            LOGGER.info("CLICK");
        });
        subMenu1.addClickableEntry("menu_entry_3", Component.literal("Menu Entry 3"), (menu, entry) -> {
            LOGGER.info("CLICK");
        });
        subMenu1.addClickableEntry("menu_entry_4", Component.literal("Menu Entry 4"), (menu, entry) -> {
            LOGGER.info("CLICK");
        });
        menu1.addSubMenuEntry("sub_menu_1", Component.literal("Sub Menu 1"), subMenu1);

        ContextMenu menu2 = new ContextMenu();
        menu2.addClickableEntry("menu_entry_1", Component.literal("Menu Entry 1"), (menu, entry) -> {
            LOGGER.info("CLICK");
        });
        menu2.addClickableEntry("menu_entry_2", Component.literal("Menu Entry 2"), (menu, entry) -> {
            LOGGER.info("CLICK");
        });
        menu2.addClickableEntry("menu_entry_3", Component.literal("Menu Entry 3"), (menu, entry) -> {
            LOGGER.info("CLICK");
        });
        menu2.addClickableEntry("menu_entry_4", Component.literal("Menu Entry 4"), (menu, entry) -> {
            LOGGER.info("CLICK");
        });
        menuBar.addContextMenuEntry(MenuBar.Side.LEFT, "entry_2", Component.literal("Context Entry 2 longer"), menu2);

        ContextMenu menu3 = new ContextMenu();
        menu3.addClickableEntry("menu_entry_1", Component.literal("Menu Entry 1"), (menu, entry) -> {
            LOGGER.info("CLICK");
        });
        menu3.addClickableEntry("menu_entry_2", Component.literal("Menu Entry 2"), (menu, entry) -> {
            LOGGER.info("CLICK");
        });
        menu3.addClickableEntry("menu_entry_3", Component.literal("Menu Entry 3"), (menu, entry) -> {
            LOGGER.info("CLICK");
        });
        menu3.addClickableEntry("menu_entry_4", Component.literal("Menu Entry 4"), (menu, entry) -> {
            LOGGER.info("CLICK");
        });
        menu3.addClickableEntry("menu_entry_5", Component.literal("Menu Entry 5"), (menu, entry) -> {
            LOGGER.info("CLICK");
        });
        menuBar.addContextMenuEntry(MenuBar.Side.LEFT, "entry_3", Component.literal("3 lul"), menu3);

        e.addRenderableWidget(menuBar);

    }

}
