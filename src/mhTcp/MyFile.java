package mhTcp;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class MyFile {
	private Path path;
	private byte[] md5;
	
	public MyFile(Path path) {
		this.path = path;
		
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try (DigestInputStream dis = new DigestInputStream(new FileInputStream(path.toFile()),  md)){
			
			final int BUFFER_SIZE = 1024*1024;
			byte[] buffer = new byte[BUFFER_SIZE]; 
			
		    while( dis.read( buffer, 0 , BUFFER_SIZE ) > 0 );
		    
		  	md5 = dis.getMessageDigest().digest();
		    			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Path getPath (){
		return path;
	}
	
	public String getFileName(){
		return path.toFile().getName();
	}
	
	public boolean isMD5equals (byte [] secondMD5){
		return Arrays.equals(md5, secondMD5);
	}
	
	public byte[] getMD5(){
		return md5;
	}
	
	
}
