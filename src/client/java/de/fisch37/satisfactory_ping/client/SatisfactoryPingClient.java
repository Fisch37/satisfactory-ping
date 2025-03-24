package de.fisch37.satisfactory_ping.client;

import de.fisch37.satisfactory_ping.packets.BlockPingPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

public class SatisfactoryPingClient implements ClientModInitializer {
    public static final KeyBinding PING_KEYBIND = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.satisfactory_ping.ping",
            GLFW.GLFW_KEY_LEFT_ALT,
            KeyBinding.MISC_CATEGORY
    ));
    public static final double MAX_CAST_DISTANCE = 512;
    private boolean triggerState = false;
    private SatisfactoryPingRenderingHook rendering;

    @Override
    public void onInitializeClient() {
        SatisfactoryPingSound.initialize();
        rendering = new SatisfactoryPingRenderingHook(this);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            final var useKey = Objects.requireNonNull(KeyBinding.byId("key.use"));
            boolean lastTriggerState = triggerState;
            triggerState = PING_KEYBIND.isPressed() && useKey.isPressed();
           if (!lastTriggerState && triggerState)
               sendPing(client);
        });
        ClientPlayNetworking.registerGlobalReceiver(BlockPingPayload.ID, (payload, context) -> {
            if (payload.dimension().equals(context.player().clientWorld.getRegistryKey())) {
                // Yk this looks really bad, but if you look into the decomp,
                // you'll find this is exactly how the wither spawn sound is handled (but they use 2 instead of 5)
                var soundOrigin = payload.pos().subtract(context.player().getPos()).normalize().multiply(5);
                context.player().clientWorld.playSound(
                        soundOrigin.x, soundOrigin.y, soundOrigin.z,
                        SatisfactoryPingSound.PING_SOUND_EVENT, SoundCategory.PLAYERS,
                        1.0F, 1.0F, false
                );
                rendering.add(payload);
            }
        });
    }

    public void sendPing(MinecraftClient client) {
        if (client.player == null) return;
        var result = client.player.raycast(MAX_CAST_DISTANCE, 1.0f, false);
        if (result.getType() == HitResult.Type.MISS) return;

        var pos = result.getPos();
        // client.player.sendMessage(Text.literal("Press! " + pos.x + " " + pos.y + " " + pos.z), false);
        ClientPlayNetworking.send(new BlockPingPayload(
                client.player.clientWorld.getRegistryKey(),
                pos, client.player.getUuid())
        );
    }
}
