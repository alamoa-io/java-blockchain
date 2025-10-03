package io.alamoa.blockchain.logic;

import io.alamoa.blockchain.Utils;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;

@Component
public class WalletLogic {

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
