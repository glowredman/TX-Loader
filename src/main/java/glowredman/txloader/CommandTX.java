package glowredman.txloader;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import glowredman.txloader.Asset.Source;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.ClientCommandHandler;

class CommandTX implements ICommand {

    @Override
    public int compareTo(ICommand o) {
        return this.getName().compareTo(o.getName());
    }

    @Override
    public String getName() {
        return "tx";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/tx <save|add <version> <source> <resourceLocation> [resourceLocationOverride] [force]>";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        int length = args.length;
        if (length == 0) {
            sender.sendMessage(getColoredText("Not enough Arguments!", TextFormatting.RED));
            return;
        }
        String mode = args[0];
        if (mode.equals("save")) {
            if (ConfigHandler.save()) {
                sender.sendMessage(
                        getColoredText(
                                "Config saved successfully. The Changes take effect after restarting the Game.",
                                TextFormatting.GREEN));
                return;
            }
            sender.sendMessage(
                    getColoredText("Failed saving Config! Look at the Log to find the Cause.", TextFormatting.RED));
            return;
        }
        if (mode.equals("add")) {
            if (length < 4) {
                sender.sendMessage(getColoredText("Not enough Arguments!", TextFormatting.RED));
                return;
            }
            args = fixSpacesForVersion(args);
            if (length == 0) {
                sender.sendMessage(getColoredText("Missing closing Quotation Mark!", TextFormatting.RED));
                return;
            }
            if (length > 6) {
                sender.sendMessage(
                        getColoredText(
                                "Too many Arguments! If your Version has Spaces, wrap it in Quotation Marks.",
                                TextFormatting.RED));
                return;
            }
            final Asset asset = new Asset(args[3], args[1], Source.get(args[2]));
            if (length != 4) {
                String arg4 = args[4];
                if (length == 5) {
                    // argument 4 may either be forceLoad or resourceLocationOverride
                    if (arg4.equals("true")) {
                        asset.forceLoad = true;
                    } else if (arg4.equals("false")) {
                        asset.forceLoad = false;
                    } else {
                        asset.resourceLocationOverride = arg4;
                    }
                } else {
                    asset.resourceLocationOverride = arg4;
                    asset.forceLoad = args[5].equals("true");
                }
            }
            TXLoaderCore.REMOTE_ASSETS.add(asset);
            sender.sendMessage(getColoredText("Done. Don't forget to save!", TextFormatting.GREEN));
        }
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        int length = args.length;
        if (length == 1) {
            return CommandBase.getListOfStringsMatchingLastWord(args, "add", "save");
        }
        if (!args[0].equals("add") || length > 6) {
            return null;
        }
        if (length == 2) {
            return CommandBase.getListOfStringsMatchingLastWord(args, RemoteHandler.VERSIONS.keySet());
        }
        if (length == 3) {
            return CommandBase.getListOfStringsMatchingLastWord(args, Source.NAMES);
        }
        if (length == 5 || length == 6) {
            return CommandBase.getListOfStringsMatchingLastWord(args, "true", "false");
        }
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    private static ITextComponent getColoredText(String text, TextFormatting color) {
        return new TextComponentString(text).setStyle(new Style().setColor(color));
    }

    /**
     * Some Minecraft versions have spaces in their name (e.g. <code>1.14.2 Pre-Release 4</code>). The later part of
     * {@link CommandTX#processCommand(ICommandSender, String[])} assumes the version to only be in one element of the
     * <code>args</code>-array. Minecrafts splits the entered command at the spaces
     * 
     * @param args the command arguments, as passed to {@link CommandTX#processCommand(ICommandSender, String[])}
     * @return <b>args</b> if no version wrapping was done<br>
     *         <b>args</b> with all version elements joined together, if valid version wrapping was done<br>
     *         an empty {@link String} array, if invalid wrapping was done
     * @author glowredman
     * @see CommandHandler#executeCommand(ICommandSender, String)
     * @see ClientCommandHandler#executeCommand(ICommandSender, String)
     */
    private static String[] fixSpacesForVersion(String[] args) {

        // return early, if no wrapping is applied
        if (!args[1].startsWith("\"")) {
            return args;
        }

        // find last element
        int end = 0;
        for (int i = 1; i < args.length; i++) {
            if (args[i].endsWith("\"")) {
                end = i;
                break;
            }
        }

        // return early if no closing quotation mark was found
        if (end == 0) {
            return new String[0]; // invalid wrapping
        }

        int offset = end - 1;
        String[] fixedArgs = new String[args.length - offset];

        // join version elements
        StringBuilder sb = new StringBuilder();
        sb.append(args[1]);
        for (int i = 2; i <= end; i++) {
            sb.append(' ');
            sb.append(args[i]);
        }

        // remove quotation marks
        sb.deleteCharAt(0);
        sb.deleteCharAt(sb.length() - 1);

        // copy other elements
        fixedArgs[0] = args[0];
        fixedArgs[1] = sb.toString();
        for (int i = end + 1; i < args.length; i++) {
            fixedArgs[i - offset] = args[i];
        }

        return fixedArgs;
    }
}
