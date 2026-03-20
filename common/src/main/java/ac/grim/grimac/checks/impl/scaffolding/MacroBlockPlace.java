package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;

/**
 * Detects macro-based block placement (ClickCrystal, Meteor anchor/crystal macros)
 * by analysing the statistical consistency of inter-placement timing.
 *
 * Human players exhibit natural motor variance; even skilled players vary their
 * click intervals by tens of milliseconds. Macro clients fire placements at a
 * fixed rate with near-zero timing variation.
 *
 * Metric: Coefficient of Variation (CV = stddev / mean).
 *   - Macro: CV ≈ 0.01–0.05 (robot-like regularity)
 *   - Human: CV > 0.20 at any CPS
 *
 * Only tracks RESPAWN_ANCHOR and END_CRYSTAL – items used in crystal-PvP macros.
 */
@CheckData(name = "MacroBlockPlace",
        description = "Placing blocks with inhuman timing consistency indicative of a macro",
        experimental = true,
        setback = 10)
public class MacroBlockPlace extends Check implements PacketCheck {

    // Number of consecutive in-range intervals required before flagging.
    private static final int SAMPLE_SIZE = 12;

    // Coefficient of Variation threshold – flag when CV drops below this.
    // Macros are typically < 0.05; humans rarely drop below 0.20.
    private static final double FLAG_CV = 0.10;

    // Only count intervals within this nanosecond range:
    //   below MIN → sub-tick burst / cancelled / lag spike (not steady macro cadence)
    //   above MAX → less than 2.5 CPS, not a combat-macro pattern
    private static final long MIN_INTERVAL_NS = 45_000_000L;   // 45 ms
    private static final long MAX_INTERVAL_NS = 400_000_000L;  // 400 ms

    // Only flag when the mean is fast enough to be a combat concern (> 5 CPS).
    private static final long SUSPICIOUS_MEAN_NS = 200_000_000L; // 200 ms

    // Fixed-size circular buffer – avoids ArrayDeque boxing overhead.
    private final long[] intervals = new long[SAMPLE_SIZE];
    private int head  = 0; // next write position
    private int count = 0; // number of valid entries (saturates at SAMPLE_SIZE)

    private long     lastPlacementNs = 0;
    private ItemType lastItemType    = null;

    public MacroBlockPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return;
        // 1.8 clients use a different packet format and don't have RESPAWN_ANCHOR / END_CRYSTAL.
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) return;

        WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);

        ItemStack item = player.inventory.getItemInHand(wrapper.getHand());
        if (item == null || item.isEmpty()) return;

        ItemType type = item.getType();
        if (type != ItemTypes.RESPAWN_ANCHOR && type != ItemTypes.END_CRYSTAL
                && type != ItemTypes.OBSIDIAN) return;

        long now = System.nanoTime();

        // Reset tracking when the held item type changes.
        if (!type.equals(lastItemType)) {
            lastItemType    = type;
            head            = 0;
            count           = 0;
            lastPlacementNs = 0;
        }

        if (lastPlacementNs != 0) {
            long interval = now - lastPlacementNs;

            if (interval >= MIN_INTERVAL_NS && interval <= MAX_INTERVAL_NS) {
                intervals[head] = interval;
                head = (head + 1) % SAMPLE_SIZE;
                if (count < SAMPLE_SIZE) count++;

                if (count == SAMPLE_SIZE) {
                    double mean = mean();
                    double cv   = cv(mean);

                    if (mean < SUSPICIOUS_MEAN_NS && cv < FLAG_CV) {
                        flagAndAlertWithSetback(String.format("cv=%.4f mean=%.1fms item=%s",
                                cv, mean / 1_000_000.0, type.getName().getKey()));
                    } else {
                        reward();
                    }
                }
            } else {
                // Gap outside expected range breaks the macro cadence – reset.
                head  = 0;
                count = 0;
            }
        }

        lastPlacementNs = now;
    }

    /** Arithmetic mean of the full circular buffer (only called when count == SAMPLE_SIZE). */
    private double mean() {
        long sum = 0;
        for (long v : intervals) sum += v;
        return (double) sum / SAMPLE_SIZE;
    }

    /**
     * Population coefficient of variation.
     * Approaching 0 → perfectly regular (macro); higher → human variance.
     */
    private double cv(double mean) {
        if (mean == 0) return 0;
        double variance = 0;
        for (long v : intervals) {
            double d = v - mean;
            variance += d * d;
        }
        return Math.sqrt(variance / SAMPLE_SIZE) / mean;
    }
}
