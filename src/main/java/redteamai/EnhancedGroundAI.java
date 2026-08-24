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
    private static final float RETREAT_DIST = 2f;
    private static final float RALLY_RADIUS = 80f;
    private static final float RETARGET = 30f;
    private static boolean globalAttacking = false;
    private int frameCount = 0;
    private float moveX = Float.NaN, moveY = Float.NaN;
    private boolean arrived = false;

    @Override
    public void updateUnit() {
        try {
            if (unit == null || unit.dead()) return;
            frameCount++;
            if (target == null || isTargetDead(target)) {
                target = findBestTarget();
                moveX = Float.NaN; moveY = Float.NaN; arrived = false;
            }
            updateTargeting();
            if (target == null) { super.updateUnit(); return; }

            float tx = target.x(), ty = target.y();
            float dist = unit.dst(target);
            float range = unit.range();
            float retreatAt = range - RETREAT_DIST * 8f;

            if (dist < retreatAt) {
                float dx = unit.x - tx, dy = unit.y - ty;
                float len = (float)Math.sqrt(dx*dx + dy*dy);
                if (len > 0.01f) doMove(unit.x + (dx/len)*60f, unit.y + (dy/len)*60f, 0);
                unit.lookAt(tx, ty); updateWeapons(); updateVisuals(); updateMovement();
                return;
            }

            Vec2 rally = rallyPoint();
            if (!globalAttacking) {
                if (rally != null && unit.dst(rally) > RALLY_RADIUS) {
                    doMove(rally.x, rally.y, RALLY_RADIUS * 0.4f);
                    unit.lookAt(tx, ty); updateWeapons(); updateVisuals(); updateMovement();
                    return;
                }
                if (RedTeamAIMod.cruxGroundCount() >= rushThreshold()) {
                    globalAttacking = true;
                } else {
                    unit.moveAt(Vec2.ZERO);
                    unit.lookAt(tx, ty); updateWeapons(); updateVisuals();
                    return;
                }
            }

            boolean isTurret = (target instanceof Building) && (((Building)target).block instanceof Turret);
            if (isTurret || target instanceof Unit) {
                float desired = retreatAt * 0.85f;
                if (dist > desired + 12f) doMove(tx, ty, desired);
                else if (dist < desired - 12f) {
                    float dx = unit.x - tx, dy = unit.y - ty;
                    float len = (float)Math.sqrt(dx*dx + dy*dy);
                    if (len > 0.01f) doMove(unit.x + (dx/len)*50f, unit.y + (dy/len)*50f, 0);
                } else { arrived = true; unit.moveAt(Vec2.ZERO); }
            } else {
                float desired = range * 0.8f;
                if (dist > desired + 12f) doMove(tx, ty, desired);
                else { arrived = true; unit.moveAt(Vec2.ZERO); }
            }

            unit.lookAt(tx, ty); updateWeapons(); updateVisuals();
            if (!arrived) updateMovement();
        } catch (Exception ex) { Log.err("[RedTeamAI][地] 异常: " + ex.getMessage()); }
    }

    private void doMove(float x, float y, float radius) {
        if (Float.isNaN(moveX) || Float.isNaN(moveY)) {
            moveTo(new Vec2(x, y), radius); moveX = x; moveY = y; arrived = false; return;
        }
        float dx = x - moveX, dy = y - moveY;
        if (dx*dx + dy*dy > RETARGET*RETARGET) {
            moveTo(new Vec2(x, y), radius); moveX = x; moveY = y; arrived = false;
        }
    }

    private int rushThreshold() { return Math.max(4, Math.min(RedTeamAIMod.cruxGroundCount(), 12)); }

    private Vec2 rallyPoint() {
        Building core = mindustry.Vars.state.teams.get(mindustry.game.Team.crux).core();
        if (core != null && !core.dead) return new Vec2(core.x, core.y);
        return null;
    }

    private boolean isTargetDead(Teamc t) {
        if (t == null) return true;
        if (t instanceof Unit) return ((Unit)t).dead();
        if (t instanceof Building) return ((Building)t).dead;
        return true;
    }

    private Building findBestTarget() {
        float range = unit.range();
        Building bestTurret = null;
        float bestTDist = Float.MAX_VALUE;
        Building best = null;
        float bestDist = Float.MAX_VALUE;
        int bestPri = 0;
        for (Building b : Groups.build) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
            float d = unit.dst(b);
            if (b.block instanceof Turret && d <= range) {
                if (b.power != null && b.power.status < 0.1f) continue;
                if (d < bestTDist) { bestTDist = d; bestTurret = b; }
                continue;
            }
            int p = priority(b);
            if (p == 0) continue;
            if (p > bestPri || (p == bestPri && d < bestDist)) {
                bestPri = p; bestDist = d; best = b;
            }
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
