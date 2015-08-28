package org.apache.lucene.ngram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileNotFoundException;
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
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.GroupLayout.*;
 
public class NgramsFrontend extends JFrame
                           implements DocumentListener {
    
    public class DataPoint<E> implements Comparable<DataPoint<E>> {
      public String label;
      public E cargo;
      public double relevance;
      public DataPoint(String s, E e, double r) {
        label = s;
        cargo = e;
        relevance = r;
      }
      public String toString() {
        return label;
      }
      public int compareTo(DataPoint<E> other) {
        return (new Double(this.relevance)).compareTo(other.relevance);
      }
    }

    private JTextField  entry;
    private JLabel      jLabel1;
    private JLabel      status;
    private JList<DataPoint<List<Double>>> results;
    private DefaultListModel<DataPoint<List<Double>>> resultsModel;
    private JScrollPane resultList;
    private Graph       graph;

    private List<Double> nullData;
    private Map<Integer, Long> totalCounts;
    private String index;
    private String field;
    private String queries;
    private int repeat;
    private boolean raw;
    private String queryString;
    private int hitsPerPage;
    
    private IndexReader reader;
    private IndexSearcher searcher;
    private Analyzer analyzer;
    private QueryParser parser;


    final static Color  HILIT_COLOR = Color.LIGHT_GRAY;
    final static Color  ERROR_COLOR = Color.PINK;
    final static String CANCEL_ACTION = "cancel-search";
     
    final Color entryBg;
     
    public NgramsFrontend() {

        index = "index";
        field = "term_0";
        queries = null;
        repeat = 0;
        raw = false;
        queryString = null;
        hitsPerPage = 100;

        nullData = new ArrayList<Double>();
        for (int i = 0; i < 209; i++) {
            nullData.add(0.0);
        }
        // totalCounts = new ArrayList<Integer>();
        totalCounts = new HashMap<Integer, Long>();
        try (BufferedReader br = new BufferedReader(new FileReader("googlebooks-eng-all-totalcounts-20120701.txt"))) {
          String line;
          while ((line = br.readLine()) != null) {
            for (String tokens : line.split("\\s+")) {
              if (tokens.isEmpty()) continue;
              String[] elems = tokens.split(",");
              int year  = Integer.parseInt(elems[0].trim());
              long count = Long.parseLong(elems[1].trim());
              // System.out.println("" + year + ": " + count);
              totalCounts.put(year, count);
            }
          }
        } catch (IOException e) {
          // Deal with it
        }
        // for (int y = 1800; y <= 2008; y++) {
        //   if (!totalCounts.containsKey(y)) {
        //   }
        // }

        try {
          reader = DirectoryReader.open(FSDirectory.open(new File(index)));
        } catch (IOException e) {
          // Do stuff
        }
        searcher = new IndexSearcher(reader);
        // :Post-Release-Update-Version.LUCENE_XY:
        analyzer = new KeywordAnalyzer();
        parser = new QueryParser(Version.LUCENE_4_10_0, field, analyzer);

        initComponents();
        
        entryBg = entry.getBackground();
        entry.getDocument().addDocumentListener(this);
         
        InputMap im = entry.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = entry.getActionMap();
        im.put(KeyStroke.getKeyStroke("ESCAPE"), CANCEL_ACTION);
        am.put(CANCEL_ACTION, new CancelAction());
    }
    
    private void initComponents() {

        entry     = new JTextField();
        graph     = new Graph(nullData);
        status    = new JLabel();
        jLabel1   = new JLabel();
        resultsModel = new DefaultListModel<DataPoint<List<Double>>>();
        results   = new JList<DataPoint<List<Double>>>(resultsModel);
        resultList = new JScrollPane(results);
        
        results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Ngrams lookup");

        jLabel1.setText("Query:");

        results.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                if (results.getSelectedIndex() != -1) {
                  // if (!event.getValueIsAdjusting()) {
                    // System.out.println("[" + event.getFirstIndex() + ", " + event.getLastIndex() + "]");
                    // System.out.println("" + results.getSelectedIndex());
                    graph.setScores(resultsModel.get(results.getSelectedIndex()).cargo);
                  // }
                } else {
                  graph.setScores(nullData);
                }
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        
        layout.setHorizontalGroup(
          layout.createParallelGroup()

            .addGroup(
              layout.createSequentialGroup()
                .addGroup( // Left pane
                  layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup( // Search box
                      layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addComponent(entry)
                    )
                    .addComponent(resultList, 100, 200, 3000)
                )
                .addComponent(graph, 300, 500, 1000)
            )
            .addComponent(status)
        );
        layout.setVerticalGroup(
          layout.createSequentialGroup()

            .addGroup(
              layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
              .addGroup( // Left pane
                layout.createSequentialGroup()
                .addGroup( // Search box
                  layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                  .addComponent(jLabel1)
                  .addComponent(entry)
                )
                .addComponent(resultList)
              )
              .addComponent(graph, 240, 400, 800)
            )
            .addComponent(status)
        );
 

        // ParallelGroup hGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        
        // SequentialGroup h1 = layout.createSequentialGroup();
        // ParallelGroup h2 = layout.createParallelGroup(GroupLayout.Alignment.TRAILING);
        
        // h1.addContainerGap();
        
        // h2.addComponent(results, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE);
        // h2.addComponent(status, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE);
        
        // SequentialGroup h3 = layout.createSequentialGroup();
        // h3.addComponent(jLabel1);
        // h3.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
        // h3.addComponent(entry, GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE);
        
        // h2.addGroup(h3);
        // h1.addGroup(h2);
     
        // h1.addContainerGap();
        
        // hGroup.addGroup(GroupLayout.Alignment.TRAILING, h1);
        // layout.setHorizontalGroup(hGroup);
        
        
        // ParallelGroup vGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        
        // SequentialGroup v1 = layout.createSequentialGroup();
        // v1.addContainerGap();
        
        // ParallelGroup v2 = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
        // v2.addComponent(jLabel1);
        // v2.addComponent(entry, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
        
        // v1.addGroup(v2);
        // v1.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED);
        // v1.addComponent(results, GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE);
        // v1.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED);
        // v1.addComponent(status);
        // v1.addContainerGap();
        
        // vGroup.addGroup(v1);
        
        // layout.setVerticalGroup(vGroup);

        pack();

        message("Hello world");
    }
 
    public void search() {

        // results.clearSelection();
        resultsModel.clear();
        Set<DataPoint<List<Double>>> searchResults = new TreeSet<DataPoint<List<Double>>>(Collections.reverseOrder());

        String line = entry.getText();
        if (line.length() <= 0) {
            message("Nothing to search");
            return;
        }

        try {
          Query query = parser.parse(line);
          message("Searching for: " + query.toString(field));

          TopDocs results = searcher.search(query, 5 * hitsPerPage);
          ScoreDoc[] hits = results.scoreDocs;
          
          int numTotalHits = results.totalHits;
          message(numTotalHits + " total matching documents");

          int start = 0;
          // int end = Math.min(numTotalHits, hitsPerPage);
          int end = numTotalHits;
          
          for (int i = start; i < end; i++) {
            
            // System.out.print((i+1) + ".");
            Document doc = searcher.doc(hits[i].doc);
            // String res = "" + (i+1) + ".";
            String res = "";
            for (int j = 0; j < 5; j++) {
              String term = doc.get("term_" + j);
              String pos  = doc.get("pos_" + j);
              if (term == null && pos == null) break;
              res += " " + term + ( pos != null ? ("\\" + pos) : "");
            }
            
            double relevance = 0;
            // boolean check = false;
            List<Double> data = new ArrayList<Double>();
            for (int y = 1800; y <= 2008; y++) {
              String tf = doc.get("tf_" + y);
              // String df  = doc.get("df_" + j);
              if (tf == null) {
                data.add(0.0);
              } else {
                // check = true;

                // We should only have occurrences in years when
                // there were actually books published
                assert totalCounts.containsKey(y);
                data.add(Double.parseDouble(tf) / totalCounts.get(y));
                relevance += Double.parseDouble(tf);
              }
            }

            // if (!check) {
            //   System.out.println(res);
            // }

            searchResults.add(new DataPoint<List<Double>>(res, data, relevance));

            // System.out.println();
            // for (int j = 1800; j < 2008; j++) {
            //   String tf = doc.get("tf_" + j);
              // String df  = doc.get("df_" + j);
            //   if (tf != null) {
            //     System.out.print("tf_" + j + " = " + tf + " ");
            //   }
            // }
            // System.out.println();
                      
          }

        } catch (ParseException e) {
          // Do something better
          message("Failed to parse line");
          return;
        } catch (IOException e) {
          // Do something better
          message("Failed to retrieve data");
          return;
        }

        for (DataPoint<List<Double>> item : searchResults) {
          resultsModel.addElement(item);
        }

        // String content = textArea.getText();
        // int index = content.indexOf(s, 0);
        // if (index >= 0) {   // match found
        //     try {
        //         int end = index + s.length();
        //         hilit.addHighlight(index, end, painter);
        //         textArea.setCaretPosition(end);
        //         entry.setBackground(entryBg);
        //         message("'" + s + "' found. Press ESC to end search");
        //     } catch (BadLocationException e) {
        //         e.printStackTrace();
        //     }
        // } else {
        //     entry.setBackground(ERROR_COLOR);
        //     message("'" + s + "' not found. Press ESC to start a new search");
        // }
    }
 
    void message(String msg) {
        status.setText(msg);
    }
 
    // DocumentListener methods
     
    public void insertUpdate(DocumentEvent ev) {
        search();
    }
     
    public void removeUpdate(DocumentEvent ev) {
        search();
    }
     
    public void changedUpdate(DocumentEvent ev) {
    }
     
    class CancelAction extends AbstractAction {
        public void actionPerformed(ActionEvent ev) {
            // hilit.removeAllHighlights();
            entry.setText("");
            entry.setBackground(entryBg);
        }
    }
    
    public static void main(String args[]) {
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            new NgramsFrontend().setVisible(true);
          }
      });
    }
}