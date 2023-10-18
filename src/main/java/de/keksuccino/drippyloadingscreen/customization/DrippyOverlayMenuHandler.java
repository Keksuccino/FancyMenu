package de.keksuccino.drippyloadingscreen.customization;

import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.drippyloadingscreen.customization.deepcustomization.overlay.background.OverlayBackgroundItem;
import de.keksuccino.drippyloadingscreen.customization.deepcustomization.overlay.logo.OverlayLogoItem;
import de.keksuccino.drippyloadingscreen.customization.deepcustomization.overlay.progressbar.OverlayProgressBarItem;
import de.keksuccino.fancymenu.events.*;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayer;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayerRegistry;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;

public class DrippyOverlayMenuHandler extends MenuHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger();

    public Color customBackgroundColor = null;

    public OverlayProgressBarItem progressBarItem = null;
    public OverlayLogoItem logoItem = null;

    public DrippyOverlayMenuHandler() {
        super(DrippyOverlayScreen.class.getName());
        this.forceDisableCustomMenuTitle = true;
    }

    @Override
    @SubscribeEvent
    public void onButtonsCached(ButtonCachedEvent e) {
        if (this.shouldCustomize(e.getGui())) {
            if (MenuCustomization.isMenuCustomizable(e.getGui())) {

                try {

                    //Reset all deep customization fields
                    this.customBackgroundColor = null;
                    this.logoItem = (OverlayLogoItem) DeepCustomizationLayerRegistry.getLayerByMenuIdentifier(this.getMenuIdentifier()).getElementByIdentifier("drippy_overlay_logo").constructDefaultItemInstance();
                    this.progressBarItem = (OverlayProgressBarItem) DeepCustomizationLayerRegistry.getLayerByMenuIdentifier(this.getMenuIdentifier()).getElementByIdentifier("drippy_overlay_progress_bar").constructDefaultItemInstance();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                super.onButtonsCached(e);

            }
        }
    }

    @Override
    protected void applyLayout(PropertiesSection sec, String renderOrder, ButtonCachedEvent e) {

        String action = sec.getEntryValue("action");

        //Disable unsupported element types in Drippy layouts
        if (action.equals("custom_layout_element:fancymenu_customization_player_entity")) {
            return;
        }
        if (action.equals("custom_layout_element:fancymenu_extension:audio_item")) {
            return;
        }

        super.applyLayout(sec, renderOrder, e);

        DeepCustomizationLayer layer = DeepCustomizationLayerRegistry.getLayerByMenuIdentifier(this.getMenuIdentifier());
        if (layer != null) {

            if (action != null) {

                if (action.startsWith("deep_customization_element:")) {
                    String elementId = action.split("[:]", 2)[1];
                    DeepCustomizationElement element = layer.getElementByIdentifier(elementId);
                    if (element != null) {
                        DeepCustomizationItem i = element.constructCustomizedItemInstance(sec);
                        if (i != null) {

                            if (elementId.equals("drippy_overlay_logo")) {
                                this.logoItem = (OverlayLogoItem) i;
                            }
                            if (elementId.equals("drippy_overlay_progress_bar")) {
                                this.progressBarItem = (OverlayProgressBarItem) i;
                            }
                            if (elementId.equals("drippy_overlay_background")) {
                                this.customBackgroundColor = ((OverlayBackgroundItem)i).hexColor;
                            }

                        }
                    }
                }

            }

        }

    }

    @Override
    protected void renderBackground(GuiGraphics graphics, Screen s) {
        super.renderBackground(graphics, s);
        if (Minecraft.getInstance().getOverlay() == null) {
            if (this.shouldCustomize(s)) {
                if (!MenuCustomization.isMenuCustomizable(s)) {
                    return;
                }
                if ((this.logoItem != null) && !this.logoItem.hidden) {
                    this.logoItem.render(graphics, s);
                }
                if ((this.progressBarItem != null) && !this.progressBarItem.hidden) {
                    this.progressBarItem.render(graphics, s);
                }
            }
        }
    }

    @Override
    @SubscribeEvent
    public void onSoftReload(SoftMenuReloadEvent e) {
        super.onSoftReload(e);
    }

    @Override
    @SubscribeEvent
    public void onMenuReloaded(MenuReloadedEvent e) {
        super.onMenuReloaded(e);
    }

    @Override
    @SubscribeEvent
    public void onInitPre(InitOrResizeScreenEvent.Pre e) {
        super.onInitPre(e);
    }

    @Override
    @SubscribeEvent
    public void onRenderPre(RenderScreenEvent.Pre e) {
        super.onRenderPre(e);
    }

    @Override
    @SubscribeEvent
    public void onRenderPost(RenderScreenEvent.Post e) {
        super.onRenderPost(e);
    }

    @Override
    @SubscribeEvent
    public void drawToBackground(ScreenBackgroundRenderedEvent e) {
        super.drawToBackground(e);
    }

    @Override
    @SubscribeEvent
    public void onButtonClickSound(PlayWidgetClickSoundEvent.Pre e) {
        super.onButtonClickSound(e);
    }

    @Override
    @SubscribeEvent
    public void onButtonRenderBackground(RenderWidgetBackgroundEvent.Pre e) {
        super.onButtonRenderBackground(e);
    }

    @Override
    @SubscribeEvent
    public void onRenderListBackground(RenderListBackgroundEvent.Post e) {
        super.onRenderListBackground(e);
    }

}
