package de.keksuccino.fancymenu.customization.element.elements.playerentity;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import org.jetbrains.annotations.NotNull;

public class PlayerEntityEditorElement extends AbstractEditorElement {

    public PlayerEntityEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
        this.settings.setFadeable(false);
        this.settings.setAdvancedSizingSupported(false);
    }

    @Override
    public void init() {
        
        super.init();

        //TODO add entries

//        PlayerEntityElement item = ((PlayerEntityElement)this.element);
//
//        AdvancedButton copyClientPlayerButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
//            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//            if (!item.copyClientPlayer) {
//                item.setCopyClientPlayer(true);
//            } else {
//                item.setCopyClientPlayer(false);
//            }
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (!item.copyClientPlayer) {
//                    this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.copy_client_player.off"));
//                } else {
//                    this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.copy_client_player.on"));
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        copyClientPlayerButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.copy_client_player.desc")));
//        this.rightClickContextMenu.addContent(copyClientPlayerButton);
//
//        AdvancedButton setPlayerNameButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.helper.editor.items.playerentity.set_player_name"), true, (press) -> {
//            TextEditorScreen s = new TextEditorScreen(Component.literal(I18n.get("fancymenu.helper.editor.items.playerentity.set_player_name")), this.editor, null, (call) -> {
//                if (call != null) {
//                    if (!call.equals(item.playerName)) {
//                        this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        item.setPlayerName(call, true);
//                        if (item.autoCape) {
//                            item.setCapeByPlayerName();
//                        }
//                        if (item.autoSkin) {
//                            item.setSkinByPlayerName();
//                        }
//                    }
//                }
//            });
//            s.multilineMode = false;
//            if (item.playerName != null) {
//                s.setText(StringUtils.convertFormatCodes(item.playerName, "§", "&"));
//            }
//            Minecraft.getInstance().setScreen(s);
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (!item.copyClientPlayer) {
//                    this.active = true;
//                    this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.set_player_name.desc")));
//                } else {
//                    this.active = false;
//                    this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled")));
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        this.rightClickContextMenu.addContent(setPlayerNameButton);
//
//        this.rightClickContextMenu.addSeparator();
//
//        AdvancedButton autoSkinButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
//            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//            if (!item.autoSkin) {
//                item.autoSkin = true;
//                item.setSkinByPlayerName();
//            } else {
//                item.autoSkin = false;
//                item.setSkinTextureBySource(null, false);
//            }
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (!item.copyClientPlayer) {
//                    this.active = true;
//                    this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.skin.auto.desc")));
//                    if (!item.autoSkin) {
//                        this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.skin.auto.off"));
//                    } else {
//                        this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.skin.auto.on"));
//                    }
//                } else {
//                    this.active = false;
//                    this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled")));
//                    this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.skin.auto.off"));
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        this.rightClickContextMenu.addContent(autoSkinButton);
//
//        ContextMenu setSkinMenu = new ContextMenu();
//        this.rightClickContextMenu.addChild(setSkinMenu);
//        AdvancedButton setSkinButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.helper.editor.items.playerentity.skin.set"), true, (press) -> {
//            setSkinMenu.setParentButton((AdvancedButton) press);
//            setSkinMenu.openMenuAt(0, press.y);
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (!item.copyClientPlayer && !item.autoSkin) {
//                    this.active = true;
//                    this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.skin.set.desc")));
//                } else {
//                    this.active = false;
//                    if (item.autoSkin) {
//                        this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.skin.auto.blocked_until_disabled")));
//                    }
//                    if (item.copyClientPlayer) {
//                        this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled")));
//                    }
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        this.rightClickContextMenu.addContent(setSkinButton);
//
//        AdvancedButton setLocalSkinButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.helper.editor.items.playerentity.skin.set.local"), true, (press) -> {
//            ChooseFilePopup p = new ChooseFilePopup((call) -> {
//                if (call != null) {
//                    if (!call.replace(" ", "").equals("")) {
//                        File home = Minecraft.getInstance().gameDirectory;
//                        call = call.replace("\\", "/");
//                        File f = new File(call);
//                        if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
//                            f = new File(Minecraft.getInstance().gameDirectory, call);
//                        }
//                        String filename = CharacterFilter.getBasicFilenameCharacterFilter().filterForAllowedChars(f.getName());
//                        if (f.exists() && f.isFile() && f.getName().endsWith(".png")) {
//                            if (filename.equals(f.getName())) {
//                                if (call.startsWith(home.getAbsolutePath().replace("\\", "/"))) {
//                                    call = call.replace(home.getAbsolutePath().replace("\\", "/"), "");
//                                    if (call.startsWith("\\") || call.startsWith("/")) {
//                                        call = call.substring(1);
//                                    }
//                                }
//                                if ((item.skinPath == null) || !item.skinPath.equals(call)) {
//                                    this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                                    item.setSkinTextureBySource(call, false);
//                                }
//                            } else {
//                                UIBase.displayNotification(I18n.get("fancymenu.editor.textures.invalidcharacters"), "", "", "", "", "", "");
//                            }
//                        } else {
//                            UIBase.displayNotification("§c§l" + I18n.get("fancymenu.editor.items.playerentity.texture.invalidtexture.title"), "", I18n.get("fancymenu.editor.items.playerentity.texture.invalidtexture.desc"), "", "", "", "", "", "");
//                        }
//                    }
//                }
//            }, "png");
//            if (item.skinPath != null) {
//                p.setText(item.skinPath);
//            }
//            PopupHandler.displayPopup(p);
//        });
//        setSkinMenu.addContent(setLocalSkinButton);
//
//        AdvancedButton setWebSkinButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.helper.editor.items.playerentity.skin.set.web"), true, (press) -> {
//            TextEditorScreen s = new TextEditorScreen(Component.literal(I18n.get("fancymenu.helper.editor.items.playerentity.skin.set.web")), this.editor, null, (call) -> {
//                if (call != null) {
//                    if (!call.replace(" ", "").equals("")) {
//                        if (!call.equals(item.skinUrl)) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                            item.setSkinTextureBySource(call, true);
//                        }
//                    }
//                }
//            });
//            s.multilineMode = false;
//            if (item.skinUrl != null) {
//                s.setText(item.skinUrl);
//            }
//            Minecraft.getInstance().setScreen(s);
//        });
//        setSkinMenu.addContent(setWebSkinButton);
//
//        AdvancedButton slimButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
//            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//            if (item.slim) {
//                item.slim = false;
//            } else {
//                item.slim = true;
//            }
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (!item.copyClientPlayer && !item.autoSkin) {
//                    this.active = true;
//                    this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.slim.desc")));
//                    if (item.slim) {
//                        this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.slim.on"));
//                    } else {
//                        this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.slim.off"));
//                    }
//                } else {
//                    this.active = false;
//                    if (item.autoSkin) {
//                        this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.skin.auto.blocked_until_disabled")));
//                    }
//                    if (item.copyClientPlayer) {
//                        this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled")));
//                    }
//                    this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.slim.off"));
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        this.rightClickContextMenu.addContent(slimButton);
//
//        this.rightClickContextMenu.addSeparator();
//
//        AdvancedButton autoCapeButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
//            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//            if (!item.autoCape) {
//                item.autoCape = true;
//                item.setCapeByPlayerName();
//            } else {
//                item.autoCape = false;
//                item.setCapeTextureBySource(null, false);
//            }
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (!item.copyClientPlayer) {
//                    this.active = true;
//                    this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.cape.auto.desc")));
//                    if (!item.autoCape) {
//                        this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.cape.auto.off"));
//                    } else {
//                        this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.cape.auto.on"));
//                    }
//                } else {
//                    this.active = false;
//                    this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled")));
//                    this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.cape.auto.off"));
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        this.rightClickContextMenu.addContent(autoCapeButton);
//
//        ContextMenu setCapeMenu = new ContextMenu();
//        this.rightClickContextMenu.addChild(setCapeMenu);
//        AdvancedButton setCapeButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.helper.editor.items.playerentity.cape.set"), true, (press) -> {
//            setCapeMenu.setParentButton((AdvancedButton) press);
//            setCapeMenu.openMenuAt(0, press.y);
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (!item.copyClientPlayer && !item.autoCape) {
//                    this.active = true;
//                    this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.cape.set.desc")));
//                } else {
//                    this.active = false;
//                    if (item.autoCape) {
//                        this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.cape.auto.blocked_until_disabled")));
//                    }
//                    if (item.copyClientPlayer) {
//                        this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.copy_client_player.blocked_until_disabled")));
//                    }
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        this.rightClickContextMenu.addContent(setCapeButton);
//
//        AdvancedButton setLocalCapeButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.helper.editor.items.playerentity.cape.set.local"), true, (press) -> {
//            ChooseFilePopup p = new ChooseFilePopup((call) -> {
//                if (call != null) {
//                    if (!call.replace(" ", "").equals("")) {
//                        File home = Minecraft.getInstance().gameDirectory;
//                        call = call.replace("\\", "/");
//                        File f = new File(call);
//                        if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
//                            f = new File(Minecraft.getInstance().gameDirectory, call);
//                        }
//                        String filename = CharacterFilter.getBasicFilenameCharacterFilter().filterForAllowedChars(f.getName());
//                        if (f.exists() && f.isFile() && f.getName().endsWith(".png")) {
//                            if (filename.equals(f.getName())) {
//                                if (call.startsWith(home.getAbsolutePath().replace("\\", "/"))) {
//                                    call = call.replace(home.getAbsolutePath().replace("\\", "/"), "");
//                                    if (call.startsWith("\\") || call.startsWith("/")) {
//                                        call = call.substring(1);
//                                    }
//                                }
//                                if ((item.capePath == null) || !item.capePath.equals(call)) {
//                                    this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                                    item.setCapeTextureBySource(call, false);
//                                }
//                            } else {
//                                UIBase.displayNotification(I18n.get("fancymenu.editor.textures.invalidcharacters"), "", "", "", "", "", "");
//                            }
//                        } else {
//                            UIBase.displayNotification("§c§l" + I18n.get("fancymenu.editor.items.playerentity.texture.invalidtexture.title"), "", I18n.get("fancymenu.editor.items.playerentity.texture.invalidtexture.desc"), "", "", "", "", "", "");
//                        }
//                    }
//                }
//            }, "png");
//            if (item.capePath != null) {
//                p.setText(item.capePath);
//            }
//            PopupHandler.displayPopup(p);
//        });
//        setCapeMenu.addContent(setLocalCapeButton);
//
//        AdvancedButton setWebCapeButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.helper.editor.items.playerentity.cape.set.web"), true, (press) -> {
//            TextEditorScreen s = new TextEditorScreen(Component.literal(I18n.get("fancymenu.helper.editor.items.playerentity.cape.set.web")), this.editor, null, (call) -> {
//                if (call != null) {
//                    if (!call.replace(" ", "").equals("")) {
//                        if (!call.equals(item.capeUrl)) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                            item.setCapeTextureBySource(call, true);
//                        }
//                    }
//                }
//            });
//            s.multilineMode = false;
//            if (item.capeUrl != null) {
//                s.setText(item.capeUrl);
//            }
//            Minecraft.getInstance().setScreen(s);
//        });
//        setCapeMenu.addContent(setWebCapeButton);
//
//        this.rightClickContextMenu.addSeparator();
//
//        AdvancedButton followMouseButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
//            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//            if (!item.followMouse) {
//                item.followMouse = true;
//            } else {
//                item.followMouse = false;
//            }
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (!item.followMouse) {
//                    this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.rotation.follow_mouse.off"));
//                } else {
//                    this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.rotation.follow_mouse.on"));
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        followMouseButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.rotation.follow_mouse.desc")));
//        this.rightClickContextMenu.addContent(followMouseButton);
//
//        AdvancedButton customRotationButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.helper.editor.items.playerentity.rotation.custom"), true, (press) -> {
//            Minecraft.getInstance().setScreen(new PlayerEntityRotationScreen(this.editor, item));
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (!item.followMouse) {
//                    this.active = true;
//                    this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.rotation.custom.desc")));
//                } else {
//                    this.active = false;
//                    this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.rotation.custom.disabled")));
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        this.rightClickContextMenu.addContent(customRotationButton);
//
//        this.rightClickContextMenu.addSeparator();
//
//        AdvancedButton scaleButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.helper.editor.items.playerentity.scale"), true, (press) -> {
//            FMTextInputPopup t = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + I18n.get("fancymenu.helper.editor.items.playerentity.scale"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
//                if (call != null) {
//                    if (!call.replace(" ", "").equals("")) {
//                        if (MathUtils.isInteger(call)) {
//                            int i = Integer.parseInt(call);
//                            if (i != item.scale) {
//                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                            }
//                            item.scale = i;
//                        }
//                    } else {
//                        if (item.scale != 30) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        item.scale = 30;
//                    }
//                }
//
//            });
//            t.setText("" + item.scale);
//            PopupHandler.displayPopup(t);
//        });
//        scaleButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.scale.desc")));
//        this.rightClickContextMenu.addContent(scaleButton);
//
//        AdvancedButton crouchingButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
//            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//            if (item.crouching) {
//                item.setCrouching(false);
//            } else {
//                item.setCrouching(true);
//            }
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (item.crouching) {
//                    this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.crouching.on"));
//                } else {
//                    this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.crouching.off"));
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        crouchingButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.crouching.desc")));
//        this.rightClickContextMenu.addContent(crouchingButton);
//
//        AdvancedButton showNameButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
//            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//            if (item.showPlayerName) {
//                item.setShowPlayerName(false);
//            } else {
//                item.setShowPlayerName(true);
//            }
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (item.showPlayerName) {
//                    this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.show_name.on"));
//                } else {
//                    this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.show_name.off"));
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        showNameButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.show_name.desc")));
//        this.rightClickContextMenu.addContent(showNameButton);
//
//        this.rightClickContextMenu.addSeparator();
//
//        AdvancedButton parrotButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
//            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//            if (item.hasParrotOnShoulder) {
//                item.setHasParrotOnShoulder(false, item.parrotOnLeftShoulder);
//            } else {
//                item.setHasParrotOnShoulder(true, item.parrotOnLeftShoulder);
//            }
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (item.hasParrotOnShoulder) {
//                    this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.parrot.on"));
//                } else {
//                    this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.parrot.off"));
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        parrotButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.parrot.desc")));
//        this.rightClickContextMenu.addContent(parrotButton);
//
//        AdvancedButton parrotOnLeftButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
//            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//            if (item.parrotOnLeftShoulder) {
//                item.setHasParrotOnShoulder(item.hasParrotOnShoulder, false);
//            } else {
//                item.setHasParrotOnShoulder(item.hasParrotOnShoulder, true);
//            }
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (item.parrotOnLeftShoulder) {
//                    this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.parrot_left.on"));
//                } else {
//                    this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.parrot_left.off"));
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        parrotOnLeftButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.parrot_left.desc")));
//        this.rightClickContextMenu.addContent(parrotOnLeftButton);
//
//        this.rightClickContextMenu.addSeparator();
//
//        AdvancedButton babyButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
//            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//            if (item.isBaby) {
//                item.setIsBaby(false);
//            } else {
//                item.setIsBaby(true);
//            }
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (item.isBaby) {
//                    this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.baby.on"));
//                } else {
//                    this.setMessage(I18n.get("fancymenu.helper.editor.items.playerentity.baby.off"));
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        babyButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.playerentity.baby.desc")));
//        this.rightClickContextMenu.addContent(babyButton);

    }

}
