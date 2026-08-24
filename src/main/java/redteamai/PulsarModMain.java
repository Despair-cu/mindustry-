package redteamai;

import arc.Core;
import arc.Color;
import arc.math.Mathf;
import arc.util.Log;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.entities.unit.UnitType;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Unit;

// 调试开关
private static final boolean DEBUG = true;

// ===== 黄矮星 (100万吸入伤害) =====
class YellowDwarfUnitType extends UnitType {
    public YellowDwarfUnitType(String name) {
        super(name);
        hitSize = 8f;
        flying = true;
        rotateSpeed = 0f;
        region = Core.atlas.find("clear");
        drawBody = false;
        drawCell = false;
        drawControl = false;
        drawShadow = false;
    }

    @Override
    public void update(Unit unit) {
        super.update(unit);
        float suckRange = 40f;
        float suckDamage = 1000000f;
        for (Unit u : Groups.unit) {
            if (u == null || u.dead || u.team == unit.team) continue;
            if (Mathf.dst(unit.x, unit.y, u.x, u.y) <= suckRange + u.hitSize) {
                u.damage(suckDamage * Time.delta);
                if (DEBUG) Log.info("[PulsarMod] 黄矮星吞噬 " + u.type);
            }
        }
    }

    @Override
    public void draw(Unit unit) {
        Draw.z(100f);
        Draw.color(Color.valueOf("fff200"));
        Fill.circle(unit.x, unit.y, 5f);
        Draw.color(Color.white);
        Fill.circle(unit.x, unit.y, 1.5f);
    }
}

// ===== 中子星 (左右射线，无吸入伤害) =====
class BluePulsarUnitType extends UnitType {
    public BluePulsarUnitType(String name) {
        super(name);
        hitSize = 6f; // 缩小体积
        flying = true;
        rotateSpeed = 0f;
        region = Core.atlas.find("clear"); // 彻底屏蔽原版贴图
        drawBody = false;
        drawCell = false;
        drawControl = false;
        drawShadow = false;
    }

    @Override
    public void update(Unit unit) {
        super.update(unit);
        float length = 60f;
        float width = 3f;
        float damage = 80f;
        for (int sign : new int[]{1, -1}) {
            float angle = 0f + sign * 180f; // 锁死左右
            float ex = unit.x + Angles.trnsx(angle, length);
            float ey = unit.y + Angles.trnsy(angle, length);
            applyDamageAlongLine(unit, unit.x, unit.y, ex, ey, width, damage);
        }
    }

    @Override
    public void draw(Unit unit) {
        // 画实心核心（彻底消除中心的洞）
        Draw.z(100f);
        Draw.color(Color.valueOf("00ffff"));
        Fill.circle(unit.x, unit.y, 4f); // 缩小核心半径
        Draw.color(Color.white);
        Fill.circle(unit.x, unit.y, 1.5f);
        
        // 画左右粒子射线
        Draw.z(85f);
        float time = Time.time;
        float length = 60f;
        int particleCount = 200;
        float spacing = length / particleCount;
        float travel = time * 15f;
        Mathf.rand.setSeed(0);
        
        for (int sign : new int[]{1, -1}) {
            float angle = 0f + sign * 180f;
            for (int i = 0; i < particleCount; i++) {
                float dist = (travel + i * spacing) % length;
                float t = dist / length;
                float spread = t * 2f;
                float offset = Mathf.rand.random(-spread, spread);
                float finalAngle = angle + offset;
                
                float px = unit.x + Angles.trnsx(finalAngle, dist);
                float py = unit.y + Angles.trnsy(finalAngle, dist);
                
                Color c = (t < 0.2f) ? Color.white.lerp(Color.valueOf("00ffff"), t / 0.2f) : Color.valueOf("00ffff");
                float flicker = (Mathf.sin(dist * 0.2f - time * 0.5f) + 1f) / 2f;
                float alpha = (1f - t * 0.8f) * (0.5f + flicker * 0.5f);
                float size = (0.8f - t * 0.6f) * Mathf.rand.random(0.5f, 1.0f);
                size = Math.max(size, 0.15f);
                
                Draw.color(c, alpha);
                Fill.circle(px, py, size);
            }
        }
        Mathf.rand.setSeed(0);
    }
}

// ===== 黑洞 (上下射线，无吸入伤害) =====
class BlackHoleUnitType extends UnitType {
    public int diskParticles = 200;
    public float diskSpeed = 2f;
    public float diskRx = 12f;
    public float diskRy = 6f;

    public BlackHoleUnitType(String name) {
        super(name);
        hitSize = 6f;
        flying = true;
        rotateSpeed = 0f;
        region = Core.atlas.find("clear"); // 彻底屏蔽原版贴图
        drawBody = false;
        drawCell = false;
        drawControl = false;
        drawShadow = false;
    }

