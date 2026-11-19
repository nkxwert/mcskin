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

        openKey = new KeyBinding("Self Skin 메뉴 열기", Keyboard.KEY_K, "Self Skin");
        ClientRegistry.registerKeyBinding(openKey);
        ClientCommandHandler.instance.registerCommand(new CommandSelfSkin());
        MinecraftForge.EVENT_BUS.register(this);
        ArmorHider.install();

        // 마지막으로 쓰던 스킨 복원
        SkinConfig.SkinEntry active = config.getActive();
        if (active != null) {
            String err = skins.load(new File(SkinConfig.getSkinsDir(), active.file), active.model);
            if (err != null) {
                LOG.warn("저장된 스킨 로드 실패: " + err);
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
            return "먼저 메뉴에서 스킨을 선택하세요.";
        }
        if (on && !skins.hasTexture()) {
            return apply(active);
        }
        config.enabled = on;
        config.save();
        skins.setActive(on);
        return null;
    }

    public static void setHideArmor(boolean hide) {
        config.hideArmor = hide;
        config.save();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (openGuiNextTick) {
            openGuiNextTick = false;
            Minecraft.getMinecraft().displayGuiScreen(new GuiSelfSkin());
        }
        skins.update();
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        // 프레임마다도 확인해서 원래 스킨이 한 프레임이라도 보이는 걸 막는다
        if (event.phase == TickEvent.Phase.START) skins.update();
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (openKey.isPressed() && Minecraft.getMinecraft().currentScreen == null) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiSelfSkin());
        }
    }
}
