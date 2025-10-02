package io.alamoa.blockchain.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.alamoa.blockchain.Utils;
import io.alamoa.blockchain.constant.BlockChainConstants;
import io.alamoa.blockchain.entity.TransactionRequest;
import io.alamoa.blockchain.logic.BlockchainLogic;
import io.alamoa.blockchain.model.NeighbourDiscovery;
import io.alamoa.blockchain.model.Transaction;
import io.alamoa.blockchain.model.Wallet;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class BlockchainService {

  @Autowired NeighbourDiscovery neighbourDiscovery;
  @Autowired RestTemplate restTemplate;
  @Autowired BlockchainLogic blockchainLogic;

  private final Semaphore miningSemaphore = new Semaphore(1);
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  private final List<Map<String, Object>> transactionPool;
  private final List<Map<String, Object>> chain = new ArrayList<>();
  private final String minerBlockchainAddress;

  public BlockchainService() {
    this.transactionPool = new ArrayList<>();
    try {
      Wallet minerWallet = new Wallet();
      System.out.println("Miners Private Key: " + minerWallet.getPrivateKey());
      System.out.println("Miners Public Key: " + minerWallet.getPublicKey());
      System.out.println("Miners Blockchain Address: " + minerWallet.getBlockchainAddress());
      this.minerBlockchainAddress = minerWallet.getBlockchainAddress();
      addTransaction(
          "REWARD!!",
          minerWallet.getBlockchainAddress(),
          BlockChainConstants.MINING_REWARD.getDouble());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @PostConstruct
  public void init() {
    this.chain.add(createBlock(0, "first block"));
    startMiningLoop();
  }

  public boolean processTransaction(TransactionRequest transactionRequest) throws Exception {
    boolean isVerified = false;
    try {
      isVerified =
          blockchainLogic.verifyTransactionSignature(
              Utils.convertStringToPublicKey(transactionRequest.getSenderPublicKey()),
              transactionRequest.getSignature(),
              transactionRequest.getTransactionData());
    } catch (Exception e) {
      throw new Exception("システムエラーが発生しました。管理者にお問い合わせ下さい");
    }

    if (isVerified) {
      throw new Exception("設定しているKeyに誤りがあります。入力内容を確認して下さい。");
    }

    return addTransaction(
        transactionRequest.getSenderBlockchainAddress(),
        transactionRequest.getRecipientBlockchainAddress(),
        transactionRequest.getAmount());
  }

  public void processPutTransaction(TransactionRequest transactionRequest) throws Exception {
    boolean isTransacted = processTransaction(transactionRequest);
    if (isTransacted) {
      addTransactionToNeighbour(transactionRequest);
    }
  }

  public void startMining() {
    // tryAcquire()で非ブロッキングにセマフォを取得
    if (miningSemaphore.tryAcquire()) {
      try {
        // マイニングロジックを呼び出す
        mine();
      } finally {
        // 必ずセマフォを解放する
        miningSemaphore.release();
      }
    }
  }

  public void startMiningLoop() {
    // 定期的に startMining メソッドを実行するようにスケジューリング
    scheduler.scheduleAtFixedRate(
        this::startMining, 0, BlockChainConstants.MINING_TIMER_SEC.getLong(), TimeUnit.SECONDS);
  }

  public boolean deleteTransactionPool() {
    this.transactionPool.clear();
    return true;
  }

  public Map<String, Object> createBlock(int nonce, String previousHash) {
    Map<String, Object> block = new LinkedHashMap<>();
    block.put(BlockChainConstants.TIMESTAMP.getString(), System.currentTimeMillis());
    List<Map<String, Object>> blockTransactions = new ArrayList<>(this.transactionPool);
    block.put(BlockChainConstants.TRANSACTIONS.getString(), blockTransactions);
    block.put(BlockChainConstants.NONCE.getString(), nonce);
    block.put(BlockChainConstants.PREVIOUS_HASH.getString(), previousHash);
    this.transactionPool.clear();
    for (Object neighbour : neighbourDiscovery.getNeighbours().keySet()) {
      String neighbourAddress = (String) neighbour;
      restTemplate.delete("http://" + neighbourAddress + "/transactions");
    }
    return Utils.sortedMapByKey(block);
  }

  public boolean addTransaction(
      String senderBlockchainAddress, String recipientBlockchainAddress, double value) {
    Transaction transaction =
        new Transaction(senderBlockchainAddress, recipientBlockchainAddress, value);
    transactionPool.add(transaction.toMap());
    return true;
  }

  public void addTransactionToNeighbour(TransactionRequest transactionRequest) {
    for (Object neighbour : neighbourDiscovery.getNeighbours().keySet()) {
      String neighbourAddress = (String) neighbour;
      try {
        restTemplate.put("http://" + neighbourAddress + "/transactions", transactionRequest);
      } catch (RestClientException e) {
        // TODO ログの出力 想定された例外
      } catch (Exception e) {
        // TODO ログの出力 想定されていない例外
      }
    }
  }

  public boolean resolveConflicts() {
    List<Map<String, Object>> longestChain = null;
    int maxLength = this.chain.size();
    for (Object neighbour : neighbourDiscovery.getNeighbours().keySet()) {
      String neighbourAddress = (String) neighbour;
      try {
        List<Map<String, Object>> neighbourChain =
            restTemplate.getForObject("http://" + neighbourAddress + "/chain", List.class);

        if (neighbourChain != null) {
          int chainLength = neighbourChain.size();
          if (chainLength > maxLength && blockchainLogic.validChain(neighbourChain)) {
            maxLength = chainLength;
            longestChain = neighbourChain;
          }
        }
      } catch (Exception e) {
        System.out.println("Error while resolving conflicts with neighbour: " + neighbourAddress);
        e.printStackTrace();
      }
    }

    if (longestChain != null) {
      this.chain.clear();
      this.chain.addAll(longestChain);
      return true;
    }
    return false;
  }

  public void mine() {
    Map<String, Object> newBlock = this.chain.get(this.chain.size() - 1);
    addTransaction(
        "REWARD!!", minerBlockchainAddress, BlockChainConstants.MINING_REWARD.getDouble());
    int nonce = blockchainLogic.proofOfWork(this.transactionPool, this.chain);
    this.chain.add(createBlock(nonce, blockchainLogic.changeToHash(newBlock)));
    for (Object neighbour : neighbourDiscovery.getNeighbours().keySet()) {
      String neighbourAddress = (String) neighbour;
      restTemplate.put("http://" + neighbourAddress + "/consensus", "check consensus");
    }
  }

  public double calculateTotalAmount(String blockchainAddress) {
    return chain.stream() // リストをstreamに変換
        // 各ブロックの中のtransactionsの内容を取得し、一つのstreamにまとめる
        .flatMap(
            block ->
                ((List<Map<String, Object>>)
                        block.get(BlockChainConstants.TRANSACTIONS.getString()))
                    .stream())
        // 引数のblockchainAddressに該当する送り手、もしくは受け取り側のtransactionのみを対象とする
        .filter(
            transaction ->
                transaction
                        .get(BlockChainConstants.SENDER_BLOCKCHAIN_ADDRESS.getString())
                        .equals(blockchainAddress)
                    || transaction
                        .get(BlockChainConstants.RECIPIENT_BLOCKCHAIN_ADDRESS.getString())
                        .equals(blockchainAddress))
        // 値を取り出し送り手側なら減算、受け取り側なら加算を行う
        .mapToDouble(
            transaction -> {
              double value =
                  Double.parseDouble(
                      (String) transaction.get(BlockChainConstants.VALUE.getString()));
              return transaction
                      .get(BlockChainConstants.SENDER_BLOCKCHAIN_ADDRESS.getString())
                      .equals(blockchainAddress)
                  ? -value
                  : value;
            })
        .sum(); // 合計値を出す
  }

  public List<Map<String, Object>> getChain() {
    return this.chain;
  }

  public List<Map<String, Object>> getTransactionPool() {
    return this.transactionPool;
  }
}
