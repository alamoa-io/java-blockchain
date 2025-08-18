package io.alamoa.blockchain.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.alamoa.blockchain.Utils;
import io.alamoa.blockchain.entity.TransactionRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.*;

@Component
public class Blockchain {

    @Autowired
    NeighbourDiscovery neighbourDiscovery;
    @Autowired
    RestTemplate restTemplate;

    private static final String TIMESTAMP = "timestamp";
    private static final String TRANSACTIONS = "transactions";
    private static final String NONCE = "nonce";
    private static final String PREVIOUS_HASH = "previous_hash";
    private static final String VALUE = "value";
    private final String RECIPIENT_BLOCKCHAIN_ADDRESS = "recipient_blockchain_address";
    private final String SENDER_BLOCKCHAIN_ADDRESS = "sender_blockchain_address";

    private static final Integer DIFFICULTY = 2;
    private static final long MINING_TIMER_SEC = 20;
    private static final double MINING_REWARD = 10.0;

    private final Semaphore miningSemaphore = new Semaphore(1);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final List<Map<String, Object>> transactionPool;
    private final List<Map<String, Object>> chain = new ArrayList<>();
    private final String minerBlockchainAddress;

    public Blockchain() {
        this.transactionPool = new ArrayList<>();
        try {
            Wallet minerWallet = new Wallet();
            System.out.println("Miners Private Key: " + minerWallet.getPrivateKey());
            System.out.println("Miners Public Key: " + minerWallet.getPublicKey());
            System.out.println("Miners Blockchain Address: " + minerWallet.getBlockchainAddress());
            this.minerBlockchainAddress = minerWallet.getBlockchainAddress();
            addTransaction("REWARD!!", minerWallet.getBlockchainAddress(), MINING_REWARD);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void init() {
        this.chain.add(createBlock(0, "first block"));
        startMiningLoop();
    }

    public void startMining() {
        // tryAcquire()で非ブロッキングにセマフォを取得
        if (miningSemaphore.tryAcquire()) {
            try {
                // マイニングロジックを呼び出す
                mine();
            } finally {
                // 必ずセマフォを解放する
                miningSemaphore.release();
            }
        }
    }

    public void startMiningLoop() {
        // 定期的に startMining メソッドを実行するようにスケジューリング
        scheduler.scheduleAtFixedRate(this::startMining, 0, MINING_TIMER_SEC, TimeUnit.SECONDS);
    }

    public boolean deleteTransactionPool() {
        this.transactionPool.clear();
        return true;
    }

    public Map<String, Object> createBlock(int nonce, String previousHash) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put(TIMESTAMP, System.currentTimeMillis());
        List<Map<String, Object>> blockTransactions = new ArrayList<>(this.transactionPool);
        block.put(TRANSACTIONS, blockTransactions);
        block.put(NONCE, nonce);
        block.put(PREVIOUS_HASH, previousHash);
        this.transactionPool.clear();
        for (Object neighbour : neighbourDiscovery.getNeighbours().keySet()) {
            String neighbourAddress = (String) neighbour;
            restTemplate.delete("http://" + neighbourAddress + "/transactions");
        }
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

    public void addTransactionToNeighbour(TransactionRequest transactionRequest) {
        for (Object neighbour : neighbourDiscovery.getNeighbours().keySet()) {
            String neighbourAddress = (String) neighbour;
            restTemplate.put("http://" + neighbourAddress + "/transactions", transactionRequest);
        }
    }

    public boolean validChain(List<Map<String, Object>> chain) {
        if (chain == null || chain.isEmpty()) return false;
        Map<String, Object> preBlock = chain.get(0);
        int currentIndex = 1;
        while (currentIndex < chain.size()) {
            Map<String, Object> block = chain.get(currentIndex);
            String previousHash = (String) block.get(PREVIOUS_HASH);
            String calculatedHash = changeToHash(preBlock);
            if (!previousHash.equals(calculatedHash)) {
                return false;
            }
            List<Map<String, Object>> transactions = (List<Map<String, Object>>) block.get(TRANSACTIONS);
            int nonce = (int) block.get(NONCE);
            if (!validProof(transactions, previousHash, nonce, DIFFICULTY)) {
                return false;
            }
            preBlock = block;
            currentIndex++;
        }
        return true;
    }

    public boolean resolveConflicts() {
        List<Map<String, Object>> longestChain = null;
        int maxLength = this.chain.size();
        for (Object neighbour : neighbourDiscovery.getNeighbours().keySet()) {
            String neighbourAddress = (String) neighbour;
            try {
                List<Map<String, Object>> neighbourChain = restTemplate.getForObject(
                        "http://" + neighbourAddress + "/chain", List.class);

                if (neighbourChain != null) {
                    int chainLength = neighbourChain.size();
                    if (chainLength > maxLength && validChain(neighbourChain)) {
                        maxLength = chainLength;
                        longestChain = neighbourChain;
                    }
                }
            } catch (Exception e) {
                System.out.println("Error while resolving conflicts with neighbour: " + neighbourAddress);
                e.printStackTrace();
            }

        }

        if (longestChain != null) {
            this.chain.clear();
            this.chain.addAll(longestChain);
            return true;
        }
        return false;
    }

    public void mine() {
        Map<String, Object> newBlock = this.chain.get(this.chain.size() - 1);
        addTransaction("REWARD!!", minerBlockchainAddress, MINING_REWARD);
        int nonce = proofOfWork();
        this.chain.add(createBlock(nonce, changeToHash(newBlock)));
        for (Object neighbour : neighbourDiscovery.getNeighbours().keySet()) {
            String neighbourAddress = (String) neighbour;
            restTemplate.put("http://" + neighbourAddress + "/consensus", "check consensus");
        }

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

    public boolean verifyTransactionSignature(PublicKey senderPublicKey,
                                              String signatureHex, Map<String, Object> transaction) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        Map<String, Object> sortedTransaction = Utils.sortedMapByKey(transaction);
        String transactionJson = gson.toJson(sortedTransaction);
        // SHA-256 ハッシュアルゴリズムを使用
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.update(transactionJson.getBytes(StandardCharsets.UTF_8)); // UTF-8エンコーディングでデータを更新
        byte[] message = sha256.digest(); // ハッシュ値を計算

        byte[] publicKeyBytes = senderPublicKey.getEncoded();
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        // ECDSAによる署名検証
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initVerify(publicKey); // 公開鍵で署名器を初期化
        signature.update(message);     // ハッシュ対象のメッセージをセット

        // 署名文字列をバイト配列に変換して検証
        byte[] signatureBytes = HexFormat.of().parseHex(signatureHex);
        return signature.verify(signatureBytes); // 署名を検証
    }

    public List<Map<String, Object>> getChain() {
        return this.chain;
    }

    public List<Map<String, Object>> getTransactionPool() {
        return this.transactionPool;
    }

}
