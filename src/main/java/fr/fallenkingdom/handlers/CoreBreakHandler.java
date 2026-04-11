package fr.fallenkingdom.handlers;

import fr.fallenkingdom.game.FKTeam;
import fr.fallenkingdom.game.GameManager;
import fr.fallenkingdom.game.GamePhase;
import fr.fallenkingdom.util.ChatUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Detecte la destruction du bloc "cœur" d'une equipe et elimine celle-ci.
 *
 * Le cœur est un simple bloc (ex : beacon) designe par {@code /fk setcore}.
 * Si le bloc a la position enregistree est casse, l'equipe perd. On empeche
 * egalement un joueur de detruire son propre cœur pendant la phase de jeu.
 */
public class CoreBreakHandler {

    private final GameManager gameManager;

    public CoreBreakHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() == null || event.getPlayer().worldObj.isRemote) return;

        GamePhase phase = gameManager.getPhase();
        if (phase != GamePhase.GAME && phase != GamePhase.PREPARATION) return;

        BlockPos pos = event.pos;
        MinecraftServer server = MinecraftServer.getServer();

        for (FKTeam t : gameManager.getTeams().values()) {
            if (t.isEliminated()) continue;
            BlockPos core = t.getCorePos();
            if (core == null) continue;
            if (core.getX() != pos.getX() || core.getY() != pos.getY() || core.getZ() != pos.getZ()) continue;

            // Un joueur ne peut pas casser son propre cœur.
            FKTeam own = gameManager.getTeamOfPlayer(event.getPlayer().getUniqueID());
            if (own == t) {
                event.setCanceled(true);
                ChatUtil.send(event.getPlayer(), EnumChatFormatting.RED
                    + "Vous ne pouvez pas detruire votre propre cœur !");
                return;
            }

            // En prep, le cœur est invulnerable.
            if (phase == GamePhase.PREPARATION) {
                event.setCanceled(true);
                ChatUtil.send(event.getPlayer(), EnumChatFormatting.RED
                    + "Les cœurs sont invulnerables pendant la preparation.");
                return;
            }

            // Sinon : elimination de l'equipe.
            t.setEliminated(true);
            ChatUtil.broadcast(server, EnumChatFormatting.DARK_RED + "[Fallen Kingdom] "
                + EnumChatFormatting.RESET + "Le cœur de l'equipe " + t.getColoredName()
                + EnumChatFormatting.RESET + " a ete detruit !");

            FKTeam winner = gameManager.checkVictory();
            if (winner != null || gameManager.getTeams().size() - countEliminated() <= 1) {
                gameManager.endGame(server, winner);
            }
            return;
        }
    }

    private int countEliminated() {
        int n = 0;
        for (FKTeam t : gameManager.getTeams().values()) {
            if (t.isEliminated()) n++;
        }
        return n;
    }
}
