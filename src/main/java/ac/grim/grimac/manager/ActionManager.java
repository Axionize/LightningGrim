package ac.grim.grimac.manager;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.interfaces.PacketCheckI;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import lombok.Getter;

@Getter
public class ActionManager extends Check implements PacketCheckI {
    private boolean attacking = false;
    private long lastAttack = 0;

    public ActionManager(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity action = new WrapperPlayClientInteractEntity(event);
            if (action.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                player.totalFlyingPacketsSent = 0;
                attacking = true;
                lastAttack = System.currentTimeMillis();
            }
        } else if (isTickPacketIncludingNonMovement(event.getPacketType())) {
            player.totalFlyingPacketsSent++;
            attacking = false;
        }
    }

    public boolean hasAttackedSince(long time) {
        return System.currentTimeMillis() - lastAttack < time;
    }
}
