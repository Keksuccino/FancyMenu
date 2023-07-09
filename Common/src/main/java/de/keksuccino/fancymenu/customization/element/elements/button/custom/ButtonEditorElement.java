package de.keksuccino.fancymenu.customization.element.elements.button.custom;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionExecutor;
import de.keksuccino.fancymenu.customization.action.ActionRegistry;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.ChooseAnimationScreen;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.actions.ManageActionsScreen;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.ObjectUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ButtonEditorElement extends AbstractEditorElement {

    public ButtonEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        this.rightClickMenu.addClickableEntry("manage_actions", Component.translatable("fancymenu.editor.action.screens.manage_screen.manage"), (menu, entry) -> {
            List<ManageActionsScreen.ActionInstance> l = new ArrayList<>();
            for (ActionExecutor.ActionContainer c : this.getButtonElement().actions) {
                Action bac = ActionRegistry.getAction(c.action);
                if (bac != null) {
                    ManageActionsScreen.ActionInstance i = new ManageActionsScreen.ActionInstance(bac, c.value);
                    l.add(i);
                }
            }
            ManageActionsScreen s = new ManageActionsScreen(l, (call) -> {
                if (call != null) {
                    this.editor.history.saveSnapshot();
                    this.getButtonElement().actions.clear();
                    for (ManageActionsScreen.ActionInstance i : call) {
                        this.getButtonElement().actions.add(new ActionExecutor.ActionContainer(i.action.getIdentifier(), i.value));
                    }
                }
                Minecraft.getInstance().setScreen(this.editor);
            });
            Minecraft.getInstance().setScreen(s);
        }).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.elements.button.manage_actions.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("script"));

        this.rightClickMenu.addSeparatorEntry("button_separator_1");

        ContextMenu buttonBackgroundMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("button_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground"), buttonBackgroundMenu)
                .setStackable(true);

        ContextMenu setBackMenu = new ContextMenu();
        buttonBackgroundMenu.addSubMenuEntry("set_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.set"), setBackMenu)
                .setStackable(true);

        ContextMenu normalBackMenu = new ContextMenu();
        setBackMenu.addSubMenuEntry("set_normal_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.normal"), normalBackMenu)
                .setStackable(true);

        this.addFileChooserContextMenuEntryTo(normalBackMenu, "normal_background_texture",
                        consumes -> (consumes instanceof ButtonEditorElement),
                        null,
                        consumes -> ((ButtonElement)consumes.element).backgroundTextureNormal,
                        (element1, s) -> {
                            ((ButtonElement)element1.element).backgroundTextureNormal = s;
                            ((ButtonElement)element1.element).backgroundAnimationNormal = null;
                        },
                        Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.image"),
                        false,
                        FileFilter.IMAGE_AND_GIF_FILE_FILTER)
                .setStackable(true);

        normalBackMenu.addClickableEntry("normal_background_animation", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation"), (menu, entry) -> {
            List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof ButtonEditorElement));
            String preSelectedAnimation = null;
            List<String> allAnimations = ObjectUtils.getOfAll(String.class, selectedElements, consumes -> ((ButtonElement)consumes.element).backgroundAnimationNormal);
            if (!allAnimations.isEmpty() && ListUtils.allInListEqual(allAnimations)) {
                preSelectedAnimation = allAnimations.get(0);
            }
            ChooseAnimationScreen s = new ChooseAnimationScreen(preSelectedAnimation, (call) -> {
                if (call != null) {
                    this.editor.history.saveSnapshot();
                    for (AbstractEditorElement e : selectedElements) {
                        ((ButtonElement)e.element).backgroundAnimationNormal = call;
                        ((ButtonElement)e.element).backgroundTextureNormal = null;
                    }
                }
                Minecraft.getInstance().setScreen(this.editor);
            });
            Minecraft.getInstance().setScreen(s);
        }).setStackable(true);

        normalBackMenu.addSeparatorEntry("separator_1").setStackable(true);

        normalBackMenu.addClickableEntry("reset_normal_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.reset"), (menu, entry) -> {
            this.editor.history.saveSnapshot();
            List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof ButtonEditorElement));
            for (AbstractEditorElement e : selectedElements) {
                ((ButtonElement)e.element).backgroundTextureNormal = null;
                ((ButtonElement)e.element).backgroundAnimationNormal = null;
            }
        }).setStackable(true);

        ContextMenu hoverBackMenu = new ContextMenu();
        setBackMenu.addSubMenuEntry("set_hover_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.hover"), hoverBackMenu)
                .setStackable(true);

        this.addFileChooserContextMenuEntryTo(hoverBackMenu, "hover_background_texture",
                        consumes -> (consumes instanceof ButtonEditorElement),
                        null,
                        consumes -> ((ButtonElement)consumes.element).backgroundTextureHover,
                        (element1, s) -> {
                            ((ButtonElement)element1.element).backgroundTextureHover = s;
                            ((ButtonElement)element1.element).backgroundAnimationHover = null;
                        },
                        Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.image"),
                        false,
                        FileFilter.IMAGE_AND_GIF_FILE_FILTER)
                .setStackable(true);

        hoverBackMenu.addClickableEntry("hover_background_animation", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation"), (menu, entry) -> {
            List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof ButtonEditorElement));
            String preSelectedAnimation = null;
            List<String> allAnimations = ObjectUtils.getOfAll(String.class, selectedElements, consumes -> ((ButtonElement)consumes.element).backgroundAnimationHover);
            if (!allAnimations.isEmpty() && ListUtils.allInListEqual(allAnimations)) {
                preSelectedAnimation = allAnimations.get(0);
            }
            ChooseAnimationScreen s = new ChooseAnimationScreen(preSelectedAnimation, (call) -> {
                if (call != null) {
                    this.editor.history.saveSnapshot();
                    for (AbstractEditorElement e : selectedElements) {
                        ((ButtonElement)e.element).backgroundAnimationHover = call;
                        ((ButtonElement)e.element).backgroundTextureHover = null;
                    }
                }
                Minecraft.getInstance().setScreen(this.editor);
            });
            Minecraft.getInstance().setScreen(s);
        }).setStackable(true);

        hoverBackMenu.addSeparatorEntry("separator_1").setStackable(true);

        hoverBackMenu.addClickableEntry("reset_hover_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.reset"), (menu, entry) -> {
            this.editor.history.saveSnapshot();
            List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof ButtonEditorElement));
            for (AbstractEditorElement e : selectedElements) {
                ((ButtonElement)e.element).backgroundTextureHover = null;
                ((ButtonElement)e.element).backgroundAnimationHover = null;
            }
        }).setStackable(true);

        buttonBackgroundMenu.addSeparatorEntry("separator_1").setStackable(true);

        this.addBooleanSwitcherContextMenuEntryTo(buttonBackgroundMenu, "loop_animation",
                        consumes -> (consumes instanceof ButtonEditorElement),
                        consumes -> ((ButtonElement)consumes.element).loopBackgroundAnimations,
                        (element1, s) -> ((ButtonElement)element1.element).loopBackgroundAnimations = s,
                        "fancymenu.helper.editor.items.buttons.buttonbackground.loopanimation")
                .setStackable(true);

        this.addBooleanSwitcherContextMenuEntryTo(buttonBackgroundMenu, "restart_animation_on_hover",
                        consumes -> (consumes instanceof ButtonEditorElement),
                        consumes -> ((ButtonElement)consumes.element).restartBackgroundAnimationsOnHover,
                        (element1, s) -> ((ButtonElement)element1.element).restartBackgroundAnimationsOnHover = s,
                        "fancymenu.helper.editor.items.buttons.buttonbackground.restartonhover")
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("button_separator_2").setStackable(true);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "edit_label",
                        consumes -> (consumes instanceof ButtonEditorElement),
                        consumes -> ((ButtonElement)consumes.element).label,
                        (element1, s) -> ((ButtonElement)element1.element).label = s,
                        null, false, true, Component.translatable("fancymenu.editor.items.button.editlabel"),
                        true, null, null, null)
                .setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("text"));

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "edit_hover_label",
                        consumes -> (consumes instanceof ButtonEditorElement),
                        consumes -> ((ButtonElement)consumes.element).hoverLabel,
                        (element1, s) -> ((ButtonElement)element1.element).hoverLabel = s,
                        null, false, true, Component.translatable("fancymenu.editor.items.button.hoverlabel"),
                        true, null, null, null)
                .setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("text"));

        this.rightClickMenu.addSeparatorEntry("button_separator_3").setStackable(true);

        this.addFileChooserContextMenuEntryTo(this.rightClickMenu, "edit_hover_sound",
                        consumes -> (consumes instanceof ButtonEditorElement),
                        null,
                        consumes -> ((ButtonElement)consumes.element).hoverSound,
                        (element1, s) -> ((ButtonElement)element1.element).hoverSound = s,
                        Component.translatable("fancymenu.editor.items.button.hoversound"),
                        true, FileFilter.WAV_AUDIO_FILE_FILTER)
                .setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.addFileChooserContextMenuEntryTo(this.rightClickMenu, "edit_click_sound",
                        consumes -> (consumes instanceof ButtonEditorElement),
                        null,
                        consumes -> ((ButtonElement)consumes.element).clickSound,
                        (element1, s) -> ((ButtonElement)element1.element).clickSound = s,
                        Component.translatable("fancymenu.editor.items.button.clicksound"),
                        true, FileFilter.WAV_AUDIO_FILE_FILTER)
                .setStackable(true)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.rightClickMenu.addSeparatorEntry("button_separator_4").setStackable(true);

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "edit_tooltip",
                        consumes -> (consumes instanceof ButtonEditorElement),
                        consumes -> {
                            String t = ((ButtonElement)consumes.element).tooltip;
                            if (t != null) t = t.replace("%n%", "\n");
                            return t;
                        },
                        (element1, s) -> {
                            if (s != null) {
                                s = s.replace("\n", "%n%");
                            }
                            ((ButtonElement)element1.element).tooltip = s;
                        },
                        null, true, true, Component.translatable("fancymenu.editor.items.button.btndescription"),
                        true, null, buildNoEmptyStringTextValidator(), null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.items.button.btndescription.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("talk"));

    }

    protected ButtonElement getButtonElement() {
        return (ButtonElement) this.element;
    }

}
