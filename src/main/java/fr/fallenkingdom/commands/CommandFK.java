package fr.fallenkingdom.commands;

import fr.fallenkingdom.game.BaseRegion;
import fr.fallenkingdom.game.FKTeam;
import fr.fallenkingdom.game.GameManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Arrays;
import java.util.List;

/**
 * Commande unique {@code /fk} pour piloter une partie de Fallen Kingdom.
 *
 * <pre>
 * /fk start                                           → lance la prep (survie, PvP off)
 * /fk game                                            → lance le combat (PvP on)
 * /fk stop                                            → arrete la partie
 * /fk status
 * /fk team create &lt;nom&gt; &lt;couleur&gt; [vies]
 * /fk team add &lt;joueur&gt; &lt;equipe&gt;
 * /fk team remove &lt;joueur&gt;
 * /fk team list
 * /fk setbase &lt;equipe&gt; &lt;x1&gt; &lt;y1&gt; &lt;z1&gt; &lt;x2&gt; &lt;y2&gt; &lt;z2&gt;
 * /fk setcore &lt;equipe&gt; [x y z]
 * /fk setspawn &lt;equipe&gt; [x y z]
 * </pre>
 */
public class CommandFK extends CommandBase {

    private final GameManager gm;

    public CommandFK(GameManager gm) {
        this.gm = gm;
    }

    @Override
    public String getCommandName() {
        return "fk";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/fk <start|game|stop|status|team|setbase|setcore|setspawn>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        String sub = args[0].toLowerCase();
        MinecraftServer server = MinecraftServer.getServer();

        if ("start".equals(sub)) {
            String err = gm.start(server);
            if (err != null) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + err));
            } else {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN
                    + "Phase de preparation demarree. Tapez /fk game pour lancer le combat."));
            }
            return;
        }

        if ("game".equals(sub)) {
            String err = gm.startGamePhase(server);
            if (err != null) {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + err));
            } else {
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "Phase de jeu lancee !"));
            }
            return;
        }

        if ("stop".equals(sub)) {
            gm.stop(server);
            return;
        }

        if ("status".equals(sub)) {
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD
                + "Phase : " + gm.getPhase()));
            for (FKTeam t : gm.getTeams().values()) {
                sender.addChatMessage(new ChatComponentText(" - " + t.getColoredName()
                    + EnumChatFormatting.GRAY + " (" + t.getPlayers().size() + " joueurs, "
                    + (t.isEliminated() ? "eliminee" : "en vie") + ")"));
            }
            return;
        }

        if ("team".equals(sub)) {
            handleTeam(sender, args);
            return;
        }

        if ("setbase".equals(sub)) {
            if (args.length < 8) throw new WrongUsageException("/fk setbase <equipe> <x1> <y1> <z1> <x2> <y2> <z2>");
            FKTeam t = requireTeam(args[1]);
            BlockPos a = new BlockPos(parseInt(args[2]), parseInt(args[3]), parseInt(args[4]));
            BlockPos b = new BlockPos(parseInt(args[5]), parseInt(args[6]), parseInt(args[7]));
            t.setBase(new BaseRegion(a, b));
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN
                + "Base definie pour " + t.getName() + " : " + t.getBase()));
            return;
        }

        if ("setcore".equals(sub)) {
            if (args.length < 2) throw new WrongUsageException("/fk setcore <equipe> [x y z]");
            FKTeam t = requireTeam(args[1]);
            BlockPos pos = readPosOrSender(sender, args, 2);
            t.setCorePos(pos);
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN
                + "Coeur de " + t.getName() + " place en " + pos));
            return;
        }

        if ("setspawn".equals(sub)) {
            if (args.length < 2) throw new WrongUsageException("/fk setspawn <equipe> [x y z]");
            FKTeam t = requireTeam(args[1]);
            BlockPos pos = readPosOrSender(sender, args, 2);
            t.setSpawnPos(pos);
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN
                + "Spawn de " + t.getName() + " place en " + pos));
            return;
        }

        throw new WrongUsageException(getCommandUsage(sender));
    }

    private void handleTeam(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) throw new WrongUsageException("/fk team <create|add|remove|list>");
        String action = args[1].toLowerCase();

        if ("create".equals(action)) {
            if (args.length < 4) throw new WrongUsageException("/fk team create <nom> <couleur> [vies]");
            String name = args[2];
            EnumChatFormatting color = parseColor(args[3]);
            int lives = args.length >= 5 ? parseInt(args[4], 1, 99) : 3;
            FKTeam t = gm.createTeam(name, color, lives);
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN
                + "Equipe creee : " + t.getColoredName() + " (vies=" + lives + ")"));
            return;
        }

        if ("add".equals(action)) {
            if (args.length < 4) throw new WrongUsageException("/fk team add <joueur> <equipe>");
            EntityPlayerMP p = getPlayer(sender, args[2]);
            FKTeam t = requireTeam(args[3]);
            FKTeam existing = gm.getTeamOfPlayer(p.getUniqueID());
            if (existing != null) existing.removePlayer(p.getUniqueID());
            t.addPlayer(p.getUniqueID());
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN
                + p.getName() + " ajoute a l'equipe " + t.getColoredName()));
            return;
        }

        if ("remove".equals(action)) {
            if (args.length < 3) throw new WrongUsageException("/fk team remove <joueur>");
            EntityPlayerMP p = getPlayer(sender, args[2]);
            FKTeam t = gm.getTeamOfPlayer(p.getUniqueID());
            if (t != null) t.removePlayer(p.getUniqueID());
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN
                + p.getName() + " retire de son equipe."));
            return;
        }

        if ("list".equals(action)) {
            for (FKTeam t : gm.getTeams().values()) {
                sender.addChatMessage(new ChatComponentText(" - " + t.getColoredName()
                    + EnumChatFormatting.GRAY + " joueurs=" + t.getPlayers().size()));
            }
            return;
        }

        throw new WrongUsageException("/fk team <create|add|remove|list>");
    }

    private FKTeam requireTeam(String name) throws CommandException {
        FKTeam t = gm.getTeam(name);
        if (t == null) throw new CommandException("Equipe inconnue : " + name);
        return t;
    }

    private EnumChatFormatting parseColor(String s) throws CommandException {
        try {
            return EnumChatFormatting.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CommandException("Couleur invalide : " + s
                + " (ex: RED, BLUE, GREEN, YELLOW, GOLD, AQUA, LIGHT_PURPLE)");
        }
    }

    private BlockPos readPosOrSender(ICommandSender sender, String[] args, int from) throws CommandException {
        if (args.length >= from + 3) {
            return new BlockPos(parseInt(args[from]), parseInt(args[from + 1]), parseInt(args[from + 2]));
        }
        BlockPos p = sender.getPosition();
        if (p == null) throw new CommandException("Position non disponible, specifiez x y z.");
        return p;
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("fallenkingdom");
    }
}
