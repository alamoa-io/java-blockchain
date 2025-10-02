package io.alamoa.blockchain.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.alamoa.blockchain.Utils;
import io.alamoa.blockchain.constant.BlockChainConstants;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

@Component
public class BlockchainLogic {
  private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  public boolean verifyTransactionSignature(
      PublicKey senderPublicKey, String signatureHex, Map<String, Object> transaction)
      throws NoSuchAlgorithmException,
          NoSuchProviderException,
          InvalidKeySpecException,
          InvalidKeyException,
          SignatureException {
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
    signature.update(message); // ハッシュ対象のメッセージをセット

    // 署名文字列をバイト配列に変換して検証
    byte[] signatureBytes = HexFormat.of().parseHex(signatureHex);
    return signature.verify(signatureBytes); // 署名を検証
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

  public boolean validProof(
      List<Map<String, Object>> transaction, String previousHash, int nonce, int difficulty) {
    Map<String, Object> guessBlock = new LinkedHashMap<>();
    guessBlock.put(BlockChainConstants.TRANSACTIONS.getString(), transaction);
    guessBlock.put(BlockChainConstants.PREVIOUS_HASH.getString(), previousHash);
    guessBlock.put(BlockChainConstants.NONCE.getString(), nonce);
    guessBlock = Utils.sortedMapByKey(guessBlock);
    String guessHash = changeToHash(guessBlock);
    return guessHash != null && guessHash.startsWith("0".repeat(difficulty));
  }

  public int proofOfWork(
      List<Map<String, Object>> transactionPool, List<Map<String, Object>> chain) {
    List<Map<String, Object>> copiedTransactionPool = new ArrayList<>(transactionPool);
    String previousHash = changeToHash(chain.get(chain.size() - 1));
    int nonce = 0;
    while (!validProof(
        copiedTransactionPool, previousHash, nonce, BlockChainConstants.DIFFICULTY.getInteger())) {
      nonce++;
    }
    return nonce;
  }

  public boolean validChain(List<Map<String, Object>> chain) {
    if (chain == null || chain.isEmpty()) return false;
    Map<String, Object> preBlock = chain.get(0);
    int currentIndex = 1;
    while (currentIndex < chain.size()) {
      Map<String, Object> block = chain.get(currentIndex);
      String previousHash = (String) block.get(BlockChainConstants.PREVIOUS_HASH.getString());
      String calculatedHash = changeToHash(preBlock);
      if (!previousHash.equals(calculatedHash)) {
        return false;
      }
      List<Map<String, Object>> transactions =
          (List<Map<String, Object>>) block.get(BlockChainConstants.TRANSACTIONS.getString());
      int nonce = (int) block.get(BlockChainConstants.NONCE.getString());
      if (!validProof(
          transactions, previousHash, nonce, BlockChainConstants.DIFFICULTY.getInteger())) {
        return false;
      }
      preBlock = block;
      currentIndex++;
    }
    return true;
  }
}
