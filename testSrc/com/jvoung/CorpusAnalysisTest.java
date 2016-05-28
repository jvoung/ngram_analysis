package com.jvoung;

import com.google.common.truth.Truth;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Tests CorpusAnalysis.
 */
public class CorpusAnalysisTest {

  // TODO: make temp files in jimfs?
  private Path temp;

  @Before
  public void setupFiles() throws IOException {
    temp = Files.createTempDirectory("test_temp");
  }

  private Path createFile(String name, String... lines) throws IOException {
    Path file = temp.resolve(name);
    try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
      for (String line : lines) {
        writer.write(line);
        writer.newLine();
      }
    }
    return file;
  }

  @Test
  public void emptyFile() throws IOException {
    Path testFile = createFile("test1");
    CorpusAnalysis corpus = new CorpusAnalysis();
    corpus.readFile(testFile);
    List<String> words = corpus.getWords(testFile);
    Truth.assertThat(words).isEmpty();
  }

  @Test
  public void readAFile() throws IOException {
    Path testFile = createFile("test1",
        "Hello?",
        "Hello governor!",
        "Another line with many words..."
    );
    CorpusAnalysis corpus = new CorpusAnalysis();
    corpus.readFile(testFile);
    List<String> words = corpus.getWords(testFile);
    Truth.assertThat(words).containsExactly(
        "Hello", "Hello", "governor", "Another", "line", "with", "many", "words"
    ).inOrder();
  }

}