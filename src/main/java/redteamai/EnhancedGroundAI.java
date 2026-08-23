package redteamai;

import arc.math.geom.Vec2;
import mindustry.ai.types.GroundAI;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.gen.Building;
import mindustry.world.blocks.defense.turrets.Turret;
import arc.util.Log;

public class EnhancedGroundAI extends GroundAI {

    private int tickCount = 0;
    private boolean initialized = false;

    @Override
    public void init() {
        super.init();
        initialized = true;
        Log.info("[RedTeamAI] [AI] EnhancedGroundAI.init() 被调用！单位: "
            + (unit != null ? unit.type.name : "null")
            + " | unit 有效: " + (unit != null && !unit.dead));
    }

    @Override
    public void updateUnit() {
        super.updateUnit();
    }

    @Override
    public void updateMovement() {
        tickCount++;

        if (unit == null || unit.dead) {
            if (tickCount % 60 == 1) {
                Log.info("[RedTeamAI] [AI] unit 为 null 或已死亡，跳过");
            }
            return;
        }

        // 每 120 帧（约2秒）打印一次状态摘要
        if (tickCount % 120 == 1) {
            Log.info("[RedTeamAI] [AI] updateMovement 运行中 | 单位: " + unit.type.name
                + " | 位置: (" + (int)unit.x + "," + (int)unit.y + ")"
                + " | target: " + (target == null ? "null" : target.getClass().getSimpleName())
                + " | 存活: " + !unit.dead);
        }

        // 目标有效性检查
        if (target == null) {
            if (tickCount % 120 == 1) {
                Log.info("[RedTeamAI] [AI] target 为 null，走默认逻辑");
            }
            super.updateMovement();
            return;
        }

        boolean targetAlive = false;
        if (target instanceof Unit) {
            targetAlive = !((Unit) target).dead();
        } else if (target instanceof Building) {
            targetAlive = !((Building) target).dead();
        }

        if (!targetAlive) {
            if (tickCount % 120 == 1) {
                Log.info("[RedTeamAI] [AI] target 已死亡，走默认逻辑");
            }
            super.updateMovement();
            return;
        }

        float tx = target.x();
        float ty = target.y();
        float range = unit.range();
        float desired = range * 0.95f;
        float dist = unit.dst(target);

        // 统计目标附近 180 范围内的敌方炮塔数量
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

        if (tickCount % 120 == 1) {
            Log.info("[RedTeamAI] [AI] 炮塔数=" + turretCount
                + " | 距离=" + (int)dist + " | 射程=" + (int)range
                + " | 期望距离=" + (int)desired
                + " | 目标位置: (" + (int)tx + "," + (int)ty + ")");
        }

        if (turretCount >= 3) {
            // 绕道
            float dx = tx - unit.x;
            float dy = ty - unit.y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 0.01f) {
                float perpX = -dy / len * 60f;
                float perpY = dx / len * 60f;
                if (tickCount % 120 == 1) {
                    Log.info("[RedTeamAI] [AI] >>> 执行绕道! 偏移后目标: ("
                        + (int)(tx + perpX) + "," + (int)(ty + perpY) + ")");
                }
                moveTo(new Vec2(tx + perpX, ty + perpY), 0);
            }
        } else {
            // 卡射程
            if (dist > desired + 10f) {
                if (tickCount % 120 == 1) {
                    Log.info("[RedTeamAI] [AI] >>> 前进至目标! dist=" + (int)dist + " > desired+10=" + (int)(desired+10));
                }
                moveTo(new Vec2(tx, ty), 0);
            } else {
                if (tickCount % 120 == 1) {
                    Log.info("[RedTeamAI] [AI] >>> 站定! 已在射程内");
                }
            }
        }
    }
}
