package dev.selfskin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.io.File;

@Mod(modid = "selfskin", useMetadata = true, clientSideOnly = true, acceptableRemoteVersions = "*")
public class SelfSkinMod {

    public static final Logger LOG = LogManager.getLogger("SelfSkin");

    public static SkinConfig config;
    public static SkinManager skins;
    public static KeyBinding openKey;

    /** 명령어로 GUI를 열 때 채팅창이 닫힌 다음 틱에 열기 위한 플래그 */
    public static boolean openGuiNextTick;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        config = SkinConfig.load(mc.mcDataDir);
        skins = new SkinManager();

        openKey = new KeyBinding("key.selfskin.open", Keyboard.KEY_K, "key.categories.selfskin");
        ClientRegistry.registerKeyBinding(openKey);
        ClientCommandHandler.instance.registerCommand(new CommandSelfSkin());
        MinecraftForge.EVENT_BUS.register(this);
        ArmorHider.install();

        // 마지막으로 쓰던 스킨 복원
        SkinConfig.SkinEntry active = config.getActive();
        if (active != null) {
            String err = skins.load(new File(SkinConfig.getSkinsDir(), active.file), active.model);
            if (err != null) {
                LOG.warn("Failed to load saved skin: " + err);
                config.enabled = false;
                config.save();
            }
        }
        skins.setActive(config.enabled && skins.hasTexture());
    }

    /** 스킨을 선택해 바로 적용. GUI와 명령어에서 공통 사용. 실패 시 오류 메시지 반환. */
    public static String apply(SkinConfig.SkinEntry entry) {
        String err = skins.load(new File(SkinConfig.getSkinsDir(), entry.file), entry.model);
        if (err != null) return err;
        config.activeFile = entry.file;
        config.enabled = true;
        config.save();
        skins.setActive(true);
        return null;
    }

    /** 켜기/끄기. 켜기에 실패하면 오류 메시지를 반환한다. */
    public static String setEnabled(boolean on) {
        SkinConfig.SkinEntry active = config.getActive();
        if (on && active == null) {
            return L.t("error.noSkin");
        }
        if (on && !skins.hasTexture()) {
            return apply(active);
        }
        config.enabled = on;
        config.save();
        skins.setActive(on);
        return null;
    }

    /** 한 부위의 갑옷 숨김 설정 (slot: SkinConfig.SLOT_*) */
    public static void setArmorHidden(int slot, boolean hide) {
        config.setArmorHidden(slot, hide);
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

    /** 스킨 이름 바꾸기. 실패 시 오류 메시지 반환. (텍스처는 그대로 쓰므로 다시 읽을 필요 없음) */
    public static String rename(SkinConfig.SkinEntry entry, String newName) {
        return config.rename(entry, newName);
    }

    /** 스킨 삭제 (파일도 지움). 사용 중인 스킨이면 원래 스킨으로 돌아간다. 실패 시 오류 메시지 반환. */
    public static String delete(SkinConfig.SkinEntry entry) {
        boolean wasActive = entry.file.equals(config.activeFile);
        String err = config.delete(entry);
        if (err == null && wasActive) {
            skins.setActive(false);
            skins.unload();
        }
        return err;
    }

    /** 모든 부위의 갑옷 숨김 설정 */
    public static void setAllArmorHidden(boolean hide) {
        for (int slot = 0; slot < 4; slot++) config.setArmorHidden(slot, hide);
        config.save();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (openGuiNextTick) {
            openGuiNextTick = false;
            Minecraft.getMinecraft().displayGuiScreen(new GuiSelfSkin());
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        // 그리기 직전에 한 번만 확인한다. 화면을 그리지 않을 때(창 최소화 등)는 적용할 필요도 없다.
        if (event.phase == TickEvent.Phase.START) skins.update();
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (openKey.isPressed() && Minecraft.getMinecraft().currentScreen == null) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiSelfSkin());
        }
    }
}
