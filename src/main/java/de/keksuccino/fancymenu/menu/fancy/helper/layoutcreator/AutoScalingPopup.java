package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator;

import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.math.MathUtils;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

public class AutoScalingPopup extends FMPopup {

    private LayoutEditorScreen parent;

    private AdvancedButton cancelButton;
    private AdvancedButton doneButton;

    private AdvancedTextField widthTextField;
    private AdvancedTextField heightTextField;

    protected Consumer<Boolean> callback = null;

    public AutoScalingPopup(LayoutEditorScreen parent, @Nullable Consumer<Boolean> callback) {
        super(240);

        this.callback = callback;
        this.parent = parent;

        this.cancelButton = new AdvancedButton(0, 0, 80, 20, Locals.localize("popup.yesno.cancel"), true, (press) -> {
            this.onCancelButtonPressed();
        });
        this.addButton(cancelButton);

        this.doneButton = new AdvancedButton(0, 0, 80, 20, Locals.localize("popup.done"), true, (press) -> {
            this.onDoneButtonPressed();
        });
        this.addButton(doneButton);

        this.widthTextField = new AdvancedTextField(Minecraft.getInstance().font, 0, 0, 200, 20, true, CharacterFilter.getIntegerCharacterFiler());
        this.widthTextField.setValue("" + Minecraft.getInstance().getWindow().getScreenWidth());

        this.heightTextField = new AdvancedTextField(Minecraft.getInstance().font, 0, 0, 200, 20, true, CharacterFilter.getIntegerCharacterFiler());
        this.heightTextField.setValue("" + Minecraft.getInstance().getWindow().getScreenHeight());

        KeyboardHandler.addKeyPressedListener(this::onEnterPressed);
        KeyboardHandler.addKeyPressedListener(this::onEscapePressed);
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, Screen renderIn) {
        super.render(matrix, mouseX, mouseY, renderIn);

        float partial = Minecraft.getInstance().getFrameTime();
        Font font = Minecraft.getInstance().font;
        int screenCenterX = renderIn.width / 2;
        int screenCenterY = renderIn.height / 2;
        
        drawCenteredString(matrix, font, Locals.localize("fancymenu.helper.editor.properties.autoscale.basesize.popup.desc.line1"), screenCenterX, screenCenterY - 90, -1);
        drawCenteredString(matrix, font, Locals.localize("fancymenu.helper.editor.properties.autoscale.basesize.popup.desc.line2"), screenCenterX, screenCenterY - 80, -1);
        drawCenteredString(matrix, font, Locals.localize("fancymenu.helper.editor.properties.autoscale.basesize.popup.desc.line3"), screenCenterX, screenCenterY - 70, -1);

        drawCenteredString(matrix, font, Locals.localize("general.width"), screenCenterX, screenCenterY - 50, -1);
        this.widthTextField.x = screenCenterX - (this.widthTextField.getWidth() / 2);
        this.widthTextField.y = screenCenterY - 35;
        this.widthTextField.render(matrix, mouseX, mouseY, partial);

        drawCenteredString(matrix, font, Locals.localize("general.height"), screenCenterX, screenCenterY - 5, -1);
        this.heightTextField.x = screenCenterX - (this.heightTextField.getWidth() / 2);
        this.heightTextField.y = screenCenterY + 10;
        this.heightTextField.render(matrix, mouseX, mouseY, partial);

        drawCenteredString(matrix, font, Locals.localize("helper.creator.windowsize.currentwidth") + ": " + Minecraft.getInstance().getWindow().getScreenWidth(), screenCenterX, screenCenterY + 45, -1);
        drawCenteredString(matrix, font, Locals.localize("helper.creator.windowsize.currentheight") + ": " + Minecraft.getInstance().getWindow().getScreenHeight(), screenCenterX, screenCenterY + 55, -1);

        this.doneButton.x = screenCenterX - this.doneButton.getWidth() - 5;
        this.doneButton.y = screenCenterY + 80;

        this.cancelButton.x = screenCenterX + 5;
        this.cancelButton.y = screenCenterY + 80;

        this.renderButtons(matrix, mouseX, mouseY);
    }

    protected void onDoneButtonPressed() {
        try {
            if (MathUtils.isInteger(this.widthTextField.getValue()) && MathUtils.isInteger(this.heightTextField.getValue())) {
                int w = Integer.parseInt(this.widthTextField.getValue());
                int h = Integer.parseInt(this.heightTextField.getValue());
                if ((w > 0) && (h > 0)) {
                    this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
                    this.parent.autoScalingWidth = w;
                    this.parent.autoScalingHeight = h;
                    this.answerCallback(true);
                } else {
                    this.answerCallback(false);
                    LayoutEditorScreen.displayNotification(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.properties.autoscale.error"), "%n%"));
                }
            } else {
                this.answerCallback(false);
                LayoutEditorScreen.displayNotification(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.properties.autoscale.error"), "%n%"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.setDisplayed(false);
    }

    protected void onCancelButtonPressed() {
        this.answerCallback(false);
        this.setDisplayed(false);
    }

    protected void answerCallback(boolean b) {
        if (callback != null) {
            callback.accept(b);
        }
    }

    public void onEnterPressed(KeyboardData d) {
        if ((d.keycode == 257) && this.isDisplayed()) {
            if ((this.doneButton != null) && this.doneButton.visible) {
                this.onDoneButtonPressed();
            }
        }
    }

    public void onEscapePressed(KeyboardData d) {
        if ((d.keycode == 256) && this.isDisplayed()) {
            this.onCancelButtonPressed();
        }
    }

}
