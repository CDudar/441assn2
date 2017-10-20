package assignment2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Scanner;

import org.omg.CORBA.SystemException;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ServerResponseThread implements Runnable {

	private final Socket socket;
	
	private File requestedObjectFile;
	private PrintWriter stringOutputStream;
	private DataOutputStream byteOutputStream;
	
	public ServerResponseThread(Socket socket){
		this.socket = socket;
		
	}
	
	@Override
	public void run() {
		System.out.println("Connection established");

		String line;
		
		FileInputStream fileInputStream = null;
		
		boolean badRequest = false;
		boolean fileExists = true;
		
		//Set up string outputstream
		try {
			byteOutputStream = new DataOutputStream(socket.getOutputStream());
			stringOutputStream = new PrintWriter(byteOutputStream);
		} catch (IOException e1) {
			System.out.println("Problem setting up String output stream");
			System.out.println("Error " + e1.getMessage());
		}
		
		
		//take in get request until \r\n\r\n encountered
		String get_request_string = receiveInputStream(socket);
		
		Scanner scan = new Scanner(get_request_string);
		line = scan.nextLine();
		String[] args = line.split(" ");
		
		//requested object url is the second argument of get request
		String requestedObject = "";
		try {
		requestedObject = args[1].substring(1);
		}
		catch(ArrayIndexOutOfBoundsException e) {
			badRequest = true;
		}
		
		
		//check if getLine is well-formed
		boolean validGetLine = checkValidGet(line);
		
		
		//check every line past the initial GET line
		while(scan.hasNextLine()){
			line = scan.nextLine();
			
			//break out when blank line is encountered
			if(line.equals(""))
				break;
			
			//check each line has a semicolon, 
			//and that there is at least 1 char before it (field value)
			if( line.indexOf(":") == -1 || line.indexOf(":") == 0)
				badRequest = true;

		}
		
		if(!validGetLine)
			badRequest = true;
		
		
		if(badRequest) {
			//400 Bad Request String
			//Create and send header response
			
			stringOutputStream.print("HTTP/1.0 400 Bad Request\r\n"
									+ getDate() + "Server: Christian/441\r\n"
									+ "Connection: close\r\n\r\n");
			stringOutputStream.flush();
		}
		
		else {
			
			
			//Make requested object file
			requestedObjectFile = new File(requestedObject);
			
			//Open fileInputStream to object file and see if file exists
			try {
			fileInputStream = new FileInputStream(requestedObjectFile);
				
			} catch (FileNotFoundException e) {
				fileExists = false;

			} 
			
			
			
			
			if(!fileExists) {
				//404 Not Found
				//Create and send header response
				stringOutputStream.print("HTTP/1.0 404 Not Found\r\n"
						+ getDate() + "Server: Christian/441\r\n"
						+ "Connection: close\r\n\r\n");
				stringOutputStream.flush();
				
				
			}
			
			else {
				
				//200 OK
				
				//Create Header response
				String httpHeader = "HTTP/1.0 200 OK\r\n"
						+ getDate() + "Server: Christian/441\r\n" +
						getLastModified() + getContentLength() + getContentType()
						+ "Connection: close"
						+ "\r\n\r\n";
				
				byte[] httpHeaderBytes = null;
				
				//Convert header response to low level bytes
				try {
					httpHeaderBytes = httpHeader.getBytes("US-ASCII");
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
				
				//Send header response as bytes
				try {
					socket.getOutputStream().write(httpHeaderBytes);
					socket.getOutputStream().flush();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				
				System.out.println("Setting up output-stream");


				long contentLength = requestedObjectFile.length();
				
				//need to write byte[] to outputstream
				try {
					
					
					byte[] buffer = new byte[(int) contentLength];
					
					//Read the files bytes into a buffer
					fileInputStream.read(buffer);
					
					//write the buffer to outputStream and flush
					socket.getOutputStream().write(buffer);
					socket.getOutputStream().flush();

					fileInputStream.close();
					
				} catch (IOException e) {
					e.printStackTrace();
				}

				
				
				
			}
			
		}
		
		//Close socket connection
		try {
			System.out.println("Closing connection");
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Close scanner
		scan.close();
		
		
	}
	
	
	public boolean checkValidGet(String getLine){
		
		
		String[] getRequestArgs = getLine.split(" ");
		
		//Make sure all get request args are correct
		//Supports HTTP 1.0 and HTTP 1,1
		if(getRequestArgs.length != 3 
	  || !(getRequestArgs[0].equals("GET"))
	  || (!(getRequestArgs[2].equals("HTTP/1.0"))
	  && !(getRequestArgs[2].equals("HTTP/1.1")))
			)
		
			return false;


		return true;
		
	}
	
	public String receiveInputStream(Socket socket){
		
		//integers to represent offset while reading and the number of bytes read
		int off = 0;
		int num_byte_read = 0;
		
		//initialize bytelist to hold data as it is read in
		byte[] get_request_bytes = new byte[4096];
		//String to hold request
		String get_request_string = "";

		/*read get request*/
		try {
		while(num_byte_read != -1) {
			
			//Read in one byte at a time until the end of get request is reached
			socket.getInputStream().read(get_request_bytes, off, 1);				
			off++;
			get_request_string = new String(get_request_bytes, 0, off, "US-ASCII");
			if(get_request_string.contains("\r\n\r\n"))
					break;
			}
		}
		catch(IOException e) {
			System.out.println("Error " + e.getMessage());
		}
		catch(StringIndexOutOfBoundsException e1) {
		}
		
		return get_request_string;
		
	}
	
	public String getLastModified() {
		SimpleDateFormat dateFormatter=new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss zzz");
		return "Last-Modified: " + dateFormatter.format(requestedObjectFile.lastModified()) + "\r\n";
	}
	
	public String getContentLength() {
		return("Content-Length: " + requestedObjectFile.length()) + "\r\n";
		
	}
	
	public String getContentType(){
		try {
			return "Content-Type: "
					+ Files.probeContentType(requestedObjectFile.toPath())
					+ "\r\n";
		} catch (IOException e) {
			return "Content-Type: \r\n";
		}
		
	}
	
	public String getDate() {
		SimpleDateFormat dateFormatter=new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss zzz");
		return "Date: " + dateFormatter.format(new Date())+"\r\n";
	}

}
