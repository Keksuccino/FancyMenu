package de.keksuccino.fancymenu.customization.frontend.layouteditor.elements.button;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.backend.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.backend.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.elements.ChooseFilePopup;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.fancymenu.rendering.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.rendering.ui.popup.FMPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.gui.content.HorizontalSwitcher;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

public class ButtonBackgroundPopup extends FMPopup {

    protected LayoutEditorScreen handler;
    protected MenuHandlerBase.ButtonCustomizationContainer customizationContainer;

    protected HorizontalSwitcher normalBackgroundTypeSwitcher;
    protected HorizontalSwitcher hoverBackgroundTypeSwitcher;

    protected AdvancedTextField normalBackgroundImageTextField;
    protected AdvancedTextField hoverBackgroundImageTextField;

    protected HorizontalSwitcher normalBackgroundAnimationSwitcher;
    protected HorizontalSwitcher hoverBackgroundAnimationSwitcher;

    protected AdvancedButton chooseNormalBackgroundImageButton;
    protected AdvancedButton chooseHoverBackgroundImageButton;

    protected AdvancedButton doneButton;
    protected AdvancedButton cancelButton;

    protected Runnable onClose = null;
    public boolean saveSnapshots = true;

    public ButtonBackgroundPopup(LayoutEditorScreen handler, MenuHandlerBase.ButtonCustomizationContainer customizationContainer) {
        super(240);
        this.handler = handler;
        this.customizationContainer = customizationContainer;

        Font font = Minecraft.getInstance().font;

        this.normalBackgroundTypeSwitcher = new HorizontalSwitcher(100, true,
                Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.image"),
                Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation")
        );
        this.normalBackgroundTypeSwitcher.setButtonColor(UIBase.getButtonIdleColor(), UIBase.getButtonHoverColor(), UIBase.getButtonBorderIdleColor(), UIBase.getButtonBorderHoverColor(), 1);
        this.normalBackgroundTypeSwitcher.setValueBackgroundColor(UIBase.getButtonIdleColor());
        if ((this.customizationContainer.normalBackground != null) && this.customizationContainer.normalBackground.startsWith("animation:")) {
            this.normalBackgroundTypeSwitcher.setSelectedValue(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation"));
        }

        this.hoverBackgroundTypeSwitcher = new HorizontalSwitcher(100, true,
                Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.image"),
                Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation")
        );
        this.hoverBackgroundTypeSwitcher.setButtonColor(UIBase.getButtonIdleColor(), UIBase.getButtonHoverColor(), UIBase.getButtonBorderIdleColor(), UIBase.getButtonBorderHoverColor(), 1);
        this.hoverBackgroundTypeSwitcher.setValueBackgroundColor(UIBase.getButtonIdleColor());
        if ((this.customizationContainer.hoverBackground != null) && this.customizationContainer.hoverBackground.startsWith("animation:")) {
            this.hoverBackgroundTypeSwitcher.setSelectedValue(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation"));
        }

        List<String> aniList = new ArrayList<String>();
        aniList.add("-- " + Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation.none") + " --");
        aniList.addAll(AnimationHandler.getCustomAnimationNames());
        String[] aniArray = aniList.toArray(new String[0]);

        this.normalBackgroundAnimationSwitcher = new HorizontalSwitcher(200, true, aniArray);
        if (this.customizationContainer.normalBackground != null) {
            if (this.customizationContainer.normalBackground.startsWith("animation:")) {
                String aniName = this.customizationContainer.normalBackground.split("[:]", 2)[1];
                this.normalBackgroundAnimationSwitcher.setSelectedValue(aniName);
                if (!this.normalBackgroundAnimationSwitcher.getSelectedValue().equals(aniName)) {
                    this.normalBackgroundAnimationSwitcher.setSelectedValue("-- " + Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation.none") + " --");
                }
            }
        }
        this.normalBackgroundAnimationSwitcher.setButtonColor(UIBase.getButtonIdleColor(), UIBase.getButtonHoverColor(), UIBase.getButtonBorderIdleColor(), UIBase.getButtonBorderHoverColor(), 1);
        this.normalBackgroundAnimationSwitcher.setValueBackgroundColor(UIBase.getButtonIdleColor());

        this.hoverBackgroundAnimationSwitcher = new HorizontalSwitcher(200, true, aniArray);
        if (this.customizationContainer.hoverBackground != null) {
            if (this.customizationContainer.hoverBackground.startsWith("animation:")) {
                String aniName = this.customizationContainer.hoverBackground.split("[:]", 2)[1];
                this.hoverBackgroundAnimationSwitcher.setSelectedValue(aniName);
                if (!this.hoverBackgroundAnimationSwitcher.getSelectedValue().equals(aniName)) {
                    this.hoverBackgroundAnimationSwitcher.setSelectedValue("-- " + Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation.none") + " --");
                }
            }
        }
        this.hoverBackgroundAnimationSwitcher.setButtonColor(UIBase.getButtonIdleColor(), UIBase.getButtonHoverColor(), UIBase.getButtonBorderIdleColor(), UIBase.getButtonBorderHoverColor(), 1);
        this.hoverBackgroundAnimationSwitcher.setValueBackgroundColor(UIBase.getButtonIdleColor());

        this.normalBackgroundImageTextField = new AdvancedTextField(font, 0, 0, 200, 20, true, null);
        this.normalBackgroundImageTextField.setMaxLength(10000);
        if (this.customizationContainer.normalBackground != null) {
            if (!this.customizationContainer.normalBackground.startsWith("animation:")) {
                this.normalBackgroundImageTextField.setValue(this.customizationContainer.normalBackground);
            }
        }

        this.hoverBackgroundImageTextField = new AdvancedTextField(font, 0, 0, 200, 20, true, null);
        this.hoverBackgroundImageTextField.setMaxLength(10000);
        if (this.customizationContainer.hoverBackground != null) {
            if (!this.customizationContainer.hoverBackground.startsWith("animation:")) {
                this.hoverBackgroundImageTextField.setValue(this.customizationContainer.hoverBackground);
            }
        }

        this.chooseNormalBackgroundImageButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.image.chooseimage"), true, (press) -> {
            ChooseFilePopup cf = new ChooseFilePopup((call) -> {
                if (call != null) {
                    File home = Minecraft.getInstance().gameDirectory;
                    call = call.replace("\\", "/");
                    File f = new File(call);
                    if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
                        f = new File(Minecraft.getInstance().gameDirectory, call);
                    }
                    String filteredName = CharacterFilter.getBasicFilenameCharacterFilter().filterForAllowedChars(f.getName());
                    if (f.isFile()) {
                        if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
                            if (filteredName.equals(f.getName())) {
                                if (call.startsWith(home.getAbsolutePath().replace("\\", "/"))) {
                                    call = call.replace(home.getAbsolutePath().replace("\\", "/"), "");
                                    if (call.startsWith("\\") || call.startsWith("/")) {
                                        call = call.substring(1);
                                    }
                                }
                                this.normalBackgroundImageTextField.setValue(call);
                                PopupHandler.displayPopup(this);
                            } else {
                                FMNotificationPopup pop = new FMNotificationPopup(300, new Color(0,0,0,0), 240, () -> {
                                    PopupHandler.displayPopup(this);
                                }, Locals.localize("helper.creator.textures.invalidcharacters"), "", "", "", "", "", "");
                                PopupHandler.displayPopup(pop);
                            }
                        } else {
                            PopupHandler.displayPopup(this);
                        }
                    } else {
                        FMNotificationPopup pop = new FMNotificationPopup(300, new Color(0,0,0,0), 240, () -> {
                            PopupHandler.displayPopup(this);
                        }, "§c§l" + Locals.localize("helper.creator.invalidimage.title"), "", Locals.localize("helper.creator.invalidimage.desc"), "", "", "", "", "", "");
                        PopupHandler.displayPopup(pop);
                    }
                } else {
                    PopupHandler.displayPopup(this);
                }
            }, "jpg", "jpeg", "png");
            PopupHandler.displayPopup(cf);
        });
        this.colorizePopupButton(chooseNormalBackgroundImageButton);
        this.addButton(chooseNormalBackgroundImageButton);

        this.chooseHoverBackgroundImageButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.image.chooseimage"), true, (press) -> {
            ChooseFilePopup cf = new ChooseFilePopup((call) -> {
                if (call != null) {
                    File home = Minecraft.getInstance().gameDirectory;
                    call = call.replace("\\", "/");
                    File f = new File(call);
                    if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
                        f = new File(Minecraft.getInstance().gameDirectory, call);
                    }
                    String filteredName = CharacterFilter.getBasicFilenameCharacterFilter().filterForAllowedChars(f.getName());
                    if (f.isFile()) {
                        if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
                            if (filteredName.equals(f.getName())) {
                                if (call.startsWith(home.getAbsolutePath().replace("\\", "/"))) {
                                    call = call.replace(home.getAbsolutePath().replace("\\", "/"), "");
                                    if (call.startsWith("\\") || call.startsWith("/")) {
                                        call = call.substring(1);
                                    }
                                }
                                this.hoverBackgroundImageTextField.setValue(call);
                                PopupHandler.displayPopup(this);
                            } else {
                                FMNotificationPopup pop = new FMNotificationPopup(300, new Color(0,0,0,0), 240, () -> {
                                    PopupHandler.displayPopup(this);
                                }, Locals.localize("helper.creator.textures.invalidcharacters"), "", "", "", "", "", "");
                                PopupHandler.displayPopup(pop);
                            }
                        } else {
                            PopupHandler.displayPopup(this);
                        }
                    } else {
                        FMNotificationPopup pop = new FMNotificationPopup(300, new Color(0,0,0,0), 240, () -> {
                            PopupHandler.displayPopup(this);
                        }, "§c§l" + Locals.localize("helper.creator.invalidimage.title"), "", Locals.localize("helper.creator.invalidimage.desc"), "", "", "", "", "", "");
                        PopupHandler.displayPopup(pop);
                    }
                } else {
                    PopupHandler.displayPopup(this);
                }
            }, "jpg", "jpeg", "png");
            PopupHandler.displayPopup(cf);
        });
        this.colorizePopupButton(chooseHoverBackgroundImageButton);
        this.addButton(chooseHoverBackgroundImageButton);

        this.doneButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("popup.done"), true, (call) -> {
            this.applyChangesToLayout();
            if (this.onClose != null) {
                this.onClose.run();
            }
            this.setDisplayed(false);
        });
        this.colorizePopupButton(doneButton);
        this.addButton(doneButton);

        this.cancelButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("popup.yesno.cancel"), true, (call) -> {
            if (this.onClose != null) {
                this.onClose.run();
            }
            this.setDisplayed(false);
        });
        this.colorizePopupButton(cancelButton);
        this.addButton(cancelButton);

        KeyboardHandler.addKeyPressedListener(this::onEscapePressed);
        KeyboardHandler.addKeyPressedListener(this::onEnterPressed);

    }

    public ButtonBackgroundPopup(LayoutEditorScreen handler, MenuHandlerBase.ButtonCustomizationContainer customizationContainer, Runnable onClose) {
        this(handler, customizationContainer);
        this.onClose = onClose;
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, Screen renderIn) {
        super.render(matrix, mouseX, mouseY, renderIn);

        Font font = Minecraft.getInstance().font;
        int midX = renderIn.width / 2;
        int midY = renderIn.height / 2;
        float partial = Minecraft.getInstance().getFrameTime();

        //Normal Background
        drawCenteredString(matrix, font, "§l" + Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.normalbackground"), midX, midY - 113, -1);

        this.normalBackgroundTypeSwitcher.render(matrix, midX - (this.normalBackgroundTypeSwitcher.getTotalWidth() / 2), midY - 100);

        if (this.normalBackgroundTypeSwitcher.getSelectedValue().equals(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.image"))) {

            this.normalBackgroundImageTextField.x = midX - (this.normalBackgroundImageTextField.getWidth() / 2);
            this.normalBackgroundImageTextField.y = midY - 70;
            this.normalBackgroundImageTextField.render(matrix, mouseX, mouseY, partial);

            this.chooseNormalBackgroundImageButton.visible = true;
            this.chooseNormalBackgroundImageButton.x = midX - (this.chooseNormalBackgroundImageButton.getWidth() / 2);
            this.chooseNormalBackgroundImageButton.y = midY - 45;

        } else if (this.normalBackgroundTypeSwitcher.getSelectedValue().equals(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation"))) {

            this.chooseNormalBackgroundImageButton.visible = false;

            this.normalBackgroundAnimationSwitcher.render(matrix, midX - (this.normalBackgroundAnimationSwitcher.getTotalWidth() / 2), midY - 62);

        }

        //Hover Background
        drawCenteredString(matrix, font, "§l" + Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.hoverbackground"), midX, midY - 15, -1);

        this.hoverBackgroundTypeSwitcher.render(matrix, midX - (this.hoverBackgroundTypeSwitcher.getTotalWidth() / 2), midY - 2); // 98

        if (this.hoverBackgroundTypeSwitcher.getSelectedValue().equals(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.image"))) {

            this.hoverBackgroundImageTextField.x = midX - (this.hoverBackgroundImageTextField.getWidth() / 2);
            this.hoverBackgroundImageTextField.y = midY + 28;
            this.hoverBackgroundImageTextField.render(matrix, mouseX, mouseY, partial);

            this.chooseHoverBackgroundImageButton.visible = true;
            this.chooseHoverBackgroundImageButton.x = midX - (this.chooseHoverBackgroundImageButton.getWidth() / 2);
            this.chooseHoverBackgroundImageButton.y = midY + 53;

        } else if (this.hoverBackgroundTypeSwitcher.getSelectedValue().equals(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation"))) {

            this.chooseHoverBackgroundImageButton.visible = false;

            this.hoverBackgroundAnimationSwitcher.render(matrix, midX - (this.hoverBackgroundAnimationSwitcher.getTotalWidth() / 2), midY + 40);

        }

        this.doneButton.x = midX - this.doneButton.getWidth() - 5;
        this.doneButton.y = midY + 80;

        this.cancelButton.x = midX + 5;
        this.cancelButton.y = midY + 80;

        this.renderButtons(matrix, mouseX, mouseY);
    }

    protected void applyChangesToLayout() {
        if (this.saveSnapshots) {
            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
        }

        this.customizationContainer.normalBackground = null;
        this.customizationContainer.hoverBackground = null;

        if (this.normalBackgroundTypeSwitcher.getSelectedValue().equals(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.image"))) {
            if ((this.normalBackgroundImageTextField.getValue() != null) && !this.normalBackgroundImageTextField.getValue().replace(" ", "").equals("")) {
                File f = new File(this.normalBackgroundImageTextField.getValue());
                if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
                    f = new File(Minecraft.getInstance().gameDirectory, this.normalBackgroundImageTextField.getValue());
                }
                if (f.isFile()) {
                    if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png") || f.getPath().toLowerCase().endsWith(".gif")) {
                        this.customizationContainer.normalBackground = this.normalBackgroundImageTextField.getValue();
                    }
                }
            }
        } else if (this.normalBackgroundTypeSwitcher.getSelectedValue().equals(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation"))) {
            if (!this.normalBackgroundAnimationSwitcher.getSelectedValue().equals("-- " + Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation.none") + " --")) {
                if (AnimationHandler.animationExists(this.normalBackgroundAnimationSwitcher.getSelectedValue())) {
                    this.customizationContainer.normalBackground = "animation:" + this.normalBackgroundAnimationSwitcher.getSelectedValue();
                }
            }
        }

        if (this.hoverBackgroundTypeSwitcher.getSelectedValue().equals(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.image"))) {
            if ((this.hoverBackgroundImageTextField.getValue() != null) && !this.hoverBackgroundImageTextField.getValue().replace(" ", "").equals("")) {
                File f = new File(this.hoverBackgroundImageTextField.getValue());
                if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
                    f = new File(Minecraft.getInstance().gameDirectory, this.hoverBackgroundImageTextField.getValue());
                }
                if (f.isFile()) {
                    if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png") || f.getPath().toLowerCase().endsWith(".gif")) {
                        this.customizationContainer.hoverBackground = this.hoverBackgroundImageTextField.getValue();
                    }
                }
            }
        } else if (this.hoverBackgroundTypeSwitcher.getSelectedValue().equals(Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation"))) {
            if (!this.hoverBackgroundAnimationSwitcher.getSelectedValue().equals("-- " + Locals.localize("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation.none") + " --")) {
                if (AnimationHandler.animationExists(this.hoverBackgroundAnimationSwitcher.getSelectedValue())) {
                    this.customizationContainer.hoverBackground = "animation:" + this.hoverBackgroundAnimationSwitcher.getSelectedValue();
                }
            }
        }
    }

    protected void onEnterPressed(KeyboardData d) {
        if ((d.keycode == 257) && this.isDisplayed()) {
            this.applyChangesToLayout();
            if (this.onClose != null) {
                this.onClose.run();
            }
            this.setDisplayed(false);
        }
    }

    protected void onEscapePressed(KeyboardData d) {
        if ((d.keycode == 256) && this.isDisplayed()) {
            if (this.onClose != null) {
                this.onClose.run();
            }
            this.setDisplayed(false);
        }
    }

}
