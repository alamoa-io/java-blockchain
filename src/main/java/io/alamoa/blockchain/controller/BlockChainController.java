package io.alamoa.blockchain.controller;

import io.alamoa.blockchain.Utils;
import io.alamoa.blockchain.entity.TransactionRequest;
import io.alamoa.blockchain.model.Blockchain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class BlockChainController {

    @Autowired
    Blockchain blockchain;

    @GetMapping("/transactions")
    public ResponseEntity<String> getTransactions() {
        return new ResponseEntity<>(blockchain.getTransactionPool().toString(), HttpStatus.OK);
    }

    @DeleteMapping("/transactions")
    public ResponseEntity<String> deleteTransactions() {
        blockchain.deleteTransactionPool();
        return new ResponseEntity<>("Transaction pool cleared!", HttpStatus.OK);
    }

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
            boolean isTransacted = blockchain.addTransaction(transactionRequest.getSenderBlockchainAddress(),
                    transactionRequest.getRecipientBlockchainAddress(),
                    transactionRequest.getAmount());
            if (isTransacted) {
                blockchain.addTransactionToNeighbour(transactionRequest);
            }
        } else {
            return ResponseEntity.badRequest().body("Invalid transaction signature!");
        }
        return ResponseEntity.ok("Transaction added successfully!");
    }

    @PutMapping("/transactions")
    public ResponseEntity<String> putTransaction(@RequestBody TransactionRequest transactionRequest) {
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

    @GetMapping("/amount")
    public ResponseEntity<String> getAmount(@RequestParam("address") String blockchainAddress) {
        return new ResponseEntity<>(String.valueOf(blockchain.calculateTotalAmount(blockchainAddress)), HttpStatus.OK);
    }

    @GetMapping("/mine")
    public ResponseEntity<String> getMine() {
        blockchain.mine();
        return new ResponseEntity<>("Mined!", HttpStatus.OK);
    }

    @GetMapping("/chain")
    public ResponseEntity<List<Map<String, Object>>> getChain() {
        return new ResponseEntity<>(blockchain.getChain(), HttpStatus.OK);
    }

    @PutMapping("/consensus")
    public ResponseEntity<String> consensus(){
        blockchain.resolveConflicts();
        return new ResponseEntity<>("Consensus reached!", HttpStatus.OK);
    }
}
