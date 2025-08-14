package io.alamoa.blockchain.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.alamoa.blockchain.entity.TransactionRequest;
import io.alamoa.blockchain.model.Transaction;
import io.alamoa.blockchain.model.Wallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

@Controller
public class WalletController {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/wallet")
    public String makeWallet(Model model) {
        try {
            Wallet wallet = new Wallet();
            model.addAttribute("publicKey", wallet.getPublicKey());
            model.addAttribute("privateKey", wallet.getPrivateKey());
            model.addAttribute("blockChainAddress", wallet.getBlockchainAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "wallet";
    }

    @PostMapping("/addTransaction")
    @ResponseBody
    public ResponseEntity<String> addTransaction(@RequestBody TransactionRequest transactionRequest) {
        try {
            Wallet wallet = new Wallet();
            Transaction transaction = new Transaction(transactionRequest.getSenderBlockchainAddress(),
                    transactionRequest.getSenderBlockchainAddress(),
                    transactionRequest.getAmount());
            String transactionJson = gson.toJson(transaction.toMap());
            String signature = wallet.generateSignature(transactionJson, transactionRequest.getSenderPrivateKey());
            transactionRequest.setSignature(signature);
            transactionRequest.setTransactionData(transaction.toMap());


            restTemplate.postForObject(
                    "http://localhost:8080//transactions",
                    transactionRequest,
                    String.class
            );
            return ResponseEntity.ok("Transaction added successfully!");

        } catch (Exception e) {
            System.err.println("Error processing transaction: " + e.getMessage());
            return ResponseEntity.status(500).body("Error processing transaction.");
        }
    }
}
