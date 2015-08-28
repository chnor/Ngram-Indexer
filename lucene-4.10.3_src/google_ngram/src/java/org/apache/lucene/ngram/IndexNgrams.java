package org.apache.lucene.ngram;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Integer;
import java.lang.Long;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class IndexNgrams {
  
  private IndexNgrams() {}

  /** Index all text files under a directory. */
  public static void main(String[] args) {
    String usage = "java org.apache.lucene.demo.IndexFiles"
                 + " [-index INDEX_PATH] [-docs DOCS_PATH]\n\n"
                 + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                 + "in INDEX_PATH that can be searched with SearchFiles";
    String indexPath = "index";
    String docsPath = null;
    for(int i=0;i<args.length;i++) {
      if ("-index".equals(args[i])) {
        indexPath = args[i+1];
        i++;
      } else if ("-docs".equals(args[i])) {
        docsPath = args[i+1];
        i++;
      }
    }

    // if (docsPath == null) {
    //   System.err.println("Usage: " + usage);
    //   System.exit(1);
    // }

    Date start = new Date();
    try {
      System.out.println("Indexing to directory '" + indexPath + "'...");

      Directory dir = FSDirectory.open(new File(indexPath));
      // :Post-Release-Update-Version.LUCENE_XY:
      Analyzer analyzer = new KeywordAnalyzer();
      IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_4_10_0, analyzer);

      iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
      
      // Optional: for better indexing performance, if you
      // are indexing many documents, increase the RAM
      // buffer.  But if you do this, increase the max heap
      // size to the JVM (eg add -Xmx512m or -Xmx1g):
      //
      // iwc.setRAMBufferSizeMB(256.0);

      IndexWriter writer = new IndexWriter(dir, iwc);
      if (docsPath == null) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Reading from standard input.");
        indexStream(writer, reader);
      } else {
        final File docDir = new File(docsPath);
        if (!docDir.exists() || !docDir.canRead()) {
          System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
          System.exit(1);
        }
        indexDocs(writer, docDir);
      }

      // NOTE: if you want to maximize search performance,
      // you can optionally call forceMerge here.  This can be
      // a terribly costly operation, so generally it's only
      // worth it when your index is relatively static (ie
      // you're done adding documents to it):
      //
      // writer.forceMerge(1);

      writer.close();

      Date end = new Date();
      System.out.println(end.getTime() - start.getTime() + " total milliseconds");

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() +
       "\n with message: " + e.getMessage());
    }
  }

  static void indexStream(IndexWriter writer, BufferedReader reader)
    throws IOException {
    Pattern entry_pattern_1 = Pattern.compile("^(.+?)(?:_([A-Z]+))?$");
    Pattern entry_pattern_2 = Pattern.compile("^_([A-Z]+)_$");

    String line;
    String prev_entry = "";
    String[] prev_term  = null;
    String[] prev_pos   = null;
    long[] tf = new long[209];
    long[] df = new long[209];
    while ((line = reader.readLine()) != null) {
      try {
        // System.out.println(line);
        String[] tokens = line.split("\\s+");
        // System.out.println("'" + tokens[0] + "'");

        String[] term = new String[tokens.length - 3];
        String[] pos  = new String[tokens.length - 3];

        String entry = "";
        for (int i = 0; i < tokens.length - 3; i++) {
          Matcher match = entry_pattern_2.matcher(tokens[i]);
          if (match.matches()) {
            term[i] = "";
            pos[i]  = match.group(1);
          } else {
            match = entry_pattern_1.matcher(tokens[i]);
            if (match.matches()) {
              term[i] = match.group(1);
              pos[i]  = match.group(2);
            } else {
              continue;
              // throw new IllegalStateException("Failed to parse line: " + line);
            }
          }
          if (i != 0) entry += " ";
          entry += tokens[i];
        }
        // for (int i = 0; i < tokens.length - 3; i++) {
        //   System.out.println(term[i] + "\\" + pos[i]);
        // }

        int year = Integer.parseInt(tokens[tokens.length - 3]);
        long t_c = Long.parseLong(tokens[tokens.length - 2]);
        long d_c = Long.parseLong(tokens[tokens.length - 1]);

        // System.out.println("" + year + ": " + t_c);

        if (year < 1800) continue;

        // On new entry or last line: flush buffer to index
        // if (!entry.equals(prev_entry) || !reader.ready()) {
        if (!entry.equals(prev_entry)) {
          // System.out.println("'" + entry + "' != '" + prev_entry + "'");
          if (!prev_entry.equals("")) {
            Document doc = new Document();

            for (int i = 0; i < prev_term.length; i++) {
              doc.add(new TextField("term_" + i, prev_term[i], Field.Store.YES));
            }
            for (int y = 1800; y <= 2008; y++) {
              long tf_year = tf[y - 1800];
              long df_year = df[y - 1800];
              if (tf_year != 0) {
                doc.add(new StoredField("tf_" + y, tf_year));
                // System.out.println("" + y + ": " + tf_year);
              }
              // if (df_year != 0) {
              //   doc.add(new StoredField("df_" + y, df_year));
              // }
            }
            for (int i = 0; i < prev_pos.length; i++) {
              if (prev_pos[i] != null) {
                doc.add(new StringField("pos_" + i, prev_pos[i], Field.Store.YES));
              }
            }

            System.out.print("adding");
            for (int i = 0; i < prev_term.length; i++) {
              System.out.print(" " + prev_term[i] + ( prev_pos[i] != null ? ("\\" + prev_pos[i]) : ""));
            }
            System.out.println();
            writer.addDocument(doc);
          }

          Arrays.fill(tf, 0);
          Arrays.fill(df, 0);

        }
        
        tf[year - 1800] = t_c;
        df[year - 1800] = d_c;

        prev_entry = entry;
        prev_term  = term;
        prev_pos   = pos;

      } catch (Exception e) { // Do this better!
        System.out.println("Failed to parse line: '" + line + "'");
        System.out.println("Reason:");
        System.out.println(e.toString());
      }
    }
  }

  /**
   * Indexes the given file using the given writer, or if a directory is given,
   * recurses over files and directories found under the given directory.
   * 
   * NOTE: This method indexes one document per input file.  This is slow.  For good
   * throughput, put multiple documents into your input file(s).  An example of this is
   * in the benchmark module, which can create "line doc" files, one document per line,
   * using the
   * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
   * >WriteLineDocTask</a>.
   *  
   * @param writer Writer to the index where the given file/dir info will be stored
   * @param file The file to index, or the directory to recurse into to find files to index
   * @throws IOException If there is a low-level I/O error
   */
  static void indexDocs(IndexWriter writer, File file)
    throws IOException {
    // do not try to index files that cannot be read
    if (file.canRead()) {
      if (file.isDirectory()) {
        String[] files = file.list();
        // an IO error could occur
        if (files != null) {
          for (int i = 0; i < files.length; i++) {
            indexDocs(writer, new File(file, files[i]));
          }
        }
      } else {

        FileInputStream fis;
        try {
          fis = new FileInputStream(file);
        } catch (FileNotFoundException fnfe) {
          // at least on windows, some temporary files raise this exception with an "access denied" message
          // checking if the file can be read doesn't help
          return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));

        indexStream(writer, reader);

        reader.close();
      }
    }
  }
}
