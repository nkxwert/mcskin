package dev.selfskin;

import net.minecraft.client.resources.I18n;

/**
 * 번역 문자열 도우미. 문장은 assets/selfskin/lang/*.lang 에 있고,
 * 게임 언어 설정에 맞춰 표시된다. (한국어 = ko_KR, 그 외 언어는 영어 en_US로 대체)
 */
public final class L {

    private L() {}

    /** "selfskin." + key 를 번역한다. 인자는 %s 자리에 들어간다. */
    public static String t(String key, Object... args) {
        return I18n.format("selfskin." + key, args);
    }

    /** 숨김/보임 표시 (색 포함) */
    public static String hidden(boolean hidden) {
        return t(hidden ? "state.hidden" : "state.shown");
    }

    /** 갑옷 칸 이름 (slot: SkinConfig.SLOT_*) */
    public static String slot(int slot) {
        return t("slot." + slot);
    }
}
