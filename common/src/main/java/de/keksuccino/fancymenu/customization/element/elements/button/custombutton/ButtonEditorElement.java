package de.keksuccino.fancymenu.customization.element.elements.button.custombutton;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.ChooseAnimationScreen;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.actions.ManageActionsScreen;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.file.ResourceFile;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.ObjectUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class ButtonEditorElement extends AbstractEditorElement {

    public ButtonEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        boolean isButton = (this.getElement().getWidget() instanceof AbstractButton);
        boolean isSlider = (this.getElement().getWidget() instanceof CustomizableSlider);

        this.rightClickMenu.addClickableEntry("manage_actions", Component.translatable("fancymenu.editor.action.screens.manage_screen.manage"), (menu, entry) -> {
            ManageActionsScreen s = new ManageActionsScreen(this.getElement().getExecutableBlock(), (call) -> {
                if (call != null) {
                    this.editor.history.saveSnapshot();
                    this.getElement().actionExecutor = call;
                }
                Minecraft.getInstance().setScreen(this.editor);
            });
            Minecraft.getInstance().setScreen(s);
        }).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.elements.button.manage_actions.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("script"))
                .setStackable(false);

        this.rightClickMenu.addSeparatorEntry("button_separator_1");

        ContextMenu buttonBackgroundMenu = new ContextMenu();
        if (isSlider || isButton) {
            this.rightClickMenu.addSubMenuEntry("button_background", isButton ? Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground") : Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.alternate.slider"), buttonBackgroundMenu)
                    .setIcon(ContextMenu.IconFactory.getIcon("image"))
                    .setStackable(true);
        }

        ContextMenu setBackMenu = new ContextMenu();
        buttonBackgroundMenu.addSubMenuEntry("set_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.set"), setBackMenu)
                .setStackable(true);

        ContextMenu normalBackMenu = new ContextMenu();
        setBackMenu.addSubMenuEntry("set_normal_background", isButton ? Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.normal") : Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.normal.alternate.slider"), normalBackMenu)
                .setStackable(true);

        this.addImageResourceChooserContextMenuEntryTo(normalBackMenu, "normal_background_texture",
                ButtonEditorElement.class,
                null,
                consumes -> consumes.getElement().backgroundTextureNormal,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.getElement().backgroundTextureNormal = iTextureResourceSupplier;
                    buttonEditorElement.getElement().backgroundAnimationNormal = null;
                }, Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.image"), false, null);

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
        setBackMenu.addSubMenuEntry("set_hover_background", isButton ? Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.hover") : Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.hover.alternate.slider"), hoverBackMenu)
                .setStackable(true);

        this.addImageResourceChooserContextMenuEntryTo(hoverBackMenu, "hover_background_texture",
                ButtonEditorElement.class,
                null,
                consumes -> consumes.getElement().backgroundTextureHover,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.getElement().backgroundTextureHover = iTextureResourceSupplier;
                    buttonEditorElement.getElement().backgroundAnimationHover = null;
                }, Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.image"), false, null);

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

        ContextMenu inactiveBackMenu = new ContextMenu();
        setBackMenu.addSubMenuEntry("set_inactive_background", isButton ? Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.inactive") : Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.inactive.alternate.slider"), inactiveBackMenu)
                .setStackable(true);

        this.addImageResourceChooserContextMenuEntryTo(inactiveBackMenu, "inactive_background_texture",
                ButtonEditorElement.class,
                null,
                consumes -> consumes.getElement().backgroundTextureInactive,
                (buttonEditorElement, iTextureResourceSupplier) -> {
                    buttonEditorElement.getElement().backgroundTextureInactive = iTextureResourceSupplier;
                    buttonEditorElement.getElement().backgroundAnimationInactive = null;
                }, Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.image"), false, null);

        inactiveBackMenu.addClickableEntry("inactive_background_animation", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.type.animation"), (menu, entry) -> {
            List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof ButtonEditorElement));
            String preSelectedAnimation = null;
            List<String> allAnimations = ObjectUtils.getOfAll(String.class, selectedElements, consumes -> ((ButtonElement)consumes.element).backgroundAnimationInactive);
            if (!allAnimations.isEmpty() && ListUtils.allInListEqual(allAnimations)) {
                preSelectedAnimation = allAnimations.get(0);
            }
            ChooseAnimationScreen s = new ChooseAnimationScreen(preSelectedAnimation, (call) -> {
                if (call != null) {
                    this.editor.history.saveSnapshot();
                    for (AbstractEditorElement e : selectedElements) {
                        ((ButtonElement)e.element).backgroundAnimationInactive = call;
                        ((ButtonElement)e.element).backgroundTextureInactive = null;
                    }
                }
                Minecraft.getInstance().setScreen(this.editor);
            });
            Minecraft.getInstance().setScreen(s);
        }).setStackable(true);

        inactiveBackMenu.addSeparatorEntry("separator_after_inactive_back_animation").setStackable(true);

        inactiveBackMenu.addClickableEntry("reset_inactive_background", Component.translatable("fancymenu.helper.editor.items.buttons.buttonbackground.reset"), (menu, entry) -> {
            this.editor.history.saveSnapshot();
            List<AbstractEditorElement> selectedElements = ListUtils.filterList(this.editor.getSelectedElements(), consumes -> (consumes instanceof ButtonEditorElement));
            for (AbstractEditorElement e : selectedElements) {
                ((ButtonElement)e.element).backgroundTextureInactive = null;
                ((ButtonElement)e.element).backgroundAnimationInactive = null;
            }
        }).setStackable(true);

        buttonBackgroundMenu.addSeparatorEntry("separator_1").setStackable(true);

        this.addGenericBooleanSwitcherContextMenuEntryTo(buttonBackgroundMenu, "loop_animation",
                        consumes -> (consumes instanceof ButtonEditorElement),
                        consumes -> ((ButtonElement)consumes.element).loopBackgroundAnimations,
                        (element1, s) -> ((ButtonElement)element1.element).loopBackgroundAnimations = s,
                        "fancymenu.helper.editor.items.buttons.buttonbackground.loopanimation")
                .setStackable(true);

        this.addGenericBooleanSwitcherContextMenuEntryTo(buttonBackgroundMenu, "restart_animation_on_hover",
                        consumes -> (consumes instanceof ButtonEditorElement),
                        consumes -> ((ButtonElement)consumes.element).restartBackgroundAnimationsOnHover,
                        (element1, s) -> ((ButtonElement)element1.element).restartBackgroundAnimationsOnHover = s,
                        "fancymenu.helper.editor.items.buttons.buttonbackground.restartonhover")
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("button_separator_2").setStackable(true);

        if (!isSlider) {

            this.addStringInputContextMenuEntryTo(this.rightClickMenu, "edit_label",
                            ButtonEditorElement.class,
                            consumes -> ((ButtonElement)consumes.element).label,
                            (element1, s) -> ((ButtonElement)element1.element).label = s,
                            null, false, true, Component.translatable(isButton ? "fancymenu.editor.items.button.editlabel" : "fancymenu.editor.items.button.label.generic"),
                            true, null, null, null)
                    .setStackable(true)
                    .setIcon(ContextMenu.IconFactory.getIcon("text"));

            this.addStringInputContextMenuEntryTo(this.rightClickMenu, "edit_hover_label",
                            ButtonEditorElement.class,
                            consumes -> ((ButtonElement)consumes.element).hoverLabel,
                            (element1, s) -> ((ButtonElement)element1.element).hoverLabel = s,
                            null, false, true, Component.translatable(isButton ? "fancymenu.editor.items.button.hoverlabel" : "fancymenu.editor.items.button.hover_label.generic"),
                            true, null, null, null)
                    .setStackable(true)
                    .setIcon(ContextMenu.IconFactory.getIcon("text"));

            this.rightClickMenu.addSeparatorEntry("button_separator_3").setStackable(true);

        }

        this.addAudioResourceChooserContextMenuEntryTo(this.rightClickMenu, "hover_sound",
                        ButtonEditorElement.class,
                        null,
                        consumes -> consumes.getElement().hoverSound,
                        (buttonEditorElement, supplier) -> buttonEditorElement.getElement().hoverSound = supplier,
                        Component.translatable("fancymenu.editor.items.button.hoversound"), true, null)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.addAudioResourceChooserContextMenuEntryTo(this.rightClickMenu, "click_sound",
                        ButtonEditorElement.class,
                        null,
                        consumes -> consumes.getElement().clickSound,
                        (buttonEditorElement, supplier) -> buttonEditorElement.getElement().clickSound = supplier,
                        Component.translatable("fancymenu.editor.items.button.clicksound"), true, null)
                .setIcon(ContextMenu.IconFactory.getIcon("sound"));

        this.rightClickMenu.addSeparatorEntry("button_separator_4").setStackable(true);

        this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "edit_tooltip",
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
                        true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
                .setStackable(true)
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.items.button.btndescription.desc")))
                .setIcon(ContextMenu.IconFactory.getIcon("talk"));

    }

    protected ButtonElement getElement() {
        return (ButtonElement) this.element;
    }

}
