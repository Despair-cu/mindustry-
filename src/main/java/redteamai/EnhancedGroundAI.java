package redteamai;

import arc.math.geom.Vec2;
import mindustry.ai.types.GroundAI;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.world.blocks.defense.turrets.Turret;

public class EnhancedGroundAI extends GroundAI {

    private static final float THREAT_RADIUS = 160f;
    private static final float HIGH_THREAT = 6f;
    private static final float LOW_THREAT = 2f;
    private static final float TILE_SIZE = 8f;
    private static final float RETREAT_DIST = 2f;

    @Override
    public void updateUnit() {
        try {
            if (unit == null || unit.dead) return;

            if (target == null || target.dead()) {
                target = findBestTarget();
            }
            updateTargeting();
            if (target == null) { super.updateUnit(); return; }

            float tx = target.x();
            float ty = target.y();
            float dist = unit.dst(target);
            float range = unit.range();
            float retreatAt = range - (RETREAT_DIST * TILE_SIZE);

            if (dist < retreatAt) {
                float dx = unit.x - tx;
                float dy = unit.y - ty;
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len > 0.01f) {
                    float backX = unit.x + (dx / len) * 50f;
                    float backY = unit.y + (dy / len) * 50f;
                    moveTo(new Vec2(backX, backY), 0);
                }
                unit.lookAt(tx, ty);
                updateWeapons();
                updateVisuals();
                return;
            }

            boolean isUnitTarget = (target instanceof Unit);

            if (isUnitTarget) {
                float desired = retreatAt * 0.85f;
                moveTo(new Vec2(tx, ty), desired);
            } else {
                float threat = threatAt(tx, ty);

                if (threat >= HIGH_THREAT) {
                    float dx = tx - unit.x;
                    float dy = ty - unit.y;
                    float len = (float) Math.sqrt(dx * dx + dy * dy);
                    if (len > 0.01f) {
                        float perpX = -dy / len * 90f;
                        float perpY = dx / len * 90f;
                        moveTo(new Vec2(tx + perpX, ty + perpY), 0);
                    }
                } else {
                    float desired = range * 0.8f;
                    moveTo(new Vec2(tx, ty), desired);
                }
            }

            unit.lookAt(tx, ty);
            updateWeapons();
            updateVisuals();

        } catch (Exception ex) {
            // silent
        }
    }

    private float threatAt(float tx, float ty) {
        float r2 = THREAT_RADIUS * THREAT_RADIUS;
        float sum = 0f;
        for (Building b : mindustry.Vars.indexer.allBuildings()) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
            if (!(b.block instanceof Turret)) continue;
            if (b.ammo < 1) continue;
            if (b.power != null && b.power.status < 0.1f) continue;

            float dx = b.x - tx, dy = b.y - ty;
            if (dx * dx + dy * dy > r2) continue;
            int size = b.block.size;
            sum += size * size;
        }
        return sum;
    }

    private Building findBestTarget() {
        Building best = null;
        float bestDist = Float.MAX_VALUE;
        int bestPri = 0;
        for (Building b : mindustry.Vars.indexer.allBuildings()) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
            int p = priority(b);
            if (p == 0) continue;
            float d = unit.dst(b);
            if (p > bestPri || (p == bestPri && d < bestDist)) {
                bestPri = p; bestDist = d; best = b;
            }
        }
        return best;
    }

    private int priority(Building b) {
        if (b.block instanceof mindustry.world.blocks.power.PowerGenerator) {
            return (b.power != null && b.power.status > 0.1f) ? 3 : 0;
        }
        if (b.block instanceof mindustry.world.blocks.units.UnitFactory) {
            return (b.productionEfficiency > 0) ? 3 : 0;
        }
        if (b.block instanceof Turret) return 0;
        return 1;
    }
}
