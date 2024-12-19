package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.abstracts.AbstractPostPredictionCheck;
import ac.grim.grimac.checks.type.interfaces.PostPredictionCheckI;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "Entity control", configName = "EntityControl")
public class EntityControl extends AbstractPostPredictionCheck {
    public EntityControl(GrimPlayer player) {
        super(player);
    }

    public void rewardPlayer() {
        reward();
    }
}
