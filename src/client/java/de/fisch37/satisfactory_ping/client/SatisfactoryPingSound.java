package de.fisch37.satisfactory_ping.client;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import static de.fisch37.satisfactory_ping.SatisfactoryPing.MOD_ID;

public class SatisfactoryPingSound {
    public static final Identifier PING_SOUND_ID = Identifier.of(MOD_ID, "ping");
    public static SoundEvent PING_SOUND_EVENT = SoundEvent.of(PING_SOUND_ID);

    public static void initialize() {
        Registry.register(Registries.SOUND_EVENT, PING_SOUND_ID, PING_SOUND_EVENT);
    }
}
