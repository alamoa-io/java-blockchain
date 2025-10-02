package io.alamoa.blockchain.controller;

import io.alamoa.blockchain.entity.TransactionRequest;
import io.alamoa.blockchain.service.BlockchainService;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class BlockChainController {

  @Autowired BlockchainService blockchainService;

  @GetMapping("/transactions")
  public ResponseEntity<String> getTransactions() {
    return new ResponseEntity<>(blockchainService.getTransactionPool().toString(), HttpStatus.OK);
  }

  @DeleteMapping("/transactions")
  public ResponseEntity<String> deleteTransactions() {
    blockchainService.deleteTransactionPool();
    return new ResponseEntity<>("Transaction pool cleared!", HttpStatus.OK);
  }

  @PostMapping("/transactions")
  public ResponseEntity<String> addTransaction(@RequestBody TransactionRequest transactionRequest) {
    try {
      blockchainService.processPutTransaction(transactionRequest);
    } catch (Exception e) {
      return ResponseEntity.status(500).body(e.getMessage());
    }
    return ResponseEntity.ok().body("Transaction added successfully!");
  }

  @PutMapping("/transactions")
  public ResponseEntity<String> putTransaction(@RequestBody TransactionRequest transactionRequest) {
    try {
      blockchainService.processTransaction(transactionRequest);
    } catch (Exception e) {
      return ResponseEntity.status(500).body(e.getMessage());
    }
    return ResponseEntity.ok().body("Transaction added successfully!");
  }

  @GetMapping("/amount")
  public ResponseEntity<String> getAmount(@RequestParam("address") String blockchainAddress) {
    return new ResponseEntity<>(
        String.valueOf(blockchainService.calculateTotalAmount(blockchainAddress)), HttpStatus.OK);
  }

  @GetMapping("/mine")
  public ResponseEntity<String> getMine() {
    blockchainService.mine();
    return new ResponseEntity<>("Mined!", HttpStatus.OK);
  }

  @GetMapping("/chain")
  public ResponseEntity<List<Map<String, Object>>> getChain() {
    return new ResponseEntity<>(blockchainService.getChain(), HttpStatus.OK);
  }

  @PutMapping("/consensus")
  public ResponseEntity<String> consensus() {
    blockchainService.resolveConflicts();
    return new ResponseEntity<>("Consensus reached!", HttpStatus.OK);
  }
}
