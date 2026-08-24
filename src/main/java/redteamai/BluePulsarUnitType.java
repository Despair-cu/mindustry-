package redteamai;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.type.UnitType;

public class BluePulsarUnitType extends UnitType {

    public Color coreColor = Color.valueOf("5b6cff");  // 亮蓝核心
    public Color outerColor = Color.valueOf("9d4dff");  // 紫色外晕
    public Color jetColor = Color.valueOf("b388ff");    // 激光色（偏紫白）
    public float pulseSpeed = 35f;
    public float baseRadius = 10f;

    public BluePulsarUnitType(String name) {
        super(name);
        health = 500;
        speed = 1.2f;
        rotateSpeed = 12f;  // 转得更快，脉冲星特性
        hitSize = baseRadius * 2f;
        constructor = UnitEntity::create;
        weapons = new Seq<>();
        outlineColor = Color.valueOf("00000000");
    }

    @Override
    public void draw(Unit unit) {
        float x = unit.x;
        float y = unit.y;
        float time = Time.time;

        float pulse = Mathf.sin(time, pulseSpeed, 1f);
        float radius = baseRadius + pulse * 2f;

        // ===== 两极激光（最明显，画在最底层）=====
        // 激光长度随脉冲跳动
        float jetLength = radius * 6f + pulse * 4f;
        // 脉冲星自转轴（用单位旋转角 + 时间让它缓慢进动）
        float jetAngle = unit.rotation + Mathf.sin(time, 60f, 8f);

        Draw.z(95f); // 在波纹下面

        // --- 北极喷流（粗光晕） ---
        Draw.color(outerColor, 0.6f + pulse * 0.2f);
        Lines.stroke(5f + pulse * 2f);
        Lines.lineAngle(x, y, jetAngle, jetLength);

        // --- 北极喷流（高亮核心） ---
        Draw.color(jetColor, 0.9f);
        Lines.stroke(2.5f + pulse);
        Lines.lineAngle(x, y, jetAngle, jetLength * 0.85f);

        // --- 北极喷流（白色极核） ---
        Draw.color(Color.white, 0.7f + pulse * 0.3f);
        Lines.stroke(1f);
        Lines.lineAngle(x, y, jetAngle, jetLength * 0.6f);

        // --- 南极喷流（反方向，同样三层） ---
        Draw.color(outerColor, 0.6f + pulse * 0.2f);
        Lines.stroke(5f + pulse * 2f);
        Lines.lineAngle(x, y, jetAngle + 180f, jetLength * 0.9f);

        Draw.color(jetColor, 0.9f);
        Lines.stroke(2.5f + pulse);
        Lines.lineAngle(x, y, jetAngle + 180f, jetLength * 0.75f);

        Draw.color(Color.white, 0.7f + pulse * 0.3f);
        Lines.stroke(1f);
        Lines.lineAngle(x, y, jetAngle + 180f, jetLength * 0.5f);

        // ===== 扩散波纹 =====
        float waveProgress = (time % 35f) / 35f;
        float waveRadius = waveProgress * baseRadius * 4f;
        float waveAlpha = 1f - waveProgress;

        Draw.z(100f);
        Draw.color(coreColor, waveAlpha * 0.5f);
        Lines.stroke(2f + pulse * 1.5f);
        Lines.circle(x, y, waveRadius);

        // ===== 外发光 =====
        Draw.z(110f);
        Draw.color(outerColor, 0.3f + pulse * 0.15f);
        Fill.circle(x, y, radius * 1.8f);

        // ===== 核心 =====
        Draw.color(coreColor);
        Fill.circle(x, y, radius * 0.75f);

        // ===== 高光 =====
        Draw.color(Color.white, 0.85f);
        Fill.circle(x, y, radius * 0.3f);

        // ===== 旋转磁极节点（6个，更密集） =====
        for (int i = 0; i < 6; i++) {
            float angle = time * (30f + i * 5f) + (i * 60f);
            float dist = radius * 0.55f;
            float px = x + Angles.trnsx(angle, dist);
            float py = y + Angles.trnsy(angle, dist);
            Draw.color(Color.white, coreColor, 0.5f + Mathf.sin(time + i * 45f, 8f, 0.5f));
            Fill.circle(px, py, 2f + pulse * 1.5f);
        }

        // ===== 星火粒子 =====
        Mathf.rand.setSeed(unit.id);
        for (int i = 0; i < 5; i++) {
            float sparkAngle = Mathf.rand.random(360f);
            float sparkDist = Mathf.rand.random(radius * 0.6f, radius * 2.5f);
            float sx = x + Angles.trnsx(sparkAngle, sparkDist);
            float sy = y + Angles.trnsy(sparkAngle, sparkDist);
            float alpha = Mathf.rand.random(0.15f, 0.45f);
            Draw.color(coreColor, alpha);
            Fill.circle(sx, sy, Mathf.rand.random(1f, 2.5f));
        }
        Mathf.rand.setSeed(0);

        Draw.reset();
    }
}
