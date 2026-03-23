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
 * Detects crystal macros by combining CPS + timing consistency (CV).
 *
 * A robot-like macro will have:
 *   - High CPS (above human threshold), AND
 *   - Very low CV (extremely regular, near-zero variance in intervals)
 *
 * Complements CrystalCPS which catches raw speed regardless of regularity.
 */
@CheckData(name = "CrystalAdvanced",
        description = "Detects crystal macros using CPS + timing consistency",
        experimental = true,
        setback = 10)
public class CrystalAdvanced extends Check implements PacketCheck {

    private static final int WINDOW = 12;

    // Ignore sub-10ms intervals as duplicate/burst packets (don't reset window)
    private static final long MIN_INTERVAL_NS = 10_000_000L;   // 10 ms
    // Reset window if the macro cadence breaks for more than 400ms
    private static final long MAX_INTERVAL_NS = 400_000_000L;  // 400 ms

    private final long[] times = new long[WINDOW];
    private int head  = 0;
    private int count = 0;
    private long lastPlacementNs = 0;

    // volatile: onReload() may run on a different thread than onPacketReceive()
    private volatile int maxCps = 8;
    private volatile double maxCv = 0.15; // smaller CV = more robot-like; 0.15 is safer than 0.25

    public CrystalAdvanced(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        maxCps = config.getIntElse(getConfigName() + ".max-cps", 8);
        maxCv  = config.getDoubleElse(getConfigName() + ".max-cv", 0.15);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return;
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) return;

        WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);

        ItemStack item = player.inventory.getItemInHand(wrapper.getHand());
        if (item == null || item.isEmpty()) return;
        if (item.getType() != ItemTypes.END_CRYSTAL) return;

        long now = System.nanoTime();

        if (lastPlacementNs != 0) {
            long interval = now - lastPlacementNs;

            // Sub-threshold burst: skip this packet but keep the window intact
            if (interval < MIN_INTERVAL_NS) return;

            // Cadence broken: reset and record current as the new starting point
            if (interval > MAX_INTERVAL_NS) {
                resetWindow();
                times[head] = now;
                head = (head + 1) % WINDOW;
                count = 1;
                lastPlacementNs = now;
                return;
            }

            // Valid interval – add to circular buffer
            times[head] = now;
            head = (head + 1) % WINDOW;
            if (count < WINDOW) count++;

            if (count == WINDOW) {
                // head now points to the oldest entry
                long oldest = times[head];
                double elapsed = (now - oldest) / 1_000_000_000.0;

                if (elapsed > 0) {
                    double cps  = (WINDOW - 1) / elapsed;
                    double mean = elapsed / (WINDOW - 1);

                    // Guard: should never be zero given elapsed > 0, but be safe
                    if (mean <= 0) {
                        lastPlacementNs = now;
                        return;
                    }

                    double variance = 0;
                    for (int i = 0; i < WINDOW - 1; i++) {
                        int idx1 = (head + i)     % WINDOW;
                        int idx2 = (head + i + 1) % WINDOW;
                        double diff  = (times[idx2] - times[idx1]) / 1_000_000_000.0;
                        double delta = diff - mean;
                        variance += delta * delta;
                    }
                    variance /= (WINDOW - 1);
                    double cv = Math.sqrt(variance) / mean;

                    if (cps > maxCps && cv < maxCv) {
                        flagAndAlertWithSetback(String.format("cps=%.1f cv=%.3f", cps, cv));
                    } else {
                        reward();
                    }
                }
            }
        } else {
            // First placement ever
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
