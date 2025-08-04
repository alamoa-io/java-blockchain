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
    private static final String VALUE = "value";
    private final String RECIPIENT_BLOCKCHAIN_ADDRESS = "recipient_blockchain_address";
    private final String SENDER_BLOCKCHAIN_ADDRESS = "sender_blockchain_address";

    private static final Integer DIFFICULTY = 2;

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
        int nonce = proofOfWork();
        this.chain.add(createBlock(nonce, changeToHash(newBlock)));
    }

    public boolean validProof(List<Map<String, Object>> transaction, String previousHash, int nonce, int difficulty) {
        Map<String, Object> guessBlock = new LinkedHashMap<>();
        guessBlock.put(TRANSACTIONS, transaction);
        guessBlock.put(PREVIOUS_HASH, previousHash);
        guessBlock.put(NONCE, nonce);
        guessBlock = Utils.sortedMapByKey(guessBlock);
        String guessHash = changeToHash(guessBlock);
        return guessHash != null && guessHash.startsWith("0".repeat(difficulty));
    }

    public int proofOfWork() {
        List<Map<String, Object>> copiedTransactionPool = new ArrayList<>(this.transactionPool);
        String previousHash = changeToHash(this.chain.get(this.chain.size() - 1));
        int nonce = 0;
        while (!validProof(copiedTransactionPool, previousHash, nonce, DIFFICULTY)) {
            nonce++;
        }
        return nonce;
    }

    public double calculateTotalAmount(String blockchainAddress) {
        return chain.stream()// リストをstreamに変換
                //各ブロックの中のtransactionsの内容を取得し、一つのstreamにまとめる
                .flatMap(block -> ((List<Map<String, Object>>) block.get(TRANSACTIONS)).stream())
                //引数のblockchainAddressに該当する送り手、もしくは受け取り側のtransactionのみを対象とする
                .filter(transaction -> transaction.get(SENDER_BLOCKCHAIN_ADDRESS).equals(blockchainAddress) ||
                        transaction.get(RECIPIENT_BLOCKCHAIN_ADDRESS).equals(blockchainAddress))
                //値を取り出し送り手側なら減算、受け取り側なら加算を行う
                .mapToDouble(transaction -> {
                    double value = Double.parseDouble((String) transaction.get(VALUE));
                    return transaction.get(SENDER_BLOCKCHAIN_ADDRESS).equals(blockchainAddress) ? -value : value;
                })
                .sum();//合計値を出す
    }

    public List<Map<String, Object>> getChain() {
        return this.chain;
    }

    public List<Map<String, Object>> getTransactionPool() {
        return this.transactionPool;
    }

}
