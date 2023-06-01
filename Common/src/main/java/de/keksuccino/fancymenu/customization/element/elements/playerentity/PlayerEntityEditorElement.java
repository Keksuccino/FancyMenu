package de.keksuccino.fancymenu.customization.element.elements.playerentity;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.rendering.ui.screen.filechooser.FileChooserScreen;
import de.keksuccino.fancymenu.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

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

        this.addBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "copy_client_player",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).copyClientPlayer,
                        (element1, s) -> ((PlayerEntityElement) element1.element).setCopyClientPlayer(s),
                        "fancymenu.helper.editor.items.playerentity.copy_client_player")
                .setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.copy_client_player.desc")));

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_player_name", null,
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        null,
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
                        false, true, Component.translatable("fancymenu.helper.editor.items.playerentity.set_player_name"))
                .setIsActiveSupplier((menu, entry) -> !((PlayerEntityElement) this.element).copyClientPlayer)
                .setTooltipSupplier((menu, entry) -> {
                    if (entry.isActive()) {
                        return Tooltip.create("fancymenu.helper.editor.items.playerentity.set_player_name.desc");
                    }
                    return Tooltip.create("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled");
                });

        this.rightClickMenu.addSeparatorEntry("player_entity_separator_1");

        this.addBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "auto_skin",
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
                        return Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.skin.auto.desc"));
                    }
                    return Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled"));
                });

        ContextMenu setSkinMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("set_skin", Component.translatable("fancymenu.helper.editor.items.playerentity.skin.set"), setSkinMenu)
                .setIsActiveSupplier((menu, entry) -> (!((PlayerEntityElement) this.element).copyClientPlayer && !((PlayerEntityElement) this.element).autoSkin))
                .setTooltipSupplier((menu, entry) -> {
                    if (entry.isActive()) {
                        return Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.skin.set.desc"));
                    } else {
                        if (((PlayerEntityElement) this.element).autoSkin) {
                            return Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.skin.auto.blocked_until_disabled"));
                        }
                        if (((PlayerEntityElement) this.element).copyClientPlayer) {
                            return Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled"));
                        }
                    }
                    return Tooltip.create("");
                });

        this.addFileChooserContextMenuEntryTo(setSkinMenu, "set_local_skin",
                consumes -> (consumes instanceof PlayerEntityEditorElement),
                null,
                consumes -> ((PlayerEntityElement) consumes.element).skinPath,
                (element1, s) -> ((PlayerEntityElement) element1.element).setSkinTextureBySource(s, false),
                Component.translatable("fancymenu.helper.editor.items.playerentity.skin.set.local"),
                false,
                file -> file.getAbsolutePath().toLowerCase().endsWith(".png") && FileChooserScreen.RESOURCE_NAME_FILTER.checkFile(file));

        this.addStringInputContextMenuEntryTo(setSkinMenu, "set_web_skin", null,
                consumes -> (consumes instanceof PlayerEntityEditorElement),
                null,
                consumes -> ((PlayerEntityElement) consumes.element).skinUrl,
                (element, s) -> ((PlayerEntityElement) element.element).setSkinTextureBySource(s, false),
                false, false, Component.translatable("fancymenu.helper.editor.items.playerentity.skin.set.web"));

        this.addBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "slim_skin",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).slim,
                        (element1, s) -> ((PlayerEntityElement) element1.element).slim = s,
                        "fancymenu.helper.editor.items.playerentity.slim")
                .setIsActiveSupplier((menu, entry) -> (!((PlayerEntityElement) this.element).copyClientPlayer && !((PlayerEntityElement) this.element).autoSkin))
                .setTooltipSupplier((menu, entry) -> {
                    if (entry.isActive()) {
                        return Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.slim.desc"));
                    } else {
                        if (((PlayerEntityElement) this.element).autoSkin) {
                            return Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.skin.auto.blocked_until_disabled"));
                        }
                        if (((PlayerEntityElement) this.element).copyClientPlayer) {
                            return Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled"));
                        }
                    }
                    return Tooltip.create("");
                });

        this.rightClickMenu.addSeparatorEntry("player_entity_separator_2");

        this.addBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "auto_cape",
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
                        return Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.cape.auto.desc"));
                    }
                    return Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled"));
                });

        ContextMenu setCapeMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("set_cape", Component.translatable("fancymenu.helper.editor.items.playerentity.cape.set"), setCapeMenu)
                .setIsActiveSupplier((menu, entry) -> (!((PlayerEntityElement) this.element).copyClientPlayer && !((PlayerEntityElement) this.element).autoCape))
                .setTooltipSupplier((menu, entry) -> {
                    if (entry.isActive()) {
                        return Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.cape.set.desc"));
                    } else {
                        if (((PlayerEntityElement) this.element).autoCape) {
                            return Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.cape.auto.blocked_until_disabled"));
                        }
                        if (((PlayerEntityElement) this.element).copyClientPlayer) {
                            return Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled"));
                        }
                    }
                    return Tooltip.create("");
                });

        this.addFileChooserContextMenuEntryTo(setCapeMenu, "set_local_cape",
                consumes -> (consumes instanceof PlayerEntityEditorElement),
                null,
                consumes -> ((PlayerEntityElement) consumes.element).capePath,
                (element1, s) -> ((PlayerEntityElement) element1.element).setCapeTextureBySource(s, false),
                Component.translatable("fancymenu.helper.editor.items.playerentity.cape.set.local"),
                false,
                file -> file.getAbsolutePath().toLowerCase().endsWith(".png") && FileChooserScreen.RESOURCE_NAME_FILTER.checkFile(file));

        this.addStringInputContextMenuEntryTo(setCapeMenu, "set_web_cape", null,
                consumes -> (consumes instanceof PlayerEntityEditorElement),
                null,
                consumes -> ((PlayerEntityElement) consumes.element).capeUrl,
                (element, s) -> ((PlayerEntityElement) element.element).setCapeTextureBySource(s, false),
                false, false, Component.translatable("fancymenu.helper.editor.items.playerentity.cape.set.web"));

        this.rightClickMenu.addSeparatorEntry("player_entity_separator_3");

        this.addBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "follow_mouse",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).followMouse,
                        (element1, s) -> ((PlayerEntityElement) element1.element).followMouse = s,
                        "fancymenu.helper.editor.items.playerentity.rotation.follow_mouse")
                .setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.rotation.follow_mouse.desc")));

        this.rightClickMenu.addClickableEntry("custom_rotation", Component.translatable("fancymenu.helper.editor.items.playerentity.rotation.custom"), (menu, entry) ->
                {
                    Minecraft.getInstance().setScreen(new PlayerEntityRotationScreen((PlayerEntityElement) this.element, call -> {
                        Minecraft.getInstance().setScreen(this.editor);
                    }));
                })
                .setIsActiveSupplier((menu, entry) -> !((PlayerEntityElement) this.element).followMouse)
                .setTooltipSupplier((menu, entry) -> {
                    if (entry.isActive()) {
                        return Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.rotation.custom.desc"));
                    }
                    return Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.rotation.custom.disabled"));
                });

        this.rightClickMenu.addSeparatorEntry("player_entity_separator_4");

        this.addIntegerInputContextMenuEntryTo(this.rightClickMenu, "set_scale",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        30,
                        consumes -> ((PlayerEntityElement) consumes.element).scale,
                        (element, scale) -> ((PlayerEntityElement) element.element).scale = scale,
                        Component.translatable("fancymenu.helper.editor.items.playerentity.scale"))
                .setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.scale.desc")));

        this.addBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "crouching",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).crouching,
                        (element1, s) -> ((PlayerEntityElement) element1.element).setCrouching(s),
                        "fancymenu.helper.editor.items.playerentity.crouching")
                .setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.crouching.desc")));

        this.addBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "show_name",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).showPlayerName,
                        (element1, s) -> ((PlayerEntityElement) element1.element).setShowPlayerName(s),
                        "fancymenu.helper.editor.items.playerentity.show_name")
                .setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.show_name.desc")));

        this.addBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "is_baby",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement)consumes.element).isBaby,
                        (element1, s) -> ((PlayerEntityElement)element1.element).setIsBaby(s),
                        "fancymenu.helper.editor.items.playerentity.baby")
                .setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.baby.desc")));

        this.rightClickMenu.addSeparatorEntry("player_entity_separator_5");

        this.addBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "parrot",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).hasParrotOnShoulder,
                        (element1, s) -> ((PlayerEntityElement) element1.element).setHasParrotOnShoulder(s, ((PlayerEntityElement) element1.element).parrotOnLeftShoulder),
                        "fancymenu.helper.editor.items.playerentity.parrot")
                .setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.parrot.desc")));

        this.addBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "parrot_on_left",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).parrotOnLeftShoulder,
                        (element1, s) -> ((PlayerEntityElement) element1.element).setHasParrotOnShoulder(((PlayerEntityElement) element1.element).hasParrotOnShoulder, s),
                        "fancymenu.helper.editor.items.playerentity.parrot_left")
                .setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.playerentity.parrot_left.desc")));

    }

}
