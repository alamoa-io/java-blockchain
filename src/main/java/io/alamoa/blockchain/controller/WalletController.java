package io.alamoa.blockchain.controller;

import io.alamoa.blockchain.entity.TransactionRequest;
import io.alamoa.blockchain.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class WalletController {

  @Autowired WalletService walletService;

  @GetMapping("/wallet")
  public String makeWallet(Model model) {
    try {
      walletService.createWallet(model);
    } catch (Exception e) {
      return "error";
    }
    return "wallet";
  }

  @PostMapping("/addTransaction")
  @ResponseBody
  public ResponseEntity<String> addTransaction(@RequestBody TransactionRequest transactionRequest) {

    try {
      walletService.processTransaction(transactionRequest);
    } catch (Exception e) {
      return ResponseEntity.status(500).body("Error processing transaction.");
    }
    return ResponseEntity.ok("Transaction added successfully!");

  }
}
