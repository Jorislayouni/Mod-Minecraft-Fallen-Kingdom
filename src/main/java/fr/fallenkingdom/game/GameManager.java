package fr.fallenkingdom.game;

import fr.fallenkingdom.util.ChatUtil;
import fr.fallenkingdom.util.Kits;
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
 * Etat global d'une partie de Fallen Kingdom. Une seule instance pour tout
 * le serveur.
 *
 * Responsabilites :
 * <ul>
 *     <li>Gerer les equipes (creation, affectation des joueurs).</li>
 *     <li>Tenir le compte a rebours (prep puis jeu).</li>
 *     <li>Effectuer les transitions de phase (LOBBY -> PREPARATION -> GAME -> ENDED).</li>
 *     <li>Tracker les vies restantes par joueur.</li>
 * </ul>
 *
 * Le decompte est exprime en ticks serveur (20 ticks / seconde).
 */
public class GameManager {

    /** Duree par defaut de la phase de preparation : 20 minutes. */
    public static final int DEFAULT_PREPARATION_SECONDS = 20 * 60;

    /** Duree par defaut max de la phase de jeu : 60 minutes (securite). */
    public static final int DEFAULT_GAME_SECONDS = 60 * 60;

    private GamePhase phase = GamePhase.LOBBY;

    private final Map<String, FKTeam> teams = new LinkedHashMap<String, FKTeam>();

    /** Ticks restants dans la phase courante (prep ou jeu). */
    private int remainingTicks;

    private int preparationSeconds = DEFAULT_PREPARATION_SECONDS;
    private int gameSeconds = DEFAULT_GAME_SECONDS;

    /** Vies restantes par joueur (UUID -> vies). */
    private final Map<UUID, Integer> playerLives = new HashMap<UUID, Integer>();

    /** Dernier entier de secondes annonce (pour n'envoyer qu'une annonce / sec). */
    private int lastAnnouncedSecond = -1;

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

    public int getRemainingTicks() { return remainingTicks; }
    public int getRemainingSeconds() { return remainingTicks / 20; }

    public int getPreparationSeconds() { return preparationSeconds; }
    public void setPreparationSeconds(int s) { this.preparationSeconds = s; }

    public int getGameSeconds() { return gameSeconds; }
    public void setGameSeconds(int s) { this.gameSeconds = s; }

    /**
     * Demarre la partie : verifie que chaque equipe est prete, puis passe
     * en phase {@link GamePhase#PREPARATION}.
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
        this.remainingTicks = preparationSeconds * 20;
        this.lastAnnouncedSecond = -1;

        // Passage en creatif de tous les joueurs + teleport dans leur base.
        for (FKTeam t : teams.values()) {
            for (UUID u : t.getPlayers()) {
                EntityPlayerMP p = getPlayerByUUID(server, u);
                if (p == null) continue;
                p.setGameType(WorldSettings.GameType.CREATIVE);
                BlockPos c = t.getBase() != null ? t.getBase().getCenter() : t.getSpawnPos();
                if (c != null) {
                    p.setPositionAndUpdate(c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5);
                }
                ChatUtil.send(p, EnumChatFormatting.GOLD + "Phase de preparation demarree ! "
                    + EnumChatFormatting.YELLOW + preparationSeconds / 60 + " minutes pour construire votre base.");
            }
        }

        ChatUtil.broadcast(server, EnumChatFormatting.GOLD + "[Fallen Kingdom] "
            + EnumChatFormatting.YELLOW + "La partie commence ! Phase de preparation : "
            + (preparationSeconds / 60) + " minutes.");
        return null;
    }

    /**
     * Transition PREPARATION -> GAME : teleportation de toutes les equipes
     * devant leur base en survie, avec le kit de depart.
     */
    public void startGamePhase(MinecraftServer server) {
        this.phase = GamePhase.GAME;
        this.remainingTicks = gameSeconds * 20;
        this.lastAnnouncedSecond = -1;

        for (FKTeam t : teams.values()) {
            for (UUID u : t.getPlayers()) {
                EntityPlayerMP p = getPlayerByUUID(server, u);
                if (p == null) continue;

                p.setGameType(WorldSettings.GameType.SURVIVAL);
                p.inventory.clear();
                Kits.giveStarterKit(p, t.getColor());

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
    }

    /** Annonce la fin de la partie et bascule en ENDED. */
    public void endGame(MinecraftServer server, FKTeam winner) {
        this.phase = GamePhase.ENDED;
        this.remainingTicks = 0;
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
        this.remainingTicks = 0;
        ChatUtil.broadcast(server, EnumChatFormatting.GRAY + "[Fallen Kingdom] Partie arretee par un admin.");
    }

    // ------------------------------------------------------------------ ticks

    /** Decremente le timer d'un tick, renvoie true si la phase est finie. */
    public boolean tickDown() {
        if (phase != GamePhase.PREPARATION && phase != GamePhase.GAME) return false;
        if (remainingTicks > 0) remainingTicks--;
        return remainingTicks <= 0;
    }

    public boolean shouldAnnounceSecond() {
        int sec = getRemainingSeconds();
        if (sec == lastAnnouncedSecond) return false;
        lastAnnouncedSecond = sec;
        return true;
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
     * Verifie si une equipe doit etre eliminee (cœur detruit et/ou plus de
     * joueur en vie). Appele apres chaque mort / destruction de cœur.
     *
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
