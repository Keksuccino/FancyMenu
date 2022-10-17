//---
package de.keksuccino.fancymenu.menu.fancy.item.items.text;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.api.item.LayoutEditorElement;
import de.keksuccino.fancymenu.menu.fancy.helper.DynamicValueInputPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.ChooseFilePopup;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.FMContextMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TextLayoutEditorElement extends LayoutEditorElement {

    public TextLayoutEditorElement(TextCustomizationItemContainer parentContainer, TextCustomizationItem customizationItemInstance, LayoutEditorScreen handler) {
        super(parentContainer, customizationItemInstance, true, handler, true);
    }

    @Override
    public void init() {

//        this.resizeableX = false;

        super.init();

        TextCustomizationItem i = ((TextCustomizationItem)this.object);

        //SOURCE MODE BUTTON
        AdvancedButton toggleSourceModeButton = new AdvancedButton(0, 0, 0, 0, "", (press) -> {
            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
            if (i.sourceMode == TextCustomizationItem.SourceMode.LOCAL_SOURCE) {
                i.sourceMode = TextCustomizationItem.SourceMode.WEB_SOURCE;
            } else if (i.sourceMode == TextCustomizationItem.SourceMode.WEB_SOURCE) {
                i.sourceMode = TextCustomizationItem.SourceMode.DIRECT;
            } else if (i.sourceMode == TextCustomizationItem.SourceMode.DIRECT) {
                i.sourceMode = TextCustomizationItem.SourceMode.LOCAL_SOURCE;
            }
            i.source = null;
            i.updateContent();
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (i.sourceMode == TextCustomizationItem.SourceMode.LOCAL_SOURCE) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.text.source_mode.mode.local"));
                }
                if (i.sourceMode == TextCustomizationItem.SourceMode.WEB_SOURCE) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.text.source_mode.mode.web"));
                }
                if (i.sourceMode == TextCustomizationItem.SourceMode.DIRECT) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.text.source_mode.mode.direct"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        toggleSourceModeButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.text.source_mode.desc"), "%n%"));
        this.rightclickMenu.addContent(toggleSourceModeButton);

        //SET SOURCE BUTTON
        AdvancedButton setSourceButton = new AdvancedButton(0, 0, 0, 0, "", (press) -> {
            if (i.sourceMode == TextCustomizationItem.SourceMode.LOCAL_SOURCE) {
                ChooseFilePopup p = new ChooseFilePopup((call) -> {
                    if (call != null) {
                        if (call.length() == 0) {
                            if (i.source != null) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            i.source = null;
                        } else {
                            if ((i.source == null) || !i.source.equals(call)) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            i.source = call;
                        }
                        i.updateContent();
                    }
                }, "txt");
                if (i.source != null) {
                    File f = new File(i.source);
                    if (f.isFile()) {
                        p.setText(i.source);
                    }
                }
                PopupHandler.displayPopup(p);
            }
            if ((i.sourceMode == TextCustomizationItem.SourceMode.DIRECT) || (i.sourceMode == TextCustomizationItem.SourceMode.WEB_SOURCE)) {
                String popupTitle = Locals.localize("fancymenu.customization.items.text.set_source.direct");
                if (i.sourceMode == TextCustomizationItem.SourceMode.WEB_SOURCE) {
                    popupTitle = Locals.localize("fancymenu.customization.items.text.set_source.web");
                }
                DynamicValueInputPopup p = new DynamicValueInputPopup(new Color(0,0,0,0), popupTitle, null, 240, (call) -> {
                    if (call != null) {
                        if (call.length() == 0) {
                            if (i.source != null) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            i.source = null;
                        } else {
                            if ((i.source == null) || !i.source.equals(call)) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            i.source = call;
                        }
                        i.updateContent();
                    }
                });
                if (i.source != null) {
                    p.setText(i.source);
                }
                PopupHandler.displayPopup(p);
            }
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (i.sourceMode == TextCustomizationItem.SourceMode.LOCAL_SOURCE) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.text.set_source.local"));
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.text.set_source.local.desc"), "%n%"));
                }
                if (i.sourceMode == TextCustomizationItem.SourceMode.WEB_SOURCE) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.text.set_source.web"));
                    this.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.text.set_source.web.desc"), "%n%"));
                }
                if (i.sourceMode == TextCustomizationItem.SourceMode.DIRECT) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.text.set_source.direct"));
                    List<String> l = new ArrayList<>();
                    for (String s : StringUtils.splitLines(Locals.localize("fancymenu.customization.items.text.set_source.direct.desc"), "%n%")) {
                        l.add(s.replace("<line_break_code>", "%n%"));
                    }
                    this.setDescription(l.toArray(new String[0]));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        this.rightclickMenu.addContent(setSourceButton);

        this.rightclickMenu.addSeparator();

        //CASE MODE BUTTON
        AdvancedButton caseModeButton = new AdvancedButton(0, 0, 0, 0, "", (press) -> {
            if (i.caseMode == TextCustomizationItem.CaseMode.NORMAL) {
                i.caseMode = TextCustomizationItem.CaseMode.ALL_LOWER;
            } else if (i.caseMode == TextCustomizationItem.CaseMode.ALL_LOWER) {
                i.caseMode = TextCustomizationItem.CaseMode.ALL_UPPER;
            } else if (i.caseMode == TextCustomizationItem.CaseMode.ALL_UPPER) {
                i.caseMode = TextCustomizationItem.CaseMode.NORMAL;
            }
            i.updateContent();
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (i.caseMode == TextCustomizationItem.CaseMode.NORMAL) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.text.case_mode.normal"));
                }
                if (i.caseMode == TextCustomizationItem.CaseMode.ALL_LOWER) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.text.case_mode.lower"));
                }
                if (i.caseMode == TextCustomizationItem.CaseMode.ALL_UPPER) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.text.case_mode.upper"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        this.rightclickMenu.addContent(caseModeButton);

        //SCALE BUTTON
        AdvancedButton scaleButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.text.scale"), (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.text.scale"), CharacterFilter.getDoubleCharacterFiler(), 240, (call) -> {
                if (call != null) {
                    if (MathUtils.isFloat(call)) {
                        float s = Float.parseFloat(call);
                        if (s < 0.2F) {
                            s = 0.2F;
                        }
                        if (i.scale != s) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.scale = s;
                    } else {
                        if (i.scale != 1.0F) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.scale = 1.0F;
                    }
                    i.updateContent();
                }
            });
            p.setText("" + i.scale);
            PopupHandler.displayPopup(p);
        });
        this.rightclickMenu.addContent(scaleButton);

        //SHADOW BUTTON
        AdvancedButton shadowButton = new AdvancedButton(0, 0, 0, 0, "", (press) -> {
            if (i.shadow) {
                i.shadow = false;
            } else {
                i.shadow = true;
            }
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (i.shadow) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.text.shadow.on"));
                } else {
                    this.setMessage(Locals.localize("fancymenu.customization.items.text.shadow.off"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        this.rightclickMenu.addContent(shadowButton);

        //ALIGNMENT BUTTON
        AdvancedButton alignmentButton = new AdvancedButton(0, 0, 0, 0, "", (press) -> {
            if (i.alignment == CustomizationItemBase.Alignment.LEFT) {
                i.alignment = CustomizationItemBase.Alignment.CENTERED;
            } else if (i.alignment == CustomizationItemBase.Alignment.CENTERED) {
                i.alignment = CustomizationItemBase.Alignment.RIGHT;
            } else if (i.alignment == CustomizationItemBase.Alignment.RIGHT) {
                i.alignment = CustomizationItemBase.Alignment.LEFT;
            }
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (i.alignment == CustomizationItemBase.Alignment.LEFT) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.text.alignment.left"));
                }
                if (i.alignment == CustomizationItemBase.Alignment.CENTERED) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.text.alignment.center"));
                }
                if (i.alignment == CustomizationItemBase.Alignment.RIGHT) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.text.alignment.right"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        this.rightclickMenu.addContent(alignmentButton);

        //BASE COLOR BUTTON
        AdvancedButton baseColorButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.text.base_color"), (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.text.base_color"), null, 240, (call) -> {
                if (call != null) {
                    if (call.replace(" ", "").equals("")) {
                        if (i.baseColorHex != null) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.baseColorHex = null;
                    } else {
                        Color c = RenderUtils.getColorFromHexString(call);
                        if (c != null) {
                            if ((i.baseColorHex == null) || !i.baseColorHex.equals(call)) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            i.baseColorHex = call;
                        } else {
                            if (i.baseColorHex != null) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            i.baseColorHex = null;
                        }
                    }
                    i.updateContent();
                }
            });
            if (i.baseColorHex != null) {
                p.setText(i.baseColorHex);
            }
            PopupHandler.displayPopup(p);
        });
        baseColorButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.text.base_color.desc"), "%n%"));
        this.rightclickMenu.addContent(baseColorButton);

        //TEXT BORDER BUTTON
        AdvancedButton textBorderButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.text.text_border"), (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.text.text_border"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
                if (call != null) {
                    if (MathUtils.isInteger(call)) {
                        int border = Integer.parseInt(call);
                        if (border < 0) {
                            border = 0;
                        }
                        if (i.textBorder != border) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.textBorder = border;
                    } else {
                        if (i.textBorder != 10) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.textBorder = 10;
                    }
                    i.updateContent();
                }
            });
            p.setText("" + i.textBorder);
            PopupHandler.displayPopup(p);
        });
        textBorderButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.text.text_border.desc"), "%n%"));
        this.rightclickMenu.addContent(textBorderButton);

        //LINE SPACING BUTTON
        AdvancedButton lineSpacingButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.text.line_spacing"), (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.text.line_spacing"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
                if (call != null) {
                    if (MathUtils.isInteger(call)) {
                        int spacing = Integer.parseInt(call);
                        if (spacing < 0) {
                            spacing = 0;
                        }
                        if (i.lineSpacing != spacing) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.lineSpacing = spacing;
                    } else {
                        if (i.lineSpacing != 10) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.lineSpacing = 10;
                    }
                    i.updateContent();
                }
            });
            p.setText("" + i.lineSpacing);
            PopupHandler.displayPopup(p);
        });
        this.rightclickMenu.addContent(lineSpacingButton);

        this.rightclickMenu.addSeparator();

        //SCROLLING BUTTON
        AdvancedButton scrollingButton = new AdvancedButton(0, 0, 0, 0, "", (press) -> {
            if (i.enableScrolling) {
                i.enableScrolling = false;
            } else {
                i.enableScrolling = true;
            }
            i.updateContent();
        }) {
            @Override
            public void render(PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (i.enableScrolling) {
                    this.setMessage(Locals.localize("fancymenu.customization.items.text.scrolling.on"));
                } else {
                    this.setMessage(Locals.localize("fancymenu.customization.items.text.scrolling.off"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        scrollingButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.text.scrolling.desc"), "%n%"));
        this.rightclickMenu.addContent(scrollingButton);

        FMContextMenu grabberTextureMenu = new FMContextMenu();
        this.rightclickMenu.addChild(grabberTextureMenu);

        //GRABBER TEXTURE BUTTON
        AdvancedButton grabberTextureButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.text.scroll_grabber_texture"), true, (press) -> {
            grabberTextureMenu.setParentButton((AdvancedButton) press);
            grabberTextureMenu.openMenuAt(0, press.y);
        });
        this.rightclickMenu.addContent(grabberTextureButton);

        //GRABBER NORMAL TEXTURE BUTTON
        AdvancedButton grabberNormalTextureButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.text.scroll_grabber_texture.normal"), (press) -> {
            ChooseFilePopup p = new ChooseFilePopup((call) -> {
                if (call != null) {
                    if (call.length() == 0) {
                        if (i.scrollGrabberTextureNormal != null) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.scrollGrabberTextureNormal = null;
                    } else {
                        if ((i.scrollGrabberTextureNormal == null) || !i.scrollGrabberTextureNormal.equals(call)) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.scrollGrabberTextureNormal = call;
                    }
                    i.updateContent();
                }
            }, "png", "jpg", "jpeg");
            if (i.scrollGrabberTextureNormal != null) {
                p.setText(i.scrollGrabberTextureNormal);
            }
            PopupHandler.displayPopup(p);
        });
        grabberTextureMenu.addContent(grabberNormalTextureButton);

        //GRABBER HOVER TEXTURE BUTTON
        AdvancedButton grabberHoverTextureButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.text.scroll_grabber_texture.hover"), (press) -> {
            ChooseFilePopup p = new ChooseFilePopup((call) -> {
                if (call != null) {
                    if (call.length() == 0) {
                        if (i.scrollGrabberTextureHover != null) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.scrollGrabberTextureHover = null;
                    } else {
                        if ((i.scrollGrabberTextureHover == null) || !i.scrollGrabberTextureHover.equals(call)) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.scrollGrabberTextureHover = call;
                    }
                    i.updateContent();
                }
            }, "png", "jpg", "jpeg");
            if (i.scrollGrabberTextureHover != null) {
                p.setText(i.scrollGrabberTextureHover);
            }
            PopupHandler.displayPopup(p);
        });
        grabberTextureMenu.addContent(grabberHoverTextureButton);

        FMContextMenu grabberColorMenu = new FMContextMenu();
        this.rightclickMenu.addChild(grabberColorMenu);

        //GRABBER COLOR BUTTON
        AdvancedButton grabberColorButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.text.scroll_grabber_color"), true, (press) -> {
            grabberColorMenu.setParentButton((AdvancedButton) press);
            grabberColorMenu.openMenuAt(0, press.y);
        });
        grabberColorButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.customization.items.text.scroll_grabber_color.desc"), "%n%"));
        this.rightclickMenu.addContent(grabberColorButton);

        //GRABBER COLOR NORMAL BUTTON
        AdvancedButton grabberColorNormalButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.text.scroll_grabber_color.normal"), (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.text.scroll_grabber_color.normal"), null, 240, (call) -> {
                if (call != null) {
                    if (call.replace(" ", "").equals("")) {
                        if (i.scrollGrabberColorHexNormal != null) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.scrollGrabberColorHexNormal = null;
                    } else {
                        Color c = RenderUtils.getColorFromHexString(call);
                        if (c != null) {
                            if ((i.scrollGrabberColorHexNormal == null) || !i.scrollGrabberColorHexNormal.equals(call)) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            i.scrollGrabberColorHexNormal = call;
                        } else {
                            if (i.scrollGrabberColorHexNormal != null) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            i.scrollGrabberColorHexNormal = null;
                        }
                    }
                    i.updateContent();
                }
            });
            if (i.scrollGrabberColorHexNormal != null) {
                p.setText(i.scrollGrabberColorHexNormal);
            }
            PopupHandler.displayPopup(p);
        });
        grabberColorMenu.addContent(grabberColorNormalButton);

        //GRABBER COLOR HOVER BUTTON
        AdvancedButton grabberColorHoverButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.customization.items.text.scroll_grabber_color.hover"), (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), Locals.localize("fancymenu.customization.items.text.scroll_grabber_color.hover"), null, 240, (call) -> {
                if (call != null) {
                    if (call.replace(" ", "").equals("")) {
                        if (i.scrollGrabberColorHexHover != null) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.scrollGrabberColorHexHover = null;
                    } else {
                        Color c = RenderUtils.getColorFromHexString(call);
                        if (c != null) {
                            if ((i.scrollGrabberColorHexHover == null) || !i.scrollGrabberColorHexHover.equals(call)) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            i.scrollGrabberColorHexHover = call;
                        } else {
                            if (i.scrollGrabberColorHexHover != null) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            i.scrollGrabberColorHexHover = null;
                        }
                    }
                    i.updateContent();
                }
            });
            if (i.scrollGrabberColorHexHover != null) {
                p.setText(i.scrollGrabberColorHexHover);
            }
            PopupHandler.displayPopup(p);
        });
        grabberColorMenu.addContent(grabberColorHoverButton);

    }

    @Override
    public SimplePropertiesSection serializeItem() {

        TextCustomizationItem i = ((TextCustomizationItem)this.object);

        SimplePropertiesSection sec = new SimplePropertiesSection();

        if (i.source != null) {
            sec.addEntry("source", i.source);
        }
        if (i.sourceMode != null) {
            sec.addEntry("source_mode", i.sourceMode.name);
        }
        sec.addEntry("shadow", "" + i.shadow);
        if (i.caseMode != null) {
            sec.addEntry("case_mode", i.caseMode.name);
        }
        sec.addEntry("scale", "" + i.scale);
        if (i.alignment != null) {
            sec.addEntry("alignment", i.alignment.key);
        }
        if (i.baseColorHex != null) {
            sec.addEntry("base_color", i.baseColorHex);
        }
        sec.addEntry("text_border", "" + i.textBorder);
        sec.addEntry("line_spacing", "" + i.lineSpacing);
        if (i.scrollGrabberColorHexNormal != null) {
            sec.addEntry("grabber_color_normal", i.scrollGrabberColorHexNormal);
        }
        if (i.scrollGrabberColorHexHover != null) {
            sec.addEntry("grabber_color_hover", i.scrollGrabberColorHexHover);
        }
        if (i.scrollGrabberTextureNormal != null) {
            sec.addEntry("grabber_texture_normal", i.scrollGrabberTextureNormal);
        }
        if (i.scrollGrabberTextureHover != null) {
            sec.addEntry("grabber_texture_hover", i.scrollGrabberTextureHover);
        }
        sec.addEntry("enable_scrolling", "" + i.enableScrolling);

        return sec;

    }

}
