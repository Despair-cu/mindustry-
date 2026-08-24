package redteamai;

import arc.math.geom.Vec2;
import mindustry.ai.types.FlyingAI;
import mindustry.content.Blocks;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.world.blocks.defense.turrets.Turret;

public class EnhancedFlyingAI extends FlyingAI {

    private static final float DETECT_DIST = 180f;
    private static final float SAFE_DIST = 40f;

    @Override
    public void updateUnit() {
        if (unit == null || unit.dead()) return;

        if (target == null || isTargetDead(target)) target = findTarget();
        updateTargeting();
        if (target == null) { super.updateUnit(); return; }

        float tx = target.x(), ty = target.y();
        float dx = tx - unit.x, dy = ty - unit.y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) return;

        // 找前方路径上最近的对空炮塔
        Building danger = null;
        float dangerDist = Float.MAX_VALUE;

        for (Building b : Groups.build) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
            if (!(b.block instanceof Turret)) continue;
            Turret t = (Turret) b.block;
            if (!t.targetAir) continue;
            if (b.power != null && b.power.status < 0.1f) continue;

            float tdx = b.x - unit.x, tdy = b.y - unit.y;
            float proj = (tdx * dx + tdy * dy) / len;
            if (proj < 0 || proj > DETECT_DIST) continue;

            float perpX = tdx - (proj * dx / len);
            float perpY = tdy - (proj * dy / len);
            float perpDist = (float) Math.sqrt(perpX * perpX + perpY * perpY);

            float safeRange = t.range + SAFE_DIST;
            if (perpDist < safeRange && unit.dst(b) < safeRange) {
                if (unit.dst(b) < dangerDist) {
                    dangerDist = unit.dst(b);
                    danger = b;
                }
            }
        }

        if (danger != null) {
            // 有危险炮塔：向侧方绕开
            Turret t = (Turret) danger.block;
            float ev = t.range + SAFE_DIST + 50f;
            float nx = -dy / len, ny = dx / len;
            float sp = unit.speed() * 0.8f;
            unit.move(nx * sp, ny * sp);
        } else {
            // 无危险：直线飞向目标
            float sp = unit.speed();
            unit.move((dx / len) * sp, (dy / len) * sp);
        }

        unit.lookAt(tx, ty);
        updateWeapons();
        updateVisuals();
    }

    private boolean isTargetDead(Teamc t) {
        if (t == null) return true;
        if (t instanceof Unit) return ((Unit) t).dead();
        if (t instanceof Building) return ((Building) t).dead;
        return true;
    }

    private Building findTarget() {
        // 优先核心
        for (Building b : Groups.build) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
            if (b.block == Blocks.coreShard || b.block == Blocks.coreFoundation || b.block == Blocks.coreNucleus)
                return b;
        }
        // 其次随便找个建筑
        for (Building b : Groups.build) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
            return b;
        }
        return null;
    }
}
