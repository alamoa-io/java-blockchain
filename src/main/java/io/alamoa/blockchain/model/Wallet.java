package io.alamoa.blockchain.model;

import io.alamoa.blockchain.Utils;
import org.bitcoinj.base.Base58;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.util.encoders.Hex;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.HexFormat;

public class Wallet {
  private final String privateKey;
  private final String publicKey;
  private final String blockchainAddress;

  public Wallet() throws Exception {
    ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
    KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
    g.initialize(ecSpec, new SecureRandom());
    KeyPair keyPair = g.generateKeyPair();

    this.blockchainAddress = generateBlockchainAddress(keyPair.getPublic());
    this.privateKey = HexFormat.of().formatHex(keyPair.getPrivate().getEncoded());
    this.publicKey = HexFormat.of().formatHex(keyPair.getPublic().getEncoded());
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public String getBlockchainAddress() {
    return blockchainAddress;
  }

  private String generateBlockchainAddress(PublicKey publicKey) throws Exception {
    // sha-256ハッシュ化
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] sha256Hash = digest.digest(publicKey.getEncoded());

    // RIPEMD-160ハッシュ化
    RIPEMD160Digest ripemd160 = new RIPEMD160Digest();
    ripemd160.update(sha256Hash, 0, sha256Hash.length);
    byte[] ripemd160Hash = new byte[ripemd160.getDigestSize()];
    ripemd160.doFinal(ripemd160Hash, 0);

    // ネットワーク追加
    byte networkByte = 0x00;
    ByteBuffer buffer =
        ByteBuffer.allocate(
            1 + ripemd160Hash.length); // 1バイト (networkByte) + 20バイト (RIPEMD-160ハッシュ)
    buffer.put(networkByte); // ネットワークバイトを先頭に追加
    buffer.put(ripemd160Hash); // RIPEMD-160ハッシュを追加
    byte[] networkBitcoinPublicKeyBytes = buffer.array(); // 最終的なバイト配列を取得

    // 再度SHA-256ハッシュ化(一回目)
    byte[] sha256HashFirst = digest.digest(networkBitcoinPublicKeyBytes);
    // 再度SHA-256ハッシュ化(二回目)
    digest.reset();
    byte[] sha256HashSecond = digest.digest(sha256HashFirst);
    // 16進数の文字列に変換
    String sha256Hex = Hex.toHexString(sha256HashSecond);
    byte[] checksumBytes = new byte[4];
    System.arraycopy(sha256HashSecond, 0, checksumBytes, 0, 4); // 4バイト

    // networkBitcoinPublicKeyBytes (21バイト) と checksumBytes (4バイト) を連結
    ByteBuffer finalAddressBuffer =
        ByteBuffer.allocate(networkBitcoinPublicKeyBytes.length + checksumBytes.length);
    finalAddressBuffer.put(networkBitcoinPublicKeyBytes); // 21バイト
    finalAddressBuffer.put(checksumBytes); // 4バイト
    byte[] addressBytesForBase58 = finalAddressBuffer.array(); // 合計25バイト

    // Base58エンコード
    return Base58.encode(addressBytesForBase58);
  }

  public String generateSignature(String transactionData, String privateKeyHex) throws Exception {
    // SHA-256ハッシュ化
    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
    byte[] message = sha256.digest(transactionData.getBytes(StandardCharsets.UTF_8));

    // 署名を生成
    PrivateKey privateKey = Utils.convertStringToPrivateKey(privateKeyHex);
    java.security.Signature ecdsaSign = java.security.Signature.getInstance("SHA256withECDSA");
    ecdsaSign.initSign(privateKey, new SecureRandom());
    ecdsaSign.update(message);
    byte[] signatureBytes = ecdsaSign.sign();

    return Hex.toHexString(signatureBytes);
  }
}
