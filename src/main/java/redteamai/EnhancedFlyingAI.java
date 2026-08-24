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
    private static final float DETECT_RANGE = 130f;   // 前方探测距离
    private static final float DETECT_WIDTH = 55f;    // 探测半宽
    private static final float EVADE_DIST = 85f;      // 绕道偏移
    private static final float RETARGET = 30f;

    private float moveX = Float.NaN, moveY = Float.NaN;
    private int frameCount = 0;

    @Override
    public void updateUnit() {
        try {
            if (unit == null || unit.dead()) return;
            frameCount++;

            if (target == null || isTargetDead(target)) {
                target = findBestTarget();
                moveX = Float.NaN; moveY = Float.NaN;
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
                if (len > 0.01f) setMove(unit.x + (dx/len)*60f, unit.y + (dy/len)*60f);
                unit.lookAt(tx, ty); updateWeapons(); updateVisuals();
                doMove(); return;
            }

            // 探测前方路径上的炮塔（只判断炮台类 Turret）
            float dx = tx - unit.x, dy = ty - unit.y;
            float len = (float)Math.sqrt(dx*dx + dy*dy);
            boolean hasTurretAhead = false;

            if (len > 0.01f) {
                for (Building b : Groups.build) {
                    if (b == null || b.dead) continue;
                    if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
                    if (!(b.block instanceof Turret)) continue;       // 只判断炮台类
                    if (b.power != null && b.power.status < 0.1f) continue; // 没电的跳过

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
                // 绕道：向侧方偏移（选左右两侧中离目标更近的一侧）
                float lx = -dy/len, ly = dx/len;
                float rx = dy/len, ry = -dx/len;
                float elX = unit.x + lx*EVADE_DIST, elY = unit.y + ly*EVADE_DIST;
                float erX = unit.x + rx*EVADE_DIST, erY = unit.y + ry*EVADE_DIST;
                float dl = (elX-tx)*(elX-tx) + (elY-ty)*(elY-ty);
                float dr = (erX-tx)*(erX-tx) + (erY-ty)*(erY-ty);
                if (dl <= dr) setMove(elX, elY); else setMove(erX, erY);
                if (frameCount % 60 == 0)
                    Log.info("[RedTeamAI][空] " + unit.type.name + " 绕炮台飞行");
            } else {
                // 直飞目标
                setMove(tx, ty);
                if (frameCount % 60 == 0)
                    Log.info("[RedTeamAI][空] " + unit.type.name + " 直飞目标 dist=" + (int)dist);
            }

            unit.lookAt(tx, ty);
            updateWeapons(); updateVisuals();
            doMove();

        } catch (Exception ex) { Log.err("[RedTeamAI][空] 异常: " + ex.getMessage()); }
    }

    private void setMove(float x, float y) {
        if (Float.isNaN(moveX) || Float.isNaN(moveY) ||
            (moveX-x)*(moveX-x) + (moveY-y)*(moveY-y) > RETARGET*RETARGET) {
            moveX = x; moveY = y;
        }
    }

    private void doMove() {
        if (Float.isNaN(moveX) || Float.isNaN(moveY)) return;
        float dx = moveX - unit.x, dy = moveY - unit.y;
        float len = (float)Math.sqrt(dx*dx + dy*dy);
        if (len > 0.01f) {
            float sp = unit.speed();
            unit.move((dx/len) * sp, (dy/len) * sp);
        }
    }

    private boolean isTargetDead(Teamc t) {
        if (t == null) return true;
        if (t instanceof Unit) return ((Unit)t).dead();
        if (t instanceof Building) return ((Building)t).dead;
        return true;
    }

    /** 优先敌方核心，其次发电机/工厂 */
    private Building findBestTarget() {
        // 敌方核心（三种核心方块）
        for (Building b : Groups.build) {
            if (b == null || b.dead) conti