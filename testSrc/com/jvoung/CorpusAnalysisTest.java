package com.jvoung;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
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

  private Path createFile(String name, Charset charset, String... lines) throws IOException {
    Path file = temp.resolve(name);
    try (BufferedWriter writer = Files.newBufferedWriter(file, charset)) {
      for (String line : lines) {
        writer.write(line);
        writer.newLine();
      }
    }
    return file;
  }

  @Test
  public void emptyFile() throws IOException {
    Path testFile = createFile("test1", StandardCharsets.UTF_8);
    CorpusAnalysis corpus = new CorpusAnalysis();
    corpus.readFile(testFile);
    List<String> words = corpus.getWords(testFile);
    Truth.assertThat(words).isEmpty();
  }

  @Test
  public void readUTF8File() throws IOException {
    Path testFile = createFile("test1", StandardCharsets.UTF_8,
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
  public void readISO_8859_1File() throws IOException {
    Path testFile = createFile("test1", StandardCharsets.ISO_8859_1,
        "Hello?",
        "Hello gôvërnor!"
    );
    CorpusAnalysis corpus = new CorpusAnalysis();
    corpus.readFile(testFile);
    List<String> words = corpus.getWords(testFile);
    Truth.assertThat(words).containsExactly(
        "hello", "", "hello", "gôvërnor", ""
    ).inOrder();
  }

  @Test
  public void quotationsOnSameLineOrSplitAcrossLines() throws IOException {
    Path testFile = createFile("test1", StandardCharsets.UTF_8,
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
  public void quotationsWithFancyQuoteCharOnSameLineOrSplitAcrossLines() throws IOException {
    Path testFile = createFile("test1", StandardCharsets.UTF_8,
        "John says, “Hello there.”, and Molly replies “Yes",
        "that is the case!”");
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
  public void emptyLines() throws IOException {
    // TODO(jvoung): make it configurable how many blank lines are expected for normal line wrapping.
    Path testFile = createFile("test", StandardCharsets.UTF_8,
        "",
        "AN ARTIST OF THE FLOATING WORLD",
        "",
        "KAZUO ISHIGURO",
        "Copyright © 1986 by Kazuo Ishiguro",
        "",
        "",
        "For my parents",
        "",
        "",
        "More",
        "stuff that I don't know"
    );
    CorpusAnalysis corpus = new CorpusAnalysis();
    corpus.readFile(testFile);
    List<String> words = corpus.getWords(testFile);
    Truth.assertThat(words).containsExactly(
        "an", "artist", "of", "the", "floating", "world", "kazuo", "ishiguro", "copyright", "©",
        "1986", "by", "kazuo", "ishiguro", "", "for", "my", "parents", "", "more", "stuff", "that",
        "i", "don't", "know")
        .inOrder();
  }

  @Test
  public void apostrophe() throws IOException {
    // TODO(jvoung): make it what the apostrophe and quote characters are.
    Path testFile = createFile("test", StandardCharsets.UTF_8,

        "More stuff that I don't know"
    );
    CorpusAnalysis corpus = new CorpusAnalysis();
    corpus.readFile(testFile);
    List<String> words = corpus.getWords(testFile);
    Truth.assertThat(words).containsExactly(
        "more", "stuff", "that", "i", "don't", "know")
        .inOrder();
  }

  @Test
  public void testNGram() throws IOException {
    Path testFile = createFile("test1", StandardCharsets.UTF_8,
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
        Pair.of(ImmutableList.of("more"), 2),
        Pair.of(ImmutableList.of("much"), 2),
        Pair.of(ImmutableList.of("for"), 1),
        Pair.of(ImmutableList.of("you"), 1))
        .inOrder();
    List<Pair<List<String>, Integer>> twoGrams = corpus.computeNgrams(testFile, 2);
    Truth.assertThat(twoGrams).containsExactly(
        Pair.of(ImmutableList.of("and", "less"), 2),
        Pair.of(ImmutableList.of("and", "more"), 2),
        Pair.of(ImmutableList.of("little", "and"), 2),
        Pair.of(ImmutableList.of("much", "and"), 2),
        Pair.of(ImmutableList.of("for", "you"), 1),
        Pair.of(ImmutableList.of("more", "for"), 1))
        .inOrder();
    List<Pair<List<String>, Integer>> threeGrams = corpus.computeNgrams(testFile, 3);
    Truth.assertThat(threeGrams).containsExactly(
        Pair.of(ImmutableList.of("little", "and", "less"), 2),
        Pair.of(ImmutableList.of("much", "and", "more"), 2),
        Pair.of(ImmutableList.of("and", "more", "for"), 1),
        Pair.of(ImmutableList.of("more", "for", "you"), 1))
        .inOrder();

    List<Pair<List<String>, Integer>> fourGrams = corpus.computeNgrams(testFile, 4);
    Truth.assertThat(fourGrams).containsExactly(
        Pair.of(ImmutableList.of("and", "more", "for", "you"), 1),
        Pair.of(ImmutableList.of("much", "and", "more", "for"), 1))
        .inOrder();
  }

  @Test
  public void testNGramWithNewlineWrap() throws IOException {
    Path testFile = createFile("test1", StandardCharsets.UTF_8,
        "Much and",
        "more",
        "for you.",
        "",
        "Little",
        "and less. Much",
        "and more. Little",
        "and less."
    );
    CorpusAnalysis corpus = new CorpusAnalysis();
    corpus.readFile(testFile);
    List<Pair<List<String>, Integer>> oneGrams = corpus.computeNgrams(testFile, 1);
    Truth.assertThat(oneGrams).containsExactly(
        Pair.of(ImmutableList.of("and"), 4),
        Pair.of(ImmutableList.of("less"), 2),
        Pair.of(ImmutableList.of("little"), 2),
        Pair.of(ImmutableList.of("more"), 2),
        Pair.of(ImmutableList.of("much"), 2),
        Pair.of(ImmutableList.of("for"), 1),
        Pair.of(ImmutableList.of("you"), 1))
        .inOrder();
    List<Pair<List<String>, Integer>> twoGrams = corpus.computeNgrams(testFile, 2);
    Truth.assertThat(twoGrams).containsExactly(
        Pair.of(ImmutableList.of("and", "less"), 2),
        Pair.of(ImmutableList.of("and", "more"), 2),
        Pair.of(ImmutableList.of("little", "and"), 2),
        Pair.of(ImmutableList.of("much", "and"), 2),
        Pair.of(ImmutableList.of("for", "you"), 1),
        Pair.of(ImmutableList.of("more", "for"), 1))
        .inOrder();
    List<Pair<List<String>, Integer>> threeGrams = corpus.computeNgrams(testFile, 3);
    Truth.assertThat(threeGrams).containsExactly(
        Pair.of(ImmutableList.of("little", "and", "less"), 2),
        Pair.of(ImmutableList.of("much", "and", "more"), 2),
        Pair.of(ImmutableList.of("and", "more", "for"), 1),
        Pair.of(ImmutableList.of("more", "for", "you"), 1))
        .inOrder();

    List<Pair<List<String>, Integer>> fourGrams = corpus.computeNgrams(testFile, 4);
    Truth.assertThat(fourGrams).containsExactly(
        Pair.of(ImmutableList.of("and", "more", "for", "you"), 1),
        Pair.of(ImmutableList.of("much", "and", "more", "for"), 1))
        .inOrder();
  }
}