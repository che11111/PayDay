package dev.che1s.payday;

/**
 * 可购买的天气/时间类型。
 * 每种类型对应一个指令、一个权限节点（payday.&lt;key&gt;）、
 * 一个语言键（type.&lt;key&gt;）和一份价格配置（costs.&lt;key&gt;）。
 */
public enum PurchaseType {

    DAY("day", "/payday"),
    NOON("noon", "/paynoon"),
    CLEAR("clear", "/payweather"),
    RAIN("rain", "/payrain"),
    STORM("storm", "/paystorm"),
    NIGHT("night", "/paynight"),
    DAWN("dawn", "/paydawn"),
    DUSK("dusk", "/paydusk");

    private final String key;
    private final String command;

    PurchaseType(String key, String command) {
        this.key = key;
        this.command = command;
    }

    public String getKey() {
        return key;
    }

    public String getCommand() {
        return command;
    }

    public String getTranslationKey() {
        return "type." + key;
    }

    public String getPermission() {
        if ("clear".equals(key) || "rain".equals(key) || "storm".equals(key)) {
            return "payday.weather";
        }
        return "payday." + key;
    }

    public boolean isTimePurchase() {
        return this == DAY || this == NOON || this == NIGHT
                || this == DAWN || this == DUSK;
    }

    /**
     * 根据指令名（如 "payday"、"payrain"）查找对应的购买类型。
     *
     * @return 匹配的类型，未匹配返回 null
     */
    public static PurchaseType fromCommandName(String commandName) {
        if (commandName == null) {
            return null;
        }
        for (PurchaseType type : values()) {
            if (type.command.substring(1).equalsIgnoreCase(commandName)) {
                return type;
            }
        }
        return null;
    }
}
