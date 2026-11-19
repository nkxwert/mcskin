package dev.selfskin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 내 캐릭터의 갑옷만 화면에서 숨긴다.
 * 플레이어 렌더러의 갑옷 레이어를 감싸서, 그리는 대상이 나일 때만 건너뛴다.
 * 인벤토리나 서버에는 손대지 않으므로 다른 사람에게는 갑옷이 그대로 보인다.
 */
public final class ArmorHider {

    private ArmorHider() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void install() {
        Field fLayers;
        try {
            fLayers = SkinManager.findField(RendererLivingEntity.class, List.class, 0, "layerRenderers", "field_177097_h");
        } catch (IllegalStateException e) {
            SelfSkinMod.LOG.error("갑옷 숨기기 설치 실패", e);
            return;
        }
        int count = 0;
        for (RenderPlayer renderer : Minecraft.getMinecraft().getRenderManager().getSkinMap().values()) {
            try {
                List<LayerRenderer> layers = (List<LayerRenderer>) fLayers.get(renderer);
                for (int i = 0; i < layers.size(); i++) {
                    LayerRenderer layer = layers.get(i);
                    if (layer instanceof LayerBipedArmor) {
                        layers.set(i, new HiddenForSelf(layer));
                        count++;
                    }
                }
            } catch (Exception e) {
                SelfSkinMod.LOG.error("갑옷 숨기기 설치 실패", e);
            }
        }
        if (count == 0) SelfSkinMod.LOG.warn("갑옷 레이어를 찾지 못했습니다. 갑옷 숨기기가 동작하지 않습니다.");
    }

    private static final class HiddenForSelf implements LayerRenderer<EntityLivingBase> {
        private final LayerRenderer<EntityLivingBase> delegate;

        HiddenForSelf(LayerRenderer<EntityLivingBase> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks,
                                  float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            if (SelfSkinMod.config.hideArmor && entity == Minecraft.getMinecraft().thePlayer) return;
            delegate.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
        }

        @Override
        public boolean shouldCombineTextures() {
            return delegate.shouldCombineTextures();
        }
    }
}