    @Override
    public void update(Unit unit) {
        super.update(unit);
        float length = 120f; // 射线加长
        float width = 3f;
        float damage = 80f;
        for (int sign : new int[]{1, -1}) {
            float angle = 90f * sign; // 锁死上下
            float ex = unit.x + Angles.trnsx(angle, length);
            float ey = unit.y + Angles.trnsy(angle, length);
            applyDamageAlongLine(unit, unit.x, unit.y, ex, ey, width, damage);
        }
    }

    @Override
    public void draw(Unit unit) {
        float time = Time.time;
        
        // 画纯黑核心 (最顶层)
        Draw.z(110f);
        Draw.color(Color.black);
        Fill.circle(unit.x, unit.y, 5f);
        Draw.color(Color.valueOf("fff200")); // 极小黄点高光
        Fill.circle(unit.x, unit.y, 1f);
        
        // 画黄蓝椭圆吸积盘
        Draw.z(105f);
        drawEllipticalDisk(unit.x, unit.y, time);
        
        // 画上下粒子射线
        Draw.z(85f);
        float length = 120f; // 射线加长
        int particleCount = 200;
        float spacing = length / particleCount;
        float travel = time * 15f;
        Mathf.rand.setSeed(0);
        
        for (int sign : new int[]{1, -1}) {
            float angle = 90f * sign;
            for (int i = 0; i < particleCount; i++) {
                float dist = (travel + i * spacing) % length;
                float t = dist / length;
                float spread = t * 2f;
                float offset = Mathf.rand.random(-spread, spread);
                float finalAngle = angle + offset;
                
                float px = unit.x + Angles.trnsx(finalAngle, dist);
                float py = unit.y + Angles.trnsy(finalAngle, dist);
                
                Color c = (t < 0.2f) ? Color.white.lerp(Color.valueOf("00b3ff"), t / 0.2f) : Color.valueOf("00b3ff");
                float flicker = (Mathf.sin(dist * 0.2f - time * 0.5f) + 1f) / 2f;
                float alpha = (1f - t * 0.8f) * (0.5f + flicker * 0.5f);
                float size = (0.8f - t * 0.6f) * Mathf.rand.random(0.5f, 1.0f);
                size = Math.max(size, 0.15f);
                
                Draw.color(c, alpha);
                Fill.circle(px, py, size);
            }
        }
        Mathf.rand.setSeed(0);
    }

    private void drawEllipticalDisk(float x, float y, float time) {
        Color inner = Color.valueOf("fff200");
        Color mid = Color.valueOf("ffae00");
        Color outer = Color.valueOf("00b3ff");
        Mathf.rand.setSeed(777);
        for (int i = 0; i < diskParticles; i++) {
            float t = Mathf.rand.random(0f, 1f);
            float angle = time * diskSpeed * (1f + (1f - t) * 1.5f) + t * 360f * 2f;
            float rx = diskRx * (0.3f + t * 0.7f);
            float ry = diskRy * (0.3f + t * 0.7f);
            float px = x + Angles.trnsx(angle, rx);
            float py = y + Angles.trnsy(angle, ry);
            Color c = (t < 0.4f) ? inner.lerp(mid, t / 0.4f) : mid.lerp(outer, (t - 0.4f) / 0.6f);
            float flicker = (Mathf.sin(time * 8f + i * 0.9f) + 1f) / 2f;
            float alpha = (0.7f + flicker * 0.3f) * (1f - t * 0.6f);
            float size = (1.8f - t * 1.2f) + Mathf.sin(time * 6f + i) * 0.3f;
            size = Math.max(size, 0.3f);
            Draw.color(c, alpha);
            Fill.circle(px, py, size);
        }
        Mathf.rand.setSeed(0);
    }
}

// ===== 共用伤害辅助 =====
private static int applyDamageAlongLine(Unit source, float x1, float y1, float x2, float y2, float width, float damage) {
    int hit = 0;
    Team sourceTeam = source.team;
    for (Unit u : Groups.unit) {
        if (u == null || u.dead) continue;
        if (u.team == sourceTeam) continue;
        if (distanceToSegment(u.x, u.y, x1, y1, x2, y2) <= width + u.hitSize) {
            u.damage(damage);
            hit++;
        }
    }
    return hit;
}

private static float distanceToSegment(float px, float py, float x1, float y1, float x2, float y2) {
    float dx = x2 - x1, dy = y2 - y1;
    float len2 = dx * dx + dy * dy;
    if (len2 < 0.0001f) return Mathf.dst(px, py, x1, y1);
    float t = ((px - x1) * dx + (py - y1) * dy) / len2;
    t = Mathf.clamp(t, 0f, 1f);
    float cx = x1 + t * dx, cy = y1 + t * dy;
    return Mathf.dst(px, py, cx, cy);
}
