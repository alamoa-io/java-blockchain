package io.alamoa.blockchain.controller;

import io.alamoa.blockchain.Utils;
import io.alamoa.blockchain.model.Wallet;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.PublicKey;

@Controller
public class WalletController {
    @GetMapping("/wallet")
    public String makeWallet(Model model){
        try {
            Wallet wallet = new Wallet();
            model.addAttribute("publicKey",wallet.getPublicKey());
            model.addAttribute("privateKey",wallet.getPrivateKey());
            model.addAttribute("blockChainAddress",wallet.getBlockchainAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "wallet";
    }

}
