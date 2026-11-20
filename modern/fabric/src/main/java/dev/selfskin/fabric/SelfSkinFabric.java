package dev.selfskin.fabric;

import dev.selfskin.SelfSkin;
import dev.selfskin.SelfSkinCommands;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

/** Fabric 진입점: 공통 코드에 키, 틱, 명령어만 연결한다 */
public class SelfSkinFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SelfSkin.init();
        KeyMappingHelper.registerKeyMapping(SelfSkin.getOpenKey());
        ClientTickEvents.END_CLIENT_TICK.register(SelfSkin::onClientTick);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> SelfSkinCommands.register(dispatcher));
    }
}
