package dev.selfskin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 내 캐릭터의 갑옷(부위별)과 머리 블록을 화면에서 숨긴다.
 * 플레이어 렌더러의 갑옷 레이어 / 머리 블록 레이어를 감싸서,
 * 그리는 대상이 나일 때만 숨길 칸을 빼고 그린다.
 * 인벤토리나 서버에는 손대지 않으므로 다른 사람에게는 그대로 보인다.
 */
public final class ArmorHider {

    private ArmorHider() {}

    /** 레이어가 어떤 칸을 숨겨야 하는지 */
    private interface SlotFilter {
        boolean hide(SkinConfig cfg, int slot);
    }

    /** 갑옷 레이어: 부위별 설정을 따른다 */
    private static final SlotFilter ARMOR = new SlotFilter() {
        @Override
        public boolean hide(SkinConfig cfg, int slot) {
            return cfg.isArmorHidden(slot);
        }
    };

    /** 머리 블록 레이어(호박, 머리, 블록 등): 투구 칸에 들어 있는 아이템을 그린다 */
    private static final SlotFilter HEAD_BLOCK = new SlotFilter() {
        @Override
        public boolean hide(SkinConfig cfg, int slot) {
            return slot == SkinConfig.SLOT_HELMET && cfg.hideHeadBlock;
        }
    };

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void install() {
        Field fLayers;
        try {
            fLayers = SkinManager.findField(RendererLivingEntity.class, List.class, 0, "layerRenderers", "field_177097_h");
        } catch (IllegalStateException e) {
            SelfSkinMod.LOG.error("갑옷 숨기기 설치 실패", e);
            return;
        }
        int armor = 0, head = 0;
        for (RenderPlayer renderer : Minecraft.getMinecraft().getRenderManager().getSkinMap().values()) {
            try {
                List<LayerRenderer> layers = (List<LayerRenderer>) fLayers.get(renderer);
                for (int i = 0; i < layers.size(); i++) {
                    LayerRenderer layer = layers.get(i);
                    if (layer instanceof LayerBipedArmor) {
                        layers.set(i, new HiddenForSelf(layer, ARMOR));
                        armor++;
                    } else if (layer instanceof LayerCustomHead) {
                        layers.set(i, new HiddenForSelf(layer, HEAD_BLOCK));
                        head++;
                    }
                }
            } catch (Exception e) {
                SelfSkinMod.LOG.error("갑옷 숨기기 설치 실패", e);
            }
        }
        if (armor == 0) SelfSkinMod.LOG.warn("갑옷 레이어를 찾지 못했습니다. 갑옷 숨기기가 동작하지 않습니다.");
        if (head == 0) SelfSkinMod.LOG.warn("머리 블록 레이어를 찾지 못했습니다. 머리 블록 숨기기가 동작하지 않습니다.");
    }

    private static final class HiddenForSelf implements LayerRenderer<EntityLivingBase> {
        private final LayerRenderer<EntityLivingBase> delegate;
        private final SlotFilter filter;

        HiddenForSelf(LayerRenderer<EntityLivingBase> delegate, SlotFilter filter) {
            this.delegate = delegate;
            this.filter = filter;
        }

        @Override
        public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks,
                                  float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            SkinConfig cfg = SelfSkinMod.config;
            EntityPlayer self = Minecraft.getMinecraft().thePlayer;
            if (entity != self) {
                delegate.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
                return;
            }

            ItemStack[] armor = self.inventory.armorInventory;
            boolean any = false, all = true;
            for (int slot = 0; slot < armor.length; slot++) {
                boolean h = filter.hide(cfg, slot);
                any |= h;
                all &= h;
            }
            if (!any) {
                delegate.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
                return;
            }
            if (all) return;

            // 숨길 칸만 그리는 동안 잠깐 비워 두고, 그린 뒤 바로 되돌린다.
            // (렌더링과 게임 처리는 같은 스레드라 그 사이에 인벤토리를 쓰는 코드는 없다)
            ItemStack[] saved = new ItemStack[armor.length];
            boolean[] cleared = new boolean[armor.length];
            try {
                for (int slot = 0; slot < armor.length; slot++) {
                    if (filter.hide(cfg, slot)) {
                        saved[slot] = armor[slot];
                        armor[slot] = null;
                        cleared[slot] = true;
                    }
                }
                delegate.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
            } finally {
                for (int slot = 0; slot < armor.length; slot++) {
                    if (cleared[slot]) armor[slot] = saved[slot];
                }
            }
        }

        @Override
        public boolean shouldCombineTextures() {
            return delegate.shouldCombineTextures();
        }
    }
}
