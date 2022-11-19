package glowredman.txloader;

import glowredman.txloader.ConfigHandler.Asset;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

public class TXCommand implements ICommand {

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
        return "tx <save|add <version> <resourceLocation> [resourceLocationOverride] [force]>";
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List getCommandAliases() {
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
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
            if (args.length == 3) {
                TXLoaderCore.REMOTE_ASSETS.add(new Asset(args[2], args[1]));
                return;
            }
            if (args.length == 4) {
                if (args[3].equals("true")) {
                    TXLoaderCore.REMOTE_ASSETS.add(new Asset(args[2], args[1], true));
                    return;
                }
                if (args[3].equals("false")) {
                    TXLoaderCore.REMOTE_ASSETS.add(new Asset(args[2], args[1], false));
                    return;
                }
                TXLoaderCore.REMOTE_ASSETS.add(new Asset(args[2], args[1], args[3]));
                return;
            }
            TXLoaderCore.REMOTE_ASSETS.add(new Asset(args[2], args[1], args[3], args[4].equals("true")));
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
}
