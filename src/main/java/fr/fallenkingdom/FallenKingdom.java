package fr.fallenkingdom;

import fr.fallenkingdom.commands.CommandFK;
import fr.fallenkingdom.game.GameManager;
import fr.fallenkingdom.handlers.BlockPlaceHandler;
import fr.fallenkingdom.handlers.CoreBreakHandler;
import fr.fallenkingdom.handlers.PlayerEventHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * Point d'entree du mod Fallen Kingdom.
 *
 * Le mod s'active cote serveur (ou integre). Un {@link GameManager} unique
 * pilote l'etat du jeu (LOBBY / PREPARATION / GAME / ENDED), et des handlers
 * Forge appliquent les regles (restriction de pose de blocs, destruction du
 * cœur, PvP, respawn).
 */
@Mod(
    modid = Reference.MOD_ID,
    name = Reference.MOD_NAME,
    version = Reference.VERSION,
    acceptedMinecraftVersions = Reference.ACCEPTED_MC_VERSIONS,
    acceptableRemoteVersions = "*"
)
public class FallenKingdom {

    @Mod.Instance(Reference.MOD_ID)
    public static FallenKingdom INSTANCE;

    private GameManager gameManager;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        this.gameManager = new GameManager();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new BlockPlaceHandler(gameManager));
        MinecraftForge.EVENT_BUS.register(new CoreBreakHandler(gameManager));
        MinecraftForge.EVENT_BUS.register(new PlayerEventHandler(gameManager));
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandFK(gameManager));
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
