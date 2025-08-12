package io.alamoa.blockchain;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {
    public static Map<String, Object> sortedMapByKey(Map<String, Object> unsortedMap) {
        return unsortedMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    public static PublicKey convertStringToPublicKey(String keyHex) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = HexFormat.of().parseHex(keyHex);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        return keyFactory.generatePublic(keySpec);
    }

    public static PrivateKey convertStringToPrivateKey(String keyHex) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = HexFormat.of().parseHex(keyHex);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        return keyFactory.generatePrivate(keySpec);
    }
}

