package io.alamoa.blockchain.entity;

import java.util.Map;

public class TransactionRequest {
  private String senderPrivateKey;
  private String senderBlockchainAddress;
  private String recipientBlockchainAddress;
  private String senderPublicKey;
  private double amount;
  private String signature;
  private Map<String, Object> transactionData;

  public String getSenderPrivateKey() {
    return senderPrivateKey;
  }

  public void setSenderPrivateKey(String senderPrivateKey) {
    this.senderPrivateKey = senderPrivateKey;
  }

  public String getSenderBlockchainAddress() {
    return senderBlockchainAddress;
  }

  public void setSenderBlockchainAddress(String senderBlockchainAddress) {
    this.senderBlockchainAddress = senderBlockchainAddress;
  }

  public String getRecipientBlockchainAddress() {
    return recipientBlockchainAddress;
  }

  public void setRecipientBlockchainAddress(String recipientBlockchainAddress) {
    this.recipientBlockchainAddress = recipientBlockchainAddress;
  }

  public String getSenderPublicKey() {
    return senderPublicKey;
  }

  public void setSenderPublicKey(String senderPublicKey) {
    this.senderPublicKey = senderPublicKey;
  }

  public double getAmount() {
    return amount;
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public Map<String, Object> getTransactionData() {
    return transactionData;
  }

  public void setTransactionData(Map<String, Object> transactionData) {
    this.transactionData = transactionData;
  }
}
