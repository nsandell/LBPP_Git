package tests;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import bn.IDiscreteBayesNode;
import bn.IStaticBayesNet;
import bn.commandline.StaticNetCommandLine;
import bn.messages.DiscreteMessage;

public class StaticTestCase{
	public static void main(String[] args) // System.err should not print anything if we're goo
	{
		// First arg is a lbp file, second an answer file
		try
		{
			IStaticBayesNet bn = StaticNetCommandLine.loadNetwork(args[0]);
			bn.validate();
			HashMap<String, HashMap<String,Double[]>> answers = loadAnswers(args[1]);
			for(String keyval : answers.keySet())
			{
				for(String node : bn.getNodeNames())
					bn.clearEvidence(node);
				String[] bits = keyval.split("=");
				String nname = bits[0];
				int value = Integer.parseInt(bits[1]);
				bn.addDiscreteEvidence(nname, value);
				bn.run(100, 0);
				HashMap<String,Double[]> dists = answers.get(keyval);
				for(String distnode : dists.keySet())
				{
					IDiscreteBayesNode nd = (IDiscreteBayesNode)bn.getNode(distnode);
					if(!compareMarg(dists.get(nd.getName()), nd.getMarginal()))
						throw new Exception("Failed: Node " + distnode + " has difference.");
				}
			}
		
		} catch(Exception e) {
			e.printStackTrace();
			System.err.println("Error : " + e.getMessage());
		}
	}
	
	public static boolean compareMarg(Double[] m1, DiscreteMessage m2)
	{
		if(m1.length!=m2.getCardinality())
			return false;
		for(int i = 0; i < m1.length; i++)
		{
			if(Math.abs(m1[i]-m2.getValue(i)) > 1e-5)
				return false;
		}
		return true;
	}
	
	public static HashMap<String, HashMap<String,Double[]>> loadAnswers(String filename) throws IOException
	{
		HashMap<String, HashMap<String, Double[]>> ret = new HashMap<String, HashMap<String,Double[]>>();
		
		BufferedReader br = new BufferedReader(new FileReader(filename));
		
		String[] nodes = br.readLine().split(" ");
		String line = null;
		while((line=br.readLine())!=null)
		{
			//Should be like A=1
			ret.put(line, new HashMap<String, Double[]>());
			HashMap<String,Double[]> dists = ret.get(line);
			for(int i = 0; i < nodes.length; i++)
			{
				line = br.readLine();
				if(line==null) throw new IOException("Expected more pvecs");
				dists.put(nodes[i],parse(line));
			}
		}
		return ret;
	}
	
	private static Double[] parse(String line)
	{
		String[] bits= line.split(" ");
		Double[] ret = new Double[bits.length];
		for(int i = 0; i < bits.length; i++)
			ret[i] = Double.parseDouble(bits[i]);
		return ret;
	}
}
