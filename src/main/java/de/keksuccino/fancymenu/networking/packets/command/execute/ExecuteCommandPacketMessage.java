
package de.keksuccino.fancymenu.networking.packets.command.execute;

import de.keksuccino.fancymenu.networking.PacketMessageBase;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public class ExecuteCommandPacketMessage extends PacketMessageBase {

    public int commandStringLength = 0;
    public String command;

    @Override
    public void fromBytes(ByteBuf buf) {
        try {
            this.commandStringLength = buf.readInt();
            this.command = buf.readCharSequence(this.commandStringLength, StandardCharsets.UTF_8).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        try {
            CharSequence c = this.command;
            buf.writeInt(c.length());
            buf.writeCharSequence(c, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
