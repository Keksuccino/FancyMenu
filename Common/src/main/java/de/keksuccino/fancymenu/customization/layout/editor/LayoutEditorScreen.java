package de.keksuccino.fancymenu.customization.layout.editor;

import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.customization.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.AdvancedContextMenu;
import de.keksuccino.fancymenu.utils.ScreenTitleUtils;
import de.keksuccino.fancymenu.properties.PropertyContainer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LayoutEditorScreen extends Screen {

	//TODO komplett rewriten

	//TODO alte key press handler weg -> stattdessen die von Screen nutzen

	//TODO GUI event listener methoden von Editor Elementen hier in richtigen methoden callen

	//TODO das meiste der gecachten top-level layout properties von hier in eigene Layout Klasse verschieben

	private static final Logger LOGGER = LogManager.getLogger();

	public static final Screen PLACEHOLDER_SCREEN = new CustomGuiBase("%empty_menu_fancymenu%", "%empty_menu_fancymenu%", false, null, null);

	protected static final List<PropertyContainer> COPIED_ELEMENT_CACHE = new ArrayList<>();
	
	public Screen screenToCustomize;
	public Layout layout;
	public LayoutEditorHistory history = new LayoutEditorHistory(this);
	public LayoutEditorUI ui = new LayoutEditorUI(this);
	public AdvancedContextMenu rightClickContextMenu = new AdvancedContextMenu();
	
	public LayoutEditorScreen(@Nullable Screen screenToCustomize, @Nullable Layout layout) {

		super(Component.literal(""));

		this.screenToCustomize = (screenToCustomize != null) ? screenToCustomize : PLACEHOLDER_SCREEN;

		this.layout = layout;
		if (this.layout == null) {
			this.layout = new Layout();

		}

		Component cachedOriTitle = ScreenCustomizationLayer.cachedOriginalMenuTitles.get(this.screenToCustomize.getClass());
		if (cachedOriTitle != null) {
			ScreenTitleUtils.setScreenTitle(this.screenToCustomize, cachedOriTitle);
		}

	}

	@Override
	protected void init() {

		this.ui.updateUI();

	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

}
