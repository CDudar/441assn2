package assignment2;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Scanner;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ServerResponseThread implements Runnable {

	private final Socket socket;

	
	public ServerResponseThread(Socket socket){
		this.socket = socket;
		
	}
	
	@Override
	public void run() {
		System.out.println("hello");

		String line;
		boolean badRequest = false;
		boolean fileExists = true;
		
		String get_request_string = receiveInputStream(socket);
		
		System.out.println("done getting");
		System.out.println(get_request_string);

		
		Scanner scan = new Scanner(get_request_string);
		line = scan.nextLine();
		String[] args = line.split(" ");
		
		String requestedObject = args[1].substring(1);
		System.out.println(requestedObject);
		
		boolean validGetLine = checkValidGet(line);
		
		
		while(scan.hasNextLine()){
			line = scan.nextLine();
			
			if(line.equals(""))
				break;
			
			System.out.println("current line: " + line);
			
			//check line has semicolon, and that
			if( line.indexOf(":") == -1 || line.indexOf(":") == 0)
				badRequest = true;
			
			String[] fieldArgs = line.split(":");
			
			if(fieldArgs.length > 2)
				badRequest = true;
			
			
			if(badRequest){
				System.out.println("bad request");
			}
			
		}

		
		File requestedObjectFile = new File(requestedObject);
		
		try {
			byte[] fileBytes = getFileByteList(requestedObjectFile);
			
		} catch (FileNotFoundException e) {
			System.out.println("File does not exist");
			fileExists = false;

		} catch (IOException e) {
			System.out.println("Error " + e.getMessage());
		}
		
		
		
		
		
		
		
		
		
	}
	
	
	public byte[] getFileByteList(File f) throws FileNotFoundException, IOException{
		

		InputStream fileInputStream = new FileInputStream(f);

		System.out.println("File exists");
		long length = f.length();
		byte[] fileByteList = new byte[(int) length];
		
		
		
		
		return fileByteList;
	}
	
	
	public boolean checkValidGet(String getLine){
		
		
		String[] getRequestArgs = getLine.split(" ");
		
		if(getRequestArgs.length != 3 
	  || !(getRequestArgs[0].equals("GET"))
	  || !(getRequestArgs[2].equals("HTTP/1.0"))
		)
			return false;

		
		//System.out.println(getRequestArgs[0]);
		//System.out.println(getRequestArgs[1]);
		//System.out.println(getRequestArgs[2]);

		
		return true;
		
	}
	
	public String receiveInputStream(Socket socket){
		
		//integers to represent offset while reading and the number of bytes read
		int off = 0;
		int num_byte_read = 0;
		
		//initialize bytelist to hold data as it is read in
		byte[] get_request_bytes = new byte[2048];
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
		
		return get_request_string;
		
	}

}
