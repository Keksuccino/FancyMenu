
package de.keksuccino.fancymenu.menu.fancy.item.items.ticker;

import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import de.keksuccino.fancymenu.api.buttonaction.ButtonActionRegistry;
import de.keksuccino.fancymenu.api.item.LayoutEditorElement;
import de.keksuccino.fancymenu.menu.button.ButtonScriptEngine;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.actions.ManageActionsScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

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

        AdvancedButton manageActionsButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.editor.action.screens.manage_screen.manage"), (press) -> {
            List<ManageActionsScreen.ActionInstance> l = new ArrayList<>();
            for (ButtonScriptEngine.ActionContainer c : i.actions) {
                ButtonActionContainer bac = ButtonActionRegistry.getActionByName(c.action);
                if (bac != null) {
                    ManageActionsScreen.ActionInstance i2 = new ManageActionsScreen.ActionInstance(bac, c.value);
                    l.add(i2);
                }
            }
            ManageActionsScreen s = new ManageActionsScreen(this.handler, l, (call) -> {
                if (call != null) {
                    this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                    i.actions.clear();
                    for (ManageActionsScreen.ActionInstance i2 : call) {
                        i.actions.add(new ButtonScriptEngine.ActionContainer(i2.action.getAction(), i2.value));
                    }
                }
            });
            Minecraft.getMinecraft().displayGuiScreen(s);
        });
        manageActionsButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.elements.ticker.manage_actions.desc"), "%n%"));
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
            public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
                if (i.isAsync) {
                    this.displayString = Locals.localize("fancymenu.customization.items.ticker.async.on");
                } else {
                    this.displayString = Locals.localize("fancymenu.customization.items.ticker.async.off");
                }
                super.drawButton(mc, mouseX, mouseY, partialTicks);
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
            public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
                if (i.tickMode == TickerCustomizationItem.TickMode.NORMAL) {
                    this.displayString = (Locals.localize("fancymenu.customization.items.ticker.tick_mode.normal"));
                } else if (i.tickMode == TickerCustomizationItem.TickMode.ONCE_PER_SESSION) {
                    this.displayString = (Locals.localize("fancymenu.customization.items.ticker.tick_mode.once_per_session"));
                } else if (i.tickMode == TickerCustomizationItem.TickMode.ON_MENU_LOAD) {
                    this.displayString = (Locals.localize("fancymenu.customization.items.ticker.tick_mode.on_menu_load"));
                }
                super.drawButton(mc, mouseX, mouseY, partialTicks);
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
        for (ButtonScriptEngine.ActionContainer c : i.actions) {
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
