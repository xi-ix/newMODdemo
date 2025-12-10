package basicmod;

import basemod.BaseMod;
import basemod.interfaces.*;
import basicmod.character.MyCharacter;
import basicmod.util.GeneralUtils;
import basicmod.util.KeywordInfo;
import basicmod.util.Sounds;
import basicmod.util.TextureLoader;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglFileHandle;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.Patcher;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.scannotation.AnnotationDB;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SpireInitializer
public class BasicMod implements
        EditCharactersSubscriber,
        EditStringsSubscriber,
        EditKeywordsSubscriber,
        AddAudioSubscriber,
        PostInitializeSubscriber {
    public static ModInfo info;
    public static String modID; // 编辑你的 pom.xml 来更改这个
    static { loadModInfo(); }
    private static final String resourcesFolder = checkResourcesPath();
    public static final Logger logger = LogManager.getLogger(modID); // 用于输出到控制台

    // 这用于为各种对象（如卡牌和遗物）的 ID 添加前缀，
    // 以避免不同模组使用相同名称时产生冲突
    public static String makeID(String id) {
        return modID + ":" + id;
    }

    // 由于类顶部的 @SpireInitializer 注解，ModTheSpire 会调用这个方法
    public static void initialize() {
        new BasicMod();

        MyCharacter.Meta.registerColor();
    }

    public BasicMod() {
        BaseMod.subscribe(this); // 这将使 BaseMod 在适当的时间触发所有订阅者
        logger.info(modID + " 已订阅到 BaseMod");
    }

    @Override
    public void receivePostInitialize() {
        // 这加载了在游戏内模组菜单中用作图标的图片
        Texture badgeTexture = TextureLoader.getTexture(imagePath("badge.png"));
        // 设置游戏内模组菜单中显示的模组信息
        // 使用的信息取自你的 pom.xml 文件

        // 如果你想设置配置面板，将在这里完成
        // 你可以在 BaseMod 维基页面 "Mod Config and Panel" 上找到相关信息
        BaseMod.registerModBadge(badgeTexture, info.Name, GeneralUtils.arrToString(info.Authors), info.Description, null);
    }

    /*----------本地化----------*/

    // 这用于根据语言加载适当的本地化文件
    private static String getLangString()
    {
        return Settings.language.name().toLowerCase();
    }
    private static final String defaultLanguage = "eng";

    public static final Map<String, KeywordInfo> keywords = new HashMap<>();

    @Override
    public void receiveEditStrings() {
        /*
            首先，加载默认本地化
            然后，如果当前语言不同，尝试加载该语言的本地化
            这导致默认本地化用于任何可能缺失的内容
            稍后使用相同的过程加载关键词
        */
        loadLocalization(defaultLanguage); // 默认本地化不捕获异常；你最好至少有一个可用的
        if (!defaultLanguage.equals(getLangString())) {
            try {
                loadLocalization(getLangString());
            }
            catch (GdxRuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadLocalization(String lang) {
        // 虽然这加载了所有类型的本地化，但这些文件大多只是大纲，以便你查看它们的格式
        // 你可以注释掉/删除任何你最终不使用的文件
        BaseMod.loadCustomStringsFile(CardStrings.class,
                localizationPath(lang, "CardStrings.json"));
        BaseMod.loadCustomStringsFile(CharacterStrings.class,
                localizationPath(lang, "CharacterStrings.json"));
        BaseMod.loadCustomStringsFile(EventStrings.class,
                localizationPath(lang, "EventStrings.json"));
        BaseMod.loadCustomStringsFile(OrbStrings.class,
                localizationPath(lang, "OrbStrings.json"));
        BaseMod.loadCustomStringsFile(PotionStrings.class,
                localizationPath(lang, "PotionStrings.json"));
        BaseMod.loadCustomStringsFile(PowerStrings.class,
                localizationPath(lang, "PowerStrings.json"));
        BaseMod.loadCustomStringsFile(RelicStrings.class,
                localizationPath(lang, "RelicStrings.json"));
        BaseMod.loadCustomStringsFile(UIStrings.class,
                localizationPath(lang, "UIStrings.json"));
    }

    @Override
    public void receiveEditKeywords()
    {
        Gson gson = new Gson();
        String json = Gdx.files.internal(localizationPath(defaultLanguage, "Keywords.json")).readString(String.valueOf(StandardCharsets.UTF_8));
        KeywordInfo[] keywords = gson.fromJson(json, KeywordInfo[].class);
        for (KeywordInfo keyword : keywords) {
            keyword.prep();
            registerKeyword(keyword);
        }

        if (!defaultLanguage.equals(getLangString())) {
            try
            {
                json = Gdx.files.internal(localizationPath(getLangString(), "Keywords.json")).readString(String.valueOf(StandardCharsets.UTF_8));
                keywords = gson.fromJson(json, KeywordInfo[].class);
                for (KeywordInfo keyword : keywords) {
                    keyword.prep();
                    registerKeyword(keyword);
                }
            }
            catch (Exception e)
            {
                logger.warn(modID + " 不支持 " + getLangString() + " 关键词");
            }
        }
    }

    private void registerKeyword(KeywordInfo info) {
        BaseMod.addKeyword(modID.toLowerCase(), info.PROPER_NAME, info.NAMES, info.DESCRIPTION, info.COLOR);
        if (!info.ID.isEmpty())
        {
            keywords.put(info.ID, info);
        }
    }

    @Override
    public void receiveEditCharacters() {
        MyCharacter.Meta.registerCharacter();
    }

    @Override
    public void receiveAddAudio() {
        loadAudio(Sounds.class);
    }

    private static final String[] AUDIO_EXTENSIONS = { ".ogg", ".wav", ".mp3" }; // 还有更多有效类型，但在这里检查所有类型并不值得
    private void loadAudio(Class<?> cls) {
        try {
            Field[] fields = cls.getDeclaredFields();
            outer:
            for (Field f : fields) {
                int modifiers = f.getModifiers();
                if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers) && f.getType().equals(String.class)) {
                    String s = (String) f.get(null);
                    if (s == null) { // 如果未定义值，则使用字段名确定路径
                        s = audioPath(f.getName());

                        for (String ext : AUDIO_EXTENSIONS) {
                            String testPath = s + ext;
                            if (Gdx.files.internal(testPath).exists()) {
                                s = testPath;
                                BaseMod.addAudio(s, s);
                                f.set(null, s);
                                continue outer;
                            }
                        }
                        throw new Exception("未能在 " + resourcesFolder + "/audio 中找到音频文件 \"" + f.getName() + "\"；请检查大小写和文件名是否正确");
                    }
                    else { // 否则，加载定义的路径
                        if (Gdx.files.internal(s).exists()) {
                            BaseMod.addAudio(s, s);
                        }
                        else {
                            throw new Exception("未找到音频文件 \"" + s + "\"；请检查这是否是正确的文件路径");
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error("在 loadAudio 中发生异常：", e);
        }
    }

    // 这些方法用于生成到资源文件夹中各个部分的正确文件路径
    public static String localizationPath(String lang, String file) {
        return resourcesFolder + "/localization/" + lang + "/" + file;
    }

    public static String audioPath(String file) {
        return resourcesFolder + "/audio/" + file;
    }
    public static String imagePath(String file) {
        return resourcesFolder + "/images/" + file;
    }
    public static String characterPath(String file) {
        return resourcesFolder + "/images/character/" + file;
    }
    public static String powerPath(String file) {
        return resourcesFolder + "/images/powers/" + file;
    }
    public static String relicPath(String file) {
        return resourcesFolder + "/images/relics/" + file;
    }

    /**
     * 根据包名检查预期的资源路径
     */
    private static String checkResourcesPath() {
        String name = BasicMod.class.getName(); // 使用类名而不是 getPackage，因为打补丁时 getPackage 可能不可靠
        int separator = name.indexOf('.');
        if (separator > 0)
            name = name.substring(0, separator);

        FileHandle resources = new LwjglFileHandle(name, Files.FileType.Internal);

        if (!resources.exists()) {
            throw new RuntimeException("\n\t未找到资源文件夹；预期它在 \"resources/" + name + "\"" +
                    " 请确保 resources 下的文件夹与你的模组包名相同，或者更改\n" +
                    "\t\"private static final String resourcesFolder = checkResourcesPath();\"\n" +
                    "\t在 " + BasicMod.class.getSimpleName() + " java 文件顶部的这一行");
        }
        if (!resources.child("images").exists()) {
            throw new RuntimeException("\n\t未在模组的 'resources/" + name + "' 文件夹中找到 'images' 文件夹；请确保 " +
                    "images 文件夹在正确的位置");
        }
        if (!resources.child("localization").exists()) {
            throw new RuntimeException("\n\t未在模组的 'resources/" + name + "' 文件夹中找到 'localization' 文件夹；请确保 " +
                    "localization 文件夹在正确的位置");
        }

        return name;
    }

    /**
     * 这根据 ModTheSpire 存储的信息确定模组的 ID
     */
    private static void loadModInfo() {
        Optional<ModInfo> infos = Arrays.stream(Loader.MODINFOS).filter((modInfo)->{
            AnnotationDB annotationDB = Patcher.annotationDBMap.get(modInfo.jarURL);
            if (annotationDB == null)
                return false;
            Set<String> initializers = annotationDB.getAnnotationIndex().getOrDefault(SpireInitializer.class.getName(), Collections.emptySet());
            return initializers.contains(BasicMod.class.getName());
        }).findFirst();
        if (infos.isPresent()) {
            info = infos.get();
            modID = info.ID;
        }
        else {
            throw new RuntimeException("基于初始化器未能确定模组信息/ID");
        }
    }
}
