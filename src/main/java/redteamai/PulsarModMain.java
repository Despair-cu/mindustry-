package redteamai;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.game.Team;
import mindustry.mod.Mod;
import mindustry.type.UnitType;

public class PulsarModMain extends Mod {

    public static boolean DEBUG = false;

    @Override
    public void loadContent() {
        Log.info("[PulsarMod] 加载恒星单位...");
        new YellowDwarfUnitType("yellow-dwarf").load();
        new BluePulsarUnitType("blue-pulsar").load();
        new BlackHoleUnitType("black-hole").load();
        Log.info("[PulsarMod] 所有单位注册完成");
    }

    // ==================== 黄矮星：吸入100万伤害，无射线 ====================
    public static class YellowDwarfUnitType extends UnitType {
        public YellowDwarfUnitType(String name) {
            super(name);
            health = Integer.MAX_VALUE;
            speed = 0f;
            hitSize = 20f;
            constructor = UnitEntity::create;
            weapons = new Seq<>();
            outlineColor = Color.valueOf("00000000");
            localizedName = "黄矮星";
        }

        @Override
        public void update(Unit unit) {
            unit.health = health;
            for (Unit u : Groups.unit) {
                if (u == null || u.dead || u.team == unit.team) continue;
                float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);
                if (dst < 150f) {
                    float angle = Angles.angle(unit.x, unit.y, u.x, u.y);
                    u.x -= Angles.trnsx(angle, 1.0f);
                    u.y -= Angles.trnsy(angle, 1.0f);
                    u.damage(1000000f * Time.delta); // ✅ 100万吸入伤害
                }
                if (dst <= unit.hitSize + u.hitSize + 5f) {
                    u.kill();
                }
            }
        }

        @Override
        public void draw(Unit unit) {
            Draw.reset();
            Draw.z(100f);
            Draw.color(Color.valueOf("ffd37f"));
            Fill.circle(unit.x, unit.y, 15f);
            Draw.color(Color.valueOf("ff9d00"));
            Fill.circle(unit.x, unit.y, 10f);
            Draw.color(Color.white);
            Fill.circle(unit.x, unit.y, 5f);
            Draw.reset();
        }
    }

    // ==================== 中子星：射线左右，无吸入伤害 ====================
    public static class BluePulsarUnitType extends UnitType {
        public BluePulsarUnitType(String name) {
            super(name);
            health = Integer.MAX_VALUE;
            speed = 0f;
            hitSize = 10f; // ✅ 缩小
            constructor = UnitEntity::create;
            weapons = new Seq<>();
            outlineColor = Color.valueOf("00000000");
            localizedName = "中子星";
        }

        @Override
        public void update(Unit unit) {
            unit.health = health;
            float length = 50f;
            float damage = 80f * Time.delta;
            // ✅ 射线左右
            for (int sign : new int[]{1, -1}) {
                float a = sign > 0 ? 0f : 180f;
                float ex = unit.x + Angles.trnsx(a, length);
                float ey = unit.y + Angles.trnsy(a, length);
                applyDamageAlongLine(unit, unit.x, unit.y, ex, ey, 3f, damage);
            }
        }

        @Override
        public void draw(Unit unit) {
            Draw.reset();
            float time = Time.time;
            // ✅ 实心核心（无洞）
            Draw.z(100f);
            Draw.color(Color.valueOf("00e5ff"));
            Fill.circle(unit.x, unit.y, 6f);
            Draw.color(Color.white);
            Fill.circle(unit.x, unit.y, 2f);

            // ✅ 左右射线
            Draw.z(85f);
            for (int sign : new int[]{1, -1}) {
                drawJet(unit.x, unit.y, sign > 0 ? 0f : 180f, 50f, time, sign);
            }
            Draw.reset();
        }
    }

    // ==================== 黑洞：射线上下，无吸入伤害，纯黑核心 ====================
    public static class BlackHoleUnitType extends UnitType {
        public BlackHoleUnitType(String name) {
            super(name);
            health = Integer.MAX_VALUE;
            speed = 0f;
            hitSize = 12f;
            constructor = UnitEntity::create;
            weapons = new Seq<>();
            outlineColor = Color.valueOf("00000000");
            localizedName = "黑洞";
        }

        @Override
        public void update(Unit unit) {
            unit.health = health;
            float length = 100f; // ✅ 射线加长
            float damage = 200f * Time.delta;
            // ✅ 射线上下
            for (int sign : new int[]{1, -1}) {
                float a = 90f * sign;
                float ex = unit.x + Angles.trnsx(a, length);
                float ey = unit.y + Angles.trnsy(a, length);
                applyDamageAlongLine(unit, unit.x, unit.y, ex, ey, 4f, damage);
            }
        }

        @Override
        public void draw(Unit unit) {
            Draw.reset();
            float time = Time.time;

            // 1. 上下射线（底层）
            Draw.z(85f);
            for (int sign : new int[]{1, -1}) {
                drawJet(unit.x, unit.y, 90f * sign, 100f, time, sign);
            }

            // 2. 纯黑核心（顶层，直接盖住原版贴图）
            Draw.z(110f);
            Draw.color(Color.black);
            Fill.circle(unit.x, unit.y, 8f); // ✅ 纯黑，无黄点

            Draw.reset();
        }
    }

    // ===== 共用射线绘制 =====
    private static void drawJet(float x, float y, float angle, float length, float time, int seed) {
        int count = 200;
        float speed = 20f;
        float spacing = length / count;
        float travel = time * speed;
        Mathf.rand.setSeed(seed);
        for (int i = 0; i < count; i++) {
            float dist = (travel + i * spacing) % length;
            float t = dist / length;
            float spread = t * 3f;
            float offset = Mathf.rand.random(-spread, spread);
            float a = angle + offset;
            float px = x + Angles.trnsx(a, dist);
            float py = y + Angles.trnsy(a, dist);
            Color c = (t < 0.2f) ? Color.white.lerp(Color.valueOf("00b3ff"), t / 0.2f) : Color.valueOf("00b3ff");
            float alpha = 1f - t * 0.8f;
            float size = (0.8f - t * 0.5f) * Mathf.rand.random(0.5f, 1f);
            Draw.color(c, alpha);
            Fill.circle(px, py, Math.max(size, 0.2f));
        }
        Mathf.rand.setSeed(0);
    }

    // ===== 共用伤害辅助 =====
    private static int applyDamageAlongLine(Unit source, float x1, float y1, float x2, float y2, float width, float damage) {
        int hit = 0;
        for (Unit u : Groups.unit) {
            if (u == null || u.dead || u.team == source.team) continue;
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
        return Mathf.dst(px, py, x1 + t * dx, y1 + t * dy);
    }
}
