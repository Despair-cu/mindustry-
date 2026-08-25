package your.mod.units;

import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import mindustry.core.Core;
import mindustry.entities.abilities.ForceFieldAbility;
import mindustry.entities.abilities.RepairFieldAbility;
import mindustry.entities.abilities.ShieldRegenFieldAbility;
import mindustry.entities.abilities.SpawnerAbility;
import mindustry.entities.bullet.Bullets;
import mindustry.entities.bullet.MissileBulletType;
import mindustry.entities.bullet.RailBulletType;
import mindustry.entities.weapons.Weapon;
import mindustry.mod.Mod;
import mindustry.type.UnitType;

import static mindustry.Vars.content;

public class AllUnitsMod extends Mod {

    // ==================== 公开所有单位静态实例 ====================
    // 第1轮 (DeepSeek)
    public static UnitType greenGroundUnit;
    public static UnitType blueFlyUnit;
    public static UnitType redFactoryUnit;
    // 第2轮 (元宝)
    public static UnitType purpleNavalUnit;
    public static UnitType yellowMinerUnit;
    // 第3轮 (DeepSeek)
    public static UnitType cyanSupportUnit;
    public static UnitType graySiegeUnit;
    // 第4轮 (元宝)
    public static UnitType whiteAntiAirUnit;
    public static UnitType pinkEWarUnit;
    // 第5轮 (DeepSeek)
    public static UnitType orangeSuicideUnit;
    public static UnitType blueSubmarineUnit;
    // 第6轮 (元宝)
    public static UnitType brownTransportUnit;
    public static UnitType blackReaperUnit;
    // 第7轮 (DeepSeek)
    public static UnitType redSniperUnit;
    public static UnitType greenHiveUnit;
    // 第8轮 (元宝)
    public static UnitType goldPaladinUnit;
    public static UnitType silverRailgunUnit;
    // 第9轮 (DeepSeek)
    public static UnitType crimsonDemonUnit;
    public static UnitType cobaltShielderUnit;
    // 第10轮 (元宝 最终收尾)
    public static UnitType rainbowTitanUnit;
    public static UnitType emeraldCommanderUnit;

    // ==================== 通用贴图画师（基础版） ====================
    private TextureRegion generateSprite(String name, int mainColor, int accentColor, boolean isFly) {
        int size = 32;
        Pixmap pix = new Pixmap(size, size);
        pix.fill(Color.valueOf("2a2a2a"));

        pix.setColor(new Color(mainColor));
        if (isFly) {
            pix.fillTriangle(4, 28, 16, 4, 28, 28);
            pix.setColor(new Color(accentColor));
            pix.fillTriangle(10, 26, 16, 10, 22, 26);
        } else {
            pix.fillRectangle(4, 4, 24, 24);
            pix.setColor(new Color(accentColor));
            pix.fillRectangle(10, 10, 12, 12);
            if (!name.contains("factory")) {
                pix.setColor(Color.valueOf("888888"));
                pix.drawRect(4, 4, 24, 24);
            }
        }
        pix.setColor(new Color(accentColor));
        pix.drawRect(0, 0, size - 1, size - 1);

        TextureRegion region = new TextureRegion(new Texture(pix));
        Core.atlas.addRegion(name, region);
        pix.dispose();
        return region;
    }

