package io.alamoa.blockchain.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.alamoa.blockchain.Utils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Component
public class Blockchain {

    private static final String TIMESTAMP = "timestamp";
    private static final String TRANSACTIONS = "transactions";
    private static final String NONCE = "nonce";
    private static final String PREVIOUS_HASH = "previous_hash";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final List<Map<String, Object>> transactionPool;
    private final List<Map<String, Object>> chain = new ArrayList<>();

    public Blockchain() {
        this.transactionPool = new ArrayList<>();
        this.chain.add(createBlock(0, "first block"));
    }

    public Map<String, Object> createBlock(int nonce, String previousHash) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put(TIMESTAMP, System.currentTimeMillis());
        List<Map<String, Object>> blockTransactions = new ArrayList<>(this.transactionPool);
        block.put(TRANSACTIONS, blockTransactions);
        block.put(NONCE, nonce);
        block.put(PREVIOUS_HASH, previousHash);
        this.transactionPool.clear();
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

    public boolean addTransaction(String senderBlockchainAddress,
                                  String recipientBlockchainAddress, double value) {
        Transaction transaction = new Transaction(senderBlockchainAddress, recipientBlockchainAddress, value);
        transactionPool.add(transaction.toMap());
        return true;
    }

    public void mine() {
        Map<String, Object> newBlock = this.chain.get(this.chain.size() - 1);
        this.chain.add(createBlock(0, changeToHash(newBlock)));
    }

    public List<Map<String, Object>> getChain() {
        return this.chain;
    }

    public  List<Map<String, Object>> getTransactionPool(){
        return this.transactionPool;
    }

}
