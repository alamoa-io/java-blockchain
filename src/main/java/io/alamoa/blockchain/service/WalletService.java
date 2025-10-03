package io.alamoa.blockchain.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.alamoa.blockchain.entity.TransactionRequest;
import io.alamoa.blockchain.logic.WalletLogic;
import io.alamoa.blockchain.model.Transaction;
import io.alamoa.blockchain.model.Wallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.client.RestTemplate;

@Service
public class WalletService {

  @Autowired WalletLogic walletLogic;

  @Autowired private RestTemplate restTemplate;

  private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  public void createWallet(Model model) throws Exception {
    try {
      Wallet wallet = new Wallet();
      model.addAttribute("publicKey", wallet.getPublicKey());
      model.addAttribute("privateKey", wallet.getPrivateKey());
      model.addAttribute("blockChainAddress", wallet.getBlockchainAddress());
    } catch (Exception e) {
      // TODO ログの出力
      throw new Exception();
    }
  }

  public void processTransaction(TransactionRequest transactionRequest) throws Exception {
    Transaction transaction =
        new Transaction(
            transactionRequest.getSenderBlockchainAddress(),
            transactionRequest.getSenderBlockchainAddress(),
            transactionRequest.getAmount());
    String transactionJson = gson.toJson(transaction.toMap());

    String signature;
    try {
      signature =
          walletLogic.generateSignature(transactionJson, transactionRequest.getSenderPrivateKey());
    } catch (Exception e) {
      // TODO ログの出力
      throw new Exception();
    }
    transactionRequest.setSignature(signature);
    transactionRequest.setTransactionData(transaction.toMap());

    try{
      restTemplate.postForObject(
              "http://localhost:8080//transactions", transactionRequest, String.class);
    } catch (Exception e){
      // TODO ログの出力
      throw new Exception();
    }
  }
}
