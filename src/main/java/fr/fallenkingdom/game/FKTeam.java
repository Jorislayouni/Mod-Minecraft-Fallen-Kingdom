package fr.fallenkingdom.game;

import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Une equipe Fallen Kingdom. Regroupe ses joueurs, sa base, son cœur
 * (bloc/beacon a defendre) et son spawn de respawn pendant la phase de jeu.
 */
public class FKTeam {

    private final String name;
    private final EnumChatFormatting color;
    private final Set<UUID> players = new HashSet<UUID>();

    private BaseRegion base;
    private BlockPos corePos;
    private BlockPos spawnPos;

    private int livesPerPlayer;
    private boolean eliminated;

    public FKTeam(String name, EnumChatFormatting color, int livesPerPlayer) {
        this.name = name;
        this.color = color;
        this.livesPerPlayer = livesPerPlayer;
        this.eliminated = false;
    }

    public String getName() { return name; }
    public EnumChatFormatting getColor() { return color; }
    public String getColoredName() { return color + name + EnumChatFormatting.RESET; }

    public Set<UUID> getPlayers() { return players; }
    public void addPlayer(UUID uuid) { players.add(uuid); }
    public void removePlayer(UUID uuid) { players.remove(uuid); }
    public boolean hasPlayer(UUID uuid) { return players.contains(uuid); }

    public BaseRegion getBase() { return base; }
    public void setBase(BaseRegion base) { this.base = base; }

    public BlockPos getCorePos() { return corePos; }
    public void setCorePos(BlockPos corePos) { this.corePos = corePos; }

    public BlockPos getSpawnPos() { return spawnPos; }
    public void setSpawnPos(BlockPos spawnPos) { this.spawnPos = spawnPos; }

    public int getLivesPerPlayer() { return livesPerPlayer; }
    public void setLivesPerPlayer(int livesPerPlayer) { this.livesPerPlayer = livesPerPlayer; }

    public boolean isEliminated() { return eliminated; }
    public void setEliminated(boolean eliminated) { this.eliminated = eliminated; }

    /**
     * Une equipe est valide pour demarrer une partie si elle a au moins un
     * joueur, une base et un cœur definis.
     */
    public boolean isReady() {
        return !players.isEmpty() && base != null && corePos != null && spawnPos != null;
    }
}
