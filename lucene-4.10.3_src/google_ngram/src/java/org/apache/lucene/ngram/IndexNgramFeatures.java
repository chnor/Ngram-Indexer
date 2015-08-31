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
import org.apache.lucene.document.DoubleField;
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

/**
 * Index ngram mean and standard deviation taken from
 * standard input or the specified path in raw format.
 * Based heavily on the lucene demo.
 */
public class IndexNgramFeatures {
  
  private static final double[] tf_total = {
    70784405.0,
    107290136.0,
    95731997.0,
    104173226.0,
    114051906.0,
    115330195.0,
    118229517.0,
    128904931.0,
    129988114.0,
    137911980.0,
    150961261.0,
    177318465.0,
    172538907.0,
    144660671.0,
    168441689.0,
    156318674.0,
    161561836.0,
    182422107.0,
    204446854.0,
    174156635.0,
    231277724.0,
    181677006.0,
    271213007.0,
    254327070.0,
    309237910.0,
    318701311.0,
    243758959.0,
    253677933.0,
    273678947.0,
    293815859.0,
    342378710.0,
    313388047.0,
    314184783.0,
    310441320.0,
    301383644.0,
    355491202.0,
    365982104.0,
    337485292.0,
    358600155.0,
    413876708.0,
    423904296.0,
    387286321.0,
    348396317.0,
    404133447.0,
    419311001.0,
    456885448.0,
    459546575.0,
    443868440.0,
    466134080.0,
    472315353.0,
    504143257.0,
    537705793.0,
    558718364.0,
    625159477.0,
    683559348.0,
    605758582.0,
    652385453.0,
    568489706.0,
    541848821.0,
    588343315.0,
    607952196.0,
    463190641.0,
    396839451.0,
    418297294.0,
    493159851.0,
    503022451.0,
    548257863.0,
    518622969.0,
    547590187.0,
    558291347.0,
    548870828.0,
    560339562.0,
    566620105.0,
    583981485.0,
    636667506.0,
    643873731.0,
    676820039.0,
    667722549.0,
    629401874.0,
    654448581.0,
    784223075.0,
    789254798.0,
    828502461.0,
    930196929.0,
    881638914.0,
    857166435.0,
    727723136.0,
    801865869.0,
    795886071.0,
    763170247.0,
    787152479.0,
    849750639.0,
    936056142.0,
    915629979.0,
    899615494.0,
    984856075.0,
    1050921103.0,
    1031909734.0,
    1109257706.0,
    1232717908.0,
    1341057959.0,
    1285712637.0,
    1311315033.0,
    1266236889.0,
    1405505328.0,
    1351302005.0,
    1397090480.0,
    1409945274.0,
    1417130893.0,
    1283265090.0,
    1354824248.0,
    1350964981.0,
    1431385638.0,
    1356693322.0,
    1324894757.0,
    1211361619.0,
    1175413415.0,
    1183132092.0,
    1039343103.0,
    1136614538.0,
    1388696469.0,
    1216676110.0,
    1413237707.0,
    1151386048.0,
    1069007206.0,
    1113107246.0,
    1053565430.0,
    1216023821.0,
    1212716430.0,
    1153722574.0,
    1244889331.0,
    1183806248.0,
    1057602772.0,
    915956659.0,
    1053600093.0,
    1157109310.0,
    1199843463.0,
    1232280287.0,
    1261812592.0,
    1249209591.0,
    1179404138.0,
    1084154164.0,
    1045379066.0,
    890214397.0,
    812192380.0,
    926378706.0,
    1203221497.0,
    1385834769.0,
    1486005621.0,
    1641024100.0,
    1644401950.0,
    1603394676.0,
    1621780754.0,
    1590464886.0,
    1662160145.0,
    1751719755.0,
    1817491821.0,
    1952474329.0,
    1976098333.0,
    2064236476.0,
    2341981521.0,
    2567977722.0,
    2818694749.0,
    2955051696.0,
    2931038992.0,
    3300623502.0,
    3466842517.0,
    3658119990.0,
    3968752101.0,
    3942222509.0,
    4086393350.0,
    4058576649.0,
    4174172415.0,
    4058707895.0,
    4045487401.0,
    4104379941.0,
    4242326406.0,
    4314577619.0,
    4365839878.0,
    4528331460.0,
    4611609946.0,
    4627406112.0,
    4839530894.0,
    4982167985.0,
    5309222580.0,
    5475269397.0,
    5793946882.0,
    5936558026.0,
    6191886939.0,
    6549339038.0,
    7075013106.0,
    6895715366.0,
    7596808027.0,
    7492130348.0,
    8027353540.0,
    8276258599.0,
    8745049453.0,
    8979708108.0,
    9406708249.0,
    9997156197.0,
    11190986329.0,
    11349375656.0,
    12519922882.0,
    13632028136.0,
    14705541576.0,
    14425183957.0,
    15310495914.0,
    16206118071.0,
    19482936409.0
  };

