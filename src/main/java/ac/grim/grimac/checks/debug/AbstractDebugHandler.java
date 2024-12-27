package ac.grim.grimac.checks.debug;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.entity.Player;

public abstract class AbstractDebugHandler extends Check {

    protected final GrimPlayer grimPlayer;

    public AbstractDebugHandler(GrimPlayer grimPlayer) {
        super(grimPlayer);
        this.grimPlayer = grimPlayer;
    }

    public GrimPlayer getPlayer() {
        return grimPlayer;
    }

    public abstract void toggleListener(Player player);

    public abstract boolean toggleConsoleOutput();
}
