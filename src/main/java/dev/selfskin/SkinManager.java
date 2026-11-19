package dev.selfskin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 내 NetworkPlayerInfo의 스킨 필드만 바꿔치기한다.
 * 3인칭 모델, 1인칭 팔, 인벤토리 미리보기, 탭 리스트가 모두 이 값을 읽는다.
 * 서버로는 아무것도 보내지 않으므로 다른 사람에게는 원래 스킨이 보인다.
 */
public class SkinManager {

    /** 항상 같은 위치를 쓴다. 스킨을 바꿀 때는 이 위치의 텍스처 객체만 교체한다. */
    private static final ResourceLocation TEXTURE = new ResourceLocation("selfskin", "skins/self");

    private final Minecraft mc = Minecraft.getMinecraft();

    private final Field fSkin;       // NetworkPlayerInfo.locationSkin
    private final Field fType;       // NetworkPlayerInfo.skinType ("default" / "slim")
    private final Field fLoaded;     // NetworkPlayerInfo.playerTexturesLoaded
    private final Field fPlayerInfo; // AbstractClientPlayer.playerInfo (플레이어가 캐시해 둔 객체)

    private DynamicTexture texture;
    private String customModel = SkinConfig.MODEL_CLASSIC;
    private boolean active;
    /** 현재 스킨을 덮어쓴 객체들 (탭 리스트의 것 / 플레이어가 캐시한 것). 원래 스킨으로 되돌릴 때 사용한다. */
    private NetworkPlayerInfo appliedList, appliedCached;

    public SkinManager() {
        // 개발 환경(MCP 이름) → 실제 실행(SRG 이름) → 타입으로 찾기 순서로 시도
        fSkin = findField(NetworkPlayerInfo.class, ResourceLocation.class, 0, "locationSkin", "field_178865_e");
        fType = findField(NetworkPlayerInfo.class, String.class, 0, "skinType", "field_178863_g");
        fLoaded = findField(NetworkPlayerInfo.class, boolean.class, 0, "playerTexturesLoaded", "field_178864_d");
        fPlayerInfo = findField(AbstractClientPlayer.class, NetworkPlayerInfo.class, 0, "playerInfo", "field_175157_a");
    }

    /** png를 읽어 텍스처로 등록한다. 메인 스레드에서 호출해야 한다. */
    public String load(File png, String model) {
        BufferedImage img;
        try {
            img = ImageIO.read(png);
        } catch (Exception e) {
            return L.t("error.unreadable", e.getMessage());
        }
        if (img == null) return L.t("error.notPng");
        if (img.getWidth() != 64 || (img.getHeight() != 64 && img.getHeight() != 32)) {
            return L.t("error.badSize", img.getWidth(), img.getHeight());
        }
        // 바닐라와 같은 처리: 64x32 구형 스킨 변환 + 투명 영역 정리
        img = new ImageBufferDownload().parseUserSkin(img);

        // 새 텍스처를 먼저 등록한 뒤 이전 텍스처를 지운다.
        // (이전 텍스처를 먼저 지우면 그 사이에 렌더링되는 프레임이 삭제된 GL 텍스처를 참조함)
        DynamicTexture old = texture;
        texture = new DynamicTexture(img);
        mc.getTextureManager().loadTexture(TEXTURE, texture);
        if (old != null) old.deleteGlTexture();

        customModel = SkinConfig.MODEL_SLIM.equals(model) ? SkinConfig.MODEL_SLIM : SkinConfig.MODEL_CLASSIC;
        return null;
    }

    public void setModel(String model) {
        customModel = SkinConfig.MODEL_SLIM.equals(model) ? SkinConfig.MODEL_SLIM : SkinConfig.MODEL_CLASSIC;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) restoreAll();
    }

    public boolean hasTexture() {
        return texture != null;
    }

    public void unload() {
        restoreAll();
        if (texture != null) {
            mc.getTextureManager().deleteTexture(TEXTURE);
            texture.deleteGlTexture();
            texture = null;
        }
    }

    /** 매 프레임 호출. 바닐라가 원래 스킨을 늦게 받아와 덮어써도 다시 적용된다. (할당 없이 동작) */
    public void update() {
        if (!active || texture == null) {
            if (appliedList != null || appliedCached != null) restoreAll();
            return;
        }
        if (mc.thePlayer == null || mc.getNetHandler() == null) return;

        // 탭 리스트는 NetHandler의 객체를 쓰지만, 플레이어 엔티티는 처음 받은 객체를 캐시해 두고 계속 쓴다.
        // 서버가 탭 리스트 항목만 다시 보내면 두 객체가 달라지므로 둘 다 덮어써야 한다.
        NetworkPlayerInfo list = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        NetworkPlayerInfo cached = null;
        try {
            cached = (NetworkPlayerInfo) fPlayerInfo.get(mc.thePlayer);
        } catch (IllegalAccessException ignored) {
        }
        if (cached == list) cached = null;
        if (list == null && cached == null) return;

        // 서버/로비 이동으로 객체가 바뀐 경우 이전 객체는 원래대로 돌려 놓는다
        if (appliedList != null && appliedList != list && appliedList != cached) restore(appliedList);
        if (appliedCached != null && appliedCached != list && appliedCached != cached) restore(appliedCached);
        appliedList = list;
        appliedCached = cached;
        override(list);
        override(cached);
    }

    private void override(NetworkPlayerInfo info) {
        if (info == null) return;
        try {
            if (fSkin.get(info) != TEXTURE) fSkin.set(info, TEXTURE);
            if (fType.get(info) != customModel) fType.set(info, customModel); // customModel은 상수 문자열
        } catch (IllegalAccessException e) {
            SelfSkinMod.LOG.error("Failed to apply skin", e);
        }
    }

    private void restoreAll() {
        if (appliedList != null) restore(appliedList);
        if (appliedCached != null) restore(appliedCached);
        appliedList = null;
        appliedCached = null;
    }

    /** 원래 스킨으로 되돌린다: 필드를 비우면 바닐라가 Mojang 스킨을 다시 불러온다. */
    private void restore(NetworkPlayerInfo info) {
        try {
            fSkin.set(info, null);
            fType.set(info, null);
            fLoaded.setBoolean(info, false);
        } catch (IllegalAccessException e) {
            SelfSkinMod.LOG.error("Failed to restore skin", e);
        }
    }

    static Field findField(Class<?> owner, Class<?> type, int index, String... names) {
        for (String name : names) {
            try {
                Field f = owner.getDeclaredField(name);
                if (f.getType() == type) {
                    f.setAccessible(true);
                    return f;
                }
            } catch (NoSuchFieldException ignored) {
            }
        }
        // 클라이언트가 이름을 다르게 매핑한 경우 대비: 같은 타입의 index번째 필드
        int i = 0;
        for (Field f : owner.getDeclaredFields()) {
            if (f.getType() == type && !Modifier.isStatic(f.getModifiers())) {
                if (i++ == index) {
                    f.setAccessible(true);
                    return f;
                }
            }
        }
        throw new IllegalStateException(owner.getSimpleName() + " field not found: " + names[0]);
    }
}
