package de.keksuccino.fancymenu.customization.backend.element.elements.playerentity;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.backend.element.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.elements.ChooseFilePopup;
import de.keksuccino.fancymenu.rendering.ui.FMContextMenu;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorScreen;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.io.File;

public class PlayerEntityEditorElement extends AbstractEditorElement {

    public PlayerEntityEditorElement(PlayerEntityElementBuilder parentContainer, PlayerEntityElement customizationItemInstance, LayoutEditorScreen handler) {
        super(parentContainer, customizationItemInstance, true, handler, true);
    }

    @Override
    public void init() {

        this.fadeable = false;
        this.supportsAdvancedSizing = false;
        
        super.init();

        PlayerEntityElement item = ((PlayerEntityElement)this.element);

        AdvancedButton copyClientPlayerButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
            if (!item.copyClientPlayer) {
                item.setCopyClientPlayer(true);
            } else {
                item.setCopyClientPlayer(false);
            }
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (!item.copyClientPlayer) {
                    this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.copy_client_player.off"));
                } else {
                    this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.copy_client_player.on"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        copyClientPlayerButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.copy_client_player.desc"), "%n%"));
        this.rightClickContextMenu.addContent(copyClientPlayerButton);

        AdvancedButton setPlayerNameButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.playerentity.set_player_name"), true, (press) -> {
            TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("fancymenu.helper.editor.items.playerentity.set_player_name")), this.editor, null, (call) -> {
                if (call != null) {
                    if (!call.equals(item.playerName)) {
                        this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                        item.setPlayerName(call, true);
                        if (item.autoCape) {
                            item.setCapeByPlayerName();
                        }
                        if (item.autoSkin) {
                            item.setSkinByPlayerName();
                        }
                    }
                }
            });
            s.multilineMode = false;
            if (item.playerName != null) {
                s.setText(StringUtils.convertFormatCodes(item.playerName, "§", "&"));
            }
            Minecraft.getInstance().setScreen(s);
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (!item.copyClientPlayer) {
                    this.active = true;
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.set_player_name.desc"), "%n%"));
                } else {
                    this.active = false;
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled"), "%n%"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        this.rightClickContextMenu.addContent(setPlayerNameButton);

        this.rightClickContextMenu.addSeparator();

        AdvancedButton autoSkinButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
            if (!item.autoSkin) {
                item.autoSkin = true;
                item.setSkinByPlayerName();
            } else {
                item.autoSkin = false;
                item.setSkinTextureBySource(null, false);
            }
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (!item.copyClientPlayer) {
                    this.active = true;
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.skin.auto.desc"), "%n%"));
                    if (!item.autoSkin) {
                        this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.skin.auto.off"));
                    } else {
                        this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.skin.auto.on"));
                    }
                } else {
                    this.active = false;
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled"), "%n%"));
                    this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.skin.auto.off"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        this.rightClickContextMenu.addContent(autoSkinButton);

        FMContextMenu setSkinMenu = new FMContextMenu();
        this.rightClickContextMenu.addChild(setSkinMenu);
        AdvancedButton setSkinButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.playerentity.skin.set"), true, (press) -> {
            setSkinMenu.setParentButton((AdvancedButton) press);
            setSkinMenu.openMenuAt(0, press.y);
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (!item.copyClientPlayer && !item.autoSkin) {
                    this.active = true;
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.skin.set.desc"), "%n%"));
                } else {
                    this.active = false;
                    if (item.autoSkin) {
                        this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.skin.auto.blocked_until_disabled"), "%n%"));
                    }
                    if (item.copyClientPlayer) {
                        this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled"), "%n%"));
                    }
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        this.rightClickContextMenu.addContent(setSkinButton);

        AdvancedButton setLocalSkinButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.playerentity.skin.set.local"), true, (press) -> {
            ChooseFilePopup p = new ChooseFilePopup((call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equals("")) {
                        File home = Minecraft.getInstance().gameDirectory;
                        call = call.replace("\\", "/");
                        File f = new File(call);
                        if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
                            f = new File(Minecraft.getInstance().gameDirectory, call);
                        }
                        String filename = CharacterFilter.getBasicFilenameCharacterFilter().filterForAllowedChars(f.getName());
                        if (f.exists() && f.isFile() && f.getName().endsWith(".png")) {
                            if (filename.equals(f.getName())) {
                                if (call.startsWith(home.getAbsolutePath().replace("\\", "/"))) {
                                    call = call.replace(home.getAbsolutePath().replace("\\", "/"), "");
                                    if (call.startsWith("\\") || call.startsWith("/")) {
                                        call = call.substring(1);
                                    }
                                }
                                if ((item.skinPath == null) || !item.skinPath.equals(call)) {
                                    this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                                    item.setSkinTextureBySource(call, false);
                                }
                            } else {
                                LayoutEditorScreen.displayNotification(Locals.localize("helper.creator.textures.invalidcharacters"), "", "", "", "", "", "");
                            }
                        } else {
                            LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.items.playerentity.texture.invalidtexture.title"), "", Locals.localize("helper.creator.items.playerentity.texture.invalidtexture.desc"), "", "", "", "", "", "");
                        }
                    }
                }
            }, "png");
            if (item.skinPath != null) {
                p.setText(item.skinPath);
            }
            PopupHandler.displayPopup(p);
        });
        setSkinMenu.addContent(setLocalSkinButton);

        AdvancedButton setWebSkinButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.playerentity.skin.set.web"), true, (press) -> {
            TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("fancymenu.helper.editor.items.playerentity.skin.set.web")), this.editor, null, (call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equals("")) {
                        if (!call.equals(item.skinUrl)) {
                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                            item.setSkinTextureBySource(call, true);
                        }
                    }
                }
            });
            s.multilineMode = false;
            if (item.skinUrl != null) {
                s.setText(item.skinUrl);
            }
            Minecraft.getInstance().setScreen(s);
        });
        setSkinMenu.addContent(setWebSkinButton);

        AdvancedButton slimButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
            if (item.slim) {
                item.slim = false;
            } else {
                item.slim = true;
            }
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (!item.copyClientPlayer && !item.autoSkin) {
                    this.active = true;
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.slim.desc"), "%n%"));
                    if (item.slim) {
                        this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.slim.on"));
                    } else {
                        this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.slim.off"));
                    }
                } else {
                    this.active = false;
                    if (item.autoSkin) {
                        this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.skin.auto.blocked_until_disabled"), "%n%"));
                    }
                    if (item.copyClientPlayer) {
                        this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled"), "%n%"));
                    }
                    this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.slim.off"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        this.rightClickContextMenu.addContent(slimButton);

        this.rightClickContextMenu.addSeparator();

        AdvancedButton autoCapeButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
            if (!item.autoCape) {
                item.autoCape = true;
                item.setCapeByPlayerName();
            } else {
                item.autoCape = false;
                item.setCapeTextureBySource(null, false);
            }
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (!item.copyClientPlayer) {
                    this.active = true;
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.cape.auto.desc"), "%n%"));
                    if (!item.autoCape) {
                        this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.cape.auto.off"));
                    } else {
                        this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.cape.auto.on"));
                    }
                } else {
                    this.active = false;
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled"), "%n%"));
                    this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.cape.auto.off"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        this.rightClickContextMenu.addContent(autoCapeButton);

        FMContextMenu setCapeMenu = new FMContextMenu();
        this.rightClickContextMenu.addChild(setCapeMenu);
        AdvancedButton setCapeButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.playerentity.cape.set"), true, (press) -> {
            setCapeMenu.setParentButton((AdvancedButton) press);
            setCapeMenu.openMenuAt(0, press.y);
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (!item.copyClientPlayer && !item.autoCape) {
                    this.active = true;
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.cape.set.desc"), "%n%"));
                } else {
                    this.active = false;
                    if (item.autoCape) {
                        this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.cape.auto.blocked_until_disabled"), "%n%"));
                    }
                    if (item.copyClientPlayer) {
                        this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled"), "%n%"));
                    }
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        this.rightClickContextMenu.addContent(setCapeButton);

        AdvancedButton setLocalCapeButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.playerentity.cape.set.local"), true, (press) -> {
            ChooseFilePopup p = new ChooseFilePopup((call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equals("")) {
                        File home = Minecraft.getInstance().gameDirectory;
                        call = call.replace("\\", "/");
                        File f = new File(call);
                        if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
                            f = new File(Minecraft.getInstance().gameDirectory, call);
                        }
                        String filename = CharacterFilter.getBasicFilenameCharacterFilter().filterForAllowedChars(f.getName());
                        if (f.exists() && f.isFile() && f.getName().endsWith(".png")) {
                            if (filename.equals(f.getName())) {
                                if (call.startsWith(home.getAbsolutePath().replace("\\", "/"))) {
                                    call = call.replace(home.getAbsolutePath().replace("\\", "/"), "");
                                    if (call.startsWith("\\") || call.startsWith("/")) {
                                        call = call.substring(1);
                                    }
                                }
                                if ((item.capePath == null) || !item.capePath.equals(call)) {
                                    this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                                    item.setCapeTextureBySource(call, false);
                                }
                            } else {
                                LayoutEditorScreen.displayNotification(Locals.localize("helper.creator.textures.invalidcharacters"), "", "", "", "", "", "");
                            }
                        } else {
                            LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.items.playerentity.texture.invalidtexture.title"), "", Locals.localize("helper.creator.items.playerentity.texture.invalidtexture.desc"), "", "", "", "", "", "");
                        }
                    }
                }
            }, "png");
            if (item.capePath != null) {
                p.setText(item.capePath);
            }
            PopupHandler.displayPopup(p);
        });
        setCapeMenu.addContent(setLocalCapeButton);

        AdvancedButton setWebCapeButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.playerentity.cape.set.web"), true, (press) -> {
            TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("fancymenu.helper.editor.items.playerentity.cape.set.web")), this.editor, null, (call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equals("")) {
                        if (!call.equals(item.capeUrl)) {
                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                            item.setCapeTextureBySource(call, true);
                        }
                    }
                }
            });
            s.multilineMode = false;
            if (item.capeUrl != null) {
                s.setText(item.capeUrl);
            }
            Minecraft.getInstance().setScreen(s);
        });
        setCapeMenu.addContent(setWebCapeButton);

        this.rightClickContextMenu.addSeparator();

        AdvancedButton followMouseButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
            if (!item.followMouse) {
                item.followMouse = true;
            } else {
                item.followMouse = false;
            }
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (!item.followMouse) {
                    this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.rotation.follow_mouse.off"));
                } else {
                    this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.rotation.follow_mouse.on"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        followMouseButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.rotation.follow_mouse.desc"), "%n%"));
        this.rightClickContextMenu.addContent(followMouseButton);

        AdvancedButton customRotationButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.playerentity.rotation.custom"), true, (press) -> {
            Minecraft.getInstance().setScreen(new PlayerEntityRotationScreen(this.editor, item));
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (!item.followMouse) {
                    this.active = true;
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.rotation.custom.desc"), "%n%"));
                } else {
                    this.active = false;
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.rotation.custom.disabled"), "%n%"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        this.rightClickContextMenu.addContent(customRotationButton);

        this.rightClickContextMenu.addSeparator();

        AdvancedButton scaleButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.playerentity.scale"), true, (press) -> {
            FMTextInputPopup t = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("fancymenu.helper.editor.items.playerentity.scale"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equals("")) {
                        if (MathUtils.isInteger(call)) {
                            int i = Integer.parseInt(call);
                            if (i != item.scale) {
                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                            }
                            item.scale = i;
                        }
                    } else {
                        if (item.scale != 30) {
                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
                        }
                        item.scale = 30;
                    }
                }

            });
            t.setText("" + item.scale);
            PopupHandler.displayPopup(t);
        });
        scaleButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.scale.desc"), "%n%"));
        this.rightClickContextMenu.addContent(scaleButton);

        AdvancedButton crouchingButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
            if (item.crouching) {
                item.setCrouching(false);
            } else {
                item.setCrouching(true);
            }
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (item.crouching) {
                    this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.crouching.on"));
                } else {
                    this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.crouching.off"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        crouchingButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.crouching.desc"), "%n%"));
        this.rightClickContextMenu.addContent(crouchingButton);

        AdvancedButton showNameButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
            if (item.showPlayerName) {
                item.setShowPlayerName(false);
            } else {
                item.setShowPlayerName(true);
            }
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (item.showPlayerName) {
                    this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.show_name.on"));
                } else {
                    this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.show_name.off"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        showNameButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.show_name.desc"), "%n%"));
        this.rightClickContextMenu.addContent(showNameButton);

        this.rightClickContextMenu.addSeparator();

        AdvancedButton parrotButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
            if (item.hasParrotOnShoulder) {
                item.setHasParrotOnShoulder(false, item.parrotOnLeftShoulder);
            } else {
                item.setHasParrotOnShoulder(true, item.parrotOnLeftShoulder);
            }
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (item.hasParrotOnShoulder) {
                    this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.parrot.on"));
                } else {
                    this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.parrot.off"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        parrotButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.parrot.desc"), "%n%"));
        this.rightClickContextMenu.addContent(parrotButton);

        AdvancedButton parrotOnLeftButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
            if (item.parrotOnLeftShoulder) {
                item.setHasParrotOnShoulder(item.hasParrotOnShoulder, false);
            } else {
                item.setHasParrotOnShoulder(item.hasParrotOnShoulder, true);
            }
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (item.parrotOnLeftShoulder) {
                    this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.parrot_left.on"));
                } else {
                    this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.parrot_left.off"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        parrotOnLeftButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.parrot_left.desc"), "%n%"));
        this.rightClickContextMenu.addContent(parrotOnLeftButton);

        this.rightClickContextMenu.addSeparator();

        AdvancedButton babyButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
            if (item.isBaby) {
                item.setIsBaby(false);
            } else {
                item.setIsBaby(true);
            }
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (item.isBaby) {
                    this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.baby.on"));
                } else {
                    this.setMessage(Locals.localize("fancymenu.helper.editor.items.playerentity.baby.off"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        babyButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.playerentity.baby.desc"), "%n%"));
        this.rightClickContextMenu.addContent(babyButton);

    }

    @Override
    public SerializedElement serializeItem() {

        PlayerEntityElement i = ((PlayerEntityElement)this.element);

        SerializedElement sec = new SerializedElement();

        sec.addEntry("copy_client_player", "" + i.copyClientPlayer);
        if (i.playerName != null) {
            sec.addEntry("playername", i.playerName);
        }
        sec.addEntry("auto_skin", "" + i.autoSkin);
        sec.addEntry("auto_cape", "" + i.autoCape);
        sec.addEntry("slim", "" + i.slim);
        if (i.skinUrl != null) {
            sec.addEntry("skinurl", i.skinUrl);
        }
        if (i.skinPath != null) {
            sec.addEntry("skinpath", i.skinPath);
        }
        if (i.capeUrl != null) {
            sec.addEntry("capeurl", i.capeUrl);
        }
        if (i.capePath != null) {
            sec.addEntry("capepath", i.capePath);
        }
        sec.addEntry("scale", "" + i.scale);
        sec.addEntry("parrot", "" + i.hasParrotOnShoulder);
        sec.addEntry("parrot_left_shoulder", "" + i.parrotOnLeftShoulder);
        sec.addEntry("is_baby", "" + i.isBaby);
        sec.addEntry("crouching", "" + i.crouching);
        sec.addEntry("showname", "" + i.showPlayerName);
        sec.addEntry("follow_mouse", "" + i.followMouse);
        sec.addEntry("headrotationx", "" + i.headRotationX);
        sec.addEntry("headrotationy", "" + i.headRotationY);
        sec.addEntry("bodyrotationx", "" + i.bodyRotationX);
        sec.addEntry("bodyrotationy", "" + i.bodyRotationY);

        return sec;

    }

}
