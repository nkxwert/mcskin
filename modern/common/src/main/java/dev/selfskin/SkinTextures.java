package dev.selfskin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 내 스킨 텍스처. 항상 같은 위치(selfskin:skins/self)에 등록하고, 스킨을 바꿀 때는 그 위치의 텍스처만 교체한다.
 * 텍스처 교체 시 이전 텍스처는 TextureManager가 닫아 준다.
 */
public final class SkinTextures {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(SelfSkin.MOD_ID, "skins/self");
    private static final ClientAsset.Texture ASSET = new ClientAsset.ResourceTexture(TEXTURE, TEXTURE);

    private boolean loaded;
    private PlayerModelType model = PlayerModelType.WIDE;

    // getSkin()은 매 프레임 여러 번 불리므로 결과를 캐시한다
    private PlayerSkin cachedFor;
    private PlayerSkin cachedResult;

    /** png를 읽어 텍스처로 등록한다. 메인(렌더) 스레드에서 호출해야 한다. 실패하면 오류 메시지 */
    public Component load(Path png, boolean slim) {
        NativeImage img;
        try (InputStream in = Files.newInputStream(png)) {
            img = NativeImage.read(in);
        } catch (Exception e) {
            return L.c("error.unreadable", String.valueOf(e.getMessage()));
        }
        int w = img.getWidth(), h = img.getHeight();
        if (w != 64 || (h != 64 && h != 32)) {
            img.close();
            return L.c("error.badSize", w, h);
        }
        img = processLegacySkin(img);
        Minecraft.getInstance().getTextureManager().register(TEXTURE, new DynamicTexture(() -> "selfskin", img));
        loaded = true;
        setModel(slim);
        return null;
    }

    public void setModel(boolean slim) {
        model = slim ? PlayerModelType.SLIM : PlayerModelType.WIDE;
        cachedFor = null;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void unload() {
        if (loaded) Minecraft.getInstance().getTextureManager().release(TEXTURE);
        loaded = false;
        cachedFor = null;
    }

    /** 원래 스킨에서 몸 텍스처와 체형만 바꾼다. 망토와 겉날개는 원래 것을 그대로 쓴다. */
    public PlayerSkin apply(PlayerSkin original) {
        if (original != cachedFor) {
            cachedResult = new PlayerSkin(ASSET, original.cape(), original.elytra(), model, original.secure());
            cachedFor = original;
        }
        return cachedResult;
    }

    // ----- 바닐라와 같은 스킨 처리 (64x32 구형 스킨 변환 + 투명 영역 정리) -----

    private static NativeImage processLegacySkin(NativeImage image) {
        boolean isLegacy = image.getHeight() == 32;
        if (isLegacy) {
            NativeImage newImage = new NativeImage(64, 64, true);
            newImage.copyFrom(image);
            image.close();
            image = newImage;
            image.fillRect(0, 32, 64, 32, 0);
            image.copyRect(4, 16, 16, 32, 4, 4, true, false);
            image.copyRect(8, 16, 16, 32, 4, 4, true, false);
            image.copyRect(0, 20, 24, 32, 4, 12, true, false);
            image.copyRect(4, 20, 16, 32, 4, 12, true, false);
            image.copyRect(8, 20, 8, 32, 4, 12, true, false);
            image.copyRect(12, 20, 16, 32, 4, 12, true, false);
            image.copyRect(44, 16, -8, 32, 4, 4, true, false);
            image.copyRect(48, 16, -8, 32, 4, 4, true, false);
            image.copyRect(40, 20, 0, 32, 4, 12, true, false);
            image.copyRect(44, 20, -8, 32, 4, 12, true, false);
            image.copyRect(48, 20, -16, 32, 4, 12, true, false);
            image.copyRect(52, 20, -8, 32, 4, 12, true, false);
        }
        setNoAlpha(image, 0, 0, 32, 16);
        if (isLegacy) doNotchTransparencyHack(image, 32, 0, 64, 32);
        setNoAlpha(image, 0, 16, 64, 32);
        setNoAlpha(image, 16, 48, 48, 64);
        return image;
    }

    private static void doNotchTransparencyHack(NativeImage image, int x0, int y0, int x1, int y1) {
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                if (ARGB.alpha(image.getPixel(x, y)) < 128) return;
            }
        }
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                image.setPixel(x, y, image.getPixel(x, y) & 0xFFFFFF);
            }
        }
    }

    private static void setNoAlpha(NativeImage image, int x0, int y0, int x1, int y1) {
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                image.setPixel(x, y, ARGB.opaque(image.getPixel(x, y)));
            }
        }
    }
}
