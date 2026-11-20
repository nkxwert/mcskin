package dev.selfskin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.selfskin.gui.SelfSkinScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Self Skin 공통 코드 (Fabric / Forge 공용).
 * 로더별 코드는 init(), getOpenKey() 등록, onClientTick() 호출, 명령어 등록만 연결한다.
 */
public final class SelfSkin {

    public static final String MOD_ID = "selfskin";
    public static final Logger LOG = LoggerFactory.getLogger("SelfSkin");

    public static SkinConfig config;
    public static final SkinTextures skins = new SkinTextures();

    private static KeyMapping openKey;
    private static boolean restored;
    /** 명령어로 메뉴를 열 때, 채팅창이 닫힌 다음 틱에 열기 위한 플래그 */
    public static boolean openNextTick;

    private SelfSkin() {}

    /** 모드 초기화 (설정 읽기). 텍스처는 첫 틱에 불러온다 (그래픽 장치 준비 후). */
    public static void init() {
        config = SkinConfig.load(Minecraft.getInstance().gameDirectory.toPath());
    }

    public static KeyMapping getOpenKey() {
        if (openKey == null) {
            KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
            openKey = new KeyMapping("key.selfskin.open", InputConstants.KEY_K, category);
        }
        return openKey;
    }

    /** 매 클라이언트 틱 끝에 로더가 호출한다 */
    public static void onClientTick(Minecraft mc) {
        if (config == null) return;
        if (!restored) {
            restored = true;
            restoreActiveSkin();
        }
        while (openKey != null && openKey.consumeClick()) {
            if (mc.gui.screen() == null) openNextTick = true;
        }
        if (openNextTick) {
            openNextTick = false;
            mc.gui.setScreen(new SelfSkinScreen());
        }
    }

    /** 마지막으로 쓰던 스킨 복원 */
    private static void restoreActiveSkin() {
        SkinConfig.SkinEntry active = config.getActive();
        if (active == null) return;
        Component err = skins.load(SkinConfig.getSkinsDir().resolve(active.file), active.isSlim());
        if (err != null) {
            LOG.warn("Failed to load saved skin: {}", err.getString());
            config.enabled = false;
            config.save();
        }
    }

    // ---------- Mixin에서 쓰는 판단 ----------

    /** 스킨을 바꿔 보여 줄지 */
    public static boolean isActive() {
        return config != null && config.enabled && skins.isLoaded();
    }

    /** 이 UUID가 나(로그인한 계정)인지 */
    public static boolean isSelf(UUID id) {
        if (id == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (id.equals(mc.getUser().getProfileId())) return true;
        return mc.player != null && id.equals(mc.player.getUUID());
    }

    /** 내 캐릭터를 그리기 직전, 숨기기로 한 부분을 그릴 정보에서만 뺀다 (인벤토리는 그대로) */
    public static void hideParts(AvatarRenderState state) {
        SkinConfig cfg = config;
        if (cfg == null) return;
        if (cfg.hideHelmet) state.headEquipment = ItemStack.EMPTY;
        if (cfg.hideChestplate) state.chestEquipment = ItemStack.EMPTY;
        if (cfg.hideLeggings) state.legsEquipment = ItemStack.EMPTY;
        if (cfg.hideBoots) state.feetEquipment = ItemStack.EMPTY;
        if (cfg.hideHeadBlock) {
            state.headItem.clear();
            state.wornHeadType = null;
        }
        if (cfg.hideCape) state.showCape = false;
    }

    // ---------- 메뉴와 명령어에서 쓰는 동작 ----------

    /** 스킨을 골라 바로 적용. 실패하면 오류 메시지 */
    public static Component apply(SkinConfig.SkinEntry entry) {
        Component err = skins.load(SkinConfig.getSkinsDir().resolve(entry.file), entry.isSlim());
        if (err != null) return err;
        config.activeFile = entry.file;
        config.enabled = true;
        config.save();
        return null;
    }

    /** 켜기/끄기. 켜기에 실패하면 오류 메시지 */
    public static Component setEnabled(boolean on) {
        SkinConfig.SkinEntry active = config.getActive();
        if (on && active == null) return L.c("error.noSkin");
        if (on && !skins.isLoaded()) return apply(active);
        config.enabled = on;
        config.save();
        return null;
    }

    public static void setModel(SkinConfig.SkinEntry e, boolean slim) {
        e.model = slim ? SkinConfig.MODEL_SLIM : SkinConfig.MODEL_CLASSIC;
        config.save();
        if (e.file.equals(config.activeFile)) skins.setModel(slim);
    }

    public static Component rename(SkinConfig.SkinEntry e, String newName) {
        String err = config.rename(e, newName);
        return err == null ? null : L.error(err);
    }

    /** 스킨 삭제 (파일도 지움). 사용 중인 스킨이면 원래 스킨으로 돌아간다. */
    public static Component delete(SkinConfig.SkinEntry e) {
        boolean wasActive = e.file.equals(config.activeFile);
        String err = config.delete(e);
        if (err != null) return L.error(err);
        if (wasActive) skins.unload();
        return null;
    }

    /** 폴더와 목록을 맞추고, 사용 중인 png가 바뀌었을 수 있으니 다시 읽는다 */
    public static Component refresh() {
        config.syncWithFolder();
        SkinConfig.SkinEntry active = config.getActive();
        if (active == null) {
            skins.unload();
            return null;
        }
        return config.enabled ? apply(active) : null;
    }

    public static void setArmorHidden(int slot, boolean hide) {
        config.setArmorHidden(slot, hide);
        config.save();
    }

    public static void setAllArmorHidden(boolean hide) {
        for (int slot = 0; slot < 4; slot++) config.setArmorHidden(slot, hide);
        config.save();
    }

    public static void setHeadBlockHidden(boolean hide) {
        config.hideHeadBlock = hide;
        config.save();
    }

    public static void setCapeHidden(boolean hide) {
        config.hideCape = hide;
        config.save();
    }
}
