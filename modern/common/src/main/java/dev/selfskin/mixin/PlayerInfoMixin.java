package dev.selfskin.mixin;

import com.mojang.authlib.GameProfile;
import dev.selfskin.SelfSkin;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 내 플레이어 정보의 스킨만 바꿔치기한다.
 * 3인칭 모델, 1인칭 팔, 인벤토리 미리보기, 탭 리스트가 모두 PlayerInfo.getSkin()을 거친다.
 * 서버로는 아무것도 보내지 않으므로 다른 사람에게는 원래 스킨이 보인다.
 */
@Mixin(PlayerInfo.class)
public abstract class PlayerInfoMixin {

    @Shadow
    public abstract GameProfile getProfile();

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void selfskin$replaceSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        if (SelfSkin.isActive() && SelfSkin.isSelf(getProfile().id())) {
            cir.setReturnValue(SelfSkin.skins.apply(cir.getReturnValue()));
        }
    }
}
