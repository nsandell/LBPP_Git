package tests;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import bn.messages.DiscreteMessage;

public class TmpSerializeTest {
	
	public static void main(String[] args) throws Exception
	{
		String filename = "/Users/nsandell/test_serialize.tmp";
		FileOutputStream fos = new FileOutputStream(filename);
		ObjectOutputStream os = new ObjectOutputStream(fos);
		
		DiscreteMessage blah = new DiscreteMessage(2);
		blah.setInitial();
		
		HashMap<String, HashMap<String, DiscreteMessage>> msgMap = new HashMap<String, HashMap<String,DiscreteMessage>>();
		
		for(int i = 0; i < 20; i++)
		{
			msgMap.put("X"+i, new HashMap<String, DiscreteMessage>());
			msgMap.get("X"+i).put("X"+i, blah.copy());
			msgMap.get("X"+i).put("X"+(i+1), blah.copy());
		}
		
		
		long startTime = System.currentTimeMillis();
		for(int i = 0; i < 10000; i++)
		{
			os.writeObject(msgMap);
		}
		long endTime = System.currentTimeMillis();
		
		System.out.println("Total time is " + (endTime-startTime) + " milliseconds");
		System.out.println("Average serialization time is " + ((double)(endTime-startTime))/(10000.0*1000.0));
	}

}
