package fr.fallenkingdom.handlers;

import fr.fallenkingdom.game.FKTeam;
import fr.fallenkingdom.game.GameManager;
import fr.fallenkingdom.game.GamePhase;
import fr.fallenkingdom.util.ChatUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Tick serveur : decremente le timer du {@link GameManager}, annonce le temps
 * restant aux moments cles et declenche les transitions de phase.
 *
 * Annonces : toutes les minutes pendant les 5 premieres minutes, puis
 * 60/30/15/10/5/4/3/2/1 secondes avant la fin.
 */
public class TickHandler {

    private final GameManager gameManager;

    public TickHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        GamePhase phase = gameManager.getPhase();
        if (phase != GamePhase.PREPARATION && phase != GamePhase.GAME) return;

        boolean finished = gameManager.tickDown();
        MinecraftServer server = MinecraftServer.getServer();

        if (gameManager.shouldAnnounceSecond()) {
            announceIfNeeded(server, phase, gameManager.getRemainingSeconds());
        }

        if (finished) {
            if (phase == GamePhase.PREPARATION) {
                gameManager.startGamePhase(server);
            } else if (phase == GamePhase.GAME) {
                // Fin du timer de jeu : equipe avec le plus de joueurs gagne,
                // ou egalite.
                FKTeam winner = pickWinnerByPopulation();
                gameManager.endGame(server, winner);
            }
        }
    }

    private void announceIfNeeded(MinecraftServer server, GamePhase phase, int sec) {
        boolean announce = false;
        if (sec > 0 && sec <= 5) announce = true;
        else if (sec == 10 || sec == 15 || sec == 30 || sec == 60) announce = true;
        else if (sec > 0 && sec % 60 == 0) announce = true;

        if (!announce) return;

        String label = (phase == GamePhase.PREPARATION) ? "Preparation" : "Jeu";
        EnumChatFormatting color = (phase == GamePhase.PREPARATION)
            ? EnumChatFormatting.YELLOW : EnumChatFormatting.RED;

        String timeStr;
        if (sec >= 60 && sec % 60 == 0) {
            timeStr = (sec / 60) + " minute" + (sec / 60 > 1 ? "s" : "");
        } else {
            timeStr = sec + " seconde" + (sec > 1 ? "s" : "");
        }

        ChatUtil.broadcast(server, color + "[Fallen Kingdom] " + label
            + " : " + timeStr + " restant" + (sec > 1 ? "es" : "") + ".");
    }

    private FKTeam pickWinnerByPopulation() {
        FKTeam best = null;
        int bestCount = -1;
        boolean tie = false;
        for (FKTeam t : gameManager.getTeams().values()) {
            if (t.isEliminated()) continue;
            int n = t.getPlayers().size();
            if (n > bestCount) {
                bestCount = n;
                best = t;
                tie = false;
            } else if (n == bestCount) {
                tie = true;
            }
        }
        return tie ? null : best;
    }
}
