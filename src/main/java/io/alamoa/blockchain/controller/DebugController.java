package io.alamoa.blockchain.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.alamoa.blockchain.Utils;
import io.alamoa.blockchain.logic.BlockchainLogic;
import io.alamoa.blockchain.service.BlockchainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class DebugController {

  @Autowired private BlockchainService blockchainService;
  @Autowired private BlockchainLogic blockchainLogic;

  @GetMapping("/test")
  public String test(Model model) {
    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    // 初期ブロック作成
    String initBlock = gson.toJson(blockchainService.getChain());
    model.addAttribute("first", initBlock);

    // transactionの追加
    blockchainService.addTransaction("A", "B", 10);
    blockchainService.addTransaction("C", "D", 20);
    String transactionPoolBefore = gson.toJson(blockchainService.getTransactionPool());
    model.addAttribute("transactionPoolBefore", transactionPoolBefore);

    // blockの追加
    blockchainService.mine();

    // block追加後のデータ状態の取得
    String addBlock = gson.toJson(blockchainService.getChain());
    model.addAttribute("addedBlock", addBlock);
    String transactionPoolAfter = gson.toJson(blockchainService.getTransactionPool());
    model.addAttribute("transactionPoolAfter", transactionPoolAfter);

    // nonceの検証(同じnonceを使ってハッシュ値を作成)
    int nonce = (int) blockchainService.getChain().get(1).get("nonce");
    Map<String, Object> firstBlock = blockchainService.getChain().get(0);
    Map<String, Object> secondBlock = blockchainService.getChain().get(1);
    Map<String, Object> guessBlock = new LinkedHashMap<>();
    guessBlock.put("transactions", secondBlock.get("transactions"));
    guessBlock.put("previous_hash", blockchainLogic.changeToHash(firstBlock));
    guessBlock.put("nonce", nonce);
    guessBlock = Utils.sortedMapByKey(guessBlock);
    String guessHash = blockchainLogic.changeToHash(guessBlock);
    model.addAttribute("nonce", nonce);
    model.addAttribute("reproducedHash", guessHash);

    // 各アドレスの合計値を取得する
    model.addAttribute("AddressA", blockchainService.calculateTotalAmount("A"));
    model.addAttribute("AddressB", blockchainService.calculateTotalAmount("B"));
    model.addAttribute("AddressC", blockchainService.calculateTotalAmount("C"));
    model.addAttribute("AddressD", blockchainService.calculateTotalAmount("D"));

    return "outputtest";
  }
}
