package de.keksuccino.fancymenu.customization.element.elements.button.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionExecutor;
import de.keksuccino.fancymenu.customization.action.ActionRegistry;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.actions.ManageActionsScreen;
import de.keksuccino.fancymenu.customization.layout.editor.elements.ChooseFilePopup;
import de.keksuccino.fancymenu.customization.layout.editor.elements.button.ButtonBackgroundPopup;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.ContextMenu;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorScreen;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ButtonEditorElement extends AbstractEditorElement {

    public ButtonEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        //TODO add button settings

//        AdvancedButton manageActionsButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.editor.action.screens.manage_screen.manage"), (press) -> {
//            java.util.List<ManageActionsScreen.ActionInstance> l = new ArrayList<>();
//            for (ActionExecutor.ActionContainer c : this.actions) {
//                Action bac = ActionRegistry.getActionByName(c.action);
//                if (bac != null) {
//                    ManageActionsScreen.ActionInstance i = new ManageActionsScreen.ActionInstance(bac, c.value);
//                    l.add(i);
//                }
//            }
//            ManageActionsScreen s = new ManageActionsScreen(this.editor, l, (call) -> {
//                if (call != null) {
//                    this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                    this.actions.clear();
//                    for (ManageActionsScreen.ActionInstance i : call) {
//                        this.actions.add(new ActionExecutor.ActionContainer(i.action.getIdentifier(), i.value));
//                    }
//                }
//            });
//            Minecraft.getInstance().setScreen(s);
//        });
//        manageActionsButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.elements.button.manage_actions.desc"), "%n%"));
//        this.menu.addContent(manageActionsButton);
//
//
//        this.menu.addSeparator();
//
//        AdvancedButton buttonBackgroundButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground"), (press) -> {
//            ButtonBackgroundPopup pop = new ButtonBackgroundPopup(this.editor, this.customizationContainer);
//            PopupHandler.displayPopup(pop);
//        });
//        this.menu.addContent(buttonBackgroundButton);
//
//        String loopAniLabel = Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.loopanimation.on");
//        if (!this.customizationContainer.loopAnimation) {
//            loopAniLabel = Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.loopanimation.off");
//        }
//        AdvancedButton loopBackgroundAnimationButton = new AdvancedButton(0, 0, 0, 0, loopAniLabel, (press) -> {
//            if (this.customizationContainer.loopAnimation) {
//                this.customizationContainer.loopAnimation = false;
//                ((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.loopanimation.off"));
//            } else {
//                this.customizationContainer.loopAnimation = true;
//                ((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.loopanimation.on"));
//            }
//        });
//        loopBackgroundAnimationButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.loopanimation.btn.desc"), "%n%"));
//        this.menu.addContent(loopBackgroundAnimationButton);
//
//        String restartAniLabel = Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.restartonhover.on");
//        if (!this.customizationContainer.restartAnimationOnHover) {
//            restartAniLabel = Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.restartonhover.off");
//        }
//        AdvancedButton restartAnimationOnHoverButton = new AdvancedButton(0, 0, 0, 0, restartAniLabel, (press) -> {
//            if (this.customizationContainer.restartAnimationOnHover) {
//                this.customizationContainer.restartAnimationOnHover = false;
//                ((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.restartonhover.off"));
//            } else {
//                this.customizationContainer.restartAnimationOnHover = true;
//                ((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.restartonhover.on"));
//            }
//        });
//        restartAnimationOnHoverButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.restartonhover.btn.desc"), "%n%"));
//        this.menu.addContent(restartAnimationOnHoverButton);
//
//        this.menu.addSeparator();
//
//        AdvancedButton b2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.editlabel"), (press) -> {
//
//            TextEditorScreen s = new TextEditorScreen(net.minecraft.network.chat.Component.literal(Locals.localize("helper.creator.items.button.editlabel")), this.editor, null, (call) -> {
//                if (call != null) {
//                    if (!this.element.value.equals(call)) {
//                        this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                    }
//                    this.element.value = call;
//                }
//            });
//            s.multilineMode = false;
//            s.setText(StringUtils.convertFormatCodes(this.element.value, "§", "&"));
//            Minecraft.getInstance().setScreen(s);
//
//        });
//        this.menu.addContent(b2);
//
//        this.menu.addSeparator();
//
//        AdvancedButton b5 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoverlabel"), (press) -> {
//
//            TextEditorScreen s = new TextEditorScreen(net.minecraft.network.chat.Component.literal(Locals.localize("helper.creator.items.button.hoverlabel")), this.editor, null, (call) -> {
//                if (call != null) {
//                    if ((this.customizationContainer.hoverLabel == null) || !this.customizationContainer.hoverLabel.equals(call)) {
//                        this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                    }
//
//                    this.customizationContainer.hoverLabel = call;
//                }
//            });
//            s.multilineMode = false;
//            if (this.customizationContainer.hoverLabel != null) {
//                s.setText(StringUtils.convertFormatCodes(this.customizationContainer.hoverLabel, "§", "&"));
//            }
//            Minecraft.getInstance().setScreen(s);
//
//        });
//        this.menu.addContent(b5);
//
//        AdvancedButton b6 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoverlabel.reset"), (press) -> {
//            if (this.customizationContainer.hoverLabel != null) {
//                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//            }
//            this.customizationContainer.hoverLabel = null;
//            this.menu.closeMenu();
//        });
//        this.menu.addContent(b6);
//
//        this.menu.addSeparator();
//
//        AdvancedButton b7 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoversound"), (press) -> {
//            ChooseFilePopup cf = new ChooseFilePopup((call) -> {
//                if (call != null) {
//                    if (!call.replace(" ", "").equals("")) {
//                        File f = new File(call);
//                        if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
//                            f = new File(Minecraft.getInstance().gameDirectory, call);
//                        }
//                        if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
//                            if ((this.customizationContainer.hoverSound == null) || !this.customizationContainer.hoverSound.equals(call)) {
//                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                            }
//
//                            this.customizationContainer.hoverSound = call;
//                        } else {
//                            LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "", "");
//                        }
//                    } else {
//                        if (this.customizationContainer.hoverSound != null) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        this.customizationContainer.hoverSound = null;
//                        this.menu.closeMenu();
//                    }
//                }
//            }, "wav");
//
//            if (this.customizationContainer.hoverSound != null) {
//                cf.setText(this.customizationContainer.hoverSound);
//            }
//            PopupHandler.displayPopup(cf);
//        });
//        this.menu.addContent(b7);
//
//        AdvancedButton b10 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.clicksound"), (press) -> {
//            ChooseFilePopup cf = new ChooseFilePopup((call) -> {
//                if (call != null) {
//                    if (!call.replace(" ", "").equals("")) {
//                        File f = new File(call);
//                        if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
//                            f = new File(Minecraft.getInstance().gameDirectory, call);
//                        }
//                        if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
//                            if ((this.customizationContainer.clickSound == null) || !this.customizationContainer.clickSound.equals(call)) {
//                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                            }
//                            this.customizationContainer.clickSound = call;
//                        } else {
//                            LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "", "");
//                        }
//                    } else {
//                        if (this.customizationContainer.clickSound != null) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        this.customizationContainer.clickSound = null;
//                        this.menu.closeMenu();
//                    }
//                }
//            }, "wav");
//
//            if (this.customizationContainer.clickSound != null) {
//                cf.setText(this.customizationContainer.clickSound);
//            }
//            PopupHandler.displayPopup(cf);
//        });
//        this.menu.addContent(b10);
//
//        AdvancedButton b12 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.btndescription"), (press) -> {
//
//            TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("helper.creator.items.button.btndescription")), this.editor, null, (call) -> {
//                if (call != null) {
//                    call = call.replace("\n", "%n%");
//                    if (!call.replace(" ", "").equals("")) {
//                        if ((this.customizationContainer.buttonDescription == null) || !this.customizationContainer.buttonDescription.equals(call)) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        this.customizationContainer.buttonDescription = call;
//                    } else {
//                        if (this.customizationContainer.buttonDescription != null) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        this.customizationContainer.buttonDescription = null;
//                    }
//                }
//            });
//            if (this.customizationContainer.buttonDescription != null) {
//                s.setText(this.customizationContainer.buttonDescription.replace("%n%", "\n"));
//            }
//            Minecraft.getInstance().setScreen(s);
//
//        });
//        List<String> l = new ArrayList<String>();
//        for (String s : StringUtils.splitLines(Locals.localize("helper.creator.items.button.btndescription.desc"), "%n%")) {
//            l.add(s.replace("#n#", "%n%"));
//        }
//        b12.setDescription(l.toArray(new String[0]));
//        this.menu.addContent(b12);

    }

}
