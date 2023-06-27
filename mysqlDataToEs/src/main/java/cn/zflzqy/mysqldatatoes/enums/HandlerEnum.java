package cn.zflzqy.mysqldatatoes.enums;

/**
 * 枚举类 HandlerEnum，表示两种状态：增量和全量。
 * 增量的值为0，全量值为1。
 */
public enum HandlerEnum {
    // 增量
    INCREMENTAL(0),
    // 全量
    FULL(1);

    // 表示状态的整数值
    private final int value;

    /**
     * 构造函数，初始化状态的整数值。
     *
     * @param value 状态的整数值
     */
    HandlerEnum(int value) {
        this.value = value;
    }

    /**
     * 获取状态的整数值。
     *
     * @return 状态的整数值
     */
    public int getValue() {
        return value;
    }

    /**
     * 根据整数值获取相应的状态。
     *
     * @param value 要查找的整数值
     * @return 对应的状态，如果找不到则抛出异常
     */
    public static HandlerEnum fromValue(int value) {
        for (HandlerEnum handlerEnum : HandlerEnum.values()) {
            if (handlerEnum.getValue() == value) {
                return handlerEnum;
            }
        }
        throw new IllegalArgumentException("Invalid value: " + value);
    }

    /**
     * 根据传入的整数值，返回相对的状态。
     * 如果传入的值对应增量，则返回全量；如果传入的值对应全量，则返回增量。
     *
     * @param value 要转换的状态的整数值
     * @return 转换后的状态
     */
    public static HandlerEnum toggle(int value) {
        return fromValue(value) == INCREMENTAL ? FULL : INCREMENTAL;
    }
}
