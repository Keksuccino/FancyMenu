package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMPopup;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.VisibilityRequirementContainer;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedImageButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class VisibilityRequirementsPopup extends FMPopup {

    protected CustomizationItemBase parent;
    protected List<Requirement> requirements = new ArrayList<Requirement>();
    protected int currentRequirement = 0;

    protected AdvancedButton doneButton;
    protected AdvancedButton leftButton;
    protected AdvancedButton rightButton;

    public VisibilityRequirementsPopup(CustomizationItemBase parent) {
        super(240);
        this.parent = parent;

        this.doneButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("popup.done"), true, (press) -> {
            this.setDisplayed(false);
        });
        this.addButton(this.doneButton);

        this.leftButton = new AdvancedImageButton(0, 0, 20, 20, new ResourceLocation("keksuccino", "arrow_left.png"), true, (press) -> {
            int i = this.currentRequirement - 1;
            if (i >= 0) {
                this.currentRequirement = i;
            }
        });
        this.addButton(this.leftButton);

        this.rightButton = new AdvancedImageButton(0, 0, 20, 20, new ResourceLocation("keksuccino", "arrow_right.png"), true, (press) -> {
            int i = this.currentRequirement + 1;
            if (i <= this.requirements.size() - 1) {
                this.currentRequirement = i;
            }
        });
        this.addButton(this.rightButton);

        this.initRequirements();
    }

    protected void initRequirements() {

        VisibilityRequirementContainer c = this.parent.visibilityRequirementContainer;
        CharacterFilter integerCharFilter = CharacterFilter.getIntegerCharacterFiler();

        /** Singleplayer **/
        String singleplayerName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.singleplayer");
        String singleplayerDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.singleplayer.desc");
        Requirement singleplayer = new Requirement(this, singleplayerName, singleplayerDesc, null, c.vrCheckForSingleplayer, c.vrShowIfSingleplayer,
                (enabledCallback) -> {
                    c.vrCheckForSingleplayer = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfSingleplayer = showIfCallback;
        }, null, null, null);
        this.requirements.add(singleplayer);

        /** Multiplayer **/
        String multiplayerName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.multiplayer");
        String multiplayerDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.multiplayer.desc");
        Requirement multiplayer = new Requirement(this, multiplayerName, multiplayerDesc, null, c.vrCheckForMultiplayer, c.vrShowIfMultiplayer,
                (enabledCallback) -> {
                    c.vrCheckForMultiplayer = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfMultiplayer = showIfCallback;
        }, null, null, null);
        this.requirements.add(multiplayer);

        /** World Loaded **/
        String worldLoadedName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.worldloaded");
        String worldLoadedDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.worldloaded.desc");
        Requirement worldLoaded = new Requirement(this, worldLoadedName, worldLoadedDesc, null, c.vrCheckForWorldLoaded, c.vrShowIfWorldLoaded,
                (enabledCallback) -> {
                    c.vrCheckForWorldLoaded = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfWorldLoaded = showIfCallback;
        }, null, null, null);
        this.requirements.add(worldLoaded);

        /** Is Real Time Hour **/
        String realTimeHourValuePreset = "";
        for (int i : c.vrRealTimeHour) {
            realTimeHourValuePreset += i + ",";
        }
        if (realTimeHourValuePreset.length() > 0) {
            realTimeHourValuePreset = realTimeHourValuePreset.substring(0, realTimeHourValuePreset.length() -1);
        } else {
            realTimeHourValuePreset = "1, 4";
        }
        String realTimeHourName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimehour");
        String realTimeHourDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimehour.desc");
        String realTimeHourValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimehour.valuename");
        Requirement realTimeHour = new Requirement(this, realTimeHourName, realTimeHourDesc, realTimeHourValueName, c.vrCheckForRealTimeHour, c.vrShowIfRealTimeHour,
                (enabledCallback) -> {
                    c.vrCheckForRealTimeHour = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfRealTimeHour = showIfCallback;
        }, (valueCallback) -> {
            if (valueCallback != null) {
                c.vrRealTimeHour.clear();
                if (valueCallback.contains(",")) {
                    for (String s : valueCallback.replace(" ", "").split("[,]")) {
                        if (MathUtils.isInteger(s)) {
                            c.vrRealTimeHour.add(Integer.parseInt(s));
                        }
                    }
                } else {
                    if (MathUtils.isInteger(valueCallback.replace(" ", ""))) {
                        c.vrRealTimeHour.add(Integer.parseInt(valueCallback.replace(" ", "")));
                    }
                }
            }
        }, integerCharFilter, realTimeHourValuePreset);
        this.requirements.add(realTimeHour);

        /** Is Real Time Minute **/
        String realTimeMinuteValuePreset = "";
        for (int i : c.vrRealTimeMinute) {
            realTimeMinuteValuePreset += i + ",";
        }
        if (realTimeMinuteValuePreset.length() > 0) {
            realTimeMinuteValuePreset = realTimeMinuteValuePreset.substring(0, realTimeMinuteValuePreset.length() -1);
        } else {
            realTimeMinuteValuePreset = "1, 4";
        }
        String realTimeMinuteName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimeminute");
        String realTimeMinuteDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimeminute.desc");
        String realTimeMinuteValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimeminute.valuename");
        Requirement realTimeMinute = new Requirement(this, realTimeMinuteName, realTimeMinuteDesc, realTimeMinuteValueName, c.vrCheckForRealTimeMinute, c.vrShowIfRealTimeMinute,
                (enabledCallback) -> {
                    c.vrCheckForRealTimeMinute = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfRealTimeMinute = showIfCallback;
        }, (valueCallback) -> {
            if (valueCallback != null) {
                c.vrRealTimeMinute.clear();
                if (valueCallback.contains(",")) {
                    for (String s : valueCallback.replace(" ", "").split("[,]")) {
                        if (MathUtils.isInteger(s)) {
                            c.vrRealTimeMinute.add(Integer.parseInt(s));
                        }
                    }
                } else {
                    if (MathUtils.isInteger(valueCallback.replace(" ", ""))) {
                        c.vrRealTimeMinute.add(Integer.parseInt(valueCallback.replace(" ", "")));
                    }
                }
            }
        }, integerCharFilter, realTimeMinuteValuePreset);
        this.requirements.add(realTimeMinute);

        /** Is Real Time Second **/
        String realTimeSecondValuePreset = "";
        for (int i : c.vrRealTimeSecond) {
            realTimeSecondValuePreset += i + ",";
        }
        if (realTimeSecondValuePreset.length() > 0) {
            realTimeSecondValuePreset = realTimeSecondValuePreset.substring(0, realTimeSecondValuePreset.length() -1);
        } else {
            realTimeSecondValuePreset = "1, 4";
        }
        String realTimeSecondName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimesecond");
        String realTimeSecondDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimesecond.desc");
        String realTimeSecondValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.realtimesecond.valuename");
        Requirement realTimeSecond = new Requirement(this, realTimeSecondName, realTimeSecondDesc, realTimeSecondValueName, c.vrCheckForRealTimeSecond, c.vrShowIfRealTimeSecond,
                (enabledCallback) -> {
                    c.vrCheckForRealTimeSecond = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfRealTimeSecond = showIfCallback;
        }, (valueCallback) -> {
            if (valueCallback != null) {
                c.vrRealTimeSecond.clear();
                if (valueCallback.contains(",")) {
                    for (String s : valueCallback.replace(" ", "").split("[,]")) {
                        if (MathUtils.isInteger(s)) {
                            c.vrRealTimeSecond.add(Integer.parseInt(s));
                        }
                    }
                } else {
                    if (MathUtils.isInteger(valueCallback.replace(" ", ""))) {
                        c.vrRealTimeSecond.add(Integer.parseInt(valueCallback.replace(" ", "")));
                    }
                }
            }
        }, integerCharFilter, realTimeSecondValuePreset);
        this.requirements.add(realTimeSecond);

        /** Is Window Width **/
        String windowWidthValuePreset = "";
        for (int i : c.vrWindowWidth) {
            windowWidthValuePreset += i + ",";
        }
        if (windowWidthValuePreset.length() > 0) {
            windowWidthValuePreset = windowWidthValuePreset.substring(0, windowWidthValuePreset.length() -1);
        } else {
            windowWidthValuePreset = "503, 918, 1920";
        }
        String windowWidthName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowwidth");
        String windowWidthDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowwidth.desc");
        String windowWidthValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowwidth.valuename");
        Requirement windowWidth = new Requirement(this, windowWidthName, windowWidthDesc, windowWidthValueName, c.vrCheckForWindowWidth, c.vrShowIfWindowWidth,
                (enabledCallback) -> {
                    c.vrCheckForWindowWidth = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfWindowWidth = showIfCallback;
        }, (valueCallback) -> {
            if (valueCallback != null) {
                c.vrWindowWidth.clear();
                if (valueCallback.contains(",")) {
                    for (String s : valueCallback.replace(" ", "").split("[,]")) {
                        if (MathUtils.isInteger(s)) {
                            c.vrWindowWidth.add(Integer.parseInt(s));
                        }
                    }
                } else {
                    if (MathUtils.isInteger(valueCallback.replace(" ", ""))) {
                        c.vrWindowWidth.add(Integer.parseInt(valueCallback.replace(" ", "")));
                    }
                }
            }
        }, integerCharFilter, windowWidthValuePreset);
        this.requirements.add(windowWidth);

        /** Is Window Height **/
        String windowHeightValuePreset = "";
        for (int i : c.vrWindowHeight) {
            windowHeightValuePreset += i + ",";
        }
        if (windowHeightValuePreset.length() > 0) {
            windowHeightValuePreset = windowHeightValuePreset.substring(0, windowHeightValuePreset.length() -1);
        } else {
            windowHeightValuePreset = "410, 634, 1080";
        }
        String windowHeightName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowheight");
        String windowHeightDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowheight.desc");
        String windowHeightValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowheight.valuename");
        Requirement windowHeight = new Requirement(this, windowHeightName, windowHeightDesc, windowHeightValueName, c.vrCheckForWindowHeight, c.vrShowIfWindowHeight,
                (enabledCallback) -> {
                    c.vrCheckForWindowHeight = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfWindowHeight = showIfCallback;
        }, (valueCallback) -> {
            if (valueCallback != null) {
                c.vrWindowHeight.clear();
                if (valueCallback.contains(",")) {
                    for (String s : valueCallback.replace(" ", "").split("[,]")) {
                        if (MathUtils.isInteger(s)) {
                            c.vrWindowHeight.add(Integer.parseInt(s));
                        }
                    }
                } else {
                    if (MathUtils.isInteger(valueCallback.replace(" ", ""))) {
                        c.vrWindowHeight.add(Integer.parseInt(valueCallback.replace(" ", "")));
                    }
                }
            }
        }, integerCharFilter, windowHeightValuePreset);
        this.requirements.add(windowHeight);

        /** Window Width Bigger Than **/
        String windowWidthBiggerThanName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowwidthbiggerthan");
        String windowWidthBiggerThanDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowwidthbiggerthan.desc");
        String windowWidthBiggerThanValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowwidthbiggerthan.valuename");
        Requirement windowWidthBiggerThan = new Requirement(this, windowWidthBiggerThanName, windowWidthBiggerThanDesc, windowWidthBiggerThanValueName, c.vrCheckForWindowWidthBiggerThan, c.vrShowIfWindowWidthBiggerThan,
                (enabledCallback) -> {
                    c.vrCheckForWindowWidthBiggerThan = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfWindowWidthBiggerThan = showIfCallback;
        }, (valueCallback) -> {
            if ((valueCallback != null) && MathUtils.isInteger(valueCallback)) {
                c.vrWindowWidthBiggerThan = Integer.parseInt(valueCallback);
            } else {
                c.vrWindowWidthBiggerThan = 0;
            }
        }, integerCharFilter, "" + c.vrWindowWidthBiggerThan);
        this.requirements.add(windowWidthBiggerThan);

        /** Window Height Bigger Than **/
        String windowHeightBiggerThanName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowheightbiggerthan");
        String windowHeightBiggerThanDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowheightbiggerthan.desc");
        String windowHeightBiggerThanValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.windowheightbiggerthan.valuename");
        Requirement windowHeightBiggerThan = new Requirement(this, windowHeightBiggerThanName, windowHeightBiggerThanDesc, windowHeightBiggerThanValueName, c.vrCheckForWindowHeightBiggerThan, c.vrShowIfWindowHeightBiggerThan,
                (enabledCallback) -> {
                    c.vrCheckForWindowHeightBiggerThan = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfWindowHeightBiggerThan = showIfCallback;
        }, (valueCallback) -> {
            if ((valueCallback != null) && MathUtils.isInteger(valueCallback)) {
                c.vrWindowHeightBiggerThan = Integer.parseInt(valueCallback);
            } else {
                c.vrWindowHeightBiggerThan = 0;
            }
        }, integerCharFilter, "" + c.vrWindowHeightBiggerThan);
        this.requirements.add(windowHeightBiggerThan);

        /** Button Hovered **/
        String buttonHoveredName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.buttonhovered");
        String buttonHoveredDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.buttonhovered.desc");
        String buttonHoveredValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.buttonhovered.valuename");
        String buttonHoveredValuePreset = "";
        if (c.vrButtonHovered != null) {
            buttonHoveredValuePreset = c.vrButtonHovered;
        }
        Requirement buttonHovered = new Requirement(this, buttonHoveredName, buttonHoveredDesc, buttonHoveredValueName, c.vrCheckForButtonHovered, c.vrShowIfButtonHovered,
                (enabledCallback) -> {
                    c.vrCheckForButtonHovered = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfButtonHovered = showIfCallback;
        }, (valueCallback) -> {
            c.vrButtonHovered = valueCallback;
        }, null, buttonHoveredValuePreset);
        this.requirements.add(buttonHovered);

        /** Language **/
        String languageName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.language");
        String languageDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.language.desc");
        String languageValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.language.valuename");
        String languageValuePreset = Minecraft.getInstance().options.languageCode;
        if (c.vrLanguage != null) {
            languageValuePreset = c.vrLanguage;
        }
        Requirement language = new Requirement(this, languageName, languageDesc, languageValueName, c.vrCheckForLanguage, c.vrShowIfLanguage,
                (enabledCallback) -> {
                    c.vrCheckForLanguage = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfLanguage = showIfCallback;
        }, (valueCallback) -> {
            c.vrLanguage = valueCallback;
        }, null, languageValuePreset);
        this.requirements.add(language);

        /** Fullscreen **/
        String fullscreenName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.fullscreen");
        String fullscreenDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.fullscreen.desc");
        Requirement fullscreen = new Requirement(this, fullscreenName, fullscreenDesc, null, c.vrCheckForFullscreen, c.vrShowIfFullscreen,
                (enabledCallback) -> {
                    c.vrCheckForFullscreen = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfFullscreen = showIfCallback;
        }, null, null, null);
        this.requirements.add(fullscreen);

        /** OS Windows **/
        String osWindowsName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.oswindows");
        String osWindowsDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.oswindows.desc");
        Requirement osWindows = new Requirement(this, osWindowsName, osWindowsDesc, null, c.vrCheckForOsWindows, c.vrShowIfOsWindows,
                (enabledCallback) -> {
                    c.vrCheckForOsWindows = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfOsWindows = showIfCallback;
        }, null, null, null);
        this.requirements.add(osWindows);

        /** OS Mac **/
        String osMacName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.osmac");
        String osMacDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.osmac.desc");
        Requirement osMac = new Requirement(this, osMacName, osMacDesc, null, c.vrCheckForOsMac, c.vrShowIfOsMac,
                (enabledCallback) -> {
                    c.vrCheckForOsMac = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfOsMac = showIfCallback;
        }, null, null, null);
        this.requirements.add(osMac);

        /** OS Linux **/
        String osLinuxName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.oslinux");
        String osLinuxDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.oslinux.desc");
        Requirement osLinux = new Requirement(this, osLinuxName, osLinuxDesc, null, c.vrCheckForOsLinux, c.vrShowIfOsLinux,
                (enabledCallback) -> {
                    c.vrCheckForOsLinux = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfOsLinux = showIfCallback;
        }, null, null, null);
        this.requirements.add(osLinux);

        /** Is Mod Loaded **/
        String modLoadedValuePreset = "";
        for (String s : c.vrModLoaded) {
            modLoadedValuePreset += s + ",";
        }
        if (modLoadedValuePreset.length() > 0) {
            modLoadedValuePreset = modLoadedValuePreset.substring(0, modLoadedValuePreset.length() -1);
        } else {
            modLoadedValuePreset = "fancymenu, optifine, somemodid";
        }
        String modLoadedName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.modloaded");
        String modLoadedDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.modloaded.desc");
        String modLoadedValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.modloaded.valuename");
        Requirement modLoaded = new Requirement(this, modLoadedName, modLoadedDesc, modLoadedValueName, c.vrCheckForModLoaded, c.vrShowIfModLoaded,
                (enabledCallback) -> {
                    c.vrCheckForModLoaded = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfModLoaded = showIfCallback;
        }, (valueCallback) -> {
            if (valueCallback != null) {
                c.vrModLoaded.clear();
                if (valueCallback.contains(",")) {
                    for (String s : valueCallback.replace(" ", "").split("[,]")) {
                        if (!s.equals("")) {
                            c.vrModLoaded.add(s);
                        }
                    }
                } else {
                    if (!valueCallback.replace(" ", "").equals("")) {
                        c.vrModLoaded.add(valueCallback.replace(" ", ""));
                    }
                }
            }
        }, null, modLoadedValuePreset);
        this.requirements.add(modLoaded);

        /** Server Online **/
        String serverOnlineName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.serveronline");
        String serverOnlineDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.serveronline.desc");
        String serverOnlineValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.serveronline.valuename");
        String serverOnlineValuePreset = "mycoolserver.com";
        if (c.vrServerOnline != null) {
            serverOnlineValuePreset = c.vrServerOnline;
        }
        Requirement serverOnline = new Requirement(this, serverOnlineName, serverOnlineDesc, serverOnlineValueName, c.vrCheckForServerOnline, c.vrShowIfServerOnline,
                (enabledCallback) -> {
                    c.vrCheckForServerOnline = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfServerOnline = showIfCallback;
        }, (valueCallback) -> {
            c.vrServerOnline = valueCallback;
        }, null, serverOnlineValuePreset);
        this.requirements.add(serverOnline);

        /** Is Gui Scale **/
        String guiScaleValuePreset = "";
        for (String condition : c.vrGuiScale) {
            if (condition.startsWith("double:")) {
                String value = condition.replace("double:", "");
                guiScaleValuePreset += value + ",";
            } else if (condition.startsWith("biggerthan:")) {
                String value = condition.replace("biggerthan:", "");
                guiScaleValuePreset += ">" + value + ",";
            } else if (condition.startsWith("smallerthan:")) {
                String value = condition.replace("smallerthan:", "");
                guiScaleValuePreset += "<" + value + ",";
            }
        }
        if (guiScaleValuePreset.length() > 0) {
            guiScaleValuePreset = guiScaleValuePreset.substring(0, guiScaleValuePreset.length() -1);
        } else {
            guiScaleValuePreset = ">1.20,<3.0";
        }
        String guiScaleName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.guiscale");
        String guiScaleDesc = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.guiscale.desc");
        String guiScaleValueName = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.guiscale.valuename");
        Requirement guiScale = new Requirement(this, guiScaleName, guiScaleDesc, guiScaleValueName, c.vrCheckForGuiScale, c.vrShowIfGuiScale,
                (enabledCallback) -> {
                    c.vrCheckForGuiScale = enabledCallback;
                }, (showIfCallback) -> {
            c.vrShowIfGuiScale = showIfCallback;
        }, (valueCallback) -> {
            if (valueCallback != null) {
                c.vrGuiScale.clear();
                if (valueCallback.contains(",")) {
                    for (String s : valueCallback.replace(" ", "").split("[,]")) {
                        c.vrGuiScale.add(s);
                    }
                } else {
                    if (valueCallback.length() > 0) {
                        c.vrGuiScale.add(valueCallback.replace(" ", ""));
                    }
                }
                List<String> l = new ArrayList<>();
                for (String s : c.vrGuiScale) {
                    if (MathUtils.isDouble(s)) {
                        l.add("double:" + s);
                    } else {
                        if (s.startsWith(">")) {
                            String value = s.split("[>]", 2)[1];
                            if (MathUtils.isDouble(value)) {
                                l.add("biggerthan:" + value);
                            }
                        } else if (s.startsWith("<")) {
                            String value = s.split("[<]", 2)[1];
                            if (MathUtils.isDouble(value)) {
                                l.add("smallerthan:" + value);
                            }
                        }
                    }
                }
                c.vrGuiScale = l;
            }
        }, null, guiScaleValuePreset);
        this.requirements.add(guiScale);

    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, Screen renderIn) {
        super.render(matrix, mouseX, mouseY, renderIn);

        int centerX = renderIn.width / 2;
        int centerY = renderIn.height / 2;

        this.doneButton.x = centerX - (this.doneButton.getWidth() / 2);
        this.doneButton.y = centerY + 50;

        this.leftButton.x = centerX - this.leftButton.getWidth() - 135;
        this.leftButton.y = centerY - (this.leftButton.getHeight() / 2);

        this.rightButton.x = centerX + 135;
        this.rightButton.y = centerY - (this.leftButton.getHeight() / 2);

        Requirement r = this.requirements.get(this.currentRequirement);
        if (r != null) {
            r.render(matrix, mouseX, mouseY, renderIn);
        }

        this.renderButtons(matrix, mouseX, mouseY);
    }

    public static class Requirement extends GuiComponent {

        protected VisibilityRequirementsPopup parent;
        protected String name;
        protected String desc;
        protected String valueName;
        protected Consumer<Boolean> enabledCallback;
        protected Consumer<Boolean> showIfCallback;
        protected Consumer<String> valueCallback;
        protected CharacterFilter valueFilter;
        protected boolean enabled;
        protected boolean showIf;
        protected String valueString;

        protected List<Runnable> preRenderTasks = new ArrayList<Runnable>();
        protected List<AdvancedButton> buttonList = new ArrayList<AdvancedButton>();

        protected AdvancedButton enableRequirementButton;
        protected AdvancedButton showIfButton;
        protected AdvancedButton showIfNotButton;
        protected AdvancedTextField valueTextField;

        public Requirement(VisibilityRequirementsPopup parent, String name, String desc, @Nullable String valueName, boolean enabled, boolean showIf, Consumer<Boolean> enabledCallback, Consumer<Boolean> showIfCallback, @Nullable Consumer<String> valueCallback, CharacterFilter valueFilter, String valueString) {
            this.parent = parent;
            this.name = name;
            this.desc = desc;
            this.valueName = valueName;
            this.enabledCallback = enabledCallback;
            this.showIfCallback = showIfCallback;
            this.valueCallback = valueCallback;
            this.valueFilter = valueFilter;
            this.enabled = enabled;
            this.showIf = showIf;
            this.valueString = valueString;
            this.init();
        }

        protected void init() {

            /** Toggle Requirement Button **/
            String enabledString = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.enabled", this.name);
            if (!this.enabled) {
                enabledString = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.disabled", this.name);
            }
            this.enableRequirementButton = new AdvancedButton(0, 0, 150, 20, enabledString, true, (press) -> {
                if (this.enabled) {
                    this.enabled = false;
                    this.enabledCallback.accept(false);
                    ((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.disabled", this.name));
                } else {
                    this.enabled = true;
                    this.enabledCallback.accept(true);
                    ((AdvancedButton)press).setMessage(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.enabled", this.name));
                }
            });
            List<String> descLines = new ArrayList<String>();
            descLines.addAll(Arrays.asList(StringUtils.splitLines(this.desc, "%n%")));
            descLines.add("");
            descLines.add(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.toggle.btn.desc"));
            this.enableRequirementButton.setDescription(descLines.toArray(new String[0]));
            this.preRenderTasks.add(() -> enableRequirementButton.setWidth(Minecraft.getInstance().font.width(enableRequirementButton.getMessage()) + 10));
            this.addButton(this.enableRequirementButton);

            /** Show If Button **/
            String showIfString = "§a" + Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showif");
            if (!this.showIf) {
                showIfString = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showif");
            }
            this.showIfButton = new AdvancedButton(0, 0, 100, 20, showIfString, true, (press) -> {
                this.showIf = true;
                this.showIfCallback.accept(true);
                ((AdvancedButton)press).setMessage("§a" + Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showif"));
                this.showIfNotButton.setMessage(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showifnot"));
            });
            this.showIfButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showif.btn.desc"), "%n%"));
            this.addButton(this.showIfButton);

            /** Show If Not Button **/
            String showIfNotString = "§a" + Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showifnot");
            if (this.showIf) {
                showIfNotString = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showifnot");
            }
            this.showIfNotButton = new AdvancedButton(0, 0, 100, 20, showIfNotString, true, (press) -> {
                this.showIf = false;
                this.showIfCallback.accept(false);
                ((AdvancedButton)press).setMessage("§a" + Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showifnot"));
                this.showIfButton.setMessage(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showif"));
            });
            this.showIfNotButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showifnot.btn.desc"), "%n%"));
            this.addButton(this.showIfNotButton);

            if ((this.valueCallback != null) && (this.valueName != null)) {
                this.valueTextField = new AdvancedTextField(Minecraft.getInstance().font, 0, 0, 150, 20, true, this.valueFilter);
                this.valueTextField.setCanLoseFocus(true);
                this.valueTextField.setFocus(false);
                this.valueTextField.setMaxLength(1000);
                if (this.valueString != null) {
                    this.valueTextField.setValue(this.valueString);
                }
            }

        }

        public void render(PoseStack matrix, int mouseX, int mouseY, Screen renderIn) {

            for (Runnable r : this.preRenderTasks) {
                r.run();
            }

            float partial = Minecraft.getInstance().getFrameTime();
            int centerX = renderIn.width / 2;
            int centerY = renderIn.height / 2;

            drawCenteredString(matrix, Minecraft.getInstance().font, Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.requirement") + ":", centerX, centerY - 83, -1);
            this.enableRequirementButton.x = centerX - (this.enableRequirementButton.getWidth() / 2);
            this.enableRequirementButton.y = centerY - 70;

            this.showIfButton.x = centerX - this.showIfButton.getWidth() - 5;
            this.showIfButton.y = centerY - 40;
            this.showIfNotButton.active = this.enabled;

            this.showIfNotButton.x = centerX + 5;
            this.showIfNotButton.y = centerY - 40;
            this.showIfButton.active = this.enabled;

            if (this.valueTextField != null) {
                drawCenteredString(matrix, Minecraft.getInstance().font, this.valueName + ":", centerX, centerY - 10, -1);

                this.valueTextField.x = centerX - (this.valueTextField.getWidth() / 2);
                this.valueTextField.y = centerY + 3;
                this.valueTextField.render(matrix, mouseX, mouseY, partial);
                this.valueTextField.active = this.enabled;
                this.valueTextField.setEditable(this.enabled);
                this.valueCallback.accept(this.valueTextField.getValue());
                this.valueString = this.valueTextField.getValue();
            }

            this.renderButtons(matrix, mouseX, mouseY, partial);

        }

        protected void renderButtons(PoseStack matrix, int mouseX, int mouseY, float partial) {
            for (AdvancedButton b : this.buttonList) {
                b.render(matrix, mouseX, mouseY, partial);
            }
        }

        protected void addButton(AdvancedButton b) {
            if (!this.buttonList.contains(b)) {
                this.buttonList.add(b);
                b.ignoreBlockedInput = true;
                this.parent.colorizePopupButton(b);
            }
        }

    }

}
