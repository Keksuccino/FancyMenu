package de.keksuccino.fancymenu.customization.backend.deepcustomization.layers.titlescreen.splash;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationLayoutEditorElement;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.elements.ChooseFilePopup;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationElement;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationItem;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;

public class TitleScreenSplashLayoutElement extends DeepCustomizationLayoutEditorElement {

    public TitleScreenSplashLayoutElement(@NotNull DeepCustomizationElement parentDeepCustomizationElement, @NotNull DeepCustomizationItem customizationItemInstance, @NotNull LayoutEditorScreen handler) {
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
                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
            }
            this.getSplashItem().orientation = "original";
            this.getSplashItem().rawX = 0;
            this.getSplashItem().rawY = 0;
        });
        this.rightClickContextMenu.addContent(resetOrientationButton);

        this.rightClickContextMenu.addSeparator();

        AdvancedButton setSplashFileButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash.splash_file.set"), true, (press) -> {
            ChooseFilePopup p = new ChooseFilePopup((call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equals("")) {
                        File f = new File(call);
                        if (f.exists() && f.getAbsolutePath().replace("\\", "/").toLowerCase().endsWith(".txt")) {
                            if ((this.getSplashItem().splashTextFilePath == null) || !this.getSplashItem().splashTextFilePath.equals(call)) {
                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                            }
                            this.getSplashItem().splashTextFilePath = call;
                            TitleScreenSplashItem.cachedSplashText = null;
                        }
                    } else {
                        if (this.getSplashItem().splashTextFilePath != null) {
                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
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
        this.rightClickContextMenu.addContent(setSplashFileButton);

        AdvancedButton resetSplashFileButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash.splash_file.reset"), true, (press) -> {
            if (this.getSplashItem().splashTextFilePath != null) {
                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
            }
            this.getSplashItem().splashTextFilePath = null;
            TitleScreenSplashItem.cachedSplashText = null;
        });
        this.rightClickContextMenu.addContent(resetSplashFileButton);

        this.rightClickContextMenu.addSeparator();

        AdvancedButton setRotationButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash.rotation.set"), true, (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash.rotation.set"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equals("") && MathUtils.isInteger(call)) {
                        int i = Integer.parseInt(call);
                        if (this.getSplashItem().splashRotation != i) {
                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                        }
                        this.getSplashItem().splashRotation = i;
                    } else {
                        if (this.getSplashItem().splashRotation != -20) {
                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                        }
                        this.getSplashItem().splashRotation = -20;
                    }
                }
            });
            p.setText("" + this.getSplashItem().splashRotation);
            PopupHandler.displayPopup(p);
        });
        this.rightClickContextMenu.addContent(setRotationButton);

        AdvancedButton resetRotationButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash.rotation.reset"), true, (press) -> {
            if (this.getSplashItem().splashRotation != -20) {
                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
            }
            this.getSplashItem().splashRotation = -20;
        });
        this.rightClickContextMenu.addContent(resetRotationButton);

        this.rightClickContextMenu.addSeparator();

        AdvancedButton setColorButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash.color.set"), true, (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash.color.set"), null, 240, (call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equals("")) {
                        Color c = RenderUtils.getColorFromHexString(call);
                        if (c != null) {
                            if (!this.getSplashItem().splashColorHEX.equals(call)) {
                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                            }
                            this.getSplashItem().splashColor = c;
                            this.getSplashItem().splashColorHEX = call;
                        }
                    } else {
                        if (!this.getSplashItem().splashColorHEX.equals("#ffff00")) {
                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                        }
                        this.getSplashItem().splashColorHEX = "#ffff00";
                        this.getSplashItem().splashColor = new Color(255, 255, 0);
                    }
                }
            });
            p.setText(this.getSplashItem().splashColorHEX);
            PopupHandler.displayPopup(p);
        });
        this.rightClickContextMenu.addContent(setColorButton);

        AdvancedButton resetColorButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.element.vanilla.deepcustomization.titlescreen.splash.color.reset"), true, (press) -> {
            if (!this.getSplashItem().splashColorHEX.equals("#ffff00")) {
                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
            }
            this.getSplashItem().splashColorHEX = "#ffff00";
            this.getSplashItem().splashColor = new Color(255, 255, 0);
        });
        this.rightClickContextMenu.addContent(resetColorButton);

    }

    protected TitleScreenSplashItem getSplashItem() {
        return (TitleScreenSplashItem) this.element;
    }

    @Override
    protected void setOrientation(String pos) {
        super.setOrientation(pos);
        if (!pos.equals("original")) {
            this.element.rawX += (this.element.width / 2);
            this.element.rawY += (this.element.height / 2);
        }
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY) {
        if (this.element.orientation.equals("original")) {
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