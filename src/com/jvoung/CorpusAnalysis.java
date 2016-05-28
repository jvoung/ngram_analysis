package com.jvoung;

import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Analyzes a word corpus for n-grams, etc.
 */
public class CorpusAnalysis {

  private boolean stemming;

  Map<Path, List<String>> corpusMap = new HashMap<>();

  public void setStemming(boolean stemming) {
    this.stemming = stemming;
  }

  public void readFile(Path file) throws IOException {
    ImmutableList.Builder<String> wordsBuilder = ImmutableList.builder();
    Splitter splitter = Splitter.onPattern("[\\s\",.?!-]").trimResults();
    try (BufferedReader reader = Files.newBufferedReader(file)) {
      reader.lines().forEach(line -> {
        Iterable<String> words = splitter.split(line);
        for (String word : words) {
          wordsBuilder.add(word.toLowerCase());
        }
      });
    }
    corpusMap.put(file, wordsBuilder.build());
  }

  public List<String> getWords(Path file) {
    return corpusMap.get(file);
  }

  public List<Pair<List<String>, Integer>> computeNgrams(Path file, int n) {
    Map<List<String>, AtomicInteger> result = new HashMap<>();
    List<String> currentContext = new ArrayList<>();
    for (String word : getWords(file)) {
      // Assume that empty words mark the of a sentence (two delimiters in a row).
      if (word.isEmpty()) {
        currentContext.clear();
        continue;
      }
      if (currentContext.size() < n) {
        currentContext.add(word);
      } else {
        currentContext.remove(0);
        currentContext.add(word);
      }
      if (currentContext.size() < n) {
        continue;
      }
      AtomicInteger currentCount = result.get(currentContext);
      if (currentCount == null) {
        result.put(ImmutableList.copyOf(currentContext), new AtomicInteger(1));
      } else {
        currentCount.incrementAndGet();
      }
    }
    // Sort by frequency.
    List<Pair<List<String>, Integer>> asList = result.entrySet()
        .stream()
        .map(mapEntry ->
            Pair.of(mapEntry.getKey(), mapEntry.getValue().get()))
        .collect(Collectors.toList());
    asList.sort((o1, o2) ->
        ComparisonChain.start()
            .compare(o1.second, o2.second)
            .compare(o1.first, o2.first, (list1, list2) -> {
              if (list1.size() != list2.size()) {
                return list1.size() - list2.size();
              }
              ComparisonChain chain = ComparisonChain.start();
              for (int i = 0; i < list1.size(); ++i) {
                chain.compare(list1.get(i), list2.get(i));
              }
              return chain.result();
            })
            .result());

    return asList;
  }

}
