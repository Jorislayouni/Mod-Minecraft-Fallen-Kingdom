package fr.fallenkingdom.game;

import net.minecraft.util.BlockPos;

/**
 * Represente une zone rectangulaire (AABB aligne sur les axes) delimitant une
 * base d'equipe. Les joueurs de l'equipe peuvent poser des blocs a l'interieur
 * pendant la phase de preparation, et les adversaires ne peuvent pas y poser
 * de blocs sauf la TNT pendant la phase de jeu.
 */
public class BaseRegion {

    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    public BaseRegion(BlockPos a, BlockPos b) {
        this.minX = Math.min(a.getX(), b.getX());
        this.minY = Math.min(a.getY(), b.getY());
        this.minZ = Math.min(a.getZ(), b.getZ());
        this.maxX = Math.max(a.getX(), b.getX());
        this.maxY = Math.max(a.getY(), b.getY());
        this.maxZ = Math.max(a.getZ(), b.getZ());
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= minX && pos.getX() <= maxX
            && pos.getY() >= minY && pos.getY() <= maxY
            && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    public BlockPos getCenter() {
        return new BlockPos(
            (minX + maxX) / 2,
            (minY + maxY) / 2,
            (minZ + maxZ) / 2
        );
    }

    @Override
    public String toString() {
        return "BaseRegion[" + minX + "," + minY + "," + minZ + " -> "
            + maxX + "," + maxY + "," + maxZ + "]";
    }
}
