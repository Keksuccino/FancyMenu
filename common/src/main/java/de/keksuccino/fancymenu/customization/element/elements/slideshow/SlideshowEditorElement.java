package de.keksuccino.fancymenu.customization.element.elements.slideshow;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.ChooseSlideshowScreen;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.ObjectUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SlideshowEditorElement extends AbstractEditorElement<SlideshowEditorElement, SlideshowElement> {

    public SlideshowEditorElement(@NotNull SlideshowElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.rightClickMenu.addClickableEntry("choose_slideshow", Component.translatable("fancymenu.elements.slideshow.set_slideshow"), (menu, entry) -> {
            List<AbstractEditorElement<?,?>> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof SlideshowEditorElement));
            String preSelectedSlideshow = null;
            List<String> allSlideshows = ObjectUtils.getOfAll(String.class, selectedElements, consumes -> ((SlideshowElement)consumes.element).slideshowName);
            if (!allSlideshows.isEmpty() && ListUtils.allInListEqual(allSlideshows)) {
                preSelectedSlideshow = allSlideshows.get(0);
            }
            ChooseSlideshowScreen s = new ChooseSlideshowScreen(preSelectedSlideshow, (call) -> {
                if (call != null) {
                    this.editor.history.saveSnapshot();
                    for (AbstractEditorElement<?,?> e : selectedElements) {
                        ((SlideshowElement)e.element).slideshowName = call;
                    }
                }
                Minecraft.getInstance().setScreen(this.editor);
            });
            Minecraft.getInstance().setScreen(s);
        }).setStackable(true);

        this.rightClickMenu.addClickableEntry("restore_aspect_ratio", Component.translatable("fancymenu.elements.slideshow.restore_aspect_ratio"), (menu, entry) -> {
            List<AbstractEditorElement<?,?>> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof SlideshowEditorElement));
            this.editor.history.saveSnapshot();
            for (AbstractEditorElement<?,?> e : selectedElements) {
                ((SlideshowElement)e.element).restoreAspectRatio();
            }
        }).setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("aspect_ratio"));

    }

}
