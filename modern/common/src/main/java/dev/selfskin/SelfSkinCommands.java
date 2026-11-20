package dev.selfskin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 클라이언트 명령어 (Fabric / Forge 공용, 명령어 출처 타입 S만 로더마다 다름)
 *   /selfskin (/myskin)                         메뉴 열기
 *   /selfskin on|off                            켜기/끄기
 *   /selfskin armor [on|off]                    갑옷 전체 보이기/숨기기 (생략 시 전환)
 *   /selfskin armor <부위> [on|off]             helmet, chestplate, leggings, boots, headblock
 *   /selfskin cape [on|off]                     망토 보이기/숨기기
 */
public final class SelfSkinCommands {

    private static final int HEAD_BLOCK = -2;
    private static final String[] PART_NAMES = {"boots", "leggings", "chestplate", "helmet"};

    private SelfSkinCommands() {}

    public static <S> void register(CommandDispatcher<S> dispatcher) {
        LiteralCommandNode<S> root = dispatcher.register(build("selfskin"));
        dispatcher.register(LiteralArgumentBuilder.<S>literal("myskin").executes(c -> openMenu()).redirect(root));
    }

    private static <S> LiteralArgumentBuilder<S> build(String name) {
        LiteralArgumentBuilder<S> armor = LiteralArgumentBuilder.<S>literal("armor")
                .executes(c -> armorAll(null))
                .then(LiteralArgumentBuilder.<S>literal("on").executes(c -> armorAll(false)))
                .then(LiteralArgumentBuilder.<S>literal("off").executes(c -> armorAll(true)));
        for (int slot = 0; slot < 4; slot++) armor.then(part(PART_NAMES[slot], slot));
        armor.then(part("headblock", HEAD_BLOCK));

        return LiteralArgumentBuilder.<S>literal(name)
                .executes(c -> openMenu())
                .then(LiteralArgumentBuilder.<S>literal("on").executes(c -> {
                    Component err = SelfSkin.setEnabled(true);
                    say(err == null ? L.c("cmd.on") : L.c("cmd.error", err));
                    return 1;
                }))
                .then(LiteralArgumentBuilder.<S>literal("off").executes(c -> {
                    SelfSkin.setEnabled(false);
                    say(L.c("cmd.off"));
                    return 1;
                }))
                .then(armor)
                .then(LiteralArgumentBuilder.<S>literal("cape")
                        .executes(c -> cape(null))
                        .then(LiteralArgumentBuilder.<S>literal("on").executes(c -> cape(false)))
                        .then(LiteralArgumentBuilder.<S>literal("off").executes(c -> cape(true))));
    }

    private static <S> LiteralArgumentBuilder<S> part(String name, int slot) {
        return LiteralArgumentBuilder.<S>literal(name)
                .executes(c -> part(slot, null))
                .then(LiteralArgumentBuilder.<S>literal("on").executes(c -> part(slot, false)))
                .then(LiteralArgumentBuilder.<S>literal("off").executes(c -> part(slot, true)));
    }

    private static int openMenu() {
        SelfSkin.openNextTick = true; // 채팅창이 닫힌 다음 틱에 연다
        return 1;
    }

    /** hide: true 숨김, false 보임, null 전환 */
    private static int part(int slot, Boolean hide) {
        SkinConfig cfg = SelfSkin.config;
        if (slot == HEAD_BLOCK) {
            boolean h = hide != null ? hide : !cfg.hideHeadBlock;
            SelfSkin.setHeadBlockHidden(h);
            say(L.c("cmd.line", L.c("headBlock"), L.hidden(h)));
        } else {
            boolean h = hide != null ? hide : !cfg.isArmorHidden(slot);
            SelfSkin.setArmorHidden(slot, h);
            say(L.c("cmd.line", L.slot(slot), L.hidden(h)));
        }
        return 1;
    }

    /** 전체 전환: 하나라도 보이면 모두 숨기고, 모두 숨겨져 있으면 모두 보이게 */
    private static int armorAll(Boolean hide) {
        boolean h = hide != null ? hide : !SelfSkin.config.isAllArmorHidden();
        SelfSkin.setAllArmorHidden(h);
        say(L.c(h ? "cmd.allHidden" : "cmd.allShown"));
        return 1;
    }

    private static int cape(Boolean hide) {
        boolean h = hide != null ? hide : !SelfSkin.config.hideCape;
        SelfSkin.setCapeHidden(h);
        say(L.c("cmd.line", L.c("cape"), L.hidden(h)));
        return 1;
    }

    private static void say(Component msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendSystemMessage(msg);
    }
}
