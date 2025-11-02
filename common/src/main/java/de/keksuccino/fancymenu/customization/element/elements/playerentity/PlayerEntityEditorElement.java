package de.keksuccino.fancymenu.customization.element.elements.playerentity;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.item.ItemKeyScreen;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class PlayerEntityEditorElement extends AbstractEditorElement {

    public PlayerEntityEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
        this.settings.setFadeable(false);
        this.settings.setOpacityChangeable(false);
    }

    @Override
    public void init() {

        super.init();

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "copy_client_player",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).copyClientPlayer,
                        (element1, s) -> ((PlayerEntityElement) element1.element).setCopyClientPlayer(s),
                        "fancymenu.elements.player_entity.copy_client_player")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.copy_client_player.desc")));

        this.rightClickMenu.addSeparatorEntry("separator_after_copy_client_player");

        this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "set_player_name",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).playerName,
                        (element1, s) -> {
                            PlayerEntityElement i = ((PlayerEntityElement) element1.element);
                            i.setPlayerName(s);
                            if (i.autoCape) {
                                i.setCapeByPlayerName();
                            }
                            if (i.autoSkin) {
                                i.setSkinByPlayerName();
                            }
                        },
                        null, false, true, Component.translatable("fancymenu.elements.player_entity.set_player_name"),
                        true, null, null, null)
                .setIsActiveSupplier((menu, entry) -> !((PlayerEntityElement) this.element).copyClientPlayer)
                .setTooltipSupplier((menu, entry) -> {
                    if (entry.isActive()) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.set_player_name.desc"));
                    }
                    return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.copy_client_player.blocked_until_disabled"));
                });

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "show_name",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).showPlayerName,
                        (element1, s) -> ((PlayerEntityElement) element1.element).setShowPlayerName(s),
                        "fancymenu.elements.player_entity.show_name")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.show_name.desc")));

        this.rightClickMenu.addSeparatorEntry("player_entity_separator_1");

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "auto_skin",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).autoSkin,
                        (element1, auto) -> {
                            ((PlayerEntityElement) element1.element).autoSkin = auto;
                            if (auto) {
                                ((PlayerEntityElement) element1.element).setSkinByPlayerName();
                            } else {
                                ((PlayerEntityElement) element1.element).skinTextureSupplier = null;
                            }
                        },
                        "fancymenu.elements.player_entity.skin.auto")
                .setIsActiveSupplier((menu, entry) -> !((PlayerEntityElement) this.element).copyClientPlayer)
                .setTooltipSupplier((menu, entry) -> {
                    if (entry.isActive()) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.skin.auto.desc"));
                    }
                    return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.copy_client_player.blocked_until_disabled"));
                });

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "slim_skin",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).slim,
                        (element1, s) -> ((PlayerEntityElement) element1.element).slim = s,
                        "fancymenu.elements.player_entity.slim")
                .setIsActiveSupplier((menu, entry) -> (!((PlayerEntityElement) this.element).copyClientPlayer && !((PlayerEntityElement) this.element).autoSkin))
                .setTooltipSupplier((menu, entry) -> {
                    if (entry.isActive()) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.slim.desc"));
                    } else {
                        if (((PlayerEntityElement) this.element).autoSkin) {
                            return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.skin.auto.blocked_until_disabled"));
                        }
                        if (((PlayerEntityElement) this.element).copyClientPlayer) {
                            return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.copy_client_player.blocked_until_disabled"));
                        }
                    }
                    return null;
                });

        this.addImageResourceChooserContextMenuEntryTo(this.rightClickMenu, "set_skin_texture", PlayerEntityEditorElement.class,
                        null,
                        consumes -> consumes.getElement().skinTextureSupplier,
                        (element, supplier) -> {
                            if (supplier != null) {
                                element.getElement().setSkinBySource(supplier.getSourceWithPrefix());
                            } else {
                                element.getElement().skinTextureSupplier = null;
                            }
                        },
                        Component.translatable("fancymenu.elements.player_entity.skin_texture"),
                        true, null, true, true, true)
                .setStackable(false)
                .setIcon(ContextMenu.IconFactory.getIcon("image"))
                .setIsActiveSupplier((menu, entry) -> (!this.getElement().copyClientPlayer && !this.getElement().autoSkin))
                .setTooltipSupplier((menu, entry) -> {
                    if (this.getElement().autoSkin) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.skin.auto.blocked_until_disabled"));
                    }
                    if (this.getElement().copyClientPlayer) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.copy_client_player.blocked_until_disabled"));
                    }
                    return null;
                });

        this.rightClickMenu.addSeparatorEntry("player_entity_separator_2");

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "auto_cape",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).autoCape,
                        (element1, auto) -> {
                            ((PlayerEntityElement) element1.element).autoCape = auto;
                            if (auto) {
                                ((PlayerEntityElement) element1.element).setCapeByPlayerName();
                            } else {
                                ((PlayerEntityElement) element1.element).capeTextureSupplier = null;
                            }
                        },
                        "fancymenu.elements.player_entity.cape.auto")
                .setIsActiveSupplier((menu, entry) -> !((PlayerEntityElement) this.element).copyClientPlayer)
                .setTooltipSupplier((menu, entry) -> {
                    if (entry.isActive()) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.cape.auto.desc"));
                    }
                    return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.copy_client_player.blocked_until_disabled"));
                });

        this.addImageResourceChooserContextMenuEntryTo(this.rightClickMenu, "set_cape_texture", PlayerEntityEditorElement.class,
                        null,
                        consumes -> consumes.getElement().capeTextureSupplier,
                        (element, supplier) -> {
                            if (supplier != null) {
                                element.getElement().setCapeBySource(supplier.getSourceWithPrefix());
                            } else {
                                element.getElement().capeTextureSupplier = null;
                            }
                        },
                        Component.translatable("fancymenu.elements.player_entity.cape_texture"),
                        true, null, true, true, true)
                .setStackable(false)
                .setIcon(ContextMenu.IconFactory.getIcon("image"))
                .setIsActiveSupplier((menu, entry) -> (!this.getElement().copyClientPlayer && !this.getElement().autoCape))
                .setTooltipSupplier((menu, entry) -> {
                    if (this.getElement().autoCape) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.cape.auto.blocked_until_disabled"));
                    }
                    if (this.getElement().copyClientPlayer) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.copy_client_player.blocked_until_disabled"));
                    }
                    return null;
                });

        this.rightClickMenu.addSeparatorEntry("separator_after_set_cape");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "head_follows_mouse", PlayerEntityEditorElement.class,
                element -> element.getElement().headFollowsMouse,
                (element, follow) -> element.getElement().headFollowsMouse = follow,
                "fancymenu.elements.player_entity.head_follows_mouse");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "body_follows_mouse", PlayerEntityEditorElement.class,
                element -> element.getElement().bodyFollowsMouse,
                (element, follow) -> element.getElement().bodyFollowsMouse = follow,
                "fancymenu.elements.player_entity.body_follows_mouse");

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "base_pose", List.of(PlayerEntityElement.PlayerPose.values()), PlayerEntityEditorElement.class,
                        consumes -> consumes.getElement().pose,
                        (playerEntityEditorElement, playerPose) -> playerEntityEditorElement.getElement().pose = playerPose,
                        (menu, entry, switcherValue) -> switcherValue.getCycleComponent())
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.pose.desc")));

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "body_movement", PlayerEntityEditorElement.class,
                        consumes -> consumes.getElement().bodyMovement,
                        (playerEntityEditorElement, aBoolean) -> playerEntityEditorElement.getElement().bodyMovement = aBoolean,
                        "fancymenu.elements.player_entity.body_movement")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.body_movement.desc")));

        this.rightClickMenu.addClickableEntry("entity_pose", Component.translatable("fancymenu.elements.player_entity.edit_pose"),
                (menu, entry) -> {
                    Minecraft.getInstance().setScreen(new PlayerEntityPoseScreen(this.getElement(), this.editor, () -> {
                        Minecraft.getInstance().setScreen(this.editor);
                    }));
                });

        this.rightClickMenu.addSeparatorEntry("separator_after_entity_scale");

        ContextMenu wearablesMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("wearables_menu", Component.translatable("fancymenu.elements.player_entity.wearables"), wearablesMenu);

        this.addWearableEntrySet(wearablesMenu, this.getElement().leftHandWearable, "left_hand");
        this.addWearableEntrySet(wearablesMenu, this.getElement().rightHandWearable, "right_hand");
        this.addWearableEntrySet(wearablesMenu, this.getElement().headWearable, "head");
        this.addWearableEntrySet(wearablesMenu, this.getElement().chestWearable, "chest");
        this.addWearableEntrySet(wearablesMenu, this.getElement().legsWearable, "legs");
        this.addWearableEntrySet(wearablesMenu, this.getElement().feetWearable, "feet");

        this.rightClickMenu.addSeparatorEntry("separator_after_wearables");

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "is_baby",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement)consumes.element).isBaby,
                        (element1, s) -> ((PlayerEntityElement)element1.element).setIsBaby(s),
                        "fancymenu.elements.player_entity.baby")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.baby.desc")));

        this.rightClickMenu.addSeparatorEntry("player_entity_separator_5");

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "parrot",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).hasParrotOnShoulder,
                        (element1, s) -> ((PlayerEntityElement) element1.element).setHasParrotOnShoulder(s, ((PlayerEntityElement) element1.element).parrotOnLeftShoulder),
                        "fancymenu.elements.player_entity.parrot")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.parrot.desc")));

        this.addGenericBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "parrot_on_left",
                        consumes -> (consumes instanceof PlayerEntityEditorElement),
                        consumes -> ((PlayerEntityElement) consumes.element).parrotOnLeftShoulder,
                        (element1, s) -> ((PlayerEntityElement) element1.element).setHasParrotOnShoulder(((PlayerEntityElement) element1.element).hasParrotOnShoulder, s),
                        "fancymenu.elements.player_entity.parrot_left")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.parrot_left.desc")));

    }

    protected void addWearableEntrySet(@NotNull ContextMenu contextMenu, @NotNull PlayerEntityElement.Wearable wearable, @NotNull String wearableIdentifier) {

        ConsumingSupplier<PlayerEntityEditorElement, String> itemKeyTargetFieldGetter = consumes -> wearable.isEmpty() ? null : wearable.itemKey;
        BiConsumer<PlayerEntityEditorElement, String> itemKeyTargetFieldSetter = (itemEditorElement, s) -> wearable.itemKey = Objects.requireNonNullElse(s, PlayerEntityElement.Wearable.WEARABLE_EMPTY_KEY);

        ContextMenu.ClickableContextMenuEntry<?> itemKeyEntry = this.addStringInputContextMenuEntryTo(contextMenu, "wearable_entry_" + wearableIdentifier, PlayerEntityEditorElement.class,
                        itemKeyTargetFieldGetter,
                        itemKeyTargetFieldSetter,
                        null, false, true, Component.translatable("fancymenu.elements.player_entity.wearables." + wearableIdentifier),
                        true, null, null, null)
                .setStackable(false);

        if (itemKeyEntry instanceof ContextMenu.SubMenuContextMenuEntry subMenuEntry) {

            subMenuEntry.getSubContextMenu().removeEntry("input_value");

            subMenuEntry.getSubContextMenu().addClickableEntryAt(0, "input_value", Component.translatable("fancymenu.common_components.set"), (menu, entry) ->
            {
                if (entry.getStackMeta().isFirstInStack()) {
                    Screen inputScreen = new ItemKeyScreen(itemKeyTargetFieldGetter.get(this), callback -> {
                        if (callback != null) {
                            this.editor.history.saveSnapshot();
                            itemKeyTargetFieldSetter.accept(this, callback);
                        }
                        menu.closeMenu();
                        Minecraft.getInstance().setScreen(this.editor);
                    });
                    Minecraft.getInstance().setScreen(inputScreen);
                }
            }).setStackable(false);

        }

        this.addToggleContextMenuEntryTo(contextMenu, "toggle_enchant_" + wearableIdentifier, PlayerEntityEditorElement.class,
                consumes -> wearable.enchanted,
                (playerEntityEditorElement, aBoolean) -> wearable.enchanted = aBoolean,
                "fancymenu.elements.player_entity.wearables." + wearableIdentifier + ".enchant");

        contextMenu.addSeparatorEntry("separator_after_" + wearableIdentifier);

    }

    public PlayerEntityElement getElement() {
        return (PlayerEntityElement) this.element;
    }

}