  private static final double[] df_total = {
    669,
    976,
    843,
    941,
    1079,
    1054,
    1139,
    1139,
    1172,
    1188,
    1280,
    1425,
    1285,
    1148,
    1325,
    1281,
    1375,
    1608,
    1711,
    1603,
    1876,
    1530,
    2049,
    2096,
    2402,
    2571,
    2006,
    2124,
    2320,
    2338,
    2615,
    2458,
    2501,
    2655,
    2585,
    2946,
    2951,
    2642,
    2813,
    3195,
    3196,
    3048,
    2711,
    2899,
    3086,
    3294,
    3305,
    3291,
    3648,
    3539,
    3910,
    4021,
    4461,
    4706,
    4810,
    4404,
    4728,
    4319,
    4108,
    4572,
    4921,
    3664,
    3364,
    3527,
    4089,
    4265,
    4373,
    4168,
    4509,
    4589,
    4588,
    4674,
    4768,
    4799,
    5190,
    5335,
    5691,
    5657,
    5521,
    5912,
    6659,
    6836,
    7295,
    8091,
    7906,
    7804,
    6198,
    7215,
    7054,
    6480,
    7006,
    7600,
    8320,
    8214,
    8132,
    9184,
    9663,
    9632,
    10193,
    11421,
    12204,
    11923,
    12325,
    12386,
    13406,
    12833,
    13309,
    13533,
    13826,
    12638,
    13278,
    13659,
    14314,
    14064,
    13964,
    13357,
    13449,
    13535,
    12225,
    12588,
    14671,
    12681,
    14781,
    11962,
    11221,
    11609,
    11513,
    12560,
    12610,
    12430,
    13131,
    12339,
    10940,
    10129,
    10781,
    11543,
    12168,
    12393,
    12494,
    12255,
    11539,
    10956,
    10561,
    9221,
    8696,
    9542,
    12452,
    14115,
    14721,
    15754,
    15761,
    15418,
    15307,
    15325,
    16201,
    16994,
    17453,
    18977,
    19292,
    20781,
    24048,
    25762,
    27762,
    29569,
    30661,
    32999,
    35243,
    37636,
    40613,
    40154,
    42050,
    41676,
    43701,
    42413,
    42423,
    43866,
    44785,
    45231,
    45652,
    47094,
    47197,
    46107,
    48446,
    49481,
    52068,
    53730,
    56268,
    57856,
    60672,
    64029,
    69220,
    68159,
    72393,
    71658,
    76662,
    77890,
    82091,
    84104,
    87421,
    91983,
    103405,
    104147,
    117207,
    127066,
    139616,
    138132,
    148342,
    155472,
    206272
  };

  private IndexNgramFeatures() {}

  /** Index all text files under a directory. */
  public static void main(String[] args) {
    String usage = "java org.apache.lucene.ngram.IndexNgramFeatures"
                 + " [-index INDEX_PATH] [-docs DOCS_PATH]\n\n";
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
              doc.add(new TextField("term_" + i, prev_term[i].toLowerCase(), Field.Store.YES));
            }
            
            double mu_tf = 0;
            double total_tf = 0;
            double mu_df = 0;
            double total_df = 0;

            for (int y = 1800; y <= 2008; y++) {
              long tf_year = tf[y - 1800];
              long df_year = df[y - 1800];
              mu_tf    += (double) y * tf_year / tf_total[y - 1800];
              total_tf += tf_year;
              mu_df    += (double) y * df_year / df_total[y - 1800];
              total_df += df_year;

              // if (tf_year != 0) {
              //   doc.add(new StoredField("tf_" + y, tf_year));
              // }
            }

            mu_tf /= total_tf;
            mu_df /= total_df;

            double sigma_tf = 0;
            double sigma_df = 0;

            for (int y = 1800; y <= 2008; y++) {
              long tf_year = tf[y - 1800];
              long df_year = df[y - 1800];
              sigma_tf += (double) tf_year * (y - mu_tf) * (y - mu_tf) / tf_total[y - 1800];
              sigma_df += (double) df_year * (y - mu_df) * (y - mu_df) / df_total[y - 1800];
            }

            sigma_tf /= total_tf;
            sigma_tf = Math.sqrt(sigma_tf);
            sigma_df /= total_df;
            sigma_df = Math.sqrt(sigma_df);

            doc.add(new DoubleField("mu_tf", mu_tf, Field.Store.YES));
            doc.add(new DoubleField("sigma_tf", sigma_tf, Field.Store.YES));
            doc.add(new DoubleField("tf", total_tf, Field.Store.YES));
            doc.add(new DoubleField("mu_df", mu_tf, Field.Store.YES));
            doc.add(new DoubleField("sigma_df", sigma_tf, Field.Store.YES));
            doc.add(new DoubleField("df", total_tf, Field.Store.YES));

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
