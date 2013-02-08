// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  4 Nov 2010

package edu.jhu.jerboa.app;

import java.io.*;
import java.net.*;

/**
   @author Benjamin Van Durme

   Connects to a host and port, sends each line of a provided file as a query,
   writes the results to STDOUT, along with the query.

   If the following was a query:
   dog TAB cat
   then the output would be:
   dog TAB cat TAB RESULTS

   based on: http://www.oracle.com/technetwork/java/socket-140484.html#client
*/
public class BasicClient {
  public static void main (String[] args) throws Exception {
    if (args.length != 3) {
	    System.err.println("Usage: java BasicClient host port filename");
	    System.exit(-1);
    }

    try{
	    Socket socket = new Socket(args[0], Integer.parseInt(args[1]));
	    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
	    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

	    BufferedReader reader = new BufferedReader(new FileReader(args[2]));
	    String line;
	    while ((line = reader.readLine()) != null) {
        out.println(line);
        System.out.println(line + "\t" + in.readLine());
	    }
	    reader.close();
	    out.close();
	    in.close();
	    socket.close();
    } catch (UnknownHostException e) {
	    System.out.println("Unknown host: " + args[0]);
	    System.exit(1);
    } catch  (IOException e) {
	    System.out.println("No I/O");
	    System.exit(1);
    }
  }

}
