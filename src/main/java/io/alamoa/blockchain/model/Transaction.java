package io.alamoa.blockchain.model;

import io.alamoa.blockchain.Utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class Transaction {

    private final String RECIPIENT_BLOCKCHAIN_ADDRESS = "recipient_blockchain_address";
    private final String SENDER_BLOCKCHAIN_ADDRESS = "sender_blockchain_address";
    private final String VALUE = "value";

    private String senderBlockchainAddress;
    private String recipientBlockchainAddress;
    private double value;

    public Transaction(String senderBlockchainAddress, String recipientBlockchainAddress, double value){
        this.senderBlockchainAddress = senderBlockchainAddress;
        this.recipientBlockchainAddress = recipientBlockchainAddress;
        this.value = value;
    }

    public Map<String, Object> toMap(){
        Map<String, Object> transactionData = new LinkedHashMap<>();
        transactionData.put(RECIPIENT_BLOCKCHAIN_ADDRESS, this.recipientBlockchainAddress);
        transactionData.put(SENDER_BLOCKCHAIN_ADDRESS, this.senderBlockchainAddress);
        transactionData.put(VALUE, String.valueOf(value));
        return Utils.sortedMapByKey(transactionData);
    }
}
