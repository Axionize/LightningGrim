package ac.grim.grimac.manager.init.load;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.chat.ChatTypes;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.PEVersion;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;

import java.util.concurrent.Executors;

public class PacketEventsInit implements Initable {

    PEVersion NEWEST_UNSUPPORTED_PE_VERSION = new PEVersion(2, 7, 0);

    @Override
    public void start() {
        LogUtil.info("Loading PacketEvents...");

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(GrimAPI.INSTANCE.getPlugin()));

        if (!checkPacketEventsVersion()) {
            GrimAPI.INSTANCE.signalCriticalFailure();
            throw new RuntimeException("\n" +
                    "******************************************************\n" +
                    "GrimAC requires PacketEvents > 2.7.0\n" +
                    "Please update PacketEvents to a compatible version.\n" +
                    "This is a critical error. GrimAC will be disabled.\n" +
                    "*****************************************************");
        }

        PacketEvents.getAPI().getSettings()
                .fullStackTrace(true)
                .kickOnPacketException(true)
                .checkForUpdates(false)
                .reEncodeByDefault(false)
                .debug(false);
        PacketEvents.getAPI().load();

        // This may seem useless, but it causes java to start loading stuff async before we need it
        Executors.defaultThreadFactory().newThread(() -> {
            StateTypes.AIR.getName();
            ItemTypes.AIR.getName();
            EntityTypes.PLAYER.getParent();
            EntityDataTypes.BOOLEAN.getName();
            ChatTypes.CHAT.getName();
            EnchantmentTypes.ALL_DAMAGE_PROTECTION.getName();
            ParticleTypes.DUST.getName();
        }).start();
    }

    private boolean checkPacketEventsVersion() {
        PEVersion peVersion = PacketEvents.getAPI().getVersion();
        return peVersion.isNewerThan(NEWEST_UNSUPPORTED_PE_VERSION);
    }
}
