package bn;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

public class BNDefinitionLoader
{
	public static class BNIOException extends Exception
	{
		private static final long serialVersionUID = 1L;
		public BNIOException(String message, Exception inner){super(message,inner);}
		public BNIOException(String message){super(message);}
	}
	
	public static StaticBayesianNetwork loadStaticNetworkFile(String filename) throws BNIOException
	{
		StaticBayesianNetwork bn = new StaticBayesianNetwork();
		BufferedReader input;
		try {
			input = new BufferedReader(new FileReader(filename));
			String line;

			while((line = input.readLine())!=null)
			{
				line = line.split("%")[0]; //Kill comments at the end of line
				if(line.matches("^[ ]*$"))
					continue;
				
				if(linebits.length<2)
					throw new BNIOException("Require two pieces of data for a node definition : Name and type.");
				String nodeName = linebits[0];
				String nodeType = linebits[1];
				switch(NodeTypes.valueOf(nodeType))
				{
					case Discrete:
						DiscreteBNNode.loadFromFile(bn, nodeName, linebits);
				}
				numNodesRead++;
			}
			
		} catch(Exception e) {
			throw new BNIOException("Error while loading file " + filename + " : " + e.toString(), e);
		}
		return null;
	}
	
	public static enum NodeTypes
	{
		Discrete,
	}
	
	public static enum CPDTypes
	{
		DiscreteUnconditional,
		DiscreteCPT,
		SparseDiscreteCPT,
		NoisyOr
	}
	
	public static DynamicBayesianNetwork loadDynamicNetworkFile(String filename)
	{
		return null;
	}
}