    // ==================== 高级贴图画师（支持 18 种造型） ====================
    private TextureRegion generateSpecialSprite(String name, int mainColor, int accentColor, int type) {
        int size = 32;
        Pixmap pix = new Pixmap(size, size);
        pix.fill(Color.valueOf("2a2a2a"));
        pix.setColor(new Color(mainColor));

        switch (type) {
            case 0: // 海军：船体
                pix.fillTriangle(2, 30, 16, 4, 30, 30);
                pix.fillRect(2, 20, 28, 10);
                pix.setColor(new Color(accentColor));
                pix.fillRect(12, 8, 8, 12);
                break;
            case 1: // 采矿：钻头方块
                pix.fillRectangle(2, 2, 28, 28);
                pix.setColor(new Color(accentColor));
                pix.fillRectangle(8, 8, 16, 16);
                pix.setColor(Color.valueOf("AAAAAA"));
                pix.fillTriangle(14, 4, 18, 4, 16, 12);
                break;
            case 2: // 支援机：圆形 + 医疗十字
                pix.fillCircle(16, 16, 14);
                pix.setColor(new Color(accentColor));
                pix.fillRectangle(13, 6, 6, 20);
                pix.fillRectangle(6, 13, 20, 6);
                break;
            case 3: // 攻城巨兽：厚重底盘 + 炮管
                pix.fillRectangle(2, 8, 28, 16);
                pix.setColor(new Color(accentColor));
                pix.fillRectangle(8, 2, 16, 8);
                pix.fillRectangle(22, 0, 8, 6);
                pix.setColor(Color.valueOf("FF5555"));
                pix.fillRectangle(28, 1, 4, 4);
                break;
            case 4: // 防空坦克：雷达碟形
                pix.fillCircle(16, 16, 12);
                pix.setColor(new Color(accentColor));
                pix.fillRect(14, 4, 4, 24);
                pix.fillRect(4, 14, 24, 4);
                break;
            case 5: // 电子战：菱形 + 脉冲核心
                pix.fillTriangle(16, 2, 30, 16, 16, 30);
                pix.fillTriangle(16, 2, 2, 16, 16, 30);
                pix.setColor(new Color(accentColor));
                pix.fillCircle(16, 16, 4);
                break;
            case 6: // 自爆无人机：骷髅/爆炸标
                pix.fillCircle(16, 16, 14);
                pix.setColor(new Color(accentColor));
                pix.fillCircle(16, 16, 8);
                pix.setColor(Color.valueOf("FF0000"));
                pix.fillCircle(10, 12, 3);
                pix.fillCircle(22, 12, 3);
                pix.fillRectangle(14, 18, 4, 2);
                break;
            case 7: // 隐形潜艇：水纹 + 流线体
                pix.fillEllipse(4, 8, 24, 16);
                pix.setColor(new Color(accentColor));
                pix.fillRect(0, 14, 6, 4);
                pix.fillRect(26, 14, 6, 4);
                pix.setColor(new Color(mainColor));
                pix.fillRect(14, 12, 4, 8);
                break;
            case 8: // 运输单位：货舱造型
                pix.fillRectangle(2, 6, 28, 20);
                pix.setColor(new Color(accentColor));
                pix.fillRectangle(6, 10, 20, 12);
                pix.setColor(Color.valueOf("FFCC00"));
                pix.fillRect(4, 8, 4, 4);
                pix.fillRect(24, 8, 4, 4);
                break;
            case 9: // 死神单位：尖刺+镰刀
                pix.fillCircle(16, 18, 10);
                pix.setColor(new Color(accentColor));
                pix.fillTriangle(16, 2, 20, 8, 12, 8);
                pix.fillRect(14, 8, 4, 16);
                pix.setColor(Color.valueOf("FF0000"));
                pix.fillCircle(12, 16, 2);
                pix.fillCircle(20, 16, 2);
                break;
            case 10: // 狙击炮台：瞄准十字 + 细长炮管
                pix.fillRectangle(4, 12, 24, 8);
                pix.setColor(new Color(accentColor));
                pix.fillRect(14, 2, 4, 20);
                pix.fillRect(2, 14, 28, 4);
                pix.setColor(Color.valueOf("FF0000"));
                pix.fillCircle(16, 16, 2);
                pix.fillRect(28, 14, 4, 4);
                break;
            case 11: // 蜂巢母舰：六边形 + 蜂巢孔
                pix.fillCircle(16, 16, 14);
                pix.setColor(new Color(accentColor));
                pix.fillTriangle(16, 4, 24, 10, 24, 22);
                pix.fillTriangle(16, 4, 8, 10, 8, 22);
                pix.setColor(Color.valueOf("FFAA00"));
                for (int i = 0; i < 5; i++) {
                    int x = 10 + (i % 3) * 6;
                    int y = 8 + (i / 3) * 8;
                    pix.fillCircle(x, y, 2);
                }
                break;
            case 12: // 圣骑士：盾牌+剑
                pix.fillRectangle(4, 4, 12, 24);
                pix.setColor(new Color(accentColor));
                pix.fillRectangle(6, 6, 8, 20);
                pix.setColor(Color.valueOf("CCCCCC"));
                pix.fillRect(20, 8, 4, 16);
                pix.fillTriangle(18, 8, 26, 8, 22, 2);
                pix.fillRect(18, 24, 8, 4);
                break;
            case 13: // 轨道炮：长条形炮管+能量核心
                pix.fillRectangle(2, 12, 28, 8);
                pix.setColor(new Color(accentColor));
                pix.fillRect(26, 10, 6, 12);
                pix.setColor(Color.valueOf("00FFFF"));
                pix.fillCircle(16, 16, 4);
                pix.fillRect(6, 14, 8, 4);
                break;
            case 14: // 恶魔：火焰纹 + 利爪
                pix.fillCircle(16, 16, 12);
                pix.setColor(new Color(accentColor));
                pix.fillTriangle(16, 2, 22, 10, 10, 10);
                pix.setColor(Color.valueOf("FF5500"));
                for (int i = 0; i < 6; i++) {
                    float angle = i * 60f + 30;
                    int x = (int) (16 + 14 * Math.cos(angle * Math.PI / 180));
                    int y = (int) (16 + 14 * Math.sin(angle * Math.PI / 180));
                    pix.fillCircle(x, y, 2);
                }
                break;
            case 15: // 护盾兵：圆顶+护盾波纹
                pix.fillCircle(16, 16, 14);
                pix.setColor(new Color(accentColor));
                pix.fillCircle(16, 16, 10);
                pix.setColor(Color.valueOf("88CCFF"));
                pix.drawCircle(16, 16, 6);
                pix.drawCircle(16, 16, 10);
                break;
            case 16: // [第10轮新增] 彩虹泰坦：皇冠+王座
                pix.fillRectangle(2, 8, 28, 20);
                pix.setColor(new Color(accentColor));
                // 皇冠
                pix.fillTriangle(4, 8, 8, 2, 12, 8);
                pix.fillTriangle(12, 8, 16, 2, 20, 8);
                pix.fillTriangle(20, 8, 24, 2, 28, 8);
                // 王座纹理
                pix.fillRect(6, 14, 20, 4);
                pix.fillRect(10, 18, 12, 6);
                break;
            case 17: // [第10轮新增] 翡翠指挥官：科技环+核心
                pix.fillCircle(16, 16, 14);
                pix.setColor(new Color(accentColor));
                pix.drawCircle(16, 16, 10);
                pix.drawCircle(16, 16, 6);
                pix.setColor(Color.valueOf("00FFAA"));
                pix.fillCircle(16, 16, 3);
                // 科技环
                for (int i = 0; i < 8; i++) {
                    float angle = i * 45f;
                    int x = (int) (16 + 12 * Math.cos(angle * Math.PI / 180));
                    int y = (int) (16 + 12 * Math.sin(angle * Math.PI / 180));
                    pix.fillCircle(x, y, 2);
                }
                break;
        }

        pix.setColor(new Color(accentColor));
        pix.drawRect(0, 0, size - 1, size - 1);
        TextureRegion region = new TextureRegion(new Texture(pix));
        Core.atlas.addRegion(name, region);
        pix.dispose();
        return region;
    }

