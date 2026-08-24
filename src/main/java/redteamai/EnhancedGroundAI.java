package redteamai;

import arc.math.geom.Vec2;
import arc.util.Log;
import mindustry.ai.types.GroundAI;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.blocks.units.UnitFactory;

public class EnhancedGroundAI extends GroundAI {
    private static final float RETREAT_DIST = 2f; // 射程-2格后退
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
                if (len > 0.01f) moveTo(new Vec2(unit.x + (dx/len)*60f, unit.y + (dy/len)*60f), 0);
                unit.lookAt(tx, ty); updateWeapons(); updateVisuals(); updateMovement();
                return;
            }

            boolean isTurret = (target instanceof Building) && (((Building)target).block instanceof Turret);

            if (isTurret || target instanceof Unit) {
                // 炮塔/单位：保持拉扯距离（避免贴脸硬冲）
                float desired = retreatAt * 0.85f;
                if (dist > desired + 12f) moveTo(new Vec2(tx, ty), desired);
                else if (dist < desired - 12f) {
                    float dx = unit.x - tx, dy = unit.y - ty;
                    float len = (float)Math.sqrt(dx*dx + dy*dy);
                    if (len > 0.01f) moveTo(new Vec2(unit.x + (dx/len)*50f, unit.y + (dy/len)*50f), 0);
                }
                // 在距离范围内：站桩输出（moveTo radius 会自然停）
            } else {
                // 建筑（非炮塔）：直冲卡射程
                float desired = range * 0.8f;
                if (dist > desired + 12f) moveTo(new Vec2(tx, ty), desired);
                // 到射程内停
            }

            unit.lookAt(tx, ty);
            updateWeapons(); updateVisuals();
            updateMovement(); // 每帧驱动寻路（关键：让单位真正动起来）

        } catch (Exception ex) { Log.err("[RedTeamAI][地] 异常: " + ex.getMessage()); }
    }

    private boolean isTargetDead(Teamc t) {
        if (t == null) return true;
        if (t instanceof Unit) return ((Unit)t).dead();
        if (t instanceof Building) return ((Building)t).dead;
        return true;
    }

    /** 优先射程内炮塔；否则按优先级选建筑 */
    private Building findBestTarget() {
        float range = unit.range();
        Building bestTurret = null; float bestTD = Float.MAX_VALUE;
        Building best = null; float bestD = Float.MAX_VALUE; int bestP = 0;
        for (Building b : Groups.build) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
            float d = unit.dst(b);
            if (b.block instanceof Turret && d <= range) {
                if (b.power != null && b.power.status < 0.1f) continue;
                if (d < bestTD) { bestTD = d; bestTurret = b; }
                continue;
            }
            int p = priority(b); if (p == 0) continue;
            if (p > bestP || (p == bestP && d < bestD)) { bestP = p; bestD = d; best = b; }
        }
        return (bestTurret != null) ? bestTurret : best;
    }

    private int priority(Building b) {
        if (b.block instanceof PowerGenerator) return 3;
        if (b.block instanceof UnitFactory) return 3;
        if (b.block instanceof Turret) return 0;
        return 1;
    }
}
