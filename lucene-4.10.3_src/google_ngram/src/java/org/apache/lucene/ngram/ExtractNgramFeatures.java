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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/** Simple command-line based search demo. */
public class ExtractNgramFeatures {

  private ExtractNgramFeatures() {}

  /** Simple command-line based search demo. */
  public static void main(String[] args) throws Exception {
    String usage =
      "Usage:\tjava org.apache.lucene.ngrams.ExtractNgrams [-index dir] [-field f]";
    if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
      System.out.println(usage);
      System.exit(0);
    }

    String index = "index";
    String field = "term_0";
    // String queries = null;
    // int repeat = 0;
    // boolean raw = false;
    // String queryString = null;
    // int hitsPerPage = 10;
    
    for(int i = 0;i < args.length;i++) {
      if ("-index".equals(args[i])) {
        index = args[i+1];
        i++;
      } else if ("-field".equals(args[i])) {
        field = args[i+1];
        i++;
      // } else if ("-queries".equals(args[i])) {
      //   queries = args[i+1];
      //   i++;
      // } else if ("-query".equals(args[i])) {
      //   queryString = args[i+1];
      //   i++;
      // } else if ("-repeat".equals(args[i])) {
      //   repeat = Integer.parseInt(args[i+1]);
      //   i++;
      // } else if ("-raw".equals(args[i])) {
      //   raw = true;
      // } else if ("-paging".equals(args[i])) {
      //   hitsPerPage = Integer.parseInt(args[i+1]);
      //   if (hitsPerPage <= 0) {
      //     System.err.println("There must be at least 1 hit per page.");
      //     System.exit(1);
      //   }
      //   i++;
      }
    }
    
    IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(index)));
    IndexSearcher searcher = new IndexSearcher(reader);
    // :Post-Release-Update-Version.LUCENE_XY:
    Analyzer analyzer = new KeywordAnalyzer();

    BufferedReader in = null;
    // if (queries != null) {
    //   in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), StandardCharsets.UTF_8));
    // } else {
      in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    // }
    // :Post-Release-Update-Version.LUCENE_XY:
    QueryParser parser = new QueryParser(Version.LUCENE_4_10_0, field, analyzer);
    while (true) {
      // if (queries == null && queryString == null) {                        // prompt the user
        // System.out.println("Enter query: ");
      // }

      String line = in.readLine();

      if (line == null || line.length() == -1) {
        break;
      }

      line = line.trim();
      if (line.length() == 0) {
        break;
      }
      
      Query query = parser.parse(line);
      // System.out.println("Searching for: " + query.toString(field));
            
      // if (repeat > 0) {                           // repeat & time as benchmark
      //   Date start = new Date();
      //   for (int i = 0; i < repeat; i++) {
      //     searcher.search(query, null, 100);
      //   }
      //   Date end = new Date();
      //   System.out.println("Time: "+(end.getTime()-start.getTime())+"ms");
      // }

      doPagingSearch(in, searcher, query);

      // if (queryString != null) {
      //   break;
      // }
    }
    reader.close();
  }

  /**
   * This demonstrates a typical paging search scenario, where the search engine presents 
   * pages of size n to the user. The user can then go to the next page if interested in
   * the next hits.
   * 
   * When the query is executed for the first time, then only enough results are collected
   * to fill 5 result pages. If the user wants to page beyond this limit, then the query
   * is executed another time and all hits are collected.
   * 
   */
  public static void doPagingSearch(BufferedReader in, IndexSearcher searcher, Query query) throws IOException {
 
    // Collect enough docs to show 5 pages
    TopDocs results = searcher.search(query, 50000);
    ScoreDoc[] hits = results.scoreDocs;
    
    int numTotalHits = results.totalHits;
    // System.out.println(numTotalHits + " total matching documents");

    int start = 0;
    // int end = Math.min(numTotalHits, hitsPerPage);
    int end = numTotalHits;

    // while (true) {
      // if (end > hits.length) {
      //   System.out.println("Only results 1 - " + hits.length +" of " + numTotalHits + " total matching documents collected.");
      //   System.out.println("Collect more (y/n) ?");
      //   String line = in.readLine();
      //   if (line.length() == 0 || line.charAt(0) == 'n') {
      //     break;
      //   }

      //   hits = searcher.search(query, numTotalHits).scoreDocs;
      // }
      
      // end = Math.min(hits.length, start + hitsPerPage);
      
      long[] res = new long[209];

      for (int i = start; i < end; i++) {
        // if (raw) {                              // output raw format
        //   System.out.println("doc="+hits[i].doc+" score="+hits[i].score);
        //   continue;
        // }

        // System.out.print((i+1) + ".");
        Document doc = searcher.doc(hits[i].doc);
        for (int j = 0; j < 5; j++) {
          String term = doc.get("term_" + j);
          String pos  = doc.get("pos_" + j);
          if (term == null && pos == null) break;
          // System.out.print(" " + term + ( pos != null ? ("\\" + pos) : ""));
        }

        System.out.print(doc.get("mu_tf"));
        System.out.print(" " + doc.get("sigma_tf"));
        System.out.print(" " + doc.get("tf"));
        System.out.print(" " + doc.get("mu_df"));
        System.out.print(" " + doc.get("sigma_df"));
        System.out.print(" " + doc.get("df"));
        System.out.println();
        
      }
      // for (int j = 1800; j <= 2008; j++) {
      //   System.out.print(res[j-1800]);
      //   if (j != 2008) System.out.print(" ");
      // }
      // System.out.println();

      // if (!interactive || end == 0) {
      //   break;
      // }

      // if (numTotalHits >= end) {
      //   boolean quit = false;
      //   while (true) {
      //     System.out.print("Press ");
      //     if (start - hitsPerPage >= 0) {
      //       System.out.print("(p)revious page, ");  
      //     }
      //     if (start + hitsPerPage < numTotalHits) {
      //       System.out.print("(n)ext page, ");
      //     }
      //     System.out.println("(q)uit or enter number to jump to a page.");
          
      //     String line = in.readLine();
      //     if (line.length() == 0 || line.charAt(0)=='q') {
      //       quit = true;
      //       break;
      //     }
      //     if (line.charAt(0) == 'p') {
      //       start = Math.max(0, start - hitsPerPage);
      //       break;
      //     } else if (line.charAt(0) == 'n') {
      //       if (start + hitsPerPage < numTotalHits) {
      //         start+=hitsPerPage;
      //       }
      //       break;
      //     } else {
      //       int page = Integer.parseInt(line);
      //       if ((page - 1) * hitsPerPage < numTotalHits) {
      //         start = (page - 1) * hitsPerPage;
      //         break;
      //       } else {
      //         System.out.println("No such page");
      //       }
      //     }
      //   }
      //   if (quit) break;
      //   end = Math.min(numTotalHits, start + hitsPerPage);
      // }
    // }
  }
}
