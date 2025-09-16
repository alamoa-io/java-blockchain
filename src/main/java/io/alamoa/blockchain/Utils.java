package io.alamoa.blockchain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {
  public static Map<String, Object> sortedMapByKey(Map<String, Object> unsortedMap) {
    return unsortedMap.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (oldValue, newValue) -> oldValue,
                LinkedHashMap::new));
  }
}
