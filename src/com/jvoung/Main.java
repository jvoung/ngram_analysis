package com.jvoung;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {

  public static void main(String[] args) throws IOException {
    Option stem = new Option("stemming", "Use stemming in analysis");
    Options options = new Options();
    options.addOption(stem);
    CommandLineParser parser = new DefaultParser();
    try {
      CommandLine cmdLine = parser.parse(options, args);
      CorpusAnalysis analysis = new CorpusAnalysis();
      if (cmdLine.hasOption(stem.getOpt())) {
        analysis.setStemming(true);
      }
      List<String> argList = cmdLine.getArgList();
      for (String arg : argList) {
        Path file = Paths.get(arg);
        analysis.readFile(file);
      }
    } catch (ParseException exp) {
      System.err.println("Parsing failed.  Reason: " + exp.getMessage());
    }
  }
}
