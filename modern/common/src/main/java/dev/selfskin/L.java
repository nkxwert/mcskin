package dev.selfskin;

import net.minecraft.network.chat.Component;

/** 번역 문자열 도우미. 문장은 assets/selfskin/lang/*.json 에 있고 게임 언어 설정을 따른다. */
public final class L {

    private L() {}

    /** "selfskin." + key 번역 */
    public static Component c(String key, Object... args) {
        return Component.translatable("selfskin." + key, args);
    }

    /** 숨김/보임 표시 (색 포함) */
    public static Component hidden(boolean hidden) {
        return c(hidden ? "state.hidden" : "state.shown");
    }

    /** 켜짐/꺼짐 표시 (색 포함) */
    public static Component onOff(boolean on) {
        return c(on ? "state.on" : "state.off");
    }

    /** 갑옷 칸 이름 (slot: SkinConfig.SLOT_*) */
    public static Component slot(int slot) {
        return c("slot." + slot);
    }

    /** SkinConfig가 돌려준 오류 키를 문장으로 */
    public static Component error(String key) {
        return c(key, SkinConfig.MAX_NAME_LENGTH);
    }
}
