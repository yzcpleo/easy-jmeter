package io.github.guojiaxing1995.easyJmeter.common.enumeration;


import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * @author colorful@TaleLin
 * @author Juzi@TaleLin
 */
public enum GroupLevelEnum implements IEnum<String> {
    /**
     * 超级管理员
     */
    ROOT("1"),
    /**
     * 游客
     */
    GUEST("2"),
    /**
     * 普通用户
     */
    USER("3"),
    /**
     * 未知/无效值，用于处理数据库中的无效枚举值
     */
    UNKNOWN("0");

    private final String value;

    GroupLevelEnum(String value) {
        this.value = value;
    }

    /**
     * MybatisEnumTypeHandler 转换时调用此方法
     *
     * @return 枚举对应的 code 值
     * @see com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler
     */
    @Override
    public String getValue() {
        return this.value;
    }

    /**
     * 根据值获取枚举，如果值无效则返回 UNKNOWN
     *
     * @param value 枚举值
     * @return 对应的枚举，无效值返回 UNKNOWN
     */
    public static GroupLevelEnum fromValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        for (GroupLevelEnum level : values()) {
            if (level.getValue().equals(value)) {
                return level;
            }
        }
        return UNKNOWN;
    }

}
