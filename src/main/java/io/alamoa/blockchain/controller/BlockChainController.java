package io.alamoa.blockchain.controller;

import io.alamoa.blockchain.Utils;
import io.alamoa.blockchain.entity.TransactionRequest;
import io.alamoa.blockchain.model.Blockchain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BlockChainController {

    @Autowired
    Blockchain blockchain;

    @PostMapping("/transactions")
    public ResponseEntity<String> addTransaction(@RequestBody TransactionRequest transactionRequest) {
        boolean isVerified = false;
        try {
            isVerified = blockchain.verifyTransactionSignature(Utils.convertStringToPublicKey(transactionRequest.getSenderPublicKey()),
                    transactionRequest.getSignature(), transactionRequest.getTransactionData());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("System error happened!");
        }
        if (isVerified) {
            blockchain.addTransaction(transactionRequest.getSenderBlockchainAddress(),
                    transactionRequest.getRecipientBlockchainAddress(),
                    transactionRequest.getAmount());
        } else {
            return ResponseEntity.badRequest().body("Invalid transaction signature!");
        }
        return ResponseEntity.ok("Transaction added successfully!");
    }
}
