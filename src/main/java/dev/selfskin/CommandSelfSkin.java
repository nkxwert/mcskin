package dev.selfskin;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

import java.util.Arrays;
import java.util.List;

/** /selfskin          → 메뉴 열기
 *  /selfskin on|off   → 켜기/끄기
 *  /selfskin armor [부위] [on|off] → 내 갑옷 보이기/숨기기
 *      부위: helmet, chestplate, leggings, boots, headblock (생략하면 갑옷 전체), on/off 생략 시 전환
 *  /selfskin cape [on|off] → 내 망토 보이기/숨기기 */
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
        return "/selfskin [on|off] | /selfskin armor [helmet|chestplate|leggings|boots|headblock] [on|off] | /selfskin cape [on|off]";
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
        } else if ("armor".equalsIgnoreCase(args[0])) {
            armor(sender, args);
        } else if ("cape".equalsIgnoreCase(args[0])) {
            // on = 보이기, off = 숨기기, 생략 = 전환
            boolean hide;
            if (args.length >= 2 && "on".equalsIgnoreCase(args[1])) hide = false;
            else if (args.length >= 2 && "off".equalsIgnoreCase(args[1])) hide = true;
            else hide = !SelfSkinMod.config.hideCape;
            SelfSkinMod.setCapeHidden(hide);
            sender.addChatMessage(new ChatComponentText("§e[SelfSkin] 망토: " + (hide ? "§c숨김" : "§a보임")));
        } else {
            sender.addChatMessage(new ChatComponentText("§7" + getCommandUsage(sender)));
        }
    }

    private void armor(ICommandSender sender, String[] args) {
        SkinConfig cfg = SelfSkinMod.config;
        int next = 1;
        int slot = -1; // -1 = 갑옷 전체 (머리 블록은 포함하지 않음)
        if (args.length > next) {
            int s = parsePart(args[next]);
            if (s != -1) {
                slot = s;
                next++;
            }
        }

        // on = 보이기, off = 숨기기, 생략 = 전환
        Boolean hide = null;
        if (args.length > next) {
            if ("on".equalsIgnoreCase(args[next])) hide = false;
            else if ("off".equalsIgnoreCase(args[next])) hide = true;
            else {
                sender.addChatMessage(new ChatComponentText("§c[SelfSkin] 알 수 없는 부위: " + args[next]
                        + " §7(helmet, chestplate, leggings, boots, headblock)"));
                return;
            }
        }

        if (slot == HEAD_BLOCK) {
            boolean h = hide != null ? hide : !cfg.hideHeadBlock;
            SelfSkinMod.setHeadBlockHidden(h);
            sender.addChatMessage(new ChatComponentText("§e[SelfSkin] 머리 블록: " + (h ? "§c숨김" : "§a보임")));
        } else if (slot >= 0) {
            boolean h = hide != null ? hide : !cfg.isArmorHidden(slot);
            SelfSkinMod.setArmorHidden(slot, h);
            sender.addChatMessage(new ChatComponentText("§e[SelfSkin] " + SkinConfig.SLOT_NAMES[slot] + ": "
                    + (h ? "§c숨김" : "§a보임")));
        } else {
            // 전체 전환: 하나라도 보이면 모두 숨기고, 모두 숨겨져 있으면 모두 보이게
            boolean h = hide != null ? hide : !cfg.isAllArmorHidden();
            SelfSkinMod.setAllArmorHidden(h);
            sender.addChatMessage(new ChatComponentText(h ? "§e[SelfSkin] 갑옷 전체 숨김" : "§a[SelfSkin] 갑옷 전체 보임"));
        }
    }

    /** parsePart 결과: 머리 블록 */
    private static final int HEAD_BLOCK = -2;

    /** 부위 이름 → 갑옷 칸 번호 (머리 블록은 HEAD_BLOCK). 모르는 이름이면 -1 */
    private static int parsePart(String s) {
        s = s.toLowerCase();
        if (s.equals("headblock") || s.equals("skull") || s.equals("머리블록")) return HEAD_BLOCK;
        if (s.equals("helmet") || s.equals("투구")) return SkinConfig.SLOT_HELMET;
        if (s.equals("chestplate") || s.equals("chest") || s.equals("흉갑")) return SkinConfig.SLOT_CHESTPLATE;
        if (s.equals("leggings") || s.equals("legs") || s.equals("레깅스")) return SkinConfig.SLOT_LEGGINGS;
        if (s.equals("boots") || s.equals("feet") || s.equals("부츠")) return SkinConfig.SLOT_BOOTS;
        return -1;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "on", "off", "armor", "cape");
        if (args.length == 2 && "cape".equalsIgnoreCase(args[0])) return getListOfStringsMatchingLastWord(args, "on", "off");
        if (args.length == 2 && "armor".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "helmet", "chestplate", "leggings", "boots", "headblock", "on", "off");
        }
        if (args.length == 3 && "armor".equalsIgnoreCase(args[0]) && parsePart(args[1]) != -1) {
            return getListOfStringsMatchingLastWord(args, "on", "off");
        }
        return null;
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
