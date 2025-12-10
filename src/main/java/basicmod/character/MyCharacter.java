package basicmod.character;

import basemod.BaseMod;
import basemod.abstracts.CustomEnergyOrb;
import basemod.abstracts.CustomPlayer;
import basemod.animations.SpriterAnimation;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpireEnum;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.blue.Defend_Blue;
import com.megacrit.cardcrawl.cards.green.Neutralize;
import com.megacrit.cardcrawl.cards.red.Strike_Red;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.EnergyManager;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.relics.BurningBlood;
import com.megacrit.cardcrawl.screens.CharSelectInfo;

import java.util.ArrayList;

import static basicmod.BasicMod.characterPath;
import static basicmod.BasicMod.makeID;

public class MyCharacter extends CustomPlayer {
    // 属性
    public static final int ENERGY_PER_TURN = 3;
    public static final int MAX_HP = 70;
    public static final int STARTING_GOLD = 99;
    public static final int CARD_DRAW = 5;
    public static final int ORB_SLOTS = 0;

    // 字符串
    private static final String ID = makeID("CharacterID"); // 这应该与 CharacterStrings.json 文件中的内容匹配
    private static String[] getNames() { return CardCrawlGame.languagePack.getCharacterString(ID).NAMES; }
    private static String[] getText() { return CardCrawlGame.languagePack.getCharacterString(ID).TEXT; }

    // 这个静态类是必要的，以避免在注册角色时 Java 类加载的某些特性
    public static class Meta {
        // 这些用于标识你的角色，以及角色的卡牌颜色
        // 库颜色基本上与卡牌颜色相同，但两者都需要，因为游戏就是这样设计的
        @SpireEnum
        public static PlayerClass YOUR_CHARACTER;
        @SpireEnum(name = "CHARACTER_GRAY_COLOR") // 这两个必须匹配。将其更改为你的角色独有的名称
        public static AbstractCard.CardColor CARD_COLOR;
        @SpireEnum(name = "CHARACTER_GRAY_COLOR") @SuppressWarnings("unused")
        public static CardLibrary.LibraryType LIBRARY_COLOR;

        // 角色选择界面图片
        private static final String CHAR_SELECT_BUTTON = characterPath("select/button.png");
        private static final String CHAR_SELECT_PORTRAIT = characterPath("select/portrait.png");

        // 角色卡牌图片
        private static final String BG_ATTACK = characterPath("cardback/bg_attack.png");
        private static final String BG_ATTACK_P = characterPath("cardback/bg_attack_p.png");
        private static final String BG_SKILL = characterPath("cardback/bg_skill.png");
        private static final String BG_SKILL_P = characterPath("cardback/bg_skill_p.png");
        private static final String BG_POWER = characterPath("cardback/bg_power.png");
        private static final String BG_POWER_P = characterPath("cardback/bg_power_p.png");
        private static final String ENERGY_ORB = characterPath("cardback/energy_orb.png");
        private static final String ENERGY_ORB_P = characterPath("cardback/energy_orb_p.png");
        private static final String SMALL_ORB = characterPath("cardback/small_orb.png");

        // 这用于为 *一些* 图片着色，但不是实际的卡牌。对于卡牌，请编辑 cardback 文件夹中的图片！
        private static final Color cardColor = new Color(128f/255f, 128f/255f, 128f/255f, 1f);

        // 将在主 mod 文件中使用的方法
        public static void registerColor() {
            BaseMod.addColor(CARD_COLOR, cardColor,
                    BG_ATTACK, BG_SKILL, BG_POWER, ENERGY_ORB,
                    BG_ATTACK_P, BG_SKILL_P, BG_POWER_P, ENERGY_ORB_P,
                    SMALL_ORB);
        }

        public static void registerCharacter() {
            BaseMod.addCharacter(new MyCharacter(), CHAR_SELECT_BUTTON, CHAR_SELECT_PORTRAIT);
        }
    }


    // 游戏内图片
    private static final String SHOULDER_1 = characterPath("shoulder.png"); // Shoulder 1 和 2 用于休息站点
    private static final String SHOULDER_2 = characterPath("shoulder2.png");
    private static final String CORPSE = characterPath("corpse.png"); // Corpse 是当你死亡时

    // 用于能量球的纹理
    private static final String[] orbTextures = {
            characterPath("energyorb/layer1.png"), // 当你有能量时
            characterPath("energyorb/layer2.png"),
            characterPath("energyorb/layer3.png"),
            characterPath("energyorb/layer4.png"),
            characterPath("energyorb/layer5.png"),
            characterPath("energyorb/cover.png"), // "容器"
            characterPath("energyorb/layer1d.png"), // 当你没有能量时
            characterPath("energyorb/layer2d.png"),
            characterPath("energyorb/layer3d.png"),
            characterPath("energyorb/layer4d.png"),
            characterPath("energyorb/layer5d.png")
    };

    // 能量球纹理每一层的旋转速度。负值表示向后旋转
    private static final float[] layerSpeeds = new float[] {
            -20.0F,
            20.0F,
            -40.0F,
            40.0F,
            360.0F
    };


    // 实际的角色类代码从此处开始

