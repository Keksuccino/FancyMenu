package de.keksuccino.fancymenu.customization.element.elements.playerentity;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("all")
public class PlayerEntityEditorElement extends AbstractEditorElement {

    public PlayerEntityEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
        this.settings.setFadeable(false);
        this.settings.setResizeable(false);
        this.settings.setAdvancedSizingSupported(false);
    }

    @Override
    public void init() {

        super.init();

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "copy_client_player",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).copyClientPlayer,
                        (element1, s) -> ((PlayerEntityElement) element1.element).setCopyClientPlayer(s),
                        "fancymenu.helper.editor.items.playerentity.copy_client_player")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.copy_client_player.desc")));

        this.rightClickMenu.addSeparatorEntry("separator_after_copy_client_player");

        this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "set_player_name",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).playerName,
                        (element1, s) -> {
                            PlayerEntityElement i = ((PlayerEntityElement) element1.element);
                            i.setPlayerName(s, true);
                            if (i.autoCape) {
                                i.setCapeByPlayerName();
                            }
                            if (i.autoSkin) {
                                i.setSkinByPlayerName();
                            }
                        },
                        null, false, true, Component.translatable("fancymenu.helper.editor.items.playerentity.set_player_name"),
                        true, null, null, null)
                .setIsActiveSupplier((menu, entry) -> !((PlayerEntityElement) this.element).copyClientPlayer)
                .setTooltipSupplier((menu, entry) -> {
                    if (entry.isActive()) {
                        return Tooltip.of("fancymenu.helper.editor.items.playerentity.set_player_name.desc");
                    }
                    return Tooltip.of("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled");
                });

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "show_name",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).showPlayerName,
                        (element1, s) -> ((PlayerEntityElement) element1.element).setShowPlayerName(s),
                        "fancymenu.helper.editor.items.playerentity.show_name")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.show_name.desc")));

        this.rightClickMenu.addSeparatorEntry("player_entity_separator_1");

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "auto_skin",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).autoSkin,
                        (element1, s) -> {
                            ((PlayerEntityElement) element1.element).autoSkin = s;
                            if (s) {
                                ((PlayerEntityElement) element1.element).setSkinByPlayerName();
                            } else {
                                ((PlayerEntityElement) element1.element).setSkinTextureBySource(null, false);
                            }
                        },
                        "fancymenu.helper.editor.items.playerentity.skin.auto")
                .setIsActiveSupplier((menu, entry) -> !((PlayerEntityElement) this.element).copyClientPlayer)
                .setTooltipSupplier((menu, entry) -> {
                    if (entry.isActive()) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.skin.auto.desc"));
                    }
                    return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled"));
                });

        ContextMenu setSkinMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("set_skin", Component.translatable("fancymenu.helper.editor.items.playerentity.skin.set"), setSkinMenu)
                .setIsActiveSupplier((menu, entry) -> (!((PlayerEntityElement) this.element).copyClientPlayer && !((PlayerEntityElement) this.element).autoSkin))
                .setTooltipSupplier((menu, entry) -> {
                    if (entry.isActive()) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.skin.set.desc"));
                    } else {
                        if (((PlayerEntityElement) this.element).autoSkin) {
                            return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.skin.auto.blocked_until_disabled"));
                        }
                        if (((PlayerEntityElement) this.element).copyClientPlayer) {
                            return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled"));
                        }
                    }
                    return Tooltip.of("");
                });

        this.addFileChooserContextMenuEntryTo(setSkinMenu, "set_local_skin", PlayerEntityEditorElement.class,
                null,
                element -> element.getElement().skinPath,
                (element, path) -> {
                    element.getElement().skinPath = path;
                    element.getElement().setSkinTextureBySource(ScreenCustomization.getAbsoluteGameDirectoryPath(path), false);
                },
                Component.translatable("fancymenu.helper.editor.items.playerentity.skin.set.local"), true,
                file -> file.getAbsolutePath().toLowerCase().endsWith(".png") && FileFilter.RESOURCE_NAME_FILTER.checkFile(file));

        this.addStringInputContextMenuEntryTo(setSkinMenu, "set_web_skin", PlayerEntityEditorElement.class,
                element -> element.getElement().skinUrl,
                (element, url) -> {
                    element.getElement().skinUrl = url;
                    element.getElement().setSkinTextureBySource(url, true);
                },
                null, false, true, Component.translatable("fancymenu.helper.editor.items.playerentity.skin.set.web"),
                true, null, TextValidators.BASIC_URL_TEXT_VALIDATOR, null);

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "slim_skin",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).slim,
                        (element1, s) -> ((PlayerEntityElement) element1.element).slim = s,
                        "fancymenu.helper.editor.items.playerentity.slim")
                .setIsActiveSupplier((menu, entry) -> (!((PlayerEntityElement) this.element).copyClientPlayer && !((PlayerEntityElement) this.element).autoSkin))
                .setTooltipSupplier((menu, entry) -> {
                    if (entry.isActive()) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.slim.desc"));
                    } else {
                        if (((PlayerEntityElement) this.element).autoSkin) {
                            return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.skin.auto.blocked_until_disabled"));
                        }
                        if (((PlayerEntityElement) this.element).copyClientPlayer) {
                            return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled"));
                        }
                    }
                    return Tooltip.of("");
                });

        this.rightClickMenu.addSeparatorEntry("player_entity_separator_2");

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "auto_cape",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).autoCape,
                        (element1, s) -> {
                            ((PlayerEntityElement) element1.element).autoCape = s;
                            if (s) {
                                ((PlayerEntityElement) element1.element).setCapeByPlayerName();
                            } else {
                                ((PlayerEntityElement) element1.element).setCapeTextureBySource(null, false);
                            }
                        },
                        "fancymenu.helper.editor.items.playerentity.cape.auto")
                .setIsActiveSupplier((menu, entry) -> !((PlayerEntityElement) this.element).copyClientPlayer)
                .setTooltipSupplier((menu, entry) -> {
                    if (entry.isActive()) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.cape.auto.desc"));
                    }
                    return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled"));
                });

        ContextMenu setCapeMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("set_cape", Component.translatable("fancymenu.helper.editor.items.playerentity.cape.set"), setCapeMenu)
                .setIsActiveSupplier((menu, entry) -> (!((PlayerEntityElement) this.element).copyClientPlayer && !((PlayerEntityElement) this.element).autoCape))
                .setTooltipSupplier((menu, entry) -> {
                    if (entry.isActive()) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.cape.set.desc"));
                    } else {
                        if (((PlayerEntityElement) this.element).autoCape) {
                            return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.cape.auto.blocked_until_disabled"));
                        }
                        if (((PlayerEntityElement) this.element).copyClientPlayer) {
                            return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled"));
                        }
                    }
                    return Tooltip.of("");
                });

        this.addFileChooserContextMenuEntryTo(setCapeMenu, "set_local_cape", PlayerEntityEditorElement.class,
                null,
                element -> element.getElement().capePath,
                (element, s) -> {
                    element.getElement().capePath = s;
                    element.getElement().setCapeTextureBySource(ScreenCustomization.getAbsoluteGameDirectoryPath(s), false);
                },
                Component.translatable("fancymenu.helper.editor.items.playerentity.cape.set.local"),
                false,
                file -> file.getAbsolutePath().toLowerCase().endsWith(".png") && FileFilter.RESOURCE_NAME_FILTER.checkFile(file));

        this.addStringInputContextMenuEntryTo(setCapeMenu, "set_web_cape",
                PlayerEntityEditorElement.class,
                consumes -> consumes.getElement().capeUrl,
                (element, s) -> {
                    element.getElement().capeUrl = s;
                    element.getElement().setCapeTextureBySource(s, false);
                },
                null, false, true, Component.translatable("fancymenu.helper.editor.items.playerentity.cape.set.web"),
                true, null, TextValidators.BASIC_URL_TEXT_VALIDATOR, null);

        this.rightClickMenu.addSeparatorEntry("separator_after_set_web_cape");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "head_follows_mouse", PlayerEntityEditorElement.class,
                element -> element.getElement().headFollowsMouse,
                (element, follow) -> element.getElement().headFollowsMouse = follow,
                "fancymenu.editor.elements.player_entity.head_follows_mouse");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "body_follows_mouse", PlayerEntityEditorElement.class,
                element -> element.getElement().bodyFollowsMouse,
                (element, follow) -> element.getElement().bodyFollowsMouse = follow,
                "fancymenu.editor.elements.player_entity.body_follows_mouse");

        this.rightClickMenu.addClickableEntry("entity_pose", Component.translatable("fancymenu.editor.elements.player_entity.edit_pose"),
                (menu, entry) -> {
                    Minecraft.getInstance().setScreen(new PlayerEntityPoseScreen(this.getElement(), this.editor, () -> {
                        Minecraft.getInstance().setScreen(this.editor);
                    }));
                });

