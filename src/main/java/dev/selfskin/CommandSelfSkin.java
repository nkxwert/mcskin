package dev.selfskin;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import java.util.Arrays;
import java.util.List;

/** /selfskin          → 메뉴 열기
 *  /selfskin on|off   → 켜기/끄기 */
public class CommandSelfSkin extends CommandBase {

    @Override
    public String getCommandName() {
        return "selfskin";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("myskin");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/selfskin [on|off]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            SelfSkinMod.openGuiNextTick = true;
            return;
        }
        if ("on".equalsIgnoreCase(args[0])) {
            String err = SelfSkinMod.setEnabled(true);
            sender.addChatMessage(new ChatComponentText(err == null
                    ? "§a[SelfSkin] 켜짐" : "§c[SelfSkin] " + err));
        } else if ("off".equalsIgnoreCase(args[0])) {
            SelfSkinMod.setEnabled(false);
            sender.addChatMessage(new ChatComponentText("§e[SelfSkin] 꺼짐"));
        } else {
            sender.addChatMessage(new ChatComponentText("§7" + getCommandUsage(sender)));
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
