package redteamai;

import arc.math.geom.Vec2;
import mindustry.ai.types.GroundAI;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.gen.Building;
import mindustry.world.blocks.defense.turrets.Turret;

public class EnhancedGroundAI extends GroundAI {

    @Override
    public void updateMovement() {
        // 目标无效 → 走默认逻辑（父类会自己选目标）
        if (target == null) {
            super.updateMovement();
            return;
        }

        // 目标有效性
        boolean targetAlive = false;
        if (target instanceof Unit) {
            targetAlive = !((Unit) target).dead();
        } else if (target instanceof Building) {
            targetAlive = !((Building) target).dead();
        }
        if (!targetAlive) {
            super.updateMovement();
            return;
        }

        float tx = target.x();
        float ty = target.y();
        float range = unit.range();
        float desired = range * 0.9f;  // 稍微走近一点，确保在射程内
        float dist = unit.dst(target);

        // 统计目标附近炮塔密度
        int turretCount = 0;
        for (Unit u : Groups.unit) {
            if (u == null || u.dead || u.team == unit.team) continue;
            Building b = u.buildOn();
            if (b != null && b.block instanceof Turret) {
                float dx = u.x - tx;
                float dy = u.y - ty;
                if (dx * dx + dy * dy <= 180f * 180f) {
                    turretCount++;
                }
            }
        }

        if (turretCount >= 3) {
            // 绕道：垂直偏移
            float dx = tx - unit.x;
            float dy = ty - unit.y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 0.01f) {
                float perpX = -dy / len * 60f;
                float perpY = dx / len * 60f;
                moveTo(new Vec2(tx + perpX, ty + perpY), 0);
            }
        } else {
            // 卡射程：距离大于期望就前进，否则站定
            // 注意：站定时不要强行设置 unit.rotation！让 faceTarget() 自己处理
            if (dist > desired + 10f) {
                moveTo(new Vec2(tx, ty), 0);
            } else {
                // 到站了，停住不动，让父类 faceTarget() 自己把单位转向目标
                unit.moveAt(Vec2.ZERO);
            }
        }
    }

    // 关键：不重写 shouldShoot()！
    // 父类的 shouldShoot() 会调用武器的 shootCone 判定，
    // 武器炮管转到射界内才会开火，自动保证精度。
}
