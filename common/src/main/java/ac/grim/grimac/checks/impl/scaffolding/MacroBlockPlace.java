package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.api.config.ConfigManager;
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
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;

import java.util.ArrayDeque;

/**
 * Detects macro-based block placement by measuring the statistical consistency
 * of inter-placement timing intervals.
 *
 * Human players have natural motor variance: even at high CPS their intervals
 * fluctuate by tens of milliseconds.  Macro clients (ClickCrystal, Meteor, etc.)
 * fire placements at a fixed server-tick rate with near-zero timing variation.
 *
 * Detection metric: Coefficient of Variation (CV = stddev / mean).
 * A low CV over a sufficient sample indicates robotic regularity.
 *
 * Targeted items: RESPAWN_ANCHOR (anchor macro), END_CRYSTAL (crystal macro).
 */
@CheckData(name = "MacroBlockPlace",
        description = "Placing blocks with inhuman timing consistency indicative of a macro",
        experimental = true,
        setback = 10)
public class MacroBlockPlace extends Check implements PacketCheck {

    // Rolling window size – require this many consecutive in-range intervals before flagging.
    private static final int DEFAULT_SAMPLE_SIZE = 12;

    // Only track intervals in this nanosecond range.
    //   Below MIN: sub-tick burst / cancelled packets (not a steady macro cadence).
    //   Above MAX: too slow to be a combat macro (anchor/crystal spam requires > 5 CPS).
    private static final long MIN_INTERVAL_NS = 45_000_000L;   //  45 ms
    private static final long MAX_INTERVAL_NS = 400_000_000L;  // 400 ms

    // Only flag when the mean interval is fast enough to be a combat macro concern (> 5 CPS).
    private static final long SUSPICIOUS_MEAN_NS = 200_000_000L; // 200 ms = 5 CPS

    // Rolling buffer of consecutive inter-placement durations (nanoseconds).
    private final ArrayDeque<Long> intervals = new ArrayDeque<>();

    private long lastPlacementNs = 0;
    private ItemType lastItemType = null;

    // Configurable CV threshold (lower = more strict, fewer flags).
    private double flagCv;
    private int sampleSize;

    public MacroBlockPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return;

        // 1.8 clients use a different packet format and don't have RESPAWN_ANCHOR / END_CRYSTAL.
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) return;

        WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
        InteractionHand hand = wrapper.getHand();

        ItemStack item = player.inventory.getItemInHand(hand);
        if (item == null || item.isEmpty()) return;

        ItemType type = item.getType();
        if (!isMacroItem(type)) return;

        long now = System.nanoTime();

        // Reset tracking state when the held item changes.
        if (!type.equals(lastItemType)) {
            lastItemType = type;
            intervals.clear();
            lastPlacementNs = 0;
        }

        if (lastPlacementNs != 0) {
            long interval = now - lastPlacementNs;

            if (interval >= MIN_INTERVAL_NS && interval <= MAX_INTERVAL_NS) {
                intervals.addLast(interval);
                if (intervals.size() > sampleSize) intervals.pollFirst();

                if (intervals.size() == sampleSize) {
                    double mean = computeMean();
                    double cv   = computeCV(mean);

                    if (cv < flagCv && mean < SUSPICIOUS_MEAN_NS) {
                        flagAndAlertWithSetback(String.format(
                                "cv=%.4f mean=%.1fms item=%s",
                                cv, mean / 1_000_000.0, type.getName().getKey()));
                    } else {
                        reward();
                    }
                }
            } else {
                // A gap outside the expected range breaks the macro cadence – reset.
                intervals.clear();
            }
        }

        lastPlacementNs = now;
    }

    // Items used by crystal-PvP macros that justify strict timing checks.
    private boolean isMacroItem(ItemType type) {
        return type == ItemTypes.RESPAWN_ANCHOR
                || type == ItemTypes.END_CRYSTAL;
    }

    private double computeMean() {
        double sum = 0;
        for (long v : intervals) sum += v;
        return sum / intervals.size();
    }

    /**
     * Coefficient of Variation: stddev / mean.
     * Approaching 0 → perfectly regular (macro); higher → human variance.
     */
    private double computeCV(double mean) {
        if (mean == 0) return 0;
        double variance = 0;
        for (long v : intervals) {
            double diff = v - mean;
            variance += diff * diff;
        }
        return Math.sqrt(variance / intervals.size()) / mean;
    }

    @Override
    public void onReload(ConfigManager config) {
        flagCv     = config.getDoubleElse(getConfigName() + ".cv-threshold", 0.10);
        sampleSize = config.getIntElse(getConfigName()    + ".sample-size",  DEFAULT_SAMPLE_SIZE);
        // Keep the intervals buffer consistent with the (possibly new) sample size.
        while (intervals.size() > sampleSize) intervals.pollFirst();
    }
}
