package de.keksuccino.fancymenu.customization.element.elements.playerentity;

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

public class PlayerEntityEditorElement extends AbstractEditorElement<PlayerEntityEditorElement, PlayerEntityElement> {

    public PlayerEntityEditorElement(@NotNull PlayerEntityElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
        this.settings.setFadeable(false);
        this.settings.setOpacityChangeable(false);
    }

    @Override
    public void init() {

        super.init();

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "copy_client_player",
                        PlayerEntityEditorElement.class,
                        element -> element.element.copyClientPlayer,
                        (element, s) -> element.element.setCopyClientPlayer(s),
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

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "show_name",
                        PlayerEntityEditorElement.class,
                        element -> element.element.showPlayerName,
                        (element, s) -> element.element.setShowPlayerName(s),
                        "fancymenu.elements.player_entity.show_name")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.show_name.desc")));

        this.rightClickMenu.addSeparatorEntry("player_entity_separator_1");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "auto_skin",
                        PlayerEntityEditorElement.class,
                        element -> element.element.autoSkin,
                        (element, auto) -> {
                            element.element.autoSkin = auto;
                            if (auto) {
                                element.element.setSkinByPlayerName();
                            } else {
                                element.element.skinTextureSupplier = null;
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

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "slim_skin",
                        PlayerEntityEditorElement.class,
                        element -> element.element.slim,
                        (element, s) -> element.element.slim = s,
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
                        consumes -> consumes.element.skinTextureSupplier,
                        (element, supplier) -> {
                            if (supplier != null) {
                                element.element.setSkinBySource(supplier.getSourceWithPrefix());
                            } else {
                                element.element.skinTextureSupplier = null;
                            }
                        },
                        Component.translatable("fancymenu.elements.player_entity.skin_texture"),
                        true, null, true, true, true)
                .setStackable(false)
                .setIcon(ContextMenu.IconFactory.getIcon("image"))
                .setIsActiveSupplier((menu, entry) -> (!this.element.copyClientPlayer && !this.element.autoSkin))
                .setTooltipSupplier((menu, entry) -> {
                    if (this.element.autoSkin) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.skin.auto.blocked_until_disabled"));
                    }
                    if (this.element.copyClientPlayer) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.copy_client_player.blocked_until_disabled"));
                    }
                    return null;
                });

        this.rightClickMenu.addSeparatorEntry("player_entity_separator_2");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "auto_cape",
                        PlayerEntityEditorElement.class,
                        element -> element.element.autoCape,
                        (element, auto) -> {
                            element.element.autoCape = auto;
                            if (auto) {
                                element.element.setCapeByPlayerName();
                            } else {
                                element.element.capeTextureSupplier = null;
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
                        consumes -> consumes.element.capeTextureSupplier,
                        (element, supplier) -> {
                            if (supplier != null) {
                                element.element.setCapeBySource(supplier.getSourceWithPrefix());
                            } else {
                                element.element.capeTextureSupplier = null;
                            }
                        },
                        Component.translatable("fancymenu.elements.player_entity.cape_texture"),
                        true, null, true, true, true)
                .setStackable(false)
                .setIcon(ContextMenu.IconFactory.getIcon("image"))
                .setIsActiveSupplier((menu, entry) -> (!this.element.copyClientPlayer && !this.element.autoCape))
                .setTooltipSupplier((menu, entry) -> {
                    if (this.element.autoCape) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.cape.auto.blocked_until_disabled"));
                    }
                    if (this.element.copyClientPlayer) {
                        return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.copy_client_player.blocked_until_disabled"));
                    }
                    return null;
                });

        this.rightClickMenu.addSeparatorEntry("separator_after_set_cape");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "head_follows_mouse", PlayerEntityEditorElement.class,
                element -> element.element.headFollowsMouse,
                (element, follow) -> element.element.headFollowsMouse = follow,
                "fancymenu.elements.player_entity.head_follows_mouse");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "body_follows_mouse", PlayerEntityEditorElement.class,
                element -> element.element.bodyFollowsMouse,
                (element, follow) -> element.element.bodyFollowsMouse = follow,
                "fancymenu.elements.player_entity.body_follows_mouse");

        this.addCycleContextMenuEntryTo(this.rightClickMenu, "base_pose", List.of(PlayerEntityElement.PlayerPose.values()), PlayerEntityEditorElement.class,
                        consumes -> consumes.element.pose,
                        (playerEntityEditorElement, playerPose) -> playerEntityEditorElement.element.pose = playerPose,
                        (menu, entry, switcherValue) -> switcherValue.getCycleComponent())
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.pose.desc")));

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "body_movement", PlayerEntityEditorElement.class,
                        consumes -> consumes.element.bodyMovement,
                        (playerEntityEditorElement, aBoolean) -> playerEntityEditorElement.element.bodyMovement = aBoolean,
                        "fancymenu.elements.player_entity.body_movement")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.body_movement.desc")));

        this.rightClickMenu.addClickableEntry("entity_pose", Component.translatable("fancymenu.elements.player_entity.edit_pose"),
                (menu, entry) -> {
                    Minecraft.getInstance().setScreen(new PlayerEntityPoseScreen(this.element, this.editor, () -> {
                        Minecraft.getInstance().setScreen(this.editor);
                    }));
                });

        this.rightClickMenu.addSeparatorEntry("separator_after_entity_scale");

        ContextMenu wearablesMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("wearables_menu", Component.translatable("fancymenu.elements.player_entity.wearables"), wearablesMenu);

        this.addWearableEntrySet(wearablesMenu, this.element.leftHandWearable, "left_hand");
        this.addWearableEntrySet(wearablesMenu, this.element.rightHandWearable, "right_hand");
        this.addWearableEntrySet(wearablesMenu, this.element.headWearable, "head");
        this.addWearableEntrySet(wearablesMenu, this.element.chestWearable, "chest");
        this.addWearableEntrySet(wearablesMenu, this.element.legsWearable, "legs");
        this.addWearableEntrySet(wearablesMenu, this.element.feetWearable, "feet");

        this.rightClickMenu.addSeparatorEntry("separator_after_wearables");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "is_baby",
                        PlayerEntityEditorElement.class,
                        element -> element.element.isBaby,
                        (element, s) -> element.element.setIsBaby(s),
                        "fancymenu.elements.player_entity.baby")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.baby.desc")));

        this.rightClickMenu.addSeparatorEntry("player_entity_separator_5");

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "parrot",
                        PlayerEntityEditorElement.class,
                        element -> element.element.hasParrotOnShoulder,
                        (element, s) -> element.element.setHasParrotOnShoulder(s, element.element.parrotOnLeftShoulder),
                        "fancymenu.elements.player_entity.parrot")
                .setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.player_entity.parrot.desc")));

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "parrot_on_left",
                        PlayerEntityEditorElement.class,
                        element -> element.element.parrotOnLeftShoulder,
                        (element, s) -> element.element.setHasParrotOnShoulder(element.element.hasParrotOnShoulder, s),
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


}
