package basicmod;

import basemod.BaseMod;
import basemod.interfaces.EditCharactersSubscriber;
import basemod.interfaces.EditKeywordsSubscriber;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import basicmod.character.Demo;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;

@SpireInitializer
public class Basicmod implements
        EditStringsSubscriber,
        EditKeywordsSubscriber,
        PostInitializeSubscriber,
        EditCharactersSubscriber{
    public Basicmod() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new Basicmod();
        Demo.Meta.registerColor();//设置卡牌颜色
    }

    @Override
    public void receiveEditKeywords() {

    }

    @Override
    public void receiveEditStrings() {

    }

    @Override
    public void receivePostInitialize() {

    }

    @Override
    public void receiveEditCharacters() {
        Demo.Meta.registerCharacter();
    }
}

