package assignment2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ServerResponseThread implements Runnable {

	private final Socket socket;

	private PrintWriter stringOutputStream;
	
	public ServerResponseThread(Socket socket){
		this.socket = socket;
		
	}
	
	@Override
	public void run() {
		System.out.println("hello");

		String line;
		boolean badRequest = false;
		boolean fileExists = true;
		try {
			stringOutputStream = new PrintWriter(new DataOutputStream(
					socket.getOutputStream()));
		} catch (IOException e1) {
			System.out.println("Problem setting up String output stream");
			System.out.println("Error " + e1.getMessage());
		}
		
		
		
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
		
	
		

		
		if(badRequest) {
			//400 Bad Request
			
			stringOutputStream.print("HTTP/1.0 400 Bad Request\r\n"
									+ getDate() + "Server: Christian/441\r\n"
									+ "Connection: close\r\n\r\n");
			stringOutputStream.flush();
		}
		
		else {
			
			File requestedObjectFile = new File(requestedObject);
			byte[] fileBytes = new byte[1024];
			
			try {
				fileBytes = getFileByteList(requestedObjectFile);
				
			} catch (FileNotFoundException e) {
				System.out.println("File does not exist");
				fileExists = false;

			} catch (IOException e) {
				System.out.println("Error reading in fileByteList");
				System.out.println("Error " + e.getMessage());
			}
			
			System.out.println(fileBytes.length);
			
			
			
			
			if(!fileExists) {
				//404 Not Found
				
				stringOutputStream.print("HTTP/1.0 404 Not Found\r\n"
						+ getDate() + "Server: Christian/441\r\n"
						+ "Connection: close\r\n\r\n");
				stringOutputStream.flush();
				
				
			}
			
			else {
				
				//200 OK
				
				System.out.println("setting up outStream");

				//need to write byte[] to outputstream
				try {
					OutputStream outputStream = new BufferedOutputStream(socket.getOutputStream(), 2048);
					System.out.println("writing to outStream");
					
					
					int offset = 0;
					
					while(offset + 2048 <= fileBytes.length){
						outputStream.write(fileBytes, offset, 2048);
						outputStream .flush();
						offset += 2048;
					}
					
					outputStream.write(fileBytes, offset, fileBytes.length - offset);
					
					
				} catch (IOException e) {
					e.printStackTrace();
				}

				
				
				
			}
			
		}
		
		scan.close();
		
		
		
	}
	
	
	public byte[] getFileByteList(File f) throws FileNotFoundException, IOException{
		

		InputStream fileInputStream = new FileInputStream(f);

		System.out.println("File exists");
		long length = f.length();
		byte[] fileByteList = new byte[(int) length];
		
		int bytesRead = 0;
		int totalBytesRead= 0;
		

		while( (totalBytesRead < fileByteList.length) && (bytesRead = fileInputStream.read(fileByteList, totalBytesRead, fileByteList.length - totalBytesRead)) >= 0){
			totalBytesRead += bytesRead;
			}
		
		
		fileInputStream.close();
		
		if(totalBytesRead < fileByteList.length){
			throw new IOException("Could not completely read file " + f.getName());
		}
		
		System.out.println("total bytes read in" + totalBytesRead);
		
		
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
	
	public String getDate() {
		SimpleDateFormat dateFormatter=new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss zzz");
		return "Date: " + dateFormatter.format(new Date())+"\r\n";
	}

}
