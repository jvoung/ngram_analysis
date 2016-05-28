package com.jvoung;

import com.google.common.collect.ImmutableList;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    try (BufferedReader reader = Files.newBufferedReader(file)) {
      reader.lines().forEach(line -> {
        String[] words = line.split("[\\s\",.?!-]");
        for (String word : words) {
          wordsBuilder.add(word);
        }
      });
    }
    corpusMap.put(file, wordsBuilder.build());
  }

  public List<String> getWords(Path file) {
    return corpusMap.get(file);
  }

}
