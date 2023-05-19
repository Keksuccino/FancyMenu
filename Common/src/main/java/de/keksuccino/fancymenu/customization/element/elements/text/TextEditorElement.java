
package de.keksuccino.fancymenu.customization.element.elements.text;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.elements.ChooseFilePopup;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.ContextMenu;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TextEditorElement extends AbstractEditorElement {

    public TextEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
        super(element, editor);
    }

    @Override
    public void init() {

        super.init();

        //TODO add entries

//        TextElement i = ((TextElement)this.element);
//
//        //SOURCE MODE BUTTON
//        AdvancedButton toggleSourceModeButton = new AdvancedButton(0, 0, 0, 0, "", (press) -> {
//            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//            if (i.sourceMode == TextElement.SourceMode.LOCAL_SOURCE) {
//                i.sourceMode = TextElement.SourceMode.WEB_SOURCE;
//            } else if (i.sourceMode == TextElement.SourceMode.WEB_SOURCE) {
//                i.sourceMode = TextElement.SourceMode.DIRECT;
//            } else if (i.sourceMode == TextElement.SourceMode.DIRECT) {
//                i.sourceMode = TextElement.SourceMode.LOCAL_SOURCE;
//            }
//            i.source = null;
//            i.updateContent();
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (i.sourceMode == TextElement.SourceMode.LOCAL_SOURCE) {
//                    this.setMessage(I18n.get("fancymenu.customization.items.text.source_mode.mode.local"));
//                }
//                if (i.sourceMode == TextElement.SourceMode.WEB_SOURCE) {
//                    this.setMessage(I18n.get("fancymenu.customization.items.text.source_mode.mode.web"));
//                }
//                if (i.sourceMode == TextElement.SourceMode.DIRECT) {
//                    this.setMessage(I18n.get("fancymenu.customization.items.text.source_mode.mode.direct"));
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        toggleSourceModeButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.customization.items.text.source_mode.desc")));
//        this.rightClickContextMenu.addContent(toggleSourceModeButton);
//
//        //SET SOURCE BUTTON
//        AdvancedButton setSourceButton = new AdvancedButton(0, 0, 0, 0, "", (press) -> {
//            if (i.sourceMode == TextElement.SourceMode.LOCAL_SOURCE) {
//                ChooseFilePopup p = new ChooseFilePopup((call) -> {
//                    if (call != null) {
//                        if (call.length() == 0) {
//                            if (i.source != null) {
//                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                            }
//                            i.source = null;
//                        } else {
//                            if ((i.source == null) || !i.source.equals(call)) {
//                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                            }
//                            i.source = call;
//                        }
//                        i.updateContent();
//                    }
//                }, "txt");
//                if (i.source != null) {
//                    File f = new File(i.source);
//                    if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
//                        f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + i.source);
//                    }
//                    if (f.isFile()) {
//                        p.setText(i.source);
//                    }
//                }
//                PopupHandler.displayPopup(p);
//            }
//            if ((i.sourceMode == TextElement.SourceMode.DIRECT) || (i.sourceMode == TextElement.SourceMode.WEB_SOURCE)) {
//                TextEditorScreen s = new TextEditorScreen(press.getMessage(), this.editor, null, (call) -> {
//                    if (call != null) {
//                        call = call.replace("\n");
//                        if (call.length() == 0) {
//                            if (i.source != null) {
//                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                            }
//                            i.source = null;
//                        } else {
//                            if ((i.source == null) || !i.source.equals(call)) {
//                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                            }
//                            i.source = call;
//                        }
//                        i.updateContent();
//                    }
//                });
//                if (i.sourceMode != TextElement.SourceMode.DIRECT) {
//                    s.multilineMode = false;
//                }
//                if (i.source != null) {
//                    if (i.sourceMode == TextElement.SourceMode.DIRECT) {
//                        s.setText(i.source.replace("%n%", "\n").replace("\\n", "\n"));
//                    } else {
//                        s.setText(i.source);
//                    }
//                }
//                Minecraft.getInstance().setScreen(s);
//            }
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (i.sourceMode == TextElement.SourceMode.LOCAL_SOURCE) {
//                    this.setMessage(I18n.get("fancymenu.customization.items.text.set_source.local"));
//                    this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.customization.items.text.set_source.local.desc")));
//                }
//                if (i.sourceMode == TextElement.SourceMode.WEB_SOURCE) {
//                    this.setMessage(I18n.get("fancymenu.customization.items.text.set_source.web"));
//                    this.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.customization.items.text.set_source.web.desc")));
//                }
//                if (i.sourceMode == TextElement.SourceMode.DIRECT) {
//                    this.setMessage(I18n.get("fancymenu.customization.items.text.set_source.direct"));
//                    List<String> l = new ArrayList<>();
//                    for (String s : LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.customization.items.text.set_source.direct.desc"))) {
//                        l.add(s.replace("<line_break_code>"));
//                    }
//                    this.setDescription(l.toArray(new String[0]));
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        this.rightClickContextMenu.addContent(setSourceButton);
//
//        this.rightClickContextMenu.addSeparator();
//
//        //CASE MODE BUTTON
//        AdvancedButton caseModeButton = new AdvancedButton(0, 0, 0, 0, "", (press) -> {
//            if (i.caseMode == TextElement.CaseMode.NORMAL) {
//                i.caseMode = TextElement.CaseMode.ALL_LOWER;
//            } else if (i.caseMode == TextElement.CaseMode.ALL_LOWER) {
//                i.caseMode = TextElement.CaseMode.ALL_UPPER;
//            } else if (i.caseMode == TextElement.CaseMode.ALL_UPPER) {
//                i.caseMode = TextElement.CaseMode.NORMAL;
//            }
//            i.updateContent();
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (i.caseMode == TextElement.CaseMode.NORMAL) {
//                    this.setMessage(I18n.get("fancymenu.customization.items.text.case_mode.normal"));
//                }
//                if (i.caseMode == TextElement.CaseMode.ALL_LOWER) {
//                    this.setMessage(I18n.get("fancymenu.customization.items.text.case_mode.lower"));
//                }
//                if (i.caseMode == TextElement.CaseMode.ALL_UPPER) {
//                    this.setMessage(I18n.get("fancymenu.customization.items.text.case_mode.upper"));
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        this.rightClickContextMenu.addContent(caseModeButton);
//
//        //SCALE BUTTON
//        AdvancedButton scaleButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.customization.items.text.scale"), (press) -> {
//            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), I18n.get("fancymenu.customization.items.text.scale"), CharacterFilter.getDoubleCharacterFiler(), 240, (call) -> {
//                if (call != null) {
//                    if (MathUtils.isFloat(call)) {
//                        float s = Float.parseFloat(call);
//                        if (s < 0.2F) {
//                            s = 0.2F;
//                        }
//                        if (i.scale != s) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        i.scale = s;
//                    } else {
//                        if (i.scale != 1.0F) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        i.scale = 1.0F;
//                    }
//                    i.updateContent();
//                }
//            });
//            p.setText("" + i.scale);
//            PopupHandler.displayPopup(p);
//        });
//        this.rightClickContextMenu.addContent(scaleButton);
//
//        //SHADOW BUTTON
//        AdvancedButton shadowButton = new AdvancedButton(0, 0, 0, 0, "", (press) -> {
//            if (i.shadow) {
//                i.shadow = false;
//            } else {
//                i.shadow = true;
//            }
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (i.shadow) {
//                    this.setMessage(I18n.get("fancymenu.customization.items.text.shadow.on"));
//                } else {
//                    this.setMessage(I18n.get("fancymenu.customization.items.text.shadow.off"));
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        this.rightClickContextMenu.addContent(shadowButton);
//
//        //ALIGNMENT BUTTON
//        AdvancedButton alignmentButton = new AdvancedButton(0, 0, 0, 0, "", (press) -> {
//            if (i.alignment == AbstractElement.Alignment.LEFT) {
//                i.alignment = AbstractElement.Alignment.CENTERED;
//            } else if (i.alignment == AbstractElement.Alignment.CENTERED) {
//                i.alignment = AbstractElement.Alignment.RIGHT;
//            } else if (i.alignment == AbstractElement.Alignment.RIGHT) {
//                i.alignment = AbstractElement.Alignment.LEFT;
//            }
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (i.alignment == AbstractElement.Alignment.LEFT) {
//                    this.setMessage(I18n.get("fancymenu.customization.items.text.alignment.left"));
//                }
//                if (i.alignment == AbstractElement.Alignment.CENTERED) {
//                    this.setMessage(I18n.get("fancymenu.customization.items.text.alignment.center"));
//                }
//                if (i.alignment == AbstractElement.Alignment.RIGHT) {
//                    this.setMessage(I18n.get("fancymenu.customization.items.text.alignment.right"));
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        this.rightClickContextMenu.addContent(alignmentButton);
//
//        //BASE COLOR BUTTON
//        AdvancedButton baseColorButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.customization.items.text.base_color"), (press) -> {
//            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), I18n.get("fancymenu.customization.items.text.base_color"), null, 240, (call) -> {
//                if (call != null) {
//                    if (call.replace(" ", "").equals("")) {
//                        if (i.baseColorHex != null) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        i.baseColorHex = null;
//                    } else {
//                        Color c = RenderUtils.getColorFromHexString(call);
//                        if (c != null) {
//                            if ((i.baseColorHex == null) || !i.baseColorHex.equals(call)) {
//                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                            }
//                            i.baseColorHex = call;
//                        } else {
//                            if (i.baseColorHex != null) {
//                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                            }
//                            i.baseColorHex = null;
//                        }
//                    }
//                    i.updateContent();
//                }
//            });
//            if (i.baseColorHex != null) {
//                p.setText(i.baseColorHex);
//            }
//            PopupHandler.displayPopup(p);
//        });
//        baseColorButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.customization.items.text.base_color.desc")));
//        this.rightClickContextMenu.addContent(baseColorButton);
//
//        //TEXT BORDER BUTTON
//        AdvancedButton textBorderButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.customization.items.text.text_border"), (press) -> {
//            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), I18n.get("fancymenu.customization.items.text.text_border"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
//                if (call != null) {
//                    if (MathUtils.isInteger(call)) {
//                        int border = Integer.parseInt(call);
//                        if (border < 0) {
//                            border = 0;
//                        }
//                        if (i.textBorder != border) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        i.textBorder = border;
//                    } else {
//                        if (i.textBorder != 10) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        i.textBorder = 10;
//                    }
//                    i.updateContent();
//                }
//            });
//            p.setText("" + i.textBorder);
//            PopupHandler.displayPopup(p);
//        });
//        textBorderButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.customization.items.text.text_border.desc")));
//        this.rightClickContextMenu.addContent(textBorderButton);
//
//        //LINE SPACING BUTTON
//        AdvancedButton lineSpacingButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.customization.items.text.line_spacing"), (press) -> {
//            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), I18n.get("fancymenu.customization.items.text.line_spacing"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
//                if (call != null) {
//                    if (MathUtils.isInteger(call)) {
//                        int spacing = Integer.parseInt(call);
//                        if (spacing < 0) {
//                            spacing = 0;
//                        }
//                        if (i.lineSpacing != spacing) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        i.lineSpacing = spacing;
//                    } else {
//                        if (i.lineSpacing != 10) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        i.lineSpacing = 10;
//                    }
//                    i.updateContent();
//                }
//            });
//            p.setText("" + i.lineSpacing);
//            PopupHandler.displayPopup(p);
//        });
//        this.rightClickContextMenu.addContent(lineSpacingButton);
//
//        this.rightClickContextMenu.addSeparator();
//
//        //SCROLLING BUTTON
//        AdvancedButton scrollingButton = new AdvancedButton(0, 0, 0, 0, "", (press) -> {
//            if (i.enableScrolling) {
//                i.enableScrolling = false;
//            } else {
//                i.enableScrolling = true;
//            }
//            i.updateContent();
//        }) {
//            @Override
//            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//                if (i.enableScrolling) {
//                    this.setMessage(I18n.get("fancymenu.customization.items.text.scrolling.on"));
//                } else {
//                    this.setMessage(I18n.get("fancymenu.customization.items.text.scrolling.off"));
//                }
//                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//            }
//        };
//        scrollingButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.customization.items.text.scrolling.desc")));
//        this.rightClickContextMenu.addContent(scrollingButton);
//
//        ContextMenu grabberTextureMenu = new ContextMenu();
//        this.rightClickContextMenu.addChild(grabberTextureMenu);
//
//        //GRABBER TEXTURE BUTTON
//        AdvancedButton grabberTextureButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.customization.items.text.scroll_grabber_texture"), true, (press) -> {
//            grabberTextureMenu.setParentButton((AdvancedButton) press);
//            grabberTextureMenu.openMenuAt(0, press.y);
//        });
//        this.rightClickContextMenu.addContent(grabberTextureButton);
//
//        //GRABBER NORMAL TEXTURE BUTTON
//        AdvancedButton grabberNormalTextureButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.customization.items.text.scroll_grabber_texture.normal"), (press) -> {
//            ChooseFilePopup p = new ChooseFilePopup((call) -> {
//                if (call != null) {
//                    if (call.length() == 0) {
//                        if (i.scrollGrabberTextureNormal != null) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        i.scrollGrabberTextureNormal = null;
//                    } else {
//                        if ((i.scrollGrabberTextureNormal == null) || !i.scrollGrabberTextureNormal.equals(call)) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        i.scrollGrabberTextureNormal = call;
//                    }
//                    i.updateContent();
//                }
//            }, "png", "jpg", "jpeg");
//            if (i.scrollGrabberTextureNormal != null) {
//                p.setText(i.scrollGrabberTextureNormal);
//            }
//            PopupHandler.displayPopup(p);
//        });
//        grabberTextureMenu.addContent(grabberNormalTextureButton);
//
//        //GRABBER HOVER TEXTURE BUTTON
//        AdvancedButton grabberHoverTextureButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.customization.items.text.scroll_grabber_texture.hover"), (press) -> {
//            ChooseFilePopup p = new ChooseFilePopup((call) -> {
//                if (call != null) {
//                    if (call.length() == 0) {
//                        if (i.scrollGrabberTextureHover != null) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        i.scrollGrabberTextureHover = null;
//                    } else {
//                        if ((i.scrollGrabberTextureHover == null) || !i.scrollGrabberTextureHover.equals(call)) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        i.scrollGrabberTextureHover = call;
//                    }
//                    i.updateContent();
//                }
//            }, "png", "jpg", "jpeg");
//            if (i.scrollGrabberTextureHover != null) {
//                p.setText(i.scrollGrabberTextureHover);
//            }
//            PopupHandler.displayPopup(p);
//        });
//        grabberTextureMenu.addContent(grabberHoverTextureButton);
//
//        ContextMenu grabberColorMenu = new ContextMenu();
//        this.rightClickContextMenu.addChild(grabberColorMenu);
//
//        //GRABBER COLOR BUTTON
//        AdvancedButton grabberColorButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.customization.items.text.scroll_grabber_color"), true, (press) -> {
//            grabberColorMenu.setParentButton((AdvancedButton) press);
//            grabberColorMenu.openMenuAt(0, press.y);
//        });
//        grabberColorButton.setDescription(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.customization.items.text.scroll_grabber_color.desc")));
//        this.rightClickContextMenu.addContent(grabberColorButton);
//
//        //GRABBER COLOR NORMAL BUTTON
//        AdvancedButton grabberColorNormalButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.customization.items.text.scroll_grabber_color.normal"), (press) -> {
//            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), I18n.get("fancymenu.customization.items.text.scroll_grabber_color.normal"), null, 240, (call) -> {
//                if (call != null) {
//                    if (call.replace(" ", "").equals("")) {
//                        if (i.scrollGrabberColorHexNormal != null) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        i.scrollGrabberColorHexNormal = null;
//                    } else {
//                        Color c = RenderUtils.getColorFromHexString(call);
//                        if (c != null) {
//                            if ((i.scrollGrabberColorHexNormal == null) || !i.scrollGrabberColorHexNormal.equals(call)) {
//                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                            }
//                            i.scrollGrabberColorHexNormal = call;
//                        } else {
//                            if (i.scrollGrabberColorHexNormal != null) {
//                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                            }
//                            i.scrollGrabberColorHexNormal = null;
//                        }
//                    }
//                    i.updateContent();
//                }
//            });
//            if (i.scrollGrabberColorHexNormal != null) {
//                p.setText(i.scrollGrabberColorHexNormal);
//            }
//            PopupHandler.displayPopup(p);
//        });
//        grabberColorMenu.addContent(grabberColorNormalButton);
//
//        //GRABBER COLOR HOVER BUTTON
//        AdvancedButton grabberColorHoverButton = new AdvancedButton(0, 0, 0, 0, I18n.get("fancymenu.customization.items.text.scroll_grabber_color.hover"), (press) -> {
//            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), I18n.get("fancymenu.customization.items.text.scroll_grabber_color.hover"), null, 240, (call) -> {
//                if (call != null) {
//                    if (call.replace(" ", "").equals("")) {
//                        if (i.scrollGrabberColorHexHover != null) {
//                            this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                        }
//                        i.scrollGrabberColorHexHover = null;
//                    } else {
//                        Color c = RenderUtils.getColorFromHexString(call);
//                        if (c != null) {
//                            if ((i.scrollGrabberColorHexHover == null) || !i.scrollGrabberColorHexHover.equals(call)) {
//                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                            }
//                            i.scrollGrabberColorHexHover = call;
//                        } else {
//                            if (i.scrollGrabberColorHexHover != null) {
//                                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//                            }
//                            i.scrollGrabberColorHexHover = null;
//                        }
//                    }
//                    i.updateContent();
//                }
//            });
//            if (i.scrollGrabberColorHexHover != null) {
//                p.setText(i.scrollGrabberColorHexHover);
//            }
//            PopupHandler.displayPopup(p);
//        });
//        grabberColorMenu.addContent(grabberColorHoverButton);

    }

}
