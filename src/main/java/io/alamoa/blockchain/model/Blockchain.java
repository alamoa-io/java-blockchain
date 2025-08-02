package io.alamoa.blockchain.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.alamoa.blockchain.Utils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class Blockchain {

    private static final String TIMESTAMP = "timestamp";
    private static final String TRANSACTIONS = "transactions";
    private static final String NONCE = "nonce";
    private static final String PREVIOUS_HASH = "previous_hash";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public Map<String, Object> createBlock(int nonce, String previousHash) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put(TIMESTAMP, System.currentTimeMillis());
        block.put(TRANSACTIONS, new ArrayList<>());
        block.put(NONCE, nonce);
        block.put(PREVIOUS_HASH, previousHash);
        return Utils.sortedMapByKey(block);
    }

    public String changeToHash(Map<String, Object> block) {
        // 1. ブロックの内容をJSON文字列に変換
        String sortedBlockJson = gson.toJson(Utils.sortedMapByKey(block));
        // 2. SHA-256ダイジェストを生成
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        // 3. JSON文字列のバイト配列をハッシュ化
        byte[] hashBytes = digest.digest(sortedBlockJson.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashBytes);
    }
}
