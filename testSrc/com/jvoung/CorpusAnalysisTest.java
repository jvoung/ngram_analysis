package com.jvoung;

import com.google.common.collect.ImmutableList;
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
    // Note that we include delimiters as space to signal when to reset the n-gram context.
    Truth.assertThat(words).containsExactly(
        "hello", "", "hello", "governor", "", "another", "line", "with", "many", "words", "", "", ""
    ).inOrder();
  }

  @Test
  public void quotationsOnLine() throws IOException {
    Path testFile = createFile("test1",
        "John says, \"Hello there.\", and Molly replies \"Yes",
        "that is the case!\"");
    CorpusAnalysis corpus = new CorpusAnalysis();
    corpus.readFile(testFile);
    List<String> words = corpus.getWords(testFile);
    // Note that we include delimiters as space to signal when to reset the n-gram context.
    // We could probably merge some consecutive ones...
    // Also not that Molly's quote is split between two lines, but the line split is not
    // considered a delimiter.
    Truth.assertThat(words).containsExactly(
        "john", "says", "", "", "hello", "there", "", "", "", "and", "molly", "replies",
        "", "yes", "that", "is", "the", "case", "", ""
    ).inOrder();

  }

  @Test
  public void testNGram() throws IOException {
    Path testFile = createFile("test1",
        "Much and more for you.",
        "Little and less. Much and more. Little and less."
    );
    CorpusAnalysis corpus = new CorpusAnalysis();
    corpus.readFile(testFile);
    List<Pair<List<String>, Integer>> oneGrams = corpus.computeNgrams(testFile, 1);
    Truth.assertThat(oneGrams).containsExactly(
        Pair.of(ImmutableList.of("and"), 4),
        Pair.of(ImmutableList.of("less"), 2),
        Pair.of(ImmutableList.of("little"), 2),
        Pair.of(ImmutableList.of("much"), 2),
        Pair.of(ImmutableList.of("more"), 2),
        Pair.of(ImmutableList.of("for"), 1),
        Pair.of(ImmutableList.of("you"), 1)
        );
    List<Pair<List<String>, Integer>> twoGrams = corpus.computeNgrams(testFile, 2);
    Truth.assertThat(twoGrams).containsExactly(
        Pair.of(ImmutableList.of("and", "less"), 2),
        Pair.of(ImmutableList.of("and", "more"), 2),
        Pair.of(ImmutableList.of("little", "and"), 2),
        Pair.of(ImmutableList.of("much", "and"), 2),
        Pair.of(ImmutableList.of("more", "for"), 1),
        Pair.of(ImmutableList.of("for", "you"), 1)
        );
    List<Pair<List<String>, Integer>> threeGrams = corpus.computeNgrams(testFile, 3);
    Truth.assertThat(threeGrams).containsExactly(
        Pair.of(ImmutableList.of("little", "and", "less"), 2),
        Pair.of(ImmutableList.of("much", "and", "more"), 2),
        Pair.of(ImmutableList.of("and", "more", "for"), 1),
        Pair.of(ImmutableList.of("more", "for", "you"), 1)
    );

    List<Pair<List<String>, Integer>> fourGrams = corpus.computeNgrams(testFile, 4);
    Truth.assertThat(fourGrams).containsExactly(
        Pair.of(ImmutableList.of("and", "more", "for", "you"), 1),
        Pair.of(ImmutableList.of("much", "and", "more", "for"), 1)
    );
  }
}