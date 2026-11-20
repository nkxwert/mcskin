package dev.selfskin.forge;

import dev.selfskin.SelfSkin;
import dev.selfskin.SelfSkinCommands;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Forge 진입점: 공통 코드에 키, 틱, 명령어만 연결한다 (클라이언트 전용 모드) */
@Mod(SelfSkin.MOD_ID)
public final class SelfSkinForge {
    public SelfSkinForge(FMLJavaModLoadingContext context) {
        SelfSkin.init();
        RegisterKeyMappingsEvent.BUS.addListener(e -> e.register(SelfSkin.getOpenKey()));
        RegisterClientCommandsEvent.BUS.addListener(e -> SelfSkinCommands.register(e.getDispatcher()));
        TickEvent.ClientTickEvent.Post.BUS.addListener(e -> SelfSkin.onClientTick(Minecraft.getInstance()));
    }
}
