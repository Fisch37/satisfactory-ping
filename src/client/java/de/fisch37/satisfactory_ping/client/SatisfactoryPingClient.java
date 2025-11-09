package de.fisch37.satisfactory_ping.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import de.fisch37.satisfactory_ping.client.config.Config;
import de.fisch37.satisfactory_ping.packets.BlockPingPayload;
import de.maxhenkel.configbuilder.ConfigBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static de.fisch37.satisfactory_ping.SatisfactoryPing.MOD_ID;

public class SatisfactoryPingClient implements ClientModInitializer {
    public static final KeyBinding PING_KEYBIND = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.satisfactory_ping.ping",
            GLFW.GLFW_KEY_LEFT_ALT,
            KeyBinding.Category.MISC
    ));
    public static final Logger LOGGER = LoggerFactory.getLogger("SatisfactoryPing/Client");
    public static final double MAX_CAST_DISTANCE = 512;
    public static final Path CONFIG_PATH = Paths.get(".", "config", MOD_ID);
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor();

    private static SatisfactoryPingRenderingHook rendering;
    private static Config config;
    private static Set<UUID> disabledCauses;

    private boolean triggerState = false;

    @Override
    public void onInitializeClient() {
        config = loadConfig();
        disabledCauses = loadDisabledCauses();

        SatisfactoryPingSound.initialize();
        rendering = new SatisfactoryPingRenderingHook(this);
        rendering.setMinimumHeight(config.minimumIconHeight.get());
        rendering.setApparentTextSize(config.textHeight.get());

        ClientLifecycleEvents.CLIENT_STARTED.register(this::irisWorkaround);
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            final var useKey = Objects.requireNonNull(KeyBinding.byId("key.use"));
            boolean lastTriggerState = triggerState;
            triggerState = PING_KEYBIND.isPressed() && useKey.wasPressed();
            if (!lastTriggerState && triggerState)
                sendPing(client);
            // Prevent placing blocks when the key combo was triggered. This is a bit of a hack tbh
            //noinspection StatementWithEmptyBody
            while(triggerState && useKey.wasPressed()) { }
            if (triggerState) useKey.setPressed(false);
        });
        ClientPlayNetworking.registerGlobalReceiver(BlockPingPayload.ID, (payload, context) -> {
            if (payload.dimension().equals(context.player().getEntityWorld().getRegistryKey())) {
                if (disabledCauses.contains(payload.cause().orElse(null))) return;
                // Yk this looks really bad, but if you look into the decomp,
                // you'll find this is exactly how the wither spawn sound is handled (but they use 2 instead of 5)
                var soundOrigin = payload.pos().subtract(context.player().getEyePos()).normalize().multiply(5);
                context.player().getEntityWorld().playSoundClient(
                        soundOrigin.x, soundOrigin.y, soundOrigin.z,
                        SatisfactoryPingSound.PING_SOUND_EVENT, SoundCategory.PLAYERS,
                        0.25F, 1.0F, false
                );
                rendering.add(payload);
            }
        });
    }

    public static SatisfactoryPingRenderingHook getRenderer() {
        return rendering;
    }

    public static Config getConfig() {
        return config;
    }

    public static Set<UUID> getDisabledCauses() {
        return disabledCauses;
    }

    public static void scheduleDisabledCausesSave() {
        SAVE_EXECUTOR.execute(SatisfactoryPingClient::saveDisabledCauses);
    }

    private static void saveDisabledCauses() {
        try (var writer = new FileWriter(DISABLED_CAUSES_FILE)) {
            GSON.toJson(disabledCauses, writer);
        } catch (IOException|JsonIOException e) {
            LOGGER.error("Failed to write to disabled causes file: I/O error");
        }
    }

    public void sendPing(MinecraftClient client) {
        if (client.player == null) return;
        var result = client.player.raycast(MAX_CAST_DISTANCE, 1.0f, false);
        if (result.getType() == HitResult.Type.MISS) return;

        var pos = result.getPos();
        // client.player.sendMessage(Text.literal("Press! " + pos.x + " " + pos.y + " " + pos.z), false);
        ClientPlayNetworking.send(new BlockPingPayload(
                client.player.getEntityWorld().getRegistryKey(),
                pos, Optional.of(client.player.getUuid()))
        );
    }

    private Config loadConfig() {
        var path = CONFIG_PATH.resolve("config.properties").toAbsolutePath();
        var dir = path.getParent().toFile();
        boolean createdSuccessfully;
        try {
            createdSuccessfully = dir.exists() || path.getParent().toFile().mkdirs();
        } catch (SecurityException e) { createdSuccessfully = false; }
        if (!createdSuccessfully)
            LOGGER.error("Cannot create config folder at {}", path.getParent());
        return ConfigBuilder.builder(Config::new)
                .path(path)
                .saveSyncAfterBuild(createdSuccessfully)
                .build();
    }

    private static final File DISABLED_CAUSES_FILE = CONFIG_PATH.resolve("disabled_causes.json").toFile();
    private static final TypeToken<Set<UUID>> SET_OF_UUIDS_TOKEN = new TypeToken<>() { };
    private static Set<UUID> loadDisabledCauses() {
        try (var reader = new FileReader(DISABLED_CAUSES_FILE)) {
            return GSON.fromJson(reader, SET_OF_UUIDS_TOKEN);
        } catch (FileNotFoundException e) {
            return new HashSet<>();
        } catch (IOException|JsonIOException e) {
            LOGGER.error("I/O error while reading disabled causes file. Using default instead");
            return new HashSet<>();
        } catch (JsonSyntaxException e) {
            LOGGER.error("disabled_causes.json is not a valid json file. Using default");
            return new HashSet<>();
        }
    }

    private void irisWorkaround(MinecraftClient client) {
        if (FabricLoader.getInstance().isModLoaded("iris"))
            rendering.enableIrisWorkaround();
    }
}
