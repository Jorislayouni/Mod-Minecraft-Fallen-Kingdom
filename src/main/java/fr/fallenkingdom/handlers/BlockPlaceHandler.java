package fr.fallenkingdom.handlers;

import fr.fallenkingdom.game.FKTeam;
import fr.fallenkingdom.game.GameManager;
import fr.fallenkingdom.game.GamePhase;
import fr.fallenkingdom.util.ChatUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Empeche la pose de blocs hors de la base de l'equipe du joueur pendant la
 * phase de jeu. Exceptions :
 * <ul>
 *     <li>TNT : toujours autorisee (n'importe quel joueur, n'importe ou).</li>
 *     <li>Phase de preparation : pose libre a l'interieur de la base propre uniquement.</li>
 *     <li>Phase LOBBY ou ENDED : pose libre (pour setup / cleanup).</li>
 * </ul>
 */
public class BlockPlaceHandler {

    private final GameManager gameManager;

    public BlockPlaceHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.PlaceEvent event) {
        EntityPlayer player = event.player;
        if (player == null || player.worldObj.isRemote) return;

        GamePhase phase = gameManager.getPhase();
        if (phase == GamePhase.LOBBY || phase == GamePhase.ENDED) {
            return; // pas de restriction hors partie
        }

        Block placed = event.placedBlock.getBlock();
        // TNT toujours autorisee (cœur de gameplay pour casser les bases).
        if (placed == Blocks.tnt) {
            return;
        }

        FKTeam team = gameManager.getTeamOfPlayer(player.getUniqueID());
        if (team == null) {
            // Joueur non inscrit : on laisse passer (admin / spectateur).
            return;
        }

        BlockPos pos = event.pos;

        if (phase == GamePhase.PREPARATION) {
            // En prep, un joueur ne peut construire que dans SA base.
            if (team.getBase() == null || !team.getBase().contains(pos)) {
                event.setCanceled(true);
                ChatUtil.send(player, EnumChatFormatting.RED
                    + "Vous ne pouvez construire que dans votre base pendant la preparation.");
            }
            return;
        }

        // Phase GAME : pose interdite hors de SA base, sauf TNT (deja geree).
        if (team.getBase() == null || !team.getBase().contains(pos)) {
            event.setCanceled(true);
            ChatUtil.send(player, EnumChatFormatting.RED
                + "Hors de votre base, seule la TNT peut etre posee.");
        }
    }
}
