package redteamai;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Drawf;       // ✅ 从 arc 包导入，不依赖 mindustry.gen
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.Seq;               // ✅ 用于武器列表
import arc.util.Time;                // ✅ 用全局时间代替 unit.time
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.type.UnitType;

public class PulsarUnitType extends UnitType {

    public Color coreColor = Color.valueOf("ffd37f");
    public Color outerColor = Color.valueOf("ff9d00");
    public float pulseSpeed = 40f;
    public float baseRadius = 12f;

    public PulsarUnitType(String name) {
        super(name);

        health = 450;
        speed = 1.4f;
        rotateSpeed = 8f;
        hitSize = baseRadius * 2f;

        constructor = UnitEntity::create;

        // ✅ v8 里武器是 Seq<Weapon> 列表，不是单个 weapon 字段
        weapons = new Seq<>();
        
        outlineColor = Color.valueOf("00000000");
    }

    @Override
    public void draw(Unit unit) {
        float x = unit.x;
        float y = unit.y;
        // ✅ 用 Time.time（全局时间）代替 unit.time，避免找不到符号
        float time = Time.time;

        float pulse = Mathf.sin(time, pulseSpeed, 1f);
        float radius = baseRadius + pulse * 3f;

        // 扩散波纹
        float waveProgress = (time % pulseSpeed) / pulseSpeed;
        float waveRadius = waveProgress * baseRadius * 3.5f;
        float waveAlpha = 1f - waveProgress;

        // ✅ 直接用数字指定渲染层级，不依赖 Layer 类
        Draw.z(100f); // 相当于 Layer.effect
        Draw.color(coreColor, waveAlpha * 0.4f);
        Lines.stroke(2f + pulse);
        Lines.circle(x, y, waveRadius);

        // 外发光
        Draw.z(110f); // 相当于 Layer.flyingUnitLow
        Draw.color(outerColor, 0.25f + pulse * 0.1f);
        Fill.circle(x, y, radius * 1.6f);

        // 核心
        Draw.color(coreColor);
        Fill.circle(x, y, radius * 0.7f);

        // 高光
        Draw.color(Color.white, 0.8f);
        Fill.circle(x, y, radius * 0.35f);

        // 旋转能量节点
        for (int i = 0; i < 3; i++) {
            float angle = time * (25f + i * 10f) + (i * 120f);
            float dist = radius * 0.5f;
            float px = x + Angles.trnsx(angle, dist);
            float py = y + Angles.trnsy(angle, dist);

            Draw.color(Color.white, coreColor, 0.5f + Mathf.sin(time + i * 60f, 10f, 0.5f));
            Fill.circle(px, py, 2.5f + pulse * 1.2f);
        }

        // 星火粒子
        Mathf.rand.setSeed(unit.id);
        for (int i = 0; i < 4; i++) {
            float sparkAngle = Mathf.rand.random(360f);
            float sparkDist = Mathf.rand.random(radius * 0.8f, radius * 2.2f);
            float sx = x + Angles.trnsx(sparkAngle, sparkDist);
            float sy = y + Angles.trnsy(sparkAngle, sparkDist);
            float alpha = Mathf.rand.random(0.2f, 0.5f);

            Draw.color(coreColor, alpha);
            Fill.circle(sx, sy, Mathf.rand.random(1f, 2.5f));
        }
        Mathf.rand.setSeed(0);

        // ✅ Drawf 现在从 arc.graphics.g2d 导入，肯定能找到
        Drawf.light(x, y, radius * 3.5f, coreColor, 0.7f);

        Draw.reset();
    }
}
