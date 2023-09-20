package de.keksuccino.fancymenu.customization.element.elements.animation;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.ChooseAnimationScreen;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.ObjectUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AnimationEditorElement extends AbstractEditorElement {

    public AnimationEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.rightClickMenu.addClickableEntry("choose_animation", Component.translatable("fancymenu.elements.animation.set_animation"), (menu, entry) -> {
            List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof AnimationEditorElement));
            String preSelectedAnimation = null;
            List<String> allAnimations = ObjectUtils.getOfAll(String.class, selectedElements, consumes -> ((AnimationElement)consumes.element).animationName);
            if (!allAnimations.isEmpty() && ListUtils.allInListEqual(allAnimations)) {
                preSelectedAnimation = allAnimations.get(0);
            }
            ChooseAnimationScreen s = new ChooseAnimationScreen(preSelectedAnimation, (call) -> {
                if (call != null) {
                    this.editor.history.saveSnapshot();
                    for (AbstractEditorElement e : selectedElements) {
                        ((AnimationElement)e.element).animationName = call;
                    }
                }
                Minecraft.getInstance().setScreen(this.editor);
            });
            Minecraft.getInstance().setScreen(s);
        }).setStackable(true);

        this.rightClickMenu.addClickableEntry("restore_aspect_ratio", Component.translatable("fancymenu.elements.animation.restore_aspect_ratio"), (menu, entry) -> {
            List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof AnimationEditorElement));
            this.editor.history.saveSnapshot();
            for (AbstractEditorElement e : selectedElements) {
                ((AnimationElement)e.element).restoreAspectRatio();
            }
        }).setStackable(true);

    }

}
