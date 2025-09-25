package io.alamoa.blockchain.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.alamoa.blockchain.model.Blockchain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DebugController {

  @Autowired private Blockchain blockchain;

  @GetMapping("/test")
  public String test(Model model) {
    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // 初期ブロック作成
    String initBlock = gson.toJson(blockchain.getChain());
    model.addAttribute("first", initBlock);

    // transactionの追加
    blockchain.addTransaction("A", "B", 10);
    blockchain.addTransaction("C", "D", 20);
    String transactionPoolBefore = gson.toJson(blockchain.getTransactionPool());
    model.addAttribute("transactionPoolBefore", transactionPoolBefore);

    // blockの追加
    blockchain.mine();

    // block追加後のデータ状態の取得
    String addBlock = gson.toJson(blockchain.getChain());
    model.addAttribute("addedBlock", addBlock);
    String transactionPoolAfter = gson.toJson(blockchain.getTransactionPool());
    model.addAttribute("transactionPoolAfter", transactionPoolAfter);
    return "outputtest";
  }
}
