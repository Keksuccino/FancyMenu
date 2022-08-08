package de.keksuccino.fancymenu.networking.packets.command.commands.variable;

import de.keksuccino.fancymenu.networking.PacketMessageBase;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class VariableCommandSuggestionsPacketMessage extends PacketMessageBase {

    public int variableNameSuggestionsListLength = 0;
    public List<String> variableNameSuggestions = new ArrayList<>();

    @Override
    public void fromBytes(ByteBuf buf) {
        try {
            this.variableNameSuggestionsListLength = buf.readInt();
            CharSequence c = buf.readCharSequence(this.variableNameSuggestionsListLength, StandardCharsets.UTF_8);
            if (c != null) {
                String s = c.toString();
                if (s.contains(";")) {
                    for (String s2 : s.split("[;]")) {
                        if (s2.length() > 0) {
                            this.variableNameSuggestions.add(s2);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        try {
            String s = "";
            for (String s2 : this.variableNameSuggestions) {
                s += s2 + ";";
            }
            CharSequence c = s;
            buf.writeInt(c.length());
            buf.writeCharSequence(c, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
