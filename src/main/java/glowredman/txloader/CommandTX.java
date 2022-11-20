package glowredman.txloader;

import glowredman.txloader.ConfigHandler.Asset;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.ClientCommandHandler;

public class CommandTX implements ICommand {

    @Override
    public int compareTo(Object o) {
        return this.getCommandName().compareTo(((ICommand) o).getCommandName());
    }

    @Override
    public String getCommandName() {
        return "tx";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/tx <save|add <version> <resourceLocation> [resourceLocationOverride] [force]>";
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List getCommandAliases() {
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        System.out.println(args.length);
        if (args.length == 0) {
            sender.addChatMessage(getColoredText("Not enough Arguments!", EnumChatFormatting.RED));
            return;
        }
        if (args[0].equals("save")) {
            if (ConfigHandler.save()) {
                sender.addChatMessage(getColoredText(
                        "Config saved successfully. The Changes take effect after restarting the Game.",
                        EnumChatFormatting.GREEN));
                return;
            }
            sender.addChatMessage(
                    getColoredText("Failed saving Config! Look at the Log to find the Cause.", EnumChatFormatting.RED));
        }
        if (args[0].equals("add")) {
            if (args.length < 3) {
                sender.addChatMessage(getColoredText("Not enough Arguments!", EnumChatFormatting.RED));
                return;
            }
            args = fixSpacesForVersion(args);
            if (args.length == 0) {
                sender.addChatMessage(getColoredText("Missing closing Quotation Mark!", EnumChatFormatting.RED));
                return;
            }
            if (args.length > 5) {
                sender.addChatMessage(getColoredText(
                        "Too many Arguments! If your Version has Spaces, wrap it in Quotation Marks.",
                        EnumChatFormatting.RED));
                return;
            }
            if (args.length == 3) {
                TXLoaderCore.REMOTE_ASSETS.add(new Asset(args[2], args[1]));
                sender.addChatMessage(getColoredText("Done. Don't forget to save!", EnumChatFormatting.GREEN));
                return;
            }
            if (args.length == 4) {
                if (args[3].equals("true")) {
                    TXLoaderCore.REMOTE_ASSETS.add(new Asset(args[2], args[1], true));
                    sender.addChatMessage(getColoredText("Done. Don't forget to save!", EnumChatFormatting.GREEN));
                    return;
                }
                if (args[3].equals("false")) {
                    TXLoaderCore.REMOTE_ASSETS.add(new Asset(args[2], args[1], false));
                    sender.addChatMessage(getColoredText("Done. Don't forget to save!", EnumChatFormatting.GREEN));
                    return;
                }
                TXLoaderCore.REMOTE_ASSETS.add(new Asset(args[2], args[1], args[3]));
                sender.addChatMessage(getColoredText("Done. Don't forget to save!", EnumChatFormatting.GREEN));
                return;
            }
            TXLoaderCore.REMOTE_ASSETS.add(new Asset(args[2], args[1], args[3], args[4].equals("true")));
            sender.addChatMessage(getColoredText("Done. Don't forget to save!", EnumChatFormatting.GREEN));
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return CommandBase.getListOfStringsMatchingLastWord(args, "add", "save");
        }
        if (!args[0].equals("add") || args.length > 5) {
            return null;
        }
        if (args.length == 2) {
            return CommandBase.getListOfStringsFromIterableMatchingLastWord(args, RemoteHandler.VERSIONS.keySet());
        }
        if (args.length == 4 || args.length == 5) {
            return CommandBase.getListOfStringsMatchingLastWord(args, "true", "false");
        }
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    private static IChatComponent getColoredText(String text, EnumChatFormatting color) {
        return new ChatComponentText(text).setChatStyle(new ChatStyle().setColor(color));
    }

    /**
     * Some Minecraft versions have spaces in their name (e.g. <code>1.14.2 Pre-Release 4</code>).
     * The later part of {@link CommandTX#processCommand(ICommandSender, String[])} assumes the version to only be in one element of the <code>args</code>-array.
     * Minecrafts splits the entered command at the spaces
     * @param args the command arguments, as passed to {@link CommandTX#processCommand(ICommandSender, String[])}
     * @return <b>args</b> if no version wrapping was done<br><b>args</b> with all version elements joined together, if valid version wrapping was done<br>an empty {@link String} array, if invalid wrapping was done
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
