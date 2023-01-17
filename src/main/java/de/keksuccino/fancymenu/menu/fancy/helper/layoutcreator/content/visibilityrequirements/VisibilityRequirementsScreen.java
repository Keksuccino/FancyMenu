
package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.visibilityrequirements;

import de.keksuccino.fancymenu.api.visibilityrequirements.VisibilityRequirement;
import de.keksuccino.fancymenu.menu.fancy.helper.PlaceholderEditBox;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.ScrollableScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.VisibilityRequirementContainer;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollArea;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class VisibilityRequirementsScreen extends ScrollableScreen {

    protected static final Color ENTRY_BACK_1 = new Color(0, 0, 0, 50);
    protected static final Color ENTRY_BACK_2 = new Color(0, 0, 0, 90);

    public CustomizationItemBase parentItem;

    protected int entryBackTick = 0;
    protected AdvancedButton doneButton;

    protected List<PlaceholderEditBox> contextMenuRenderQueue = new ArrayList<>();

    public VisibilityRequirementsScreen(GuiScreen parent, CustomizationItemBase parentItem) {

        super(parent, Locals.localize("fancymenu.helper.ui.visibility_requirements.manage"));

        this.parentItem = parentItem;

        this.doneButton = new AdvancedButton(0, 0, 200, 20, Locals.localize("fancymenu.guicomponents.done"), true, (press) -> {
            Minecraft.getMinecraft().displayGuiScreen(this.parent);
        });
        this.doneButton.ignoreLeftMouseDownClickBlock = true;
        UIBase.colorizeButton(this.doneButton);

        VisibilityRequirementContainer c = this.parentItem.visibilityRequirementContainer;

        //LEGACY REQUIREMENTS
        LegacyVisibilityRequirements.getLegacyRequirements(this, c).forEach((req) -> {
            this.addRequirement(req);
        });

        //API REQUIREMENTS
        for (VisibilityRequirementContainer.RequirementPackage p : c.customRequirements.values()) {

            final VisibilityRequirement v = p.requirement;

            String valuePreset = null;
            if (v.hasValue()) {
                valuePreset = v.getValuePreset();
                if (valuePreset == null) {
                    valuePreset = "";
                }
                if (p.value != null) {
                    valuePreset = p.value;
                }
            }
            Consumer<String> valueCallback = null;
            if (v.hasValue()) {
                valueCallback = (call) -> {
                    p.value = call;
                };
            }
            CharacterFilter charFilter = v.getValueInputFieldFilter();
            String desc = "";
            for (String s : v.getDescription()) {
                if (s.equalsIgnoreCase("")) {
                    s = " ";
                }
                if (desc.equalsIgnoreCase("")) {
                    desc += s;
                } else {
                    desc += "%n%" + s;
                }
            }
            Requirement req = new Requirement(this, v.getDisplayName(), desc, v.getValueDisplayName(), p.checkFor, p.showIf,
                    (enabledCallback) -> {
                        p.checkFor = enabledCallback;
                    }, (showIfCallback) -> {
                p.showIf = showIfCallback;
            }, valueCallback, charFilter, valuePreset);
            this.addRequirement(req);

        }

    }

    @Override
    public boolean isOverlayButtonHovered() {
        return this.doneButton.isMouseOver();
    }

    protected void addRequirement(Requirement requirement) {
        if (this.entryBackTick == 0) {
            this.scrollArea.addEntry(new RequirementScrollEntry(this.scrollArea, requirement, ENTRY_BACK_1));
            this.entryBackTick = 1;
        } else {
            this.scrollArea.addEntry(new RequirementScrollEntry(this.scrollArea, requirement, ENTRY_BACK_2));
            this.entryBackTick = 0;
        }
        this.scrollArea.addEntry(new SeparatorEntry(this.scrollArea, 1, new Color(255,255,255,100)));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        super.drawScreen(mouseX, mouseY, partialTicks);

        int xCenter = this.width / 2;
        this.doneButton.x = xCenter - (this.doneButton.width / 2);
        this.doneButton.y = this.height - 35;
        this.doneButton.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTicks);

        for (PlaceholderEditBox b : this.contextMenuRenderQueue) {
            b.renderContextMenu();
        }
        this.contextMenuRenderQueue.clear();

    }

    public static class RequirementScrollEntry extends ScrollAreaEntryBase {

        public Requirement requirement;
        public boolean hasValue = false;
        public Color backgroundColor;

        public RequirementScrollEntry(ScrollArea parent, Requirement requirement, Color backgroundColor) {
            super(parent, (call) -> {});
            this.requirement = requirement;
            this.backgroundColor = backgroundColor;
            if ((requirement.valueCallback != null) && (requirement.valueName != null)) {
                this.hasValue = true;
                this.setHeight(100 + 10);
            } else {
                this.setHeight(56 + 10);
            }
        }

        @Override
        public void renderEntry() {

            super.renderEntry();

            drawRect(this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), this.backgroundColor.getRGB());

            this.requirement.render(MouseInput.getMouseX(), MouseInput.getMouseY(), this);

        }

    }

    public static class Requirement extends Gui {

        protected VisibilityRequirementsScreen parent;
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

        protected boolean hasValue = false;

        protected List<Runnable> preRenderTasks = new ArrayList<>();
        protected List<AdvancedButton> buttonList = new ArrayList<>();

        protected AdvancedButton enableRequirementButton;
        protected AdvancedButton showIfButton;
        protected AdvancedButton showIfNotButton;
        protected AdvancedTextField valueTextField;

        public Requirement(VisibilityRequirementsScreen parent, String name, String desc, @Nullable String valueName, boolean enabled, boolean showIf, Consumer<Boolean> enabledCallback, Consumer<Boolean> showIfCallback, @Nullable Consumer<String> valueCallback, CharacterFilter valueFilter, String valueString) {
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
                if (!this.parent.isOverlayButtonHovered()) {
                    if (this.enabled) {
                        this.enabled = false;
                        this.enabledCallback.accept(false);
                        ((AdvancedButton) press).displayString = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.disabled", this.name);
                    } else {
                        this.enabled = true;
                        this.enabledCallback.accept(true);
                        ((AdvancedButton) press).displayString = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.enabled", this.name);
                    }
                }
            });
            List<String> descLines = new ArrayList<String>();
            descLines.addAll(Arrays.asList(StringUtils.splitLines(this.desc, "%n%")));
            descLines.add("");
            descLines.add(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.toggle.btn.desc"));
            this.enableRequirementButton.setDescription(descLines.toArray(new String[0]));
            this.preRenderTasks.add(() -> enableRequirementButton.setWidth(Minecraft.getMinecraft().fontRenderer.getStringWidth(enableRequirementButton.displayString) + 10));
            this.addButton(this.enableRequirementButton);

            /** Show If Button **/
            String showIfString = "§a" + Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showif");
            if (!this.showIf) {
                showIfString = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showif");
            }
            this.showIfButton = new AdvancedButton(0, 0, 100, 20, showIfString, true, (press) -> {
                if (!this.parent.isOverlayButtonHovered()) {
                    this.showIf = true;
                    this.showIfCallback.accept(true);
                    ((AdvancedButton) press).displayString = "§a" + Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showif");
                    this.showIfNotButton.displayString = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showifnot");
                }
            });
            this.showIfButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showif.btn.desc"), "%n%"));
            this.addButton(this.showIfButton);

            /** Show If Not Button **/
            String showIfNotString = "§a" + Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showifnot");
            if (this.showIf) {
                showIfNotString = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showifnot");
            }
            this.showIfNotButton = new AdvancedButton(0, 0, 100, 20, showIfNotString, true, (press) -> {
                if (!this.parent.isOverlayButtonHovered()) {
                    this.showIf = false;
                    this.showIfCallback.accept(false);
                    ((AdvancedButton) press).displayString = "§a" + Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showifnot");
                    this.showIfButton.displayString = Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showif");
                }
            });
            this.showIfNotButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.showifnot.btn.desc"), "%n%"));
            this.addButton(this.showIfNotButton);

            if ((this.valueCallback != null) && (this.valueName != null)) {
                this.hasValue = true;
                this.valueTextField = new PlaceholderEditBox(Minecraft.getMinecraft().fontRenderer, 0, 0, 150, 20, true, this.valueFilter);
                ((PlaceholderEditBox)this.valueTextField).renderContextMenu = false;
                this.valueTextField.setCanLoseFocus(true);
                this.valueTextField.setFocused(false);
                this.valueTextField.setMaxStringLength(1000);
                if (this.valueString != null) {
                    this.valueTextField.setText(this.valueString);
                }
            }

        }

        public void render(int mouseX, int mouseY, RequirementScrollEntry entry) {

            for (Runnable r : this.preRenderTasks) {
                r.run();
            }

            float partial = Minecraft.getMinecraft().getRenderPartialTicks();
            int originX = entry.x + (entry.getWidth() / 2);
            int originY = entry.y + (entry.getHeight() / 2);
            if (this.hasValue) {
                originY += 23;
            } else {
                originY += 45;
            }

            this.enableRequirementButton.x = originX - (this.enableRequirementButton.width / 2);
            this.enableRequirementButton.y = originY - 70;

            this.showIfButton.x = originX - this.showIfButton.width - 5;
            this.showIfButton.y = originY - 40;
            this.showIfNotButton.enabled = this.enabled;

            this.showIfNotButton.x = originX + 5;
            this.showIfNotButton.y = originY - 40;
            this.showIfButton.enabled = this.enabled;

            if (this.valueTextField != null) {
                drawCenteredString(Minecraft.getMinecraft().fontRenderer, this.valueName + ":", originX, originY - 10, -1);

                this.valueTextField.x = originX - (this.valueTextField.getWidth() / 2);
                this.valueTextField.y = originY + 3;
                this.valueTextField.drawTextBox();
                this.parent.contextMenuRenderQueue.add(((PlaceholderEditBox)this.valueTextField));
                this.valueTextField.setEnabled(this.enabled);
                this.valueCallback.accept(this.valueTextField.getText());
                this.valueString = this.valueTextField.getText();
            }

            this.renderButtons(mouseX, mouseY, partial);

        }

        protected void renderButtons(int mouseX, int mouseY, float partial) {
            for (AdvancedButton b : this.buttonList) {
                b.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partial);
            }
        }

        protected void addButton(AdvancedButton b) {
            if (!this.buttonList.contains(b)) {
                this.buttonList.add(b);
                UIBase.colorizeButton(b);
            }
        }

    }

}
