package de.keksuccino.drippyloadingscreen.customization.items.bars;

import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.api.item.LayoutEditorElement;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.ChooseFilePopup;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.resources.TextureHandler;
import net.minecraft.client.resources.language.I18n;

import java.awt.*;
import java.io.File;

public abstract class AbstractProgressBarLayoutEditorElement extends LayoutEditorElement {

    public AbstractProgressBarLayoutEditorElement(CustomizationItemContainer parentContainer, AbstractProgressBarCustomizationItem customizationItemInstance, LayoutEditorScreen handler) {
        super(parentContainer, customizationItemInstance, true, handler, true);
    }

    @Override
    public void init() {

        super.init();

        AbstractProgressBarCustomizationItem i = ((AbstractProgressBarCustomizationItem)this.object);

        this.rightclickMenu.addSeparator();

        AdvancedButton setBarColorButton = new AdvancedButton(0, 0, 0, 0, I18n.get("drippyloadingscreen.items.progress_bar.set_bar_color"), true, (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), I18n.get("drippyloadingscreen.items.progress_bar.set_bar_color"), null, 240, (call) -> {
                if (call != null) {
                    if ((i.barColorHex == null) || !call.equalsIgnoreCase(i.barColorHex)) {
                        Color c = RenderUtils.getColorFromHexString(call);
                        if (c != null) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            i.barColorHex = call;
                            i.barColor = c;
                        }
                    }
                }
            });
            if (i.barColorHex != null) {
                p.setText(i.barColorHex);
            }
            PopupHandler.displayPopup(p);
        });
        setBarColorButton.setDescription(StringUtils.splitLines(I18n.get("drippyloadingscreen.items.progress_bar.set_bar_color.desc"), "\n"));
        this.rightclickMenu.addContent(setBarColorButton);

        AdvancedButton setBarTextureButton = new AdvancedButton(0, 0, 0, 0, I18n.get("drippyloadingscreen.items.progress_bar.set_bar_texture"), true, (press) -> {
            ChooseFilePopup p = new ChooseFilePopup((call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equalsIgnoreCase("") && (call.toLowerCase().endsWith(".jpg") || call.toLowerCase().endsWith(".jpeg") || call.toLowerCase().endsWith(".png"))) {
                        File f = new File(MenuCustomization.getAbsoluteGameDirectoryPath(call));
                        if (f.isFile()) {
                            if ((i.barTexturePath == null) || !i.barTexturePath.equals(call)) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            i.barTexturePath = call;
                            i.barTexture = TextureHandler.getResource(f.getAbsolutePath()).getResourceLocation();
                        }
                    } else if (call.replace(" ", "").equals("")) {
                        if (i.barTexturePath != null) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.barTexture = null;
                        i.barTexturePath = null;
                    }
                }
            }, "jpg", "jpeg", "png");
            if (i.barTexturePath != null) {
                p.setText(i.barTexturePath);
            }
            PopupHandler.displayPopup(p);
        });
        setBarTextureButton.setDescription(StringUtils.splitLines(I18n.get("drippyloadingscreen.items.progress_bar.set_bar_texture.desc"), "\n"));
        this.rightclickMenu.addContent(setBarTextureButton);

        AdvancedButton setBackgroundColorButton = new AdvancedButton(0, 0, 0, 0, I18n.get("drippyloadingscreen.items.progress_bar.set_background_color"), true, (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), I18n.get("drippyloadingscreen.items.progress_bar.set_background_color"), null, 240, (call) -> {
                if (call != null) {
                    if ((i.backgroundColorHex == null) || !call.equalsIgnoreCase(i.backgroundColorHex)) {
                        Color c = RenderUtils.getColorFromHexString(call);
                        if (c != null) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            i.backgroundColorHex = call;
                            i.backgroundColor = c;
                        }
                    }
                }
            });
            if (i.backgroundColorHex != null) {
                p.setText(i.backgroundColorHex);
            }
            PopupHandler.displayPopup(p);
        });
        setBackgroundColorButton.setDescription(StringUtils.splitLines(I18n.get("drippyloadingscreen.items.progress_bar.set_background_color.desc"), "\n"));
        this.rightclickMenu.addContent(setBackgroundColorButton);

        AdvancedButton setBackgroundTextureButton = new AdvancedButton(0, 0, 0, 0, I18n.get("drippyloadingscreen.items.progress_bar.set_background_texture"), true, (press) -> {
            ChooseFilePopup p = new ChooseFilePopup((call) -> {
                if (call != null) {
                    if (!call.replace(" ", "").equalsIgnoreCase("") && (call.toLowerCase().endsWith(".jpg") || call.toLowerCase().endsWith(".jpeg") || call.toLowerCase().endsWith(".png"))) {
                        File f = new File(MenuCustomization.getAbsoluteGameDirectoryPath(call));
                        if (f.isFile()) {
                            if ((i.backgroundTexturePath == null) || !i.backgroundTexturePath.equals(call)) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                            }
                            i.backgroundTexturePath = call;
                            i.backgroundTexture = TextureHandler.getResource(f.getAbsolutePath()).getResourceLocation();
                        }
                    } else if (call.replace(" ", "").equals("")) {
                        if (i.backgroundTexturePath != null) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        i.backgroundTexture = null;
                        i.backgroundTexturePath = null;
                    }
                }
            }, "jpg", "jpeg", "png");
            if (i.backgroundTexturePath != null) {
                p.setText(i.backgroundTexturePath);
            }
            PopupHandler.displayPopup(p);
        });
        setBackgroundTextureButton.setDescription(StringUtils.splitLines(I18n.get("drippyloadingscreen.items.progress_bar.set_background_texture.desc"), "\n"));
        this.rightclickMenu.addContent(setBackgroundTextureButton);

        this.rightclickMenu.addSeparator();

        AdvancedButton setDirectionButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
            if (i.direction == AbstractProgressBarCustomizationItem.BarDirection.LEFT) {
                i.direction = AbstractProgressBarCustomizationItem.BarDirection.RIGHT;
            } else if (i.direction == AbstractProgressBarCustomizationItem.BarDirection.RIGHT) {
                i.direction = AbstractProgressBarCustomizationItem.BarDirection.UP;
            } else if (i.direction == AbstractProgressBarCustomizationItem.BarDirection.UP) {
                i.direction = AbstractProgressBarCustomizationItem.BarDirection.DOWN;
            } else if (i.direction == AbstractProgressBarCustomizationItem.BarDirection.DOWN) {
                i.direction = AbstractProgressBarCustomizationItem.BarDirection.LEFT;
            }
        }) {
            @Override
            public void render(GuiGraphics p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (i.direction != null) {
                    this.setMessage(I18n.get("drippyloadingscreen.items.progress_bar.fill_direction", I18n.get("drippyloadingscreen.items.progress_bar.fill_direction." + i.direction.getName())));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        setDirectionButton.setDescription(StringUtils.splitLines(I18n.get("drippyloadingscreen.items.progress_bar.fill_direction.desc"), "\n"));
        this.rightclickMenu.addContent(setDirectionButton);

        AdvancedButton toggleProgressForElementOriButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
            if (i.useProgressForElementOrientation) {
                i.useProgressForElementOrientation = false;
            } else {
                i.useProgressForElementOrientation = true;
            }
        }) {
            @Override
            public void render(GuiGraphics p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (i.useProgressForElementOrientation) {
                    this.setMessage(I18n.get("drippyloadingscreen.items.progress_bar.progress_for_orientation.on"));
                } else {
                    this.setMessage(I18n.get("drippyloadingscreen.items.progress_bar.progress_for_orientation.off"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        toggleProgressForElementOriButton.setDescription(StringUtils.splitLines(I18n.get("drippyloadingscreen.items.progress_bar.progress_for_orientation.desc"), "\n"));
        this.rightclickMenu.addContent(toggleProgressForElementOriButton);

    }

    @Override
    public SimplePropertiesSection serializeItem() {

        AbstractProgressBarCustomizationItem i = ((AbstractProgressBarCustomizationItem)this.object);

        SimplePropertiesSection sec = new SimplePropertiesSection();

        sec.addEntry("bar_color", i.barColorHex);
        if (i.barTexturePath != null) {
            sec.addEntry("bar_texture", i.barTexturePath);
        }
        sec.addEntry("background_color", i.backgroundColorHex);
        if (i.backgroundTexturePath != null) {
            sec.addEntry("background_texture", i.backgroundTexturePath);
        }
        sec.addEntry("direction", i.direction.getName());
        sec.addEntry("progress_for_element_orientation", "" + i.useProgressForElementOrientation);

        return sec;

    }

}