    public MyCharacter() {
        super(getNames()[0], Meta.YOUR_CHARACTER,
                new CustomEnergyOrb(orbTextures, characterPath("energyorb/vfx.png"), layerSpeeds), // 能量球
                new SpriterAnimation(characterPath("animation/default.scml"))); // 动画

        initializeClass(null,
                SHOULDER_2,
                SHOULDER_1,
                CORPSE,
                getLoadout(),
                20.0F, -20.0F, 200.0F, 250.0F, // 角色碰撞箱。x y 位置，然后是宽度和高度
                new EnergyManager(ENERGY_PER_TURN));

        // 文本气泡的位置。你可以在之后根据需要调整它。对于大多数角色，这些值是可以的
        dialogX = (drawX + 0.0F * Settings.scale);
        dialogY = (drawY + 220.0F * Settings.scale);
    }

    @Override
    public ArrayList<String> getStartingDeck() {
        ArrayList<String> retVal = new ArrayList<>();
        // 你的起始卡组中卡牌的 ID 列表
        // 如果你想要多张相同的卡牌，你必须多次添加它
        retVal.add(Strike_Red.ID);
        retVal.add(Strike_Red.ID);
        retVal.add(Defend_Blue.ID);
        retVal.add(Defend_Blue.ID);
        retVal.add(Neutralize.ID);

        return retVal;
    }

    @Override
    public ArrayList<String> getStartingRelics() {
        ArrayList<String> retVal = new ArrayList<>();
        // 起始遗物的 ID。你可以有多个，但建议只有一个
        retVal.add(BurningBlood.ID);

        return retVal;
    }

    @Override
    public AbstractCard getStartCardForEvent() {
        // 这张卡用于地精卡牌匹配游戏
        // 它应该是一张非打击非防御的起始卡牌，但不一定必须是
        return new Strike_Red();
    }

    /*- 以下是你可能 *应该* 调整，但不一定必须调整的方法 -*/

    @Override
    public int getAscensionMaxHPLoss() {
        return 4; // 在进阶 14+ 时的最大生命值减少
    }

    @Override
    public AbstractGameAction.AttackEffect[] getSpireHeartSlashEffect() {
        // 当你攻击心脏时将使用这些攻击效果
        return new AbstractGameAction.AttackEffect[] {
                AbstractGameAction.AttackEffect.SLASH_VERTICAL,
                AbstractGameAction.AttackEffect.SLASH_HEAVY,
                AbstractGameAction.AttackEffect.BLUNT_HEAVY
        };
    }

    private final Color cardRenderColor = Color.LIGHT_GRAY.cpy(); // 用于移动卡牌时的一些视觉效果（有时）（可能）
    private final Color cardTrailColor = Color.LIGHT_GRAY.cpy(); // 用于游戏过程中卡牌轨迹的视觉效果
    private final Color slashAttackColor = Color.LIGHT_GRAY.cpy(); // 用于攻击心脏时的屏幕色调效果
    @Override
    public Color getCardRenderColor() {
        return cardRenderColor;
    }

    @Override
    public Color getCardTrailColor() {
        return cardTrailColor;
    }

    @Override
    public Color getSlashAttackColor() {
        return slashAttackColor;
    }

    @Override
    public BitmapFont getEnergyNumFont() {
        // 用于显示你当前能量的字体
        // energyNumFontRed、Blue、Green 和 Purple 由基础游戏角色使用
        // 可以创建你自己的，但不方便
        return FontHelper.energyNumFontRed;
    }

    @Override
    public void doCharSelectScreenSelectEffect() {
        // 当你在角色选择界面点击角色的按钮时发生
        // 查看 SoundMaster 以获取现有音效的完整列表，或查看 BaseMod 的 wiki 以添加自定义音频
        CardCrawlGame.sound.playA("ATTACK_DAGGER_2", MathUtils.random(-0.2F, 0.2F));
        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.MED, ScreenShake.ShakeDur.SHORT, false);
    }
    @Override
    public String getCustomModeCharacterButtonSoundKey() {
        // 类似于 doCharSelectScreenSelectEffect，但用于自定义模式界面。没有震动
        return "ATTACK_DAGGER_2";
    }

    // 不要直接调整这四个，调整 CharacterStrings.json 文件的内容
    @Override
    public String getLocalizedCharacterName() {
        return getNames()[0];
    }
    @Override
    public String getTitle(PlayerClass playerClass) {
        return getNames()[1];
    }
    @Override
    public String getSpireHeartText() {
        return getText()[1];
    }
    @Override
    public String getVampireText() {
        return getText()[2]; // 通常，这段文本的唯一区别是吸血鬼如何称呼玩家
    }

    /*- 你不应该需要编辑以下任何方法 -*/

    // 这用于在角色选择界面上显示角色的信息
    @Override
    public CharSelectInfo getLoadout() {
        return new CharSelectInfo(getNames()[0], getText()[0],
                MAX_HP, MAX_HP, ORB_SLOTS, STARTING_GOLD, CARD_DRAW, this,
                getStartingRelics(), getStartingDeck(), false);
    }

    @Override
    public AbstractCard.CardColor getCardColor() {
        return Meta.CARD_COLOR;
    }

    @Override
    public AbstractPlayer newInstance() {
        // 创建你的角色类的新实例
        return new MyCharacter();
    }
}
