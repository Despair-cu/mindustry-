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

            // 敌进我退
            if (dist < retreatAt) {
                float dx = unit.x - tx, dy = unit.y - ty;
                float len = (float)Math.sqrt(dx*dx + dy*dy);
                if (len > 0.01f) doMove(unit.x + (dx/len)*60f, unit.y + (dy/len)*60f, 0);
                unit.lookAt(tx, ty); updateWeapons(); updateVisuals(); updateMovement();
                return;
            }

            Vec2 rally = rallyPoint();

            // 集结
            if (!globalAttacking) {
                if (rally != null && unit.dst(rally) > RALLY_RADIUS) {
                    doMove(rally.x, rally.y, RALLY_RADIUS * 0.4f);
                    if (frameCount % 60 == 0) Log.info("[RedTeamAI][地] " + unit.type.name + " 集合 " + RedTeamAIMod.cruxGroundCount() + "/" + rushThreshold());
                    unit.lookAt(tx, ty); updateWeapons(); updateVisuals(); updateMovement();
                    return;
                }
                if (RedTeamAIMod.cruxGroundCount() >= rushThreshold()) {
                    globalAttacking = true;
                    Log.info("[RedTeamAI][地] >>> 全军进攻! 人数=" + RedTeamAIMod.cruxGroundCount());
                } else {
                    unit.moveAt(Vec2.ZERO);
                    if (frameCount % 60 == 0) Log.info("[RedTeamAI][地] " + unit.type.name + " 等待集结");
                    unit.lookAt(tx, ty); updateWeapons(); updateVisuals();
                    return;
                }
            }

            // 进攻
            boolean isTurret = (target instanceof Building) && (((Building)target).block instanceof Turret);

            if (isTurret || target instanceof Unit) {
                float desired = retreatAt * 0.85f;
                if (dist > desired + 12f) {
                    doMove(tx, ty, desired);
                    if (frameCount % 60 == 0) Log.info("[RedTeamAI][地] " + unit.type.name + " 接近目标 dist=" + (int)dist);
                } else if (dist < desired - 12f) {
                    float dx = unit.x - tx, dy = unit.y - ty;
                    float len = (float)Math.sqrt(dx*dx + dy*dy);
                    if (len > 0.01f) doMove(unit.x + (dx/len)*50f, unit.y + (dy/len)*50f, 0);
                } else {
                    arrived = true;
                    unit.moveAt(Vec2.ZERO);
                    if (frameCount % 60 == 0) Log.info("[RedTeamAI][地] " + unit.type.name + " 站桩输出");
                }
            } else {
                float desired = range * 0.8f;
                if (dist > desired + 12f) {
                    doMove(tx, ty, desired);
                    if (frameCount % 60 == 0) Log.info("[RedTeamAI][地] " + unit.type.name + " 直冲建筑 dist=" + (int)dist);
                } else {
                    arrived = true;
                    unit.moveAt(Vec2.ZERO);
                }
            }

            unit.lookAt(tx, ty);
            updateWeapons(); updateVisuals();
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
        Building core = mindustry.Vars.state.teams.get(mindustry.game.Team.crux