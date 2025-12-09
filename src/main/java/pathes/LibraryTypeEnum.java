package pathes;

import com.evacipated.cardcrawl.modthespire.lib.SpireEnum;
import com.megacrit.cardcrawl.helpers.CardLibrary;
/**
 * 备注: 将角色定义的卡牌颜色TK_COLOR注册到CardLibrary,必须有。
 */
public class LibraryTypeEnum {
    @SpireEnum(name = "CHARACTER_GRAY_COLOR") @SuppressWarnings("unused")
    public static CardLibrary.LibraryType LIBRARY_COLOR;
}