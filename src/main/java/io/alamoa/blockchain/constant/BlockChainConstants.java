package io.alamoa.blockchain.constant;

public enum BlockChainConstants {

    // --- ブロック/トランザクションのフィールドキー (String) ---
    TIMESTAMP("timestamp"),
    TRANSACTIONS("transactions"),
    NONCE("nonce"),
    PREVIOUS_HASH("previous_hash"),

    // トランザクション内の値 (Value の定数)
    VALUE("value"),
    RECIPIENT_BLOCKCHAIN_ADDRESS("recipient_blockchain_address"),
    SENDER_BLOCKCHAIN_ADDRESS("sender_blockchain_address"),

    // --- マイニング設定値 (数値) ---
    DIFFICULTY(2),
    MINING_TIMER_SEC(20L), // long型として定義
    MINING_REWARD(10.0); // double型として定義

    private final Object value;

    /**
     * コンストラクタ (文字列定数用)
     */
    BlockChainConstants(String value) {
        this.value = value;
    }

    /**
     * コンストラクタ (整数/Double定数用)
     */
    BlockChainConstants(Number value) {
        this.value = value;
    }

    /**
     * Enum定数が保持する値をObjectとして取得します。
     * 呼び出し元で適切な型にキャストする必要があります。
     */
    public Object getValue() {
        return value;
    }

    /**
     * 値を String 型として取得するユーティリティメソッド。
     * (主にフィールドキー用)
     */
    public String getString() {
        if (value instanceof String) {
            return (String) value;
        }
        throw new IllegalStateException("定数 '" + this.name() + "' は String 型ではありません。");
    }

    /**
     * 値を Integer 型として取得するユーティリティメソッド。
     * (主に DIFFICULTY 用)
     */
    public Integer getInteger() {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        throw new IllegalStateException("定数 '" + this.name() + "' は Integer 型ではありません。");
    }

    /**
     * 値を long 型として取得するユーティリティメソッド。
     * (主に MINING_TIMER_SEC 用)
     */
    public long getLong() {
        if (value instanceof Long) {
            return (long) value;
        }
        throw new IllegalStateException("定数 '" + this.name() + "' は Long 型ではありません。");
    }

    /**
     * 値を double 型として取得するユーティリティメソッド。
     * (主に MINING_REWARD 用)
     */
    public double getDouble() {
        if (value instanceof Double) {
            return (double) value;
        }
        throw new IllegalStateException("定数 '" + this.name() + "' は Double 型ではありません。");
    }
}