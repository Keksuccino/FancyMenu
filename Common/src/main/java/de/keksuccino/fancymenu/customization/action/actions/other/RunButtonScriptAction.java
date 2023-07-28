package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ButtonScriptHandler;
import de.keksuccino.fancymenu.util.Legacy;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Legacy("Button scripts got replaced by the new action system, so remove this in the future.")
@Deprecated
public class RunButtonScriptAction extends Action {

    public RunButtonScriptAction() {
        super("runscript");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if (value != null) {
            if (value.endsWith(".txt")) value = value.replace(".txt", "");
            if (value.endsWith(".TXT")) value = value.replace(".TXT", "");
            ButtonScriptHandler.ButtonScript script = ButtonScriptHandler.getScript(value);
            if (script != null) script.runScript();
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.runscript");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.editor.custombutton.config.actiontype.runscript.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.runscript.desc.value");
    }

    @Override
    public String getValueExample() {
        return "some_script.txt";
    }

}
