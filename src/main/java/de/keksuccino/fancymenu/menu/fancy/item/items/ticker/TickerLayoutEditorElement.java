//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.item.items.ticker;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.api.item.LayoutEditorElement;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.buttonactions.ButtonActionScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;

import java.awt.*;

public class TickerLayoutEditorElement extends LayoutEditorElement {

    public TickerLayoutEditorElement(TickerCustomizationItemContainer parentContainer, TickerCustomizationItem customizationItemInstance, LayoutEditorScreen handler) {
        super(parentContainer, customizationItemInstance, true, handler, true);
    }

    @Override
    public void init() {

        this.supportsAdvancedPositioning = false;
        this.supportsAdvancedSizing = false;

        super.init();

        TickerCustomizationItem i = ((TickerCustomizationItem)this.object);

        AdvancedButton addActionButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.ticker.add_action"), (press) -> {
            ButtonActionScreen s = new ButtonActionScreen(this.handler, (call) -> {
                if (call != null) {
                    this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                    i.actions.add(new TickerCustomizationItem.ActionContainer(call.get(0), call.get(1)));
                }
            });
            Minecraft.getInstance().setScreen(s);
        });
        addActionButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.ticker.add_action.desc"), "%n%"));
        this.rightclickMenu.addContent(addActionButton);

        AdvancedButton manageActionsButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.ticker.manage_actions"), (press) -> {
            ManageTickerActionsScreen s = new ManageTickerActionsScreen(this.handler, this);
            Minecraft.getInstance().setScreen(s);
        });
        manageActionsButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.ticker.manage_actions.desc"), "%n%"));
        this.rightclickMenu.addContent(manageActionsButton);

        this.rightclickMenu.addSeparator();

        AdvancedButton tickDelayButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.ticker.tick_delay"), (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0), Locals.localize("fancymenu.customization.items.ticker.tick_delay"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
                if (call != null) {
                    if (MathUtils.isLong(call)) {
                        long delay = Long.parseLong(call);
                        if (i.tickDelayMs != delay) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.tickDelayMs = delay;
                    } else {
                        if (call.replace(" ", "").equals("")) {
                            if (i.tickDelayMs != 0) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            i.tickDelayMs = 0;
                        }
                    }
                }
            });
            p.setText("" + i.tickDelayMs);
            PopupHandler.displayPopup(p);
        });
        tickDelayButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.ticker.tick_delay.desc"), "%n%"));
        this.rightclickMenu.addContent(tickDelayButton);

        AdvancedButton asyncButton = new AdvancedButton(0, 0, 0, 0, "", (press) -> {
            if (i.isAsync) {
                i.isAsync = false;
            } else {
                i.isAsync = true;
            }
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (i.isAsync) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.ticker.async.on"));
                } else {
                    this.setMessage(Locals.localize("fancymenu.customization.items.ticker.async.off"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        asyncButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.ticker.async.desc"), "%n%"));
        this.rightclickMenu.addContent(asyncButton);

        AdvancedButton oneTickModeButton = new AdvancedButton(0, 0, 0, 0, "", (press) -> {
            if (i.tickMode == TickerCustomizationItem.TickMode.NORMAL) {
                i.tickMode = TickerCustomizationItem.TickMode.ONCE_PER_SESSION;
            } else if (i.tickMode == TickerCustomizationItem.TickMode.ONCE_PER_SESSION){
                i.tickMode = TickerCustomizationItem.TickMode.ON_MENU_LOAD;
            } else if (i.tickMode == TickerCustomizationItem.TickMode.ON_MENU_LOAD) {
                i.tickMode = TickerCustomizationItem.TickMode.NORMAL;
            }
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (i.tickMode == TickerCustomizationItem.TickMode.NORMAL) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.ticker.tick_mode.normal"));
                } else if (i.tickMode == TickerCustomizationItem.TickMode.ONCE_PER_SESSION){
                    this.setMessage(Locals.localize("fancymenu.customization.items.ticker.tick_mode.once_per_session"));
                } else if (i.tickMode == TickerCustomizationItem.TickMode.ON_MENU_LOAD) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.ticker.tick_mode.on_menu_load"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        oneTickModeButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.ticker.tick_mode.desc"), "%n%"));
        this.rightclickMenu.addContent(oneTickModeButton);

    }

    @Override
    public SimplePropertiesSection serializeItem() {

        TickerCustomizationItem i = ((TickerCustomizationItem)this.object);

        SimplePropertiesSection sec = new SimplePropertiesSection();

        sec.addEntry("is_async", "" + i.isAsync);
        sec.addEntry("tick_delay", "" + i.tickDelayMs);
        sec.addEntry("tick_mode", "" + i.tickMode.name);
        int index = 0;
        for (TickerCustomizationItem.ActionContainer c : i.actions) {
            String v = c.value;
            if (v == null) {
                v = "";
            }
            sec.addEntry("tickeraction_" + index + "_" + c.action, v);
            index++;
        }

        return sec;

    }

}
