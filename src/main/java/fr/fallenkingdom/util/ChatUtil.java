package fr.fallenkingdom.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.util.List;

/** Helpers d'envoi de messages en jeu. */
public final class ChatUtil {

    private ChatUtil() {}

    public static void send(EntityPlayer player, String msg) {
        player.addChatMessage(new ChatComponentText(msg));
    }

    public static void send(EntityPlayer player, IChatComponent comp) {
        player.addChatMessage(comp);
    }

    @SuppressWarnings("unchecked")
    public static void broadcast(MinecraftServer server, String msg) {
        if (server == null) return;
        List<EntityPlayerMP> players = server.getConfigurationManager().playerEntityList;
        IChatComponent comp = new ChatComponentText(msg);
        for (EntityPlayerMP p : players) {
            p.addChatMessage(comp);
        }
    }
}
