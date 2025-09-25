package io.alamoa.blockchain.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.alamoa.blockchain.Utils;
import io.alamoa.blockchain.model.Blockchain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class DebugController {

    @Autowired
    private Blockchain blockchain;

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

        // nonceの検証(同じnonceを使ってハッシュ値を作成)
        int nonce = (int) blockchain.getChain().get(1).get("nonce");
        Map<String, Object> firstBlock = blockchain.getChain().get(0);
        Map<String, Object> secondBlock = blockchain.getChain().get(1);
        Map<String, Object> guessBlock = new LinkedHashMap<>();
        guessBlock.put("transactions", secondBlock.get("transactions"));
        guessBlock.put("previous_hash", blockchain.changeToHash(firstBlock));
        guessBlock.put("nonce", nonce);
        guessBlock = Utils.sortedMapByKey(guessBlock);
        String guessHash = blockchain.changeToHash(guessBlock);
        model.addAttribute("nonce", nonce);
        model.addAttribute("reproducedHash", guessHash);

        // 各アドレスの合計値を取得する
        model.addAttribute("AddressA", blockchain.calculateTotalAmount("A"));
        model.addAttribute("AddressB", blockchain.calculateTotalAmount("B"));
        model.addAttribute("AddressC", blockchain.calculateTotalAmount("C"));
        model.addAttribute("AddressD", blockchain.calculateTotalAmount("D"));

        return "outputtest";
    }
}
