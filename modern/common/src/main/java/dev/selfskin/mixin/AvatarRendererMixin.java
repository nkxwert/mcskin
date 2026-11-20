package dev.selfskin.mixin;

import dev.selfskin.SelfSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 플레이어를 그리기 위한 정보(render state)를 만든 직후, 내 캐릭터일 때만
 * 숨기기로 한 갑옷 / 머리 블록 / 망토를 그 정보에서 뺀다. 실제 인벤토리는 건드리지 않는다.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL")
    )
    private void selfskin$hideParts(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        if (entity == Minecraft.getInstance().player) SelfSkin.hideParts(state);
    }
}
