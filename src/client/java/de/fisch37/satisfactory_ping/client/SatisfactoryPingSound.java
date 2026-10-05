package de.fisch37.satisfactory_ping.client;

import static de.fisch37.satisfactory_ping.SatisfactoryPing.MOD_ID;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class SatisfactoryPingSound {
    public static final Identifier PING_SOUND_ID = Identifier.fromNamespaceAndPath(MOD_ID, "ping");
    public static SoundEvent PING_SOUND_EVENT = SoundEvent.createVariableRangeEvent(PING_SOUND_ID);

    public static void initialize() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, PING_SOUND_ID, PING_SOUND_EVENT);
    }
}
