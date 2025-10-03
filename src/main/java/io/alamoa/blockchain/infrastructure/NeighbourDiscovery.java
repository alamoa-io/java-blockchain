package io.alamoa.blockchain.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class NeighbourDiscovery {

  @Value("${server.port}")
  private int selfPort;

  private static final int PORT_RANGE_START = 8080;
  private static final int PORT_RANGE_END = 8082;

  private final ConcurrentHashMap<String, String> neighbours = new ConcurrentHashMap<>();
  private final ReentrantLock lock = new ReentrantLock();

  public ConcurrentHashMap<String, String> getNeighbours() {
    return neighbours;
  }

  @Scheduled(fixedRate = 5000)
  public void syncNeighbours() {
    if (lock.tryLock()) {
      try {
        // ポートスキャン方式
        scanForNeighbours();
        System.out.println("同期完了。現在の隣接ノード: " + neighbours.keySet());
      } finally {
        lock.unlock();
      }
    }
  }

  private void scanForNeighbours() {
    for (int port = PORT_RANGE_START; port <= PORT_RANGE_END; port++) {
      if (port == selfPort) continue; // 自分のポートは除外
      String nodeAddress = "127.0.0.1:" + port;
      try (Socket socket = new Socket("127.0.0.1", port)) {
        neighbours.put(nodeAddress, nodeAddress);
      } catch (IOException e) {
        neighbours.remove(nodeAddress);
      }
    }
  }
}
