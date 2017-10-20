package assignment2;

/**
 * WebServer Class
 * 
 */

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;


public class WebServer extends Thread {

	ServerSocket serverSocket;
	private volatile boolean shutDown = false;
	private ExecutorService pool = null;
    /**
     * Default constructor to initialize the web server
     * 
     * @param port 	The server port at which the web server listens > 1024
     * 
     */
	public WebServer(int port) {
		
		try{
			serverSocket = new ServerSocket(port, 0, InetAddress.getByName(null));
			pool = Executors.newFixedThreadPool(4);
		}
		catch(IOException e){
			System.out.println("Error " + e.getMessage());
		}
		
	}

	
    /**
     * The main loop of the web server
     *   Opens a server socket at the specified server port
	 *   Remains in listening mode until shutdown signal
	 * 
     */
	public void run() {
		
		try {
			serverSocket.setSoTimeout(1000);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
			while(!shutDown){
				
				try{
					pool.execute(new ServerResponseThread (serverSocket.accept()));
					
				}
				
				catch(SocketTimeoutException e){
					if(shutDown)
						break;
					
				} catch (IOException e) {
					System.out.println("Error " + e.getMessage());
				}
				
				
			}
			
			
		//FOLLOWING SHUTDOWN CODE BLOCK FROM JAVADOC
		//https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html
			
		   pool.shutdown(); // Disable new tasks from being submitted
		   try {
		     // Wait a while for existing tasks to terminate
		     if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
		       pool.shutdownNow(); // Cancel currently executing tasks
		       // Wait a while for tasks to respond to being cancelled
		       if (!pool.awaitTermination(60, TimeUnit.SECONDS))
		           System.err.println("Pool did not terminate");
		     }
		   } catch (InterruptedException ie) {
		     // (Re-)Cancel if current thread also interrupted
		     pool.shutdownNow();
		     // Preserve interrupt status
		     Thread.currentThread().interrupt();
		   }
		   
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
			


	
    /**
     * Signals the server to shutdown.
	 *
     */
	public void shutdown() {
		shutDown = true;
	}

	
	/**
	 * A simple driver.
	 */
	public static void main(String[] args) {
		int serverPort = 2226;

		// parse command line args
		if (args.length == 1) {
			serverPort = Integer.parseInt(args[0]);
		}
		
		if (args.length >= 2) {
			System.out.println("wrong number of arguments");
			System.out.println("usage: WebServer <port>");
			System.exit(0);
		}
		
		System.out.println("starting the server on port " + serverPort);
		
		WebServer server = new WebServer(serverPort);
		
		server.start();
		System.out.println("server started. Type \"quit\" to stop");
		System.out.println(".....................................");

		Scanner keyboard = new Scanner(System.in);
		while ( !keyboard.next().equals("quit") );
		
		System.out.println();
		System.out.println("shutting down the server...");
		server.shutdown();
		keyboard.close();
		
		System.out.println("server stopped");
	}
	
}
