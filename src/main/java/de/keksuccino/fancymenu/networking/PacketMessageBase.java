
package de.keksuccino.fancymenu.networking;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public abstract class PacketMessageBase implements IMessage {

    /**
     * @param direction TO: server OR client
     */
    public String direction;

}