//        this.addGenericIntegerInputContextMenuEntryTo(this.rightClickMenu, "entity_scale",
//                        consumes -> (consumes instanceof PlayerEntityEditorElement),
//                        consumes -> ((PlayerEntityElement) consumes.element).scale,
//                        (element, scale) -> ((PlayerEntityElement) element.element).scale = scale,
//                        Component.translatable("fancymenu.helper.editor.items.playerentity.scale"),
//                        true, 30, null, null)
//                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.scale.desc")));

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "entity_scale", PlayerEntityEditorElement.class,
                consumes -> consumes.getElement().scale,
                (playerEntityEditorElement, s) -> playerEntityEditorElement.getElement().scale = s,
                null, false, true, Component.translatable("fancymenu.helper.editor.items.playerentity.scale"),
                true, "30", null, null);

        this.rightClickMenu.addSeparatorEntry("separator_after_entity_scale");

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "crouching",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).crouching,
                        (element1, s) -> ((PlayerEntityElement) element1.element).setCrouching(s),
                        "fancymenu.helper.editor.items.playerentity.crouching")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.crouching.desc")));

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "is_baby",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement)consumes.element).isBaby,
                        (element1, s) -> ((PlayerEntityElement)element1.element).setIsBaby(s),
                        "fancymenu.helper.editor.items.playerentity.baby")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.baby.desc")));

        this.rightClickMenu.addSeparatorEntry("player_entity_separator_5");

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "parrot",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).hasParrotOnShoulder,
                        (element1, s) -> ((PlayerEntityElement) element1.element).setHasParrotOnShoulder(s, ((PlayerEntityElement) element1.element).parrotOnLeftShoulder),
                        "fancymenu.helper.editor.items.playerentity.parrot")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.parrot.desc")));

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "parrot_on_left",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).parrotOnLeftShoulder,
                        (element1, s) -> ((PlayerEntityElement) element1.element).setHasParrotOnShoulder(((PlayerEntityElement) element1.element).hasParrotOnShoulder, s),
                        "fancymenu.helper.editor.items.playerentity.parrot_left")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.parrot_left.desc")));

    }

    public PlayerEntityElement getElement() {
        return (PlayerEntityElement) this.element;
    }

}
