package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;

/**
 * Detects macro-based crystal placement by measuring Crystals Per Second (CPS).
 *
 * Crystal-PvP macros (ClickCrystal, Meteor, etc.) place END_CRYSTAL items at a
 * fixed, high rate that humans cannot sustain consistently. This check tracks
 * the last WINDOW placements and computes the instantaneous CPS from the elapsed
 * time between the oldest and newest timestamp in the window.
 *
 * Complements MacroBlockPlace (CV-based): that check catches robot-like timing
 * regularity; this check catches raw speed regardless of regularity.
 */
@CheckData(name = "CrystalCPS",
        description = "Placing end crystals faster than a human can",
        experimental = true,
        setback = 10)
public class CrystalCPS extends Check implements PacketCheck {

    // Sliding window size – need at least this many consecutive placements to measure CPS.
    private static final int WINDOW = 10;

    // Minimum interval between two placements to be counted as intentional (not a duplicate packet).
    private static final long MIN_INTERVAL_NS = 10_000_000L; // 10 ms

    // Maximum interval – beyond this the macro cadence is broken, reset the window.
    private static final long MAX_INTERVAL_NS = 600_000_000L; // 600 ms

    // Circular buffer of placement timestamps (nanoseconds).
    private final long[] times = new long[WINDOW];
    private int head  = 0;
    private int count = 0;

    // Configurable CPS cap (reloaded from config).
    private int maxCps = 6;

    private long lastPlacementNs = 0;

    public CrystalCPS(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        maxCps = config.getIntElse(getConfigName() + ".max-cps", 6);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return;
        // 1.8 clients don't have END_CRYSTAL as a placeable item in the same way.
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) return;

        WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);

        ItemStack item = player.inventory.getItemInHand(wrapper.getHand());
        if (item == null || item.isEmpty()) return;
        if (item.getType() != ItemTypes.END_CRYSTAL) return;

        long now = System.nanoTime();

        if (lastPlacementNs != 0) {
            long interval = now - lastPlacementNs;

            if (interval < MIN_INTERVAL_NS) {
                // Duplicate / sub-tick burst – ignore, don't reset.
                return;
            }

            if (interval > MAX_INTERVAL_NS) {
                // Gap too large – macro cadence broken, start fresh.
                resetWindow();
            } else {
                // Valid interval – record this placement.
                times[head] = now;
                head = (head + 1) % WINDOW;
                if (count < WINDOW) count++;

                if (count == WINDOW) {
                    // oldest entry is the one head now points at (just overwritten position).
                    long oldest = times[head % WINDOW];
                    double elapsedSec = (now - oldest) / 1_000_000_000.0;

                    if (elapsedSec > 0) {
                        // (WINDOW - 1) intervals span WINDOW timestamps.
                        double cps = (WINDOW - 1) / elapsedSec;

                        if (cps > maxCps) {
                            flagAndAlertWithSetback(String.format("cps=%.1f max=%d", cps, maxCps));
                        } else {
                            reward();
                        }
                    }
                }
            }
        } else {
            // First placement – initialise the window.
            times[head] = now;
            head = (head + 1) % WINDOW;
            count = 1;
        }

        lastPlacementNs = now;
    }

    private void resetWindow() {
        head  = 0;
        count = 0;
    }
}
