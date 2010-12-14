package tests;

import java.io.File;
import java.net.InetAddress;

public class RDNSTest {
	public static void main(String[] args) throws Exception
	{
		InetAddress addy =InetAddress.getByAddress(new byte[]{(byte) 129,(byte) 170, 70, 74});//173.194.35.104
		System.out.println(addy.getHostName());
		
		System.out.println("DIR : "+(new File(".")).getCanonicalPath());
	}
}
