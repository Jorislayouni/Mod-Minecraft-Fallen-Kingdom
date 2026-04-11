package fr.fallenkingdom.handlers;

import fr.fallenkingdom.game.FKTeam;
import fr.fallenkingdom.game.GameManager;
import fr.fallenkingdom.game.GamePhase;
import fr.fallenkingdom.util.ChatUtil;
import fr.fallenkingdom.util.Kits;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.UUID;

/**
 * Gere les evenements joueur :
 * <ul>
 *     <li>Desactive le PvP entre equipes pendant la phase de preparation.</li>
 *     <li>Empeche le friendly fire entre coequipiers.</li>
 *     <li>Decremente les vies a chaque mort, et bascule en spectateur quand a zero.</li>
 *     <li>Respawn automatique au spawn d'equipe avec un kit neuf.</li>
 * </ul>
 */
public class PlayerEventHandler {

    private final GameManager gameManager;

    public PlayerEventHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @SubscribeEvent
    public void onAttack(LivingAttackEvent event) {
        if (!(event.entity instanceof EntityPlayer)) return;
        if (!(event.source.getEntity() instanceof EntityPlayer)) return;

        EntityPlayer victim = (EntityPlayer) event.entity;
        EntityPlayer attacker = (EntityPlayer) event.source.getEntity();

        GamePhase phase = gameManager.getPhase();
        if (phase == GamePhase.PREPARATION) {
            event.setCanceled(true);
            return;
        }

        if (phase != GamePhase.GAME) return;

        FKTeam tv = gameManager.getTeamOfPlayer(victim.getUniqueID());
        FKTeam ta = gameManager.getTeamOfPlayer(attacker.getUniqueID());
        if (tv != null && ta != null && tv == ta) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!(event.entity instanceof EntityPlayerMP)) return;
        if (gameManager.getPhase() != GamePhase.GAME) return;

        EntityPlayerMP player = (EntityPlayerMP) event.entity;
        UUID uuid = player.getUniqueID();
        FKTeam team = gameManager.getTeamOfPlayer(uuid);
        if (team == null) return;

        int remaining = gameManager.decrementLives(uuid);
        MinecraftServer server = MinecraftServer.getServer();

        if (remaining <= 0) {
            ChatUtil.broadcast(server, EnumChatFormatting.GRAY + player.getName()
                + " n'a plus de vies, il passe en spectateur.");
            // 1.8.8 n'a pas SPECTATOR => on passe en ADVENTURE et on le teleporte loin.
            // (Si vous utilisez 1.8.9+, remplacez par GameType.SPECTATOR.)
            player.setGameType(WorldSettings.GameType.ADVENTURE);

            // Si plus aucun joueur vivant, on elimine l'equipe.
            if (allTeamPlayersOut(team)) {
                team.setEliminated(true);
                ChatUtil.broadcast(server, EnumChatFormatting.DARK_RED + "[Fallen Kingdom] "
                    + EnumChatFormatting.RESET + "L'equipe " + team.getColoredName()
                    + EnumChatFormatting.RESET + " n'a plus de joueurs en vie !");
                FKTeam winner = gameManager.checkVictory();
                if (winner != null) gameManager.endGame(server, winner);
            }
        } else {
            ChatUtil.send(player, EnumChatFormatting.YELLOW + "Il vous reste "
                + remaining + " vie" + (remaining > 1 ? "s" : "") + ".");
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;

        if (gameManager.getPhase() != GamePhase.GAME) return;

        FKTeam team = gameManager.getTeamOfPlayer(player.getUniqueID());
        if (team == null) return;

        if (gameManager.getLives(player.getUniqueID()) <= 0) return;

        BlockPos sp = team.getSpawnPos();
        if (sp != null) {
            player.setPositionAndUpdate(sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5);
        }
        player.inventory.clear();
        Kits.giveStarterKit(player, team.getColor());
    }

    private boolean allTeamPlayersOut(FKTeam team) {
        for (UUID u : team.getPlayers()) {
            if (gameManager.getLives(u) > 0) return false;
        }
        return true;
    }
}
