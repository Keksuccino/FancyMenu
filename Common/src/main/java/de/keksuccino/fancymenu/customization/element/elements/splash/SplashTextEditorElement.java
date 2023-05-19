
package de.keksuccino.fancymenu.customization.element.elements.splash;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class SplashTextEditorElement extends AbstractEditorElement {

    public SplashTextEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        //TODO add "set source" entry

        //TODO add "set source mode" entry

        //TODO add back old entries

//        /** SCALE **/
//        AdvancedButton scaleButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.items.string.setscale"), true, (press) -> {
//            FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + I18n.get("fancymenu.editor.items.string.setscale") + ":", CharacterFilter.getDoubleCharacterFiler(), 240, this::setScaleCallback);
//            p.setText("" + this.getObject().scale);
//            PopupHandler.displayPopup(p);
//        });
//        this.rightClickContextMenu.addContent(scaleButton);
//
//        /** ROTATION **/
//        AdvancedButton rotationButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.editor.items.splash.rotation"), true, (press) -> {
//            FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + I18n.get("fancymenu.editor.items.splash.rotation") + ":", CharacterFilter.getDoubleCharacterFiler(), 240, (call) -> {
//                if (call != null) {
//                    if (MathUtils.isFloat(call)) {
//                        this.getObject().rotation = Float.parseFloat(call);
//                    }
//                }
//            });
//            p.setText("" + this.getObject().rotation);
//            PopupHandler.displayPopup(p);
//        });
//        this.rightClickContextMenu.addContent(rotationButton);
//
//        /** BASE COLOR **/
//        AdvancedButton colorButton = new AdvancedButton(0, 0, 0, 16, I18n.get("fancymenu.editor.items.splash.basecolor"), true, (press) -> {
//            FMTextInputPopup t = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + I18n.get("fancymenu.editor.items.splash.basecolor") + ":", null, 240, (call) -> {
//                if (call != null) {
//                    if (!call.equals("")) {
//                        Color c = RenderUtils.getColorFromHexString(call);
//                        if (c != null) {
//
//                            if (!this.getObject().basecolorString.equalsIgnoreCase(call)) {
//                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                            }
//
//                            this.getObject().basecolor = c;
//                            this.getObject().basecolorString = call;
//
//                        }
//                    } else {
//                        if (!this.getObject().basecolorString.equalsIgnoreCase("#ffff00")) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//
//                        this.getObject().basecolorString = "#ffff00";
//                        this.getObject().basecolor = new Color(255, 255, 0);
//                    }
//                }
//
//            });
//            t.setText(this.getObject().basecolorString);
//
//            PopupHandler.displayPopup(t);
//        });
//        this.rightClickContextMenu.addContent(colorButton);
//
//        /** SHADOW **/
//        String shadowLabel = I18n.get("fancymenu.editor.items.string.setshadow");
//        if (this.getObject().shadow) {
//            shadowLabel = I18n.get("fancymenu.editor.items.string.setnoshadow");
//        }
//        AdvancedButton shadowButton = new AdvancedButton(0, 0, 0, 0, shadowLabel, true, (press) -> {
//            if (this.getObject().shadow) {
//                ((AdvancedButton)press).setMessage(I18n.get("fancymenu.editor.items.string.setshadow"));
//                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//
//                this.getObject().shadow = false;
//            } else {
//                ((AdvancedButton)press).setMessage(I18n.get("fancymenu.editor.items.string.setnoshadow"));
//                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//
//                this.getObject().shadow = true;
//            }
//        });
//        this.rightClickContextMenu.addContent(shadowButton);
//
//        /** BOUNCING **/
//        String bounceLabel = I18n.get("fancymenu.editor.items.splash.bounce.off");
//        if (this.getObject().bounce) {
//            bounceLabel = I18n.get("fancymenu.editor.items.splash.bounce.on");
//        }
//        AdvancedButton bounceButton = new AdvancedButton(0, 0, 0, 0, bounceLabel, true, (press) -> {
//            if (this.getObject().bounce) {
//                ((AdvancedButton)press).setMessage(I18n.get("fancymenu.editor.items.splash.bounce.off"));
//                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//
//                this.getObject().bounce = false;
//            } else {
//                ((AdvancedButton)press).setMessage(I18n.get("fancymenu.editor.items.splash.bounce.on"));
//                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//
//                this.getObject().bounce = true;
//            }
//        });
//        this.rightClickContextMenu.addContent(bounceButton);
//
//        /** REFRESH ON MENU RELOAD **/
//        String refreshLabel = I18n.get("fancymenu.editor.items.splash.refresh.off");
//        if (this.getObject().refreshOnMenuReload) {
//            refreshLabel = I18n.get("fancymenu.editor.items.splash.refresh.on");
//        }
//        AdvancedButton refreshButton = new AdvancedButton(0, 0, 0, 0, refreshLabel, true, (press) -> {
//            if (this.getObject().refreshOnMenuReload) {
//                ((AdvancedButton)press).setMessage(I18n.get("fancymenu.editor.items.splash.refresh.off"));
//                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//
//                this.getObject().refreshOnMenuReload = false;
//            } else {
//                ((AdvancedButton)press).setMessage(I18n.get("fancymenu.editor.items.splash.refresh.on"));
//                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//
//                this.getObject().refreshOnMenuReload = true;
//            }
//        });
//        refreshButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.editor.items.splash.refresh.desc")));
//        if (this.getObject().text == null) {
//            this.rightClickContextMenu.addContent(refreshButton);
//        }

    }

}
