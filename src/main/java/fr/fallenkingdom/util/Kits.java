package fr.fallenkingdom.util;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

/**
 * Kits de demarrage donnes aux joueurs au passage en phase {@code GAME}.
 *
 * Le kit par defaut est volontairement sobre :
 * <ul>
 *     <li>Epee en fer, pioche en fer, hache en fer, pelle en fer</li>
 *     <li>Arc + 64 fleches</li>
 *     <li>16 steaks cuits</li>
 *     <li>Armure en fer complete</li>
 * </ul>
 *
 * Cela donne aux equipes de quoi se battre correctement sans retirer tout
 * l'interet du PvP (le stuff supplementaire devra etre fabrique ou vole).
 */
public final class Kits {

    private Kits() {}

    public static void giveStarterKit(EntityPlayerMP p, EnumChatFormatting teamColor) {
        // Outils
        p.inventory.addItemStackToInventory(new ItemStack(Items.iron_sword));
        p.inventory.addItemStackToInventory(new ItemStack(Items.iron_pickaxe));
        p.inventory.addItemStackToInventory(new ItemStack(Items.iron_axe));
        p.inventory.addItemStackToInventory(new ItemStack(Items.iron_shovel));

        // Arc + fleches
        p.inventory.addItemStackToInventory(new ItemStack(Items.bow));
        p.inventory.addItemStackToInventory(new ItemStack(Items.arrow, 64));

        // Bouffe
        p.inventory.addItemStackToInventory(new ItemStack(Items.cooked_beef, 16));

        // Armure (slots 0..3 : bottes, jambieres, plastron, casque)
        p.inventory.armorInventory[0] = new ItemStack(Items.iron_boots);
        p.inventory.armorInventory[1] = new ItemStack(Items.iron_leggings);
        p.inventory.armorInventory[2] = new ItemStack(Items.iron_chestplate);
        p.inventory.armorInventory[3] = new ItemStack(Items.iron_helmet);
    }
}
