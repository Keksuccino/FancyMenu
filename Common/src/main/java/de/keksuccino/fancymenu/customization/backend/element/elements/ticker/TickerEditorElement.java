package de.keksuccino.fancymenu.customization.backend.element.elements.ticker;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import de.keksuccino.fancymenu.api.buttonaction.ButtonActionRegistry;
import de.keksuccino.fancymenu.customization.backend.element.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.backend.button.ButtonScriptEngine;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.actions.ManageActionsScreen;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
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

public class TickerEditorElement extends AbstractEditorElement {

    public TickerEditorElement(TickerElementBuilder parentContainer, TickerElement customizationItemInstance, LayoutEditorScreen handler) {
        super(parentContainer, customizationItemInstance, true, handler, true);
    }

    @Override
    public void init() {

        this.supportsAdvancedPositioning = false;
        this.supportsAdvancedSizing = false;

        super.init();

        TickerElement i = ((TickerElement)this.element);

        AdvancedButton manageActionsButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.editor.action.screens.manage_screen.manage"), (press) -> {
            List<ManageActionsScreen.ActionInstance> l = new ArrayList<>();
            for (ButtonScriptEngine.ActionContainer c : i.actions) {
                ButtonActionContainer bac = ButtonActionRegistry.getActionByName(c.action);
                if (bac != null) {
                    ManageActionsScreen.ActionInstance i2 = new ManageActionsScreen.ActionInstance(bac, c.value);
                    l.add(i2);
                }
            }
            ManageActionsScreen s = new ManageActionsScreen(this.editor, l, (call) -> {
                if (call != null) {
                    this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                    i.actions.clear();
                    for (ManageActionsScreen.ActionInstance i2 : call) {
                        i.actions.add(new ButtonScriptEngine.ActionContainer(i2.action.getAction(), i2.value));
                    }
                }
            });
            Minecraft.getInstance().setScreen(s);
        });
        manageActionsButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.elements.ticker.manage_actions.desc"), "%n%"));
        this.rightClickContextMenu.addContent(manageActionsButton);

        AdvancedButton tickDelayButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.ticker.tick_delay"), (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0), Locals.localize("fancymenu.customization.items.ticker.tick_delay"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
                if (call != null) {
                    if (MathUtils.isLong(call)) {
                        long delay = Long.parseLong(call);
                        if (i.tickDelayMs != delay) {
                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                        }
                        i.tickDelayMs = delay;
                    } else {
                        if (call.replace(" ", "").equals("")) {
                            if (i.tickDelayMs != 0) {
                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
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
        this.rightClickContextMenu.addContent(tickDelayButton);

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
        this.rightClickContextMenu.addContent(asyncButton);

        AdvancedButton oneTickModeButton = new AdvancedButton(0, 0, 0, 0, "", (press) -> {
            if (i.tickMode == TickerElement.TickMode.NORMAL) {
                i.tickMode = TickerElement.TickMode.ONCE_PER_SESSION;
            } else if (i.tickMode == TickerElement.TickMode.ONCE_PER_SESSION){
                i.tickMode = TickerElement.TickMode.ON_MENU_LOAD;
            } else if (i.tickMode == TickerElement.TickMode.ON_MENU_LOAD) {
                i.tickMode = TickerElement.TickMode.NORMAL;
            }
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (i.tickMode == TickerElement.TickMode.NORMAL) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.ticker.tick_mode.normal"));
                } else if (i.tickMode == TickerElement.TickMode.ONCE_PER_SESSION){
                    this.setMessage(Locals.localize("fancymenu.customization.items.ticker.tick_mode.once_per_session"));
                } else if (i.tickMode == TickerElement.TickMode.ON_MENU_LOAD) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.ticker.tick_mode.on_menu_load"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        oneTickModeButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.ticker.tick_mode.desc"), "%n%"));
        this.rightClickContextMenu.addContent(oneTickModeButton);

    }

    @Override
    public SerializedElement serializeItem() {

        TickerElement i = ((TickerElement)this.element);

        SerializedElement sec = new SerializedElement();

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
