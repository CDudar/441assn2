package assignment1;




/**
 * UrlCache Class
 * 
 *
 */

//import statements
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;


public class UrlCache {

	//catalogue declaration
	HashMap<String, String> catalogue;
	
	
    /**
     * Default constructor to initialize data structures used for caching/etc
	 * If the cache already exists then load it. If any errors then throw runtime exception.
	 *
     * @throws IOException if encounters any errors/exceptions
     */
	@SuppressWarnings("unchecked")
	public UrlCache() throws IOException {
		
		
			//attempt to open the local cache
			//if it exists, load in the url keys and lastModified vals into the catalogue
			try {
				FileInputStream fis = new FileInputStream("catalogueFile.ser");
				ObjectInputStream ois = new ObjectInputStream(fis);
				catalogue = (HashMap<String, String>) ois.readObject();
				ois.close();
			} 
			//if the local cache does not exist, initialize a new catalogue
			catch(FileNotFoundException e) {
				catalogue = new HashMap<String, String>();
			}
			
			catch (ClassNotFoundException e) {
				System.out.println("Error: " + e.getMessage());
			}

	}
	
    /**
     * Downloads the object specified by the parameter url if the local copy is out of date.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     * @throws IOException if encounters any errors/exceptions
     */
	public void getObject(String url) throws IOException {
		System.out.println("getObject beginning");
		//Variable initialization
		String hostName;
		String pathName;
		int portNumber = 80; //Initialize portNumber to default: 80
		
		String line = "";
		
		PrintWriter outputStream;

		/* url String Parsing */
		hostName = url.substring(0, url.indexOf("/"));
		pathName = url.substring(url.indexOf("/"));
		
		
		
		//If URL has colon, grab port-number
		if(url.indexOf(":") != -1) {
			hostName = url.substring(0, url.indexOf(":"));
			portNumber = Integer.parseInt(url.substring(url.indexOf(":") + 1, url.indexOf("/")));
			
		}
		portNumber = 2225;

		try {
			// connects to port server app listening at port 8888 in the same
			// machine
			Socket socket = new Socket( "localhost" , portNumber);

			// Create necessary streams
			outputStream = new PrintWriter(new DataOutputStream(
					socket.getOutputStream()));
			
			
			//If local catalogue has url, Get the associated last-modified value
			String catalogueLastMod = "";
			if(catalogue.containsKey(url)){
				catalogueLastMod = catalogue.get(url);
			}
			
			//HTTP GET Request
			outputStream.print("GET " + pathName + " HTTP/1.0\r\n");
			
			//Check if requested page has been modified since last time it was downloaded
			outputStream.print("If-modified-since: "  + catalogueLastMod + "\r\n");
			outputStream.print("Host: "+ hostName + "\r\n");
			outputStream.print("\r\n");
			outputStream.flush();

			System.out.println("just sent get req");
			
			//Initialize byte list to hold object data
			byte[] http_object_bytes = new byte[4096];
			
			/*read-in http header */
			String http_response_header_string = getHTTPHeader(socket);

			System.out.println("done getting header");
			
			Scanner headScanner = new Scanner(http_response_header_string);
			String lastModified = "";
			int objectLength = 0;
			
			//parse out last-modified and content-length values from header
			while(headScanner.hasNextLine()) {
				line = headScanner.nextLine();
				
				if(line.contains("Last-Modified")) {
					lastModified = line.substring(line.indexOf(":") + 2);
				}else if(line.contains("Content-Length")) {
					objectLength = Integer.parseInt(line.substring(line.indexOf(":") + 2));
				}
			}
	
			headScanner.close();

			System.out.println(http_response_header_string);
			

			if(http_response_header_string.contains("304 Not Modified")) {
				//Do nothing, page has not been modified since the last time it was downloaded
				System.out.println(url + " - File already in local cache, not downloading");
			}
			//If the cache does not hold the page, download it
			else if(http_response_header_string.contains("200 OK")){
				
				System.out.println("Downloading - " + url);
				
				//Recreating directory structure locally
				File f = new File(hostName + pathName);
				f.getParentFile().mkdirs();
				FileOutputStream fos = new FileOutputStream(f);
				//fos.close();

				int counter = 0; //keeps track of amount of bytes read
				int num_byte_read = 0;
				
				while(num_byte_read != -1) {
					
					//read in bytes until the entire objects size has been read in
					if(counter == objectLength) {
						break;
					}	
					//read some amount of bytes and write them to file output stream
					try {
					num_byte_read = socket.getInputStream().read(http_object_bytes);
					fos.write(http_object_bytes);
					fos.flush();
					fos.getFD().sync();
					}
					catch(IOException e) {
						System.out.println("Error downloading document, IOEXCEPTION");
					}

					
					//increment counter by how many bytes were read for this iteration
					counter+= num_byte_read;
					
				}

				
				fos.close();
				
				
				//populate catalogue with url key and lastmodified value
				catalogue.put(url, lastModified);
					
				//Write and save the catalogue into a local file
				FileOutputStream fosObj = new FileOutputStream("catalogueFile.ser");
				ObjectOutputStream oos = new ObjectOutputStream(fosObj);
				oos.writeObject(catalogue);
				oos.flush();
				oos.close();
						
			}
			
			
			socket.close();

				
		}
		catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		}
		
		System.out.println("------------------------------------------");

	}
	
    /**
     * Returns the Last-Modified time associated with the object specified by the parameter url.
	 *
     * @param url 	URL of the object 
	 * @return the Last-Modified time in millisecond as in Date.getTime()
     */
	public long getLastModified(String url) throws RuntimeException {
		
		if(catalogue.containsKey(url)) {
			String lastModified = catalogue.get(url);
			SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss zzz");
			Date date = format.parse(lastModified, new ParsePosition(0));
			long millis = date.getTime();
			return millis;
		}
		else {
			throw new RuntimeException();
		}


	}
	
	
	public String getHTTPHeader(Socket socket) throws IOException {
		
		//integers to represent offset while reading and the number of bytes read
		int off = 0;
		int num_byte_read = 0;
		
		//initialize bytelist to hold data as it is read in
		byte[] http_response_header_bytes = new byte[2048];
		//String to hold header
		String http_response_header_string = "";

		/*read http header*/
		try {
		while(num_byte_read != -1) {
			
			//Read in one byte at a time until the end of the header is reached
			socket.getInputStream().read(http_response_header_bytes, off, 1);				
			off++;
			http_response_header_string = new String(http_response_header_bytes, 0, off, "US-ASCII");
			if(http_response_header_string.contains("\r\n\r\n"))
					break;
			}
		}
		catch(IOException e) {
			throw new IOException();
		}
		
		//return header string
		return http_response_header_string;
	}
	
	
	

}