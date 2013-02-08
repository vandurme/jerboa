// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  9 Nov 2011

package edu.jhu.jerboa.sim;

import java.io.*;
import java.net.*;
import java.util.logging.Logger;
import edu.jhu.jerboa.sim.ISimilarity;
import edu.jhu.jerboa.util.JerboaProperties;
import edu.jhu.jerboa.util.FileManager;
import java.util.Arrays;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.text.DecimalFormat;


/**
   @author Benjamin Van Durme

   A simple server that wraps an SLSH, and optionally a PLEBIndex object

   Properties:

   SimServer.port : (int) port number

   SimServer.indexFile : (String) OPTIONAL if provided, will deserialize
   the given PLEBIndex object

   The server differentiates queries to SLSH, PLEBIndex, ..., based on the first
   portion of the query string. For example, SLSH queries begin with the
   character 's', followed by a tab.

   For SLSH queries:

   Clients connect, then issue queries one per line, of the form:
   's' TAB key TAB key (TAB key)*

   with all scores for the given line being written back on a single line, space
   separated.

   For PLEBIndex queries:

   Clients connect, then issue queries one per line, of the form:
   'p' TAB key (TAB k (TAB B (TAB P)))

   where:

   k : top k elements to return
   B : beam width, per permutation
   P : max number of permutations to use (the PLEBIndex may not have as many
   permutations constructed as requested by P, in which case, it will just
   use the total number of permutations available)

   A key always needs to be provided, but k, then B, then P are optional
*/
public class SimServer {
  private static Logger logger = Logger.getLogger(SimServer.class.getName());
  static final DecimalFormat formatter = new DecimalFormat("#.####");

  class SimServerWorker implements Runnable {
    Socket socket;
    SLSH slsh;
    PLEBIndex pleb;
    Logger logger;
    DecimalFormat formatter;

    SimServerWorker (Socket socket, SLSH slsh, PLEBIndex pleb, Logger logger, DecimalFormat formatter) {
	    this.formatter = formatter;
	    this.socket = socket;
	    this.slsh = slsh;
	    this.pleb = pleb;
	    this.logger = logger;
    }

    public void run () {
	    PrintWriter out = null;
	    BufferedReader in = null;
	    String query;  // one query per line
	    String[] tokens; // query is split into tokens
	    String[] keys; //keys in a query (SLSH)
	    double[] scores; // results for a list of keys (SLSH)
	    int[] strengths; // strength values for each key (SLSH)
	    int B; // beam width (PLEBIndex)
	    int P; // max number of permutations to use (PLEBIndex)
	    int k; // top k (PLEBIndex)
	    StringBuffer sb; // appended into this buffer
	    SimpleImmutableEntry<String,Double>[] best; // (PLEBIndex)

	    try {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out = new PrintWriter(socket.getOutputStream(), true);
        while ((query = in.readLine()) != null) {
          tokens = query.split("\\t");
          if (tokens[0].matches("s")) {
            keys = Arrays.copyOfRange(tokens,1,tokens.length);
            scores = slsh.score(keys);
            sb = new StringBuffer();
            strengths = slsh.getStrength(keys);
            sb.append(strengths[0]);
            for (int i = 1; i < strengths.length; i++)
              sb.append(" " + strengths[i]);
            sb.append("\t");
            sb.append(formatter.format(scores[0]));
            for (int i = 1; i < scores.length; i++)
              sb.append(" " + formatter.format(scores[i]));
            out.println(sb);
          }
          else if (tokens[0].matches("p")) {
            k = 10;
            B = 1000;
            P = pleb.sorts.length;
            if (tokens.length >= 3)
              k = Integer.parseInt(tokens[2]);
            if (tokens.length >= 4)
              B = Integer.parseInt(tokens[3]);
            if (tokens.length == 5)
              P = Integer.parseInt(tokens[4]);

            best = pleb.kbest(tokens[1],k,B,P).toArray();
            if (best.length == 0) {
              out.print("UNK\n");
            } else {
              sb = new StringBuffer();
              for (int i = 0; i < best.length; i++) {
                if (i > 0)
                  sb.append("\t");
                sb.append(best[i].getKey() + "\t" +
                          //formatter.format(best[i].getValue()) + "\n");
                          formatter.format(best[i].getValue()));
              }
              sb.append("\n");
              out.print(sb);
            }
            out.flush();
          }
        }
        out.close();
        in.close();
	    } catch (Exception e) {
        logger.warning(e.toString());
	    }
    }
  }

  public void run () throws Exception {
    ServerSocket serverSocket;
    int port = JerboaProperties.getInt("SimServer.port");

    logger.info("Starting server on port " + port);
    serverSocket = new ServerSocket(port);

    SLSH slsh = SLSH.load();
    String plebIndexFilename = JerboaProperties.getString("SimServer.indexFile",null);
    PLEBIndex pleb = null;
    if (plebIndexFilename != null) {
	    logger.info("Reading PLEBIndex object [" + plebIndexFilename + "]");
	    ObjectInputStream in = new ObjectInputStream(new FileInputStream(plebIndexFilename));
	    pleb = (PLEBIndex) in.readObject();
	    pleb.slshAlign(slsh);
	    in.close();
    }

    logger.info("Server ready");

    Socket socket;
    while ((socket = serverSocket.accept()) != null) {
	    SimServerWorker worker = new SimServerWorker(socket,slsh,pleb,logger,formatter);
	    Thread thread = new Thread(worker);
	    thread.start();
    }
    serverSocket.close();
  }


  public static void main(String[] args) throws Exception {
    SimServer server = new SimServer();
    server.run();
  }
}