package redteamai;

import arc.math.geom.Vec2;
import mindustry.ai.types.GroundAI;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.blocks.units.UnitFactory;

public class EnhancedGroundAI extends GroundAI {

    private static final float TILE = 8f;
    private static final float RETREAT_DIST = 2f;
    private static final float TILE_SIZE = 8f;
    private static final float RALLY_RADIUS = 60f;
    private static final float SIDE_BIAS = 70f;

    private boolean attacking = false;

    @Override
    public void updateUnit() {
        try {
            if (unit == null || unit.dead()) return;

            if (target == null || isTargetDead(target)) target = findBestTarget();
            updateTargeting();
            if (target == null) { super.updateUnit(); return; }

            float tx = target.x(), ty = target.y();
            float dist = unit.dst(target);
            float range = unit.range();
            float retreatAt = range - RETREAT_DIST * TILE;

            // 敌进我退
            if (dist < retreatAt) {
                float dx = unit.x - tx, dy = unit.y - ty;
                float len = (float)Math.sqrt(dx*dx + dy*dy);
                if (len > 0.01f) { float sp = unit.speed(); unit.move((dx/len)*sp, (dy/len)*sp); }
                unit.lookAt(tx, ty); updateWeapons(); updateVisuals(); return;
            }

            // 聚兵判定
            int friends = RedTeamAIMod.cruxGroundCount();
            int threshold = rushThreshold();
            Vec2 rally = rallyPoint();

            if (!attacking) {
                if (rally != null && unit.dst(rally) > RALLY_RADIUS) {
                    moveStraight(rally.x, rally.y, 1f);
                    unit.lookAt(tx, ty); updateWeapons(); updateVisuals(); return;
                }
                if (friends >= threshold) {
                    attacking = true;
                } else {
                    unit.moveAt(Vec2.ZERO);
                    unit.lookAt(tx, ty); updateWeapons(); updateVisuals(); return;
                }
            }

            // 进攻阶段
            boolean isUnitTarget = (target instanceof Unit);

            if (isUnitTarget) {
                float desired = retreatAt * 0.85f;
                if (dist > desired + 10f) moveStraight(tx, ty, 1f);
                else if (dist < desired - 10f) moveStraight(tx, ty, -1f);
            } else {
                float turretThreat = threatAt(tx, ty);
                int hotspot = RedTeamAIMod.threatAtPoint(tx, ty);
                float totalThreat = turretThreat + hotspot * 2f;

                if (totalThreat >= 6f) {
                    // 绕侧
                    float dx = tx - unit.x, dy = ty - unit.y;
                    float len = (float)Math.sqrt(dx*dx + dy*dy);
                    if (len > 0.01f) {
                        float leftX = -dy/len, leftY = dx/len;
                        float rightX = dy/len, rightY = -dx/len;
                        int leftHot = RedTeamAIMod.threatAtPoint(unit.x + leftX*SIDE_BIAS, unit.y + leftY*SIDE_BIAS);
                        int rightHot = RedTeamAIMod.threatAtPoint(unit.x + rightX*SIDE_BIAS, unit.y + rightY*SIDE_BIAS);
                        float bx, by;
                        if (leftHot <= rightHot) { bx = leftX; by = leftY; }
                        else { bx = rightX; by = rightY; }
                        float sp = unit.speed();
                        unit.move(bx*sp, by*sp);
                    }
                } else {
                    float desired = range * 0.8f;
                    if (dist > desired + 10f) moveStraight(tx, ty, 1f);
                }
            }

            unit.lookAt(tx, ty);
            updateWeapons();
            updateVisuals();

        } catch (Exception ex) { /* silent */ }
    }

    private int rushThreshold() {
        int base = 4;
        int threat = RedTeamAIMod.totalThreat();
        int dynamic = 4 + threat;
        return Math.max(base, Math.min(dynamic, 12));
    }

    private Vec2 rallyPoint() {
        Building core = mindustry.Vars.state.teams.get(mindustry.game.Team.crux).core();
        if (core != null && !core.dead) return new Vec2(core.x, core.y);
        return null;
    }

    private void moveStraight(float tx, float ty, float dir) {
        float dx = tx - unit.x, dy = ty - unit.y;
        float len = (float)Math.sqrt(dx*dx + dy*dy);
        if (len > 0.01f) { float sp = unit.speed(); unit.move((dx/len)*sp*dir, (dy/len)*sp*dir); }
    }

    private boolean isTargetDead(Teamc t) {
        if (t == null) return true;
        if (t instanceof Unit) return ((Unit)t).dead();
        if (t instanceof Building) return ((Building)t).dead;
        return true;
    }

    private float threatAt(float tx, float ty) {
        float r2 = 160f*160f, sum = 0f;
        for (Building b : Groups.build) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
            if (!(b.block instanceof Turret)) continue;
            if (b.power != null && b.power.status < 0.1f) continue;
            float dx = b.x-tx, dy = b.y-ty;
            if (dx*dx+dy*dy > r2) continue;
            int size = b.block.size; sum += size*size;
        }
        return sum;
    }

    private Building findBestTarget() {
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
        if (b.block instanceof Turret) return 0;
        return 1;
    }
}
