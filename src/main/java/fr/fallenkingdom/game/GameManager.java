package fr.fallenkingdom.game;

import fr.fallenkingdom.util.ChatUtil;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldSettings;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Etat global d'une partie de Fallen Kingdom.
 *
 * Deux phases de jeu :
 * <ul>
 *     <li>{@code /fk start} → PREPARATION (survie, PvP off, construction dans la base).</li>
 *     <li>{@code /fk game}  → GAME (survie, PvP on, objectif = detruire le coeur).</li>
 * </ul>
 * Pas de timer : l'admin decide quand passer d'une phase a l'autre.
 */
public class GameManager {

    private GamePhase phase = GamePhase.LOBBY;

    private final Map<String, FKTeam> teams = new LinkedHashMap<String, FKTeam>();

    /** Vies restantes par joueur (UUID -> vies). */
    private final Map<UUID, Integer> playerLives = new HashMap<UUID, Integer>();

    // ------------------------------------------------------------------ teams

    public FKTeam createTeam(String name, EnumChatFormatting color, int livesPerPlayer) {
        FKTeam team = new FKTeam(name, color, livesPerPlayer);
        teams.put(name.toLowerCase(), team);
        return team;
    }

    public FKTeam getTeam(String name) {
        return teams.get(name.toLowerCase());
    }

    public Map<String, FKTeam> getTeams() {
        return teams;
    }

    public FKTeam getTeamOfPlayer(UUID uuid) {
        for (FKTeam t : teams.values()) {
            if (t.hasPlayer(uuid)) return t;
        }
        return null;
    }

    public void removeTeam(String name) {
        teams.remove(name.toLowerCase());
    }

    // ------------------------------------------------------------------ phase

    public GamePhase getPhase() { return phase; }

    /**
     * Demarre la phase de preparation : verifie que chaque equipe est prete,
     * teleporte les joueurs au centre de leur base en mode survie.
     * PvP desactive pendant cette phase.
     *
     * @return {@code null} si OK, sinon un message d'erreur.
     */
    public String start(MinecraftServer server) {
        if (phase != GamePhase.LOBBY && phase != GamePhase.ENDED) {
            return "Une partie est deja en cours (phase=" + phase + ").";
        }
        if (teams.size() < 2) {
            return "Il faut au moins 2 equipes pour demarrer.";
        }
        for (FKTeam t : teams.values()) {
            if (!t.isReady()) {
                return "Equipe '" + t.getName() + "' incomplete (joueurs/base/coeur/spawn).";
            }
        }

        // Reset des vies.
        playerLives.clear();
        for (FKTeam t : teams.values()) {
            t.setEliminated(false);
            for (UUID u : t.getPlayers()) {
                playerLives.put(u, t.getLivesPerPlayer());
            }
        }

        this.phase = GamePhase.PREPARATION;

        // Teleport au centre de la base en survie.
        for (FKTeam t : teams.values()) {
            for (UUID u : t.getPlayers()) {
                EntityPlayerMP p = getPlayerByUUID(server, u);
                if (p == null) continue;
                p.setGameType(WorldSettings.GameType.SURVIVAL);
                BlockPos c = t.getBase() != null ? t.getBase().getCenter() : t.getSpawnPos();
                if (c != null) {
                    p.setPositionAndUpdate(c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5);
                }
                ChatUtil.send(p, EnumChatFormatting.GOLD + "Phase de preparation ! "
                    + EnumChatFormatting.YELLOW + "Construisez votre base. L'admin lancera le combat avec /fk game.");
            }
        }

        ChatUtil.broadcast(server, EnumChatFormatting.GOLD + "[Fallen Kingdom] "
            + EnumChatFormatting.YELLOW + "Phase de preparation demarree. Construisez vos bases !");
        return null;
    }

    /**
     * Transition PREPARATION -> GAME : teleportation de toutes les equipes
     * devant leur spawn en survie (pas de kit, pas d'inventaire touche).
     *
     * @return {@code null} si OK, sinon un message d'erreur.
     */
    public String startGamePhase(MinecraftServer server) {
        if (phase != GamePhase.PREPARATION) {
            return "Pas en phase de preparation (phase=" + phase + ").";
        }

        this.phase = GamePhase.GAME;

        for (FKTeam t : teams.values()) {
            for (UUID u : t.getPlayers()) {
                EntityPlayerMP p = getPlayerByUUID(server, u);
                if (p == null) continue;

                p.setGameType(WorldSettings.GameType.SURVIVAL);
                BlockPos sp = t.getSpawnPos();
                if (sp != null) {
                    p.setPositionAndUpdate(sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5);
                }
                p.setHealth(p.getMaxHealth());
                p.getFoodStats().addStats(20, 20f);

                ChatUtil.send(p, EnumChatFormatting.RED + "La phase de jeu commence ! "
                    + EnumChatFormatting.WHITE + "Detruisez les coeurs adverses pour gagner !");
            }
        }

        ChatUtil.broadcast(server, EnumChatFormatting.RED + "[Fallen Kingdom] "
            + EnumChatFormatting.WHITE + "Fin de la preparation, que le combat commence !");
        return null;
    }

    /** Annonce la fin de la partie et bascule en ENDED. */
    public void endGame(MinecraftServer server, FKTeam winner) {
        this.phase = GamePhase.ENDED;
        if (winner != null) {
            ChatUtil.broadcast(server, EnumChatFormatting.GOLD + "[Fallen Kingdom] Victoire de l'equipe "
                + winner.getColoredName() + EnumChatFormatting.GOLD + " !");
        } else {
            ChatUtil.broadcast(server, EnumChatFormatting.GOLD + "[Fallen Kingdom] Fin de la partie (egalite).");
        }
    }

    /** Arret manuel de la partie par un admin. */
    public void stop(MinecraftServer server) {
        this.phase = GamePhase.LOBBY;
        ChatUtil.broadcast(server, EnumChatFormatting.GRAY + "[Fallen Kingdom] Partie arretee par un admin.");
    }

    // ------------------------------------------------------------------ vies

    public int getLives(UUID uuid) {
        Integer i = playerLives.get(uuid);
        return i == null ? 0 : i;
    }

    public int decrementLives(UUID uuid) {
        int n = getLives(uuid) - 1;
        playerLives.put(uuid, Math.max(0, n));
        return n;
    }

    /**
     * @return l'equipe gagnante si une seule survit, null sinon.
     */
    public FKTeam checkVictory() {
        int alive = 0;
        FKTeam last = null;
        for (FKTeam t : teams.values()) {
            if (!t.isEliminated()) {
                alive++;
                last = t;
            }
        }
        return alive <= 1 ? last : null;
    }

    // ------------------------------------------------------------------ utils

    public static EntityPlayerMP getPlayerByUUID(MinecraftServer server, UUID uuid) {
        if (server == null) return null;
        return (EntityPlayerMP) server.getConfigurationManager().getPlayerByUUID(uuid);
    }
}
