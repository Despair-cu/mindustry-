package redteamai;

import arc.math.geom.Vec2;
import arc.util.Log;
import mindustry.ai.types.FlyingAI;
import mindustry.content.Blocks;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.blocks.units.UnitFactory;

public class EnhancedFlyingAI extends FlyingAI {

    private static final float RETREAT_DIST = 2f;
    private static final float DETECT_RANGE = 130f;
    private static final float DETECT_WIDTH = 55f;
    private static final float EVADE_DIST = 85f;
    private int frameCount = 0;

    @Override
    public void updateUnit() {
        try {
            if (unit == null || unit.dead()) return;
            frameCount++;

            if (target == null || isTargetDead(target)) target = findBestTarget();
            updateTargeting();
            if (target == null) { super.updateUnit(); return; }

            float tx = target.x(), ty = target.y();
            float dist = unit.dst(target);
            float range = unit.range();
            float retreatAt = range - RETREAT_DIST * 8f;

            // 敌进我退
            if (dist < retreatAt) {
                float dx = unit.x - tx, dy = unit.y - ty;
                float len = (float)Math.sqrt(dx*dx + dy*dy);
                if (len > 0.01f) moveSmooth(unit.x + (dx/len)*60f, unit.y + (dy/len)*60f);
                unit.lookAt(tx, ty); updateWeapons(); updateVisuals();
                return;
            }

            // 只判断炮台类：前方路径上是否有炮塔
            float dx = tx - unit.x, dy = ty - unit.y;
            float len = (float)Math.sqrt(dx*dx + dy*dy);
            boolean hasTurretAhead = false;

            if (len > 0.01f) {
                for (Building b : Groups.build) {
                    if (b == null || b.dead) continue;
                    if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
                    // 只判断炮台类
                    if (!(b.block instanceof Turret)) continue;
                    if (b.power != null && b.power.status < 0.1f) continue;

                    float tdx = b.x - unit.x, tdy = b.y - unit.y;
                    float proj = (tdx * dx + tdy * dy) / len;
                    if (proj > 0 && proj < DETECT_RANGE) {
                        float perpX = tdx - (proj * dx / len);
                        float perpY = tdy - (proj * dy / len);
                        float perpDist = (float)Math.sqrt(perpX*perpX + perpY*perpY);
                        if (perpDist < DETECT_WIDTH) { hasTurretAhead = true; break; }
                    }
                }
            }

            if (hasTurretAhead) {
                // 绕道：选离目标更近的一侧
                float lx = -dy/len, ly = dx/len;
                float rx = dy/len, ry = -dx/len;
                float elX = unit.x + lx*EVADE_DIST, elY = unit.y + ly*EVADE_DIST;
                float erX = unit.x + rx*EVADE_DIST, erY = unit.y + ry*EVADE_DIST;
                float dl = (float)Math.sqrt((elX-tx)*(elX-tx) + (elY-ty)*(elY-ty));
                float dr = (float)Math.sqrt((erX-tx)*(erX-tx) + (erY-ty)*(erY-ty));
                if (dl <= dr) moveSmooth(elX, elY); else moveSmooth(erX, erY);
                if (frameCount % 60 == 0) Log.info("[RedTeamAI][空] " + unit.type.name + " 绕炮台飞行");
            } else {
                if (len > 0.01f) {
                    float sp = unit.speed();
                    unit.move((dx/len) * sp, (dy/len) * sp);
                }
                if (frameCount % 60 == 0) Log.info("[RedTeamAI][空] " + unit.type.name + " 直飞目标 dist=" + (int)dist);
            }

            unit.lookAt(tx, ty);
            updateWeapons(); updateVisuals();

        } catch (Exception ex) { Log.err("[RedTeamAI][空] 异常: " + ex.getMessage()); }
    }

    private void moveSmooth(float x, float y) {
        float dx = x - unit.x, dy = y - unit.y;
        float sp = unit.speed() * 0.8f;
        float len = (float)Math.sqrt(dx*dx + dy*dy);
        if (len > 0.01f) unit.move((dx/len) * sp, (dy/len) * sp);
    }

    private boolean isTargetDead(Teamc t) {
        if (t == null) return true;
        if (t instanceof Unit) return ((Unit)t).dead();
        if (t instanceof Building) return ((Building)t).dead;
        return true;
    }

    /** 优先敌方核心，其次发电机/工厂 */
    private Building findBestTarget() {
        for (Building b : Groups.build) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
            if (b.block == Blocks.coreShard || b.block == Blocks.coreFoundation || b.block == Blocks.coreNucleus) return b;
        }
        Building best = null; float bestD = Float.MAX_VALUE; int bestP = 0;
        for (Building b : Groups.build) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
            int p = priority(b); if (p == 0) continue;
            float d = unit.dst(b);
            if (p > bestP || (p == bestP && d < bestD)) { bestP = p; bestD = d; best = b; }
        }
        return best;
    }

    private int priority(Building b) {
        if (b.block instanceof PowerGenerator) return 3;
        if (b.block instanceof UnitFactory) return 3;
        return 1;
    }
}
