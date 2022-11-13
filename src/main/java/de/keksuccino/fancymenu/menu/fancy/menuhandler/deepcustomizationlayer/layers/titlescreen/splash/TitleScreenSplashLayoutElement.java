package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.splash;

import com.mojang.blaze3d.matrix.MatrixStack;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.ChooseFilePopup;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayoutEditorElement;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;

import java.awt.*;
import java.io.File;

import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayoutEditorElement.SimplePropertiesSection;

public class TitleScreenSplashLayoutElement extends DeepCustomizationLayoutEditorElement {

    public TitleScreenSplashLayoutElement(DeepCustomizationElement parentDeepCustomizationElement, DeepCustomizationItem customizationItemInstance, LayoutEditorScreen handler) {
        super(parentDeepCustomizationElement, customizationItemInstance, true, handler);
    }

    @Override
    public void deepCustomizationPreInit() {
        super.deepCustomizationPreInit();
        this.orientationCanBeChanged = true;
        this.allowOrientationByElement = false;
    }

    @Override
    public void init() {

        super.init();

        AdvancedButton resetOrientationButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash.orientation.reset"), true, (press) -> {
            if (!this.getSplashItem().orientation.equals("original")) {
                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
            }
            this.getSplashItem().orientation = "original";
            this.getSplashItem().posX = 0;
            this.getSplashItem().posY = 0;
        });
        this.rightclickMenu.addContent(resetOrientationButton);

        this.rightclickMenu.addSeparator();

        AdvancedButton setSplashFileButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash.splash_file.set"), true, (press) -> {
            ChooseFilePopup p = new ChooseFilePopup((call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equals("")) {
                        File f = new File(call);
                        if (f.exists() && f.getAbsolutePath().replace("\\", "/").toLowerCase().endsWith(".txt")) {
                            if ((this.getSplashItem().splashTextFilePath == null) || !this.getSplashItem().splashTextFilePath.equals(call)) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            this.getSplashItem().splashTextFilePath = call;
                            TitleScreenSplashItem.cachedSplashText = null;
                        }
                    } else {
                        if (this.getSplashItem().splashTextFilePath != null) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        this.getSplashItem().splashTextFilePath = null;
                        TitleScreenSplashItem.cachedSplashText = null;
                    }
                }
            }, "txt");
            if (this.getSplashItem().splashTextFilePath != null) {
                p.setText(this.getSplashItem().splashTextFilePath);
            }
            PopupHandler.displayPopup(p);
        });
        this.rightclickMenu.addContent(setSplashFileButton);

        AdvancedButton resetSplashFileButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash.splash_file.reset"), true, (press) -> {
            if (this.getSplashItem().splashTextFilePath != null) {
                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
            }
            this.getSplashItem().splashTextFilePath = null;
            TitleScreenSplashItem.cachedSplashText = null;
        });
        this.rightclickMenu.addContent(resetSplashFileButton);

        this.rightclickMenu.addSeparator();

        AdvancedButton setRotationButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash.rotation.set"), true, (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash.rotation.set"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equals("") && MathUtils.isInteger(call)) {
                        int i = Integer.parseInt(call);
                        if (this.getSplashItem().splashRotation != i) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        this.getSplashItem().splashRotation = i;
                    } else {
                        if (this.getSplashItem().splashRotation != -20) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        this.getSplashItem().splashRotation = -20;
                    }
                }
            });
            p.setText("" + this.getSplashItem().splashRotation);
            PopupHandler.displayPopup(p);
        });
        this.rightclickMenu.addContent(setRotationButton);

        AdvancedButton resetRotationButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash.rotation.reset"), true, (press) -> {
            if (this.getSplashItem().splashRotation != -20) {
                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
            }
            this.getSplashItem().splashRotation = -20;
        });
        this.rightclickMenu.addContent(resetRotationButton);

        this.rightclickMenu.addSeparator();

        AdvancedButton setColorButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash.color.set"), true, (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash.color.set"), null, 240, (call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equals("")) {
                        Color c = RenderUtils.getColorFromHexString(call);
                        if (c != null) {
                            if (!this.getSplashItem().splashColorHEX.equals(call)) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            this.getSplashItem().splashColor = c;
                            this.getSplashItem().splashColorHEX = call;
                        }
                    } else {
                        if (!this.getSplashItem().splashColorHEX.equals("#ffff00")) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        this.getSplashItem().splashColorHEX = "#ffff00";
                        this.getSplashItem().splashColor = new Color(255, 255, 0);
                    }
                }
            });
            p.setText(this.getSplashItem().splashColorHEX);
            PopupHandler.displayPopup(p);
        });
        this.rightclickMenu.addContent(setColorButton);

        AdvancedButton resetColorButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash.color.reset"), true, (press) -> {
            if (!this.getSplashItem().splashColorHEX.equals("#ffff00")) {
                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
            }
            this.getSplashItem().splashColorHEX = "#ffff00";
            this.getSplashItem().splashColor = new Color(255, 255, 0);
        });
        this.rightclickMenu.addContent(resetColorButton);

    }

    protected TitleScreenSplashItem getSplashItem() {
        return (TitleScreenSplashItem) this.object;
    }

    @Override
    protected void setOrientation(String pos) {
        super.setOrientation(pos);
        if (!pos.equals("original")) {
            this.object.posX += (this.object.width / 2);
            this.object.posY += (this.object.height / 2);
        }
    }

    @Override
    public void render(MatrixStack matrix, int mouseX, int mouseY) {
        if (this.object.orientation.equals("original")) {
            this.dragable = false;
        } else {
            this.dragable = true;
        }
        super.render(matrix, mouseX, mouseY);
    }

    @Override
    public SimplePropertiesSection serializeItem() {
        SimplePropertiesSection sec = new SimplePropertiesSection();
        if (this.getSplashItem().splashTextFilePath != null) {
            sec.addEntry("splash_file_path", this.getSplashItem().splashTextFilePath);
        }
        sec.addEntry("splash_rotation", "" + this.getSplashItem().splashRotation);
        sec.addEntry("splash_color", this.getSplashItem().splashColorHEX);
        return sec;
    }

}