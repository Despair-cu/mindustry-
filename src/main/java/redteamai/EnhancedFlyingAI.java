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

    // 绕行点缓存
    private float evadeX = Float.NaN, evadeY = Float.NaN;
    private Building evadeTarget = null; // 正在绕的炮塔

    @Override
    public void updateUnit() {
        if (unit == null || unit.dead()) return;

        if (target == null || isTargetDead(target)) {
            target = findTarget();
            evadeX = Float.NaN; evadeY = Float.NaN; evadeTarget = null;
        }
        updateTargeting();
        if (target == null) { super.updateUnit(); return; }

        float tx = target.x(), ty = target.y();
        float dx = tx - unit.x, dy = ty - unit.y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) return;

        // 检查是否还在绕行状态
        boolean evading = false;
        if (!Float.isNaN(evadeX) && !Float.isNaN(evadeY)) {
            float ed = (float) Math.sqrt((evadeX - unit.x) * (evadeX - unit.x) + (evadeY - unit.y) * (evadeY - unit.y));
            if (ed > 20f) {
                // 还没到绕行点，继续飞
                evading = true;
            } else {
                // 到了绕行点，清除缓存
                evadeX = Float.NaN; evadeY = Float.NaN; evadeTarget = null;
            }
        }

        if (evading) {
            // 持续飞向绕行点
            float edx = evadeX - unit.x, edy = evadeY - unit.y;
            float elen = (float) Math.sqrt(edx * edx + edy * edy);
            if (elen > 0.01f) {
                float sp = unit.speed() * 0.8f;
                unit.move((edx / elen) * sp, (edy / elen) * sp);
            }
        } else {
            // 正常判断前方威胁
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
                // 计算绕行点：在炮塔射程外、朝向目标方向的一侧
                Turret t = (Turret) danger.block;
                float safeRange = t.range + SAFE_DIST + 30f;

                // 炮塔到目标的向量
                float t2tx = tx - danger.x, t2ty = ty - danger.y;
                float t2tlen = (float) Math.sqrt(t2tx * t2tx + t2ty * t2ty);
                if (t2tlen > 0.01f) {
                    // 法线方向（垂直于炮塔到目标的连线）
                    float nx = -t2ty / t2tlen, ny = t2tx / t2tlen;
                    // 选远离我方当前位置的那一侧
                    float side = (unit.x - danger.x) * nx + (unit.y - danger.y) * ny;
                    if (side < 0) { nx = -nx; ny = -ny; }
                    evadeX = danger.x + nx * safeRange;
                    evadeY = danger.y + ny * safeRange;
                    evadeTarget = danger;
                }
            } else {
                // 无威胁：直线飞向目标
                float sp = unit.speed();
                unit.move((dx / len) * sp, (dy / len) * sp);
            }
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
        for (Building b : Groups.build) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
            if (b.block == Blocks.coreShard || b.block == Blocks.coreFoundation || b.block == Blocks.coreNucleus)
                return b;
        }
        for (Building b : Groups.build) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
            return b;
        }
        return null;
    }
}
