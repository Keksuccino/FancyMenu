package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation FM_LOGO_LOCATION = new ResourceLocation("fancymenu", "textures/fancymenu_logo_icon.png");

    @EventListener(priority = -2000)
    public void onInit(InitOrResizeScreenCompletedEvent e) {

//        if (!(e.getScreen() instanceof TitleScreen)) return;
//
//        MenuBar menuBar = new MenuBar();
//
//        ContextMenu menu1 = new ContextMenu();
//        menu1.addClickableEntry("menu_entry_1", Component.literal("Menu Entry 1"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu1.addClickableEntry("menu_entry_2", Component.literal("Menu Entry 2"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu1.addClickableEntry("menu_entry_3", Component.literal("Menu Entry 3"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu1.addClickableEntry("menu_entry_4", Component.literal("Menu Entry 4"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menuBar.addContextMenuEntry("entry_1", Component.literal("Context Entry 1"), menu1)
//                .setIconTexture(WrappedTexture.of(FM_LOGO_LOCATION));
//
//        ContextMenu subMenu1 = new ContextMenu();
//        subMenu1.addClickableEntry("menu_entry_1", Component.literal("Menu Entry 1"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        subMenu1.addClickableEntry("menu_entry_2", Component.literal("Menu Entry 2"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        subMenu1.addClickableEntry("menu_entry_3", Component.literal("Menu Entry 3"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        subMenu1.addClickableEntry("menu_entry_4", Component.literal("Menu Entry 4"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu1.addSubMenuEntry("sub_menu_1", Component.literal("Sub Menu 1"), subMenu1);
//
//        ContextMenu menu2 = new ContextMenu();
//        menu2.addClickableEntry("menu_entry_1", Component.literal("Menu Entry 1"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu2.addClickableEntry("menu_entry_2", Component.literal("Menu Entry 2"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu2.addClickableEntry("menu_entry_3", Component.literal("Menu Entry 3"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu2.addClickableEntry("menu_entry_4", Component.literal("Menu Entry 4"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menuBar.addContextMenuEntry("entry_2", Component.literal("Context Entry 2 longer"), menu2);
//
//        ContextMenu menu3 = new ContextMenu();
//        menu3.addClickableEntry("menu_entry_1", Component.literal("Menu Entry 1"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu3.addClickableEntry("menu_entry_2", Component.literal("Menu Entry 2"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu3.addClickableEntry("menu_entry_3", Component.literal("Menu Entry 3"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu3.addClickableEntry("menu_entry_4", Component.literal("Menu Entry 4"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu3.addClickableEntry("menu_entry_5", Component.literal("Menu Entry 5"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menuBar.addContextMenuEntry("entry_3", Component.literal("3 lul"), menu3);
//
//        ContextMenu menu4 = new ContextMenu();
//        menu4.addClickableEntry("menu_entry_1", Component.literal("Menu Entry 1"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu4.addClickableEntry("menu_entry_2", Component.literal("Menu Entry 2"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu4.addClickableEntry("menu_entry_3", Component.literal("Menu Entry 3"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu4.addClickableEntry("menu_entry_4", Component.literal("Menu Entry 4"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu4.addClickableEntry("menu_entry_5", Component.literal("Menu Entry 5"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menuBar.addContextMenuEntry("entry_4", Component.literal("Entry Right 1"), menu4);
//
//        ContextMenu menu5 = new ContextMenu();
//        menu5.addClickableEntry("menu_entry_1", Component.literal("Menu Entry 1"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu5.addClickableEntry("menu_entry_2", Component.literal("Menu Entry 2"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu5.addClickableEntry("menu_entry_3", Component.literal("Menu Entry 3"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu5.addClickableEntry("menu_entry_4", Component.literal("Menu Entry 4"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menu5.addClickableEntry("menu_entry_5", Component.literal("Menu Entry 5"), (menu, entry) -> {
//            LOGGER.info("CLICK");
//        });
//        menuBar.addContextMenuEntry("entry_5", Component.literal("Entry Right 2"), menu5);
//
//        e.addRenderableWidget(menuBar);

    }

}
