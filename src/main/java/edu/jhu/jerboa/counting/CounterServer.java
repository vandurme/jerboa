// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  15 Aug 2012

package edu.jhu.jerboa.counting;

import java.io.*;
import java.net.*;
import java.util.logging.Logger;
import edu.jhu.jerboa.counting.ICounterContainer;
import edu.jhu.jerboa.util.JerboaProperties;
import edu.jhu.jerboa.util.FileManager;
import java.util.Arrays;
import java.util.AbstractMap.SimpleImmutableEntry;

/**
   @author Benjamin Van Durme
*/
public class CounterServer {
  private static Logger logger = Logger.getLogger(CounterServer.class.getName());

  class CounterServerWorker implements Runnable {
    Socket socket;
    ICounterContainer counter;
    Logger logger;

    CounterServerWorker (Socket socket, ICounterContainer counter, Logger logger) {
	    this.socket = socket;
      this.counter = counter;
	    this.logger = logger;
    }

    public void run () {
	    DataOutputStream out = null;
	    BufferedReader in = null;
	    String query;  // one query per line

	    try {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out = new DataOutputStream(socket.getOutputStream());
        while ((query = in.readLine()) != null) {
          out.write(counter.get(query));
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
    int port = JerboaProperties.getInt("CounterServer.port");

    logger.info("Starting server on port " + port);
    serverSocket = new ServerSocket(port);

    Class c = Class.forName(JerboaProperties.getString("CounterServer.counter"));
    ICounterContainer counter = (ICounterContainer) c.newInstance();

    logger.info("Server ready");

    Socket socket;
    while ((socket = serverSocket.accept()) != null) {
	    CounterServerWorker worker = new CounterServerWorker(socket,counter,logger);
	    Thread thread = new Thread(worker);
	    thread.start();
    }
    serverSocket.close();
  }

  public static void main(String[] args) throws Exception {
    CounterServer server = new CounterServer();
    server.run();
  }
}

