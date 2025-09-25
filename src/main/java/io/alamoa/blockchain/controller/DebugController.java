package io.alamoa.blockchain.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.alamoa.blockchain.model.Blockchain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class DebugController {

  @Autowired private Blockchain blockchain;

  @GetMapping("/test")
  public String test(Model model) {
    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    Map<String, Object> sampleBlock = blockchain.createBlock(0, "hash");
    String sampleBlockJson = gson.toJson(sampleBlock);
    model.addAttribute("json", sampleBlockJson);
    String hashedSampleBlockJson = blockchain.changeToHash(sampleBlock);
    model.addAttribute("hash", hashedSampleBlockJson);
    return "outputtest";
  }
}
