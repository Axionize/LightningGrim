package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.interfaces.PacketCheckI;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow.WindowClickType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;


@CheckData(name = "BadPacketsP", experimental = true)
public class BadPacketsP extends Check implements PacketCheckI {

    public BadPacketsP(GrimPlayer playerData) {
        super(playerData);
    }

    private int containerType = -1;
    private int containerId = -1;

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            WrapperPlayServerOpenWindow window = new WrapperPlayServerOpenWindow(event);
            this.containerType = window.getType();
            this.containerId = window.getContainerId();
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow wrapper = new WrapperPlayClientClickWindow(event);
            WindowClickType clickType = wrapper.getWindowClickType();
            int button = wrapper.getButton();

            // TODO: Adjust for containers
            boolean flag;
            switch (clickType) {
                case PICKUP:
                case QUICK_MOVE:
                case THROW:
                    flag = button != 0 && button != 1;
                    break;
                case SWAP:
                    flag = (button > 8 || button < 0) && button != 40;
                    break;
                case CLONE:
                    flag = button != 2;
                    break;
                case QUICK_CRAFT:
                    flag = button == 3 || button == 7 || button > 10 || button < 0;
                    break;
                case PICKUP_ALL:
                    flag = button != 0;
                    break;
                case UNKNOWN:
                    flag = false;
                    break;
                default:
                    throw new IllegalStateException("Impossible clickType; Compiler does not know this is unreachable!");
            }

            // Allowing this to false flag to debug and find issues faster
            if (flag) {
                if (flagAndAlert("clickType=" + clickType.toString().toLowerCase() + ", button=" + button + (wrapper.getWindowId() == containerId ? ", container=" + containerType : "")) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }
    }
}
