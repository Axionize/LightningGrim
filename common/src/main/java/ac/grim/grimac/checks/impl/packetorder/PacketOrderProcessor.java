package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import lombok.Getter;
import org.jetbrains.annotations.Contract;

@Getter
public final class PacketOrderProcessor extends Check implements PacketCheck {
    public PacketOrderProcessor(final GrimPlayer player) {
        super(player);
    }

    private boolean openingInventory; // only pre 1.9 clients on pre 1.9 servers
    private boolean swapping;
    private boolean dropping;
    private boolean interacting;
    private boolean attacking;
    private boolean releasing;
    private boolean digging;
    private boolean sprinting;
    private boolean sneaking;
    private boolean placing;
    private boolean using;
    private boolean picking;
    private boolean clickingInInventory;
    private boolean closingInventory;
    private boolean quickMoveClicking;
    private boolean pickUpClicking;
    private boolean leavingBed;
    private boolean startingToGlide;
    private boolean jumpingWithMount;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Client.CLIENT_STATUS) {
            if (new WrapperPlayClientClientStatus(event).getAction() == WrapperPlayClientClientStatus.Action.OPEN_INVENTORY_ACHIEVEMENT) {
                openingInventory = true;
            }
        }

        if (packetType == PacketType.Play.Client.INTERACT_ENTITY) {
            if (new WrapperPlayClientInteractEntity(event).getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                attacking = true;
            } else {
                interacting = true;
            }
        }

        if (packetType == PacketType.Play.Client.PLAYER_DIGGING) {
            switch (new WrapperPlayClientPlayerDigging(event).getAction()) {
                case SWAP_ITEM_WITH_OFFHAND:
                    swapping = true;
                    break;
                case DROP_ITEM:
                case DROP_ITEM_STACK:
                    dropping = true;
                    break;
                case RELEASE_USE_ITEM:
                    releasing = true;
                    break;
                case FINISHED_DIGGING:
                case CANCELLED_DIGGING:
                case START_DIGGING:
                    digging = true;
                    break;
            }
        }

        if (packetType == PacketType.Play.Client.ENTITY_ACTION) {
            switch (new WrapperPlayClientEntityAction(event).getAction()) {
                case START_SPRINTING:
                case STOP_SPRINTING:
                    sprinting = true;
                    break;
                case STOP_SNEAKING:
                case START_SNEAKING:
                    sneaking = true;
                    break;
                case LEAVE_BED:
                    leavingBed = true;
                    break;
                case START_FLYING_WITH_ELYTRA:
                    startingToGlide = true;
                    break;
                case OPEN_HORSE_INVENTORY:
                    openingInventory = true;
                    break;
                case START_JUMPING_WITH_HORSE:
                case STOP_JUMPING_WITH_HORSE:
                    jumpingWithMount = true;
                    break;
            }
        }

        if (packetType == PacketType.Play.Client.USE_ITEM) {
            using = true;
        }

        if (packetType == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            if (new WrapperPlayClientPlayerBlockPlacement(event).getFace() == BlockFace.OTHER) {
                using = true;
            } else {
                placing = true;
            }
        }

        if (packetType == PacketType.Play.Client.PICK_ITEM) {
            picking = true;
        }

        if (packetType == PacketType.Play.Client.CLICK_WINDOW) {
            clickingInInventory = true;

            switch (new WrapperPlayClientClickWindow(event).getWindowClickType()) {
                case QUICK_MOVE:
                    quickMoveClicking = true;
                    break;
                case PICKUP:
                case PICKUP_ALL:
                    pickUpClicking = true;
                    break;
            }
        }

        if (packetType == PacketType.Play.Client.CLOSE_WINDOW) {
            closingInventory = true;
        }

        if (isTickPacket(packetType)) {
            openingInventory = false;
            swapping = false;
            dropping = false;
            attacking = false;
            interacting = false;
            releasing = false;
            digging = false;
            placing = false;
            using = false;
            picking = false;
            sprinting = false;
            sneaking = false;
            clickingInInventory = false;
            closingInventory = false;
            quickMoveClicking = false;
            pickUpClicking = false;
            leavingBed = false;
            startingToGlide = false;
            jumpingWithMount = false;
        }
    }

    @Contract(pure = true)
    public boolean isRightClicking() {
        return placing || using || interacting;
    }
}
