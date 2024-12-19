package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.interfaces.BlockBreakCheckI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockBreak;
import ac.grim.grimac.utils.change.BlockModification;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CheckData(name = "BadPacketsX")
public class BadPacketsX extends Check implements BlockBreakCheckI {
    public BadPacketsX(GrimPlayer player) {
        super(player);
    }

    private int lastTick;
    private boolean didLastFlag;
    private Vector3i lastBreakLoc;
    private StateType lastBlockType;

    public final boolean noFireHitbox = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_15_2);

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action != DiggingAction.START_DIGGING && blockBreak.action != DiggingAction.FINISHED_DIGGING)
            return;

        final StateType block = blockBreak.block.getType();

        // Fixes false from breaking kelp underwater
        // The client sends two start digging packets to the server both in the same tick. BadPacketsX gets called twice, doesn't false the first time, but falses the second
        // One ends up breaking the kelp, the other ends up doing nothing besides falsing this check because we think they're trying to mine water
        // I am explicitly making this patch as narrow and specific as possible to potentially discover other blocks that exhibit similar behaviour
        int newTick = GrimAPI.INSTANCE.getTickManager().currentTick;
        if (lastTick == newTick
                && lastBreakLoc.equals(blockBreak.position)
                && !didLastFlag
                && lastBlockType.getHardness() == 0.0F
                && lastBlockType.getBlastResistance() == 0.0F
                && block == StateTypes.WATER
        ) return;

        // prevents rare false on rapidly breaking short grass
        List<StateType> previousBlockStates = player.blockHistory.modificationQueue.stream()
                .filter((blockModification) -> blockModification.getLocation().equals(blockBreak.position)
                        && newTick - blockModification.getTick() < 2
                        && (blockModification.getCause() == BlockModification.Cause.START_DIGGING || blockModification.getCause() == BlockModification.Cause.HANDLE_NETTY_SYNC_TRANSACTION))
                .flatMap(mod -> Stream.of(mod.getOldBlockContents().getType()))
                .collect(Collectors.toList());

        previousBlockStates.add(0, block);

        boolean invalid = false;
        for (StateType possibleBlockState : previousBlockStates) {
            // the block does not have a hitbox
            invalid = (possibleBlockState == StateTypes.LIGHT && !(player.getInventory().getHeldItem().is(ItemTypes.LIGHT) || player.getInventory().getOffHand().is(ItemTypes.LIGHT)))
                    || possibleBlockState.isAir()
                    || possibleBlockState == StateTypes.WATER
                    || possibleBlockState == StateTypes.LAVA
                    || possibleBlockState == StateTypes.BUBBLE_COLUMN
                    || possibleBlockState == StateTypes.MOVING_PISTON
                    || possibleBlockState == StateTypes.FIRE && noFireHitbox
                    // or the client claims to have broken an unbreakable block
                    || possibleBlockState.getHardness() == -1.0f && blockBreak.action == DiggingAction.FINISHED_DIGGING;
            if (!invalid) {
                break;
            }
        }

        if (invalid && flagAndAlert("block=" + block.getName() + ", type=" + blockBreak.action)) {
            didLastFlag = true;
            if (shouldModifyPackets()) {
                blockBreak.cancel();
            }
        } else {
            didLastFlag = false;
        }
        lastTick = newTick;
        lastBreakLoc = blockBreak.position;
        lastBlockType = block;
    }
}
