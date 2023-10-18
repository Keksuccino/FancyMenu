package de.keksuccino.drippyloadingscreen.mixin.mixins.client;

import de.keksuccino.drippyloadingscreen.customization.DrippyOverlayScreen;
import de.keksuccino.drippyloadingscreen.customization.deepcustomization.overlay.background.OverlayBackgroundItem;
import de.keksuccino.drippyloadingscreen.customization.deepcustomization.overlay.background.OverlayBackgroundLayoutElement;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorUI;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutElement;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(LayoutEditorUI.LayoutPropertiesContextMenu.class)
public class MixinLayoutPropertiesContextMenu {

    private static final Logger LOGGER = LogManager.getLogger();

    @Inject(method = "openMenuAt", at = @At(value = "JUMP", ordinal = 0), remap = false)
    private void beforeAddingButtonsToContextMenu(int x, int y, int screenWidth, int screenHeight, CallbackInfo info) {

        LayoutEditorUI.LayoutPropertiesContextMenu context = ((LayoutEditorUI.LayoutPropertiesContextMenu)((Object)this));

        if ((Minecraft.getInstance().screen == null) || !(Minecraft.getInstance().screen instanceof LayoutEditorScreen)) {
            LOGGER.error("[DRIPPY LOADING SCREEN] Unable to add background color button! Current screen wasn't instance of LayoutEditorScreen!");
            return;
        }
        LayoutEditorScreen editor = (LayoutEditorScreen) Minecraft.getInstance().screen;
        if (!(editor.screen instanceof DrippyOverlayScreen)) {
            return;
        }

        AdvancedButton setBackgroundColorButton = new AdvancedButton(0, 0, 0, 16, I18n.get("drippyloadingscreen.deepcustomization.overlay.background.set_color"), true, (press) -> {
            OverlayBackgroundItem i = getBackgroundItem();
            if (i != null) {
                FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), I18n.get("drippyloadingscreen.deepcustomization.overlay.background.set_color"), null, 240, (call) -> {
                    if (call != null) {
                        if (call.replace(" ", "").equals("") || call.replace(" ", "").equalsIgnoreCase("#RRGGBB")) {
                            if (i.hexColor != null) {
                                ((LayoutEditorScreen)Minecraft.getInstance().screen).history.saveSnapshot(((LayoutEditorScreen)Minecraft.getInstance().screen).history.createSnapshot());
                            }
                            i.hexColor = null;
                            i.hexColorString = "#RRGGBB";
                        } else {
                            if (!call.equalsIgnoreCase(i.hexColorString)) {
                                Color c = RenderUtils.getColorFromHexString(call);
                                if (c != null) {
                                    ((LayoutEditorScreen)Minecraft.getInstance().screen).history.saveSnapshot(((LayoutEditorScreen)Minecraft.getInstance().screen).history.createSnapshot());
                                    i.hexColorString = call;
                                    i.hexColor = c;
                                }
                            }
                        }
                    }
                });
                if (i.hexColorString != null) {
                    p.setText(i.hexColorString);
                }
                PopupHandler.displayPopup(p);
            }
        });
        setBackgroundColorButton.setDescription(StringUtils.splitLines(I18n.get("drippyloadingscreen.deepcustomization.overlay.background.set_color.desc"), "\n"));
        context.addContent(setBackgroundColorButton);

        context.addSeparator();

    }

    @Nullable
    private OverlayBackgroundItem getBackgroundItem() {
        if ((Minecraft.getInstance().screen == null) || !(Minecraft.getInstance().screen instanceof LayoutEditorScreen)) {
            LOGGER.error("[DRIPPY LOADING SCREEN] Unable to get background customization item! Current screen wasn't instance of LayoutEditorScreen!");
            return null;
        }
        LayoutEditorScreen s = (LayoutEditorScreen) Minecraft.getInstance().screen;
        for (LayoutElement e : s.getContent()) {
            if (e instanceof OverlayBackgroundLayoutElement) {
                return (OverlayBackgroundItem) e.object;
            }
        }
        return null;
    }

}
