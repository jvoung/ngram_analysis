package com.jvoung;

import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Analyzes a word corpus for n-grams, etc.
 */
public class CorpusAnalysis {

  private static final Logger LOGGER = Logger.getLogger(CorpusAnalysis.class.getName());
  private static final Splitter SPLITTER = Splitter.onPattern("[\\s\"“”,.?!—\\-]").trimResults();

  private boolean stemming;
  private Map<Path, List<String>> corpusMap = new HashMap<>();

  public void setStemming(boolean stemming) {
    this.stemming = stemming;
  }

  public void readFile(Path file) throws IOException {
    // Sometimes files are not UTF-8, so try other charsets if it fails.
    List<Charset> charSets = ImmutableList.of(
        StandardCharsets.UTF_8,
        StandardCharsets.ISO_8859_1,
        StandardCharsets.UTF_16
    );
    for (Charset charset : charSets) {
      if (tryReadFile(file, charset)) {
        return;
      }
    }
  }

  private boolean tryReadFile(Path file, Charset charset) throws IOException {
    ImmutableList.Builder<String> wordsBuilder = ImmutableList.builder();
    int numLines = 0;
    String line = "";
    boolean prevWasBlank = false;
    try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
      line = reader.readLine();
      while (line != null) {
        // Skip single blank lines. If there are two blank lines, treat it as a separator.
        if (line.trim().isEmpty()) {
          if (prevWasBlank) {
            wordsBuilder.add("");
          }
          prevWasBlank = true;
          line = reader.readLine();
          continue;
        }
        prevWasBlank = false;
        Iterable<String> words = SPLITTER.split(line);
        for (String word : words) {
          wordsBuilder.add(word.toLowerCase());
        }
        ++numLines;
        line = reader.readLine();
      }
    } catch (MalformedInputException e) {
      LOGGER.warning(String.format("Failed with charset %s after %s lines due to %s: %s",
          charset, numLines, line, e));
      return false;
    }
    corpusMap.put(file, wordsBuilder.build());
    return true;
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
            // Bigger numbers first.
            .compare(o2.second, o1.second)
            // Alphabetical otherwise.
            .compare(o1.first, o2.first, (list1, list2) -> {
              if (list1.size() != list2.size()) {
                return list1.size() - list2.size();
              }
              ComparisonChain chain = ComparisonChain.start();
              for (int i = 0; i < list1.size(); ++i) {
                chain = chain.compare(list1.get(i), list2.get(i));
              }
              return chain.result();
            })
            .result());

    return asList;
  }

  public void printTopNGrams(Path file, int numGrams, int topN) {
    System.out.println(String.format("==== Top %d %d-grams for: %s ====", topN, numGrams, file));
    List<Pair<List<String>, Integer>> ngrams = computeNgrams(file, numGrams);
    for (Pair<List<String>, Integer> entry : Iterables.limit(ngrams, topN)) {
      System.out.println(String.format("%d : %s", entry.second, entry.first));
    }
    System.out.println("====");
  }
}