    // ==================== 加载所有内容 ====================
    @Override
    public void loadContent() {
        // ---------- 第1轮单位 (DeepSeek) ----------
        greenGroundUnit = new UnitType("green-tank") {{
            health = 240f; speed = 1.2f; hitSize = 10f; armor = 2f;
            region = generateSprite("green-tank", 0xFF4488AA, 0xFFFFFFFF, false);
            weapons.add(new Weapon() {{ reload = 20f; bullet = Bullets.standardCopper; x = 4f; y = 0f; }});
            weapons.add(new Weapon() {{ reload = 40f; bullet = Bullets.standardDagger; x = -4f; y = 0f; rotate = true; }});
            playerControllable = true;
        }};

        blueFlyUnit = new UnitType("blue-flyer") {{
            health = 120f; speed = 3.8f; hitSize = 7f; flying = true;
            region = generateSprite("blue-flyer", 0xFF3399FF, 0xFFFFFFFF, true);
            weapons.add(new Weapon() {{ reload = 10f; bullet = Bullets.standardDagger; x = 0f; y = 2f; }});
            playerControllable = true;
        }};

        redFactoryUnit = new UnitType("red-factory") {{
            health = 400f; speed = 0f; hitSize = 18f; isFactory = true; buildSpeed = 2.5f;
            region = generateSprite("red-factory", 0xFFFF5544, 0xFFFFFF00, false);
            weapons.add(new Weapon() {{ reload = 30f; bullet = Bullets.standardCopper; x = 0f; y = -4f; }});
            playerControllable = true;
        }};

        // ---------- 第2轮单位 (元宝) ----------
        purpleNavalUnit = new UnitType("purple-naval") {{
            health = 180f; speed = 2.5f; hitSize = 12f; naval = true;
            region = generateSpecialSprite("purple-naval", 0xFF9944FF, 0xFFDD88FF, 0);
            weapons.add(new Weapon() {{
                reload = 35f;
                bullet = new MissileBulletType(3.5f, 15f) {{ homingPower = 0.05f; trailLength = 5; lifetime = 60f; }};
                x = 6f; y = 0f; mirror = true;
            }});
            playerControllable = true;
        }};

        yellowMinerUnit = new UnitType("yellow-miner") {{
            health = 80f; speed = 0.8f; hitSize = 9f; miningSpeed = 3.0f;
            region = generateSpecialSprite("yellow-miner", 0xFFFFCC00, 0xFFFFFF66, 1);
            weapons.add(new Weapon() {{ reload = 50f; bullet = Bullets.standardCopper; x = 0f; y = 0f; }});
            playerControllable = true;
        }};

        // ---------- 第3轮单位 (DeepSeek) ----------
        cyanSupportUnit = new UnitType("cyan-support") {{
            health = 100f; speed = 4.2f; hitSize = 8f; flying = true;
            region = generateSpecialSprite("cyan-support", 0xFF00FFCC, 0xFFFFFFFF, 2);
            abilities.add(new RepairFieldAbility(50f, 20f, 60f));
            weapons.add(new Weapon() {{ reload = 15f; bullet = Bullets.standardCopper; x = 0f; y = -2f; }});
            playerControllable = true;
        }};

        graySiegeUnit = new UnitType("gray-siege") {{
            health = 800f; speed = 0.45f; hitSize = 22f; armor = 6f;
            region = generateSpecialSprite("gray-siege", 0xFF888888, 0xFFFFAA00, 3);
            weapons.add(new Weapon() {{
                reload = 80f; bullet = Bullets.artilleryDense;
                x = 12f; y = 0f; rotate = true; recoil = 6f; shootCone = 15f;
            }});
            weapons.add(new Weapon() {{ reload = 25f; bullet = Bullets.standardDagger; x = -6f; y = -4f; }});
            playerControllable = true;
        }};

        // ---------- 第4轮单位 (元宝) ----------
        whiteAntiAirUnit = new UnitType("white-anti-air") {{
            health = 150f; speed = 1.5f; hitSize = 11f; armor = 1f;
            region = generateSpecialSprite("white-anti-air", 0xFFFFFFFF, 0xFF888888, 4);
            weapons.add(new Weapon() {{ reload = 5f; bullet = Bullets.flakScrap; x = 4f; y = 0f; mirror = true; }});
            playerControllable = true;
        }};

        pinkEWarUnit = new UnitType("pink-e-war") {{
            health = 90f; speed = 3.5f; hitSize = 6f; flying = true;
            region = generateSpecialSprite("pink-e-war", 0xFFFF66FF, 0xFFFFFFFF, 5);
            weapons.add(new Weapon() {{ reload = 30f; bullet = Bullets.standardDagger; x = 0f; y = 0f; }});
            abilities.add(new ForceFieldAbility(40f, 0.5f, 100f, 400f));
            playerControllable = true;
        }};

        // ---------- 第5轮单位 (DeepSeek) ----------
        orangeSuicideUnit = new UnitType("orange-suicide") {{
            health = 60f; speed = 5.5f; hitSize = 6f; flying = true;
            region = generateSpecialSprite("orange-suicide", 0xFFFF8800, 0xFFFFFFFF, 6);
            damage = 80f;
            abilities.add(new SpawnerAbility(Bullets.explosive, 3, 0f, 0f));
            playerControllable = true;
        }};

        blueSubmarineUnit = new UnitType("blue-submarine") {{
            health = 160f; speed = 2.8f; hitSize = 10f; naval = true;
            region = generateSpecialSprite("blue-submarine", 0xFF0044AA, 0xFF88CCFF, 7);
            armor = 0.5f; targetPriority = -1f;
            weapons.add(new Weapon() {{
                reload = 45f;
                bullet = new MissileBulletType(4.5f, 20f) {{ homingPower = 0.1f; trailLength = 8; lifetime = 80f; }};
                x = 8f; y = 0f; mirror = true;
            }});
            playerControllable = true;
        }};

        // ---------- 第6轮单位 (元宝) ----------
        brownTransportUnit = new UnitType("brown-transport") {{
            health = 200f; speed = 1.8f; hitSize = 14f; armor = 3f;
            itemCapacity = 50;
            region = generateSpecialSprite("brown-transport", 0xFF8B4513, 0xFFDEB887, 8);
            weapons.add(new Weapon() {{ reload = 30f; bullet = Bullets.standardCopper; x = 5f; y = 0f; mirror = true; }});
            playerControllable = true;
        }};

        blackReaperUnit = new UnitType("black-reaper") {{
            health = 350f; speed = 1.0f; hitSize = 13f; armor = 4f;
            region = generateSpecialSprite("black-reaper", 0xFF1A1A1A, 0xFF666666, 9);
            weapons.add(new Weapon() {{ reload = 60f; bullet = Bullets.standardThorium; x = 0f; y = 4f; shootCone = 30f; }});
            weapons.add(new Weapon() {{ reload = 15f; bullet = Bullets.standardDagger; x = 6f; y = -2f; mirror = true; }});
            abilities.add(new ShieldRegenFieldAbility(20f, 60f, 40f, 300f));
            playerControllable = true;
        }};

        // ---------- 第7轮单位 (DeepSeek) ----------
        redSniperUnit = new UnitType("red-sniper") {{
            health = 80f; speed = 0.6f; hitSize = 10f; armor = 0f;
            region = generateSpecialSprite("red-sniper", 0xFFFF0000, 0xFFFFFFFF, 10);
            weapons.add(new Weapon() {{
                reload = 120f; bullet = Bullets.artilleryDense;
                x = 8f; y = 0f; rotate = true; recoil = 8f; shootCone = 2f; range = 30f;
            }});
            playerControllable = true;
        }};

        greenHiveUnit = new UnitType("green-hive") {{
            health = 300f; speed = 1.2f; hitSize = 18f; flying = true;
            region = generateSpecialSprite("green-hive", 0xFF00AA44, 0xFFFFFF00, 11);
            weapons.add(new Weapon() {{
                reload = 2f; bullet = Bullets.standardDagger;
                x = 0f; y = 0f; shootCone = 360f; mirror = false; alternate = false; shots = 3;
            }});
            abilities.add(new SpawnerAbility(Bullets.explosive, 2, 60f, 0f));
            playerControllable = true;
        }};

        // ---------- 第8轮单位 (元宝) ----------
        goldPaladinUnit = new UnitType("gold-paladin") {{
            health = 500f; speed = 0.8f; hitSize = 16f; armor = 8f;
            region = generateSpecialSprite("gold-paladin", 0xFFFFD700, 0xFFFFFFFF, 12);
            weapons.add(new Weapon() {{ reload = 45f; bullet = Bullets.standardThorium; x = 0f; y = 6f; shootCone = 20f; }});
            abilities.add(new ForceFieldAbility(60f, 1.0f, 150f, 600f));
            playerControllable = true;
        }};

        silverRailgunUnit = new UnitType("silver-railgun") {{
            health = 200f; speed = 0.9f; hitSize = 12f; armor = 3f;
            region = generateSpecialSprite("silver-railgun", 0xFFC0C0C0, 0xFF00FFFF, 13);
            weapons.add(new Weapon() {{
                reload = 90f;
                bullet = new RailBulletType() {{ damage = 150f; length = 200f; pierce = true; }};
                x = 8f; y = 0f; rotate = true; recoil = 10f; shootCone = 1f; range = 40f;
            }});
            playerControllable = true;
        }};

        // ---------- 第9轮单位 (DeepSeek) ----------
        crimsonDemonUnit = new UnitType("crimson-demon") {{
            health = 180f; speed = 3.2f; hitSize = 9f; flying = true;
            region = generateSpecialSprite("crimson-demon", 0xFFCC0000, 0xFFFF8800, 14);
            weapons.add(new Weapon() {{
                reload = 20f; bullet = Bullets.incendiary;
                x = 0f; y = 2f; shots = 2; spread = 10f;
            }});
            abilities.add(new SpawnerAbility(Bullets.incendiary, 1, 10f, 0f));
            playerControllable = true;
        }};

        cobaltShielderUnit = new UnitType("cobalt-shielder") {{
            health = 280f; speed = 0.9f; hitSize = 14f; armor = 5f;
            region = generateSpecialSprite("cobalt-shielder", 0xFF0044CC, 0xFFFFFFFF, 15);
            weapons.add(new Weapon() {{ reload = 25f; bullet = Bullets.standardCopper; x = 5f; y = 0f; mirror = true; }});
            abilities.add(new ShieldRegenFieldAbility(50f, 20f, 100f, 400f));
            abilities.add(new ForceFieldAbility(50f, 0.5f, 120f, 500f));
            playerControllable = true;
        }};

        // ---------- 第10轮单位 (元宝 最终收尾) ----------
        // 20. 彩虹泰坦 Boss (史诗级融合单位)
        rainbowTitanUnit = new UnitType("rainbow-titan") {{
            health = 5000f; speed = 0.5f; hitSize = 28f; armor = 12f;
            region = generateSpecialSprite("rainbow-titan", 0xFFFF00FF, 0xFFFFFFFF, 16);
            // 三武器系统
            weapons.add(new Weapon() {{
                reload = 60f; bullet = Bullets.artilleryDense;
                x = 14f; y = 0f; rotate = true; recoil = 12f; shootCone = 10f; range = 35f;
            }});
            weapons.add(new Weapon() {{
                reload = 15f; bullet = Bullets.standardThorium;
                x = 8f; y = -6f; mirror = true; shootCone = 30f;
            }});
            weapons.add(new Weapon() {{
                reload = 8f; bullet = Bullets.flakScrap;
                x = 0f; y = 8f; shootCone = 360f; mirror = false;
            }});
            // 三重光环
            abilities.add(new RepairFieldAbility(80f, 30f, 80f));
            abilities.add(new ShieldRegenFieldAbility(40f, 80f, 60f, 500f));
            abilities.add(new ForceFieldAbility(80f, 1.5f, 200f, 800f));
            playerControllable = true;
        }};

        // 21. 翡翠指挥官 (升级中枢/全场增幅)
        emeraldCommanderUnit = new UnitType("emerald-commander") {{
            health = 600f; speed = 0.7f; hitSize = 20f; armor = 5f;
            region = generateSpecialSprite("emerald-commander", 0xFF00C853, 0xFF69F0AE, 17);
            // 弱自卫武器
            weapons.add(new Weapon() {{
                reload = 40f; bullet = Bullets.standardCopper;
                x = 6f; y = 0f; mirror = true;
            }});
            // 全场增幅光环
            abilities.add(new ShieldRegenFieldAbility(60f, 25f, 120f, 600f));
            abilities.add(new RepairFieldAbility(60f, 15f, 100f));
            playerControllable = true;
        }};

        // 注册所有 21 个单位（必须）
        content.register(greenGroundUnit);
        content.register(blueFlyUnit);
        content.register(redFactoryUnit);
        content.register(purpleNavalUnit);
        content.register(yellowMinerUnit);
        content.register(cyanSupportUnit);
        content.register(graySiegeUnit);
        content.register(whiteAntiAirUnit);
        content.register(pinkEWar);
        content.register(orangeSuicideUnit);
        content.register(blueSubmarineUnit);
        content.register(brownTransportUnit);
        content.register(blackReaperUnit);
        content.register(redSniperUnit);
        content.register(greenHiveUnit);
        content.register(goldPaladinUnit);
        content.register(silverRailgunUnit);
        content.register(crimsonDemonUnit);
        content.register(cobaltShielderUnit);
        content.register(rainbowTitanUnit);
        content.register(emeraldCommanderUnit);
    }
}
