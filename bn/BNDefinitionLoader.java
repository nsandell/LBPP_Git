package bn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.regex.*;
import util.Parser;
import util.Parser.LineHandler;
import util.Parser.ParserException;

import bn.distributions.DiscreteDistribution;

public class BNDefinitionLoader
{
	
	static String staticNodeRegex = "^\\w+\\s*:\\s*\\w+(\\((\\w+(,\\w+)*)?\\))?\\s*$";
	static String staticEdgeRegex1 = "\\w+\\s*->\\s*\\w+";
	static String staticEdgeRegex2 = "\\w+\\s*<-\\s*\\w+";
	static String staticCPDRegex = "\\w+~\\w+";
	
	static class StaticNewNodeHandler implements Parser.LineHandler
	{
		public StaticNewNodeHandler(StaticBayesianNetwork net)
		{
			nodeNamePatt = Pattern.compile("^\\w+");
			nodeTypePatt = Pattern.compile(":\\s*\\w+");
			argsPatt = Pattern.compile("(\\((\\w+(,\\w+)*)?\\))");
			this.net = net;
		}
		
		public String getPrompt() {return null;}

		public boolean parseLine(String line) throws ParserException {
			
			Matcher matcher = nodeNamePatt.matcher(line);
			matcher.find();
			String nodeName = line.substring(0, matcher.end());
			matcher = nodeTypePatt.matcher(line); matcher.find();
			String nodeType = line.substring(matcher.start()+1,matcher.end());
			nodeType = nodeType.trim();
			matcher = argsPatt.matcher(line); 
			String[] args = null;
			if(matcher.find())
			{
				String argstr = line.substring(matcher.start()+1,matcher.end()-1);
				args = argstr.split(",");
			}
			switch(NodeTypes.valueOf(nodeType))
			{
				case Discrete:
				{
					if(args==null || args[0].compareTo("")==0)
						throw new ParserException("Node " + nodeName + " is discrete but has no cardinality set.");
					if(args.length > 1)
						throw new ParserException("Discrete node " + nodeName + " only expects one argument (cardinality.");
					try {
						net.addDiscreteNode(nodeName, Integer.parseInt(args[0]));
					} catch(NumberFormatException e) {
						throw new ParserException("Invalid node cardinality ('"+args[0]+"')");
					} catch(BNException e) {
						throw new ParserException(e.getMessage());
					}
					break;
				}
				default:
					throw new ParserException("Unrecognized node type '"+nodeType+"'");
			}
			return false;
		}
		
		StaticBayesianNetwork net;
		Pattern nodeNamePatt, nodeTypePatt, argsPatt;
	}
	
	public static class EdgeHandler implements LineHandler
	{

		public EdgeHandler(StaticBayesianNetwork net)
		{
			this.net = net;
		}
		
		public String getPrompt() {return null;}

		public boolean parseLine(String line) throws ParserException {
			try
			{	
				if(line.contains("->"))
				{
					String [] names = line.split("\\s*->\\s*");
					net.addEdge(names[0], names[1]);
				}
				else
				{
					String [] names = line.split("\\s*<-\\s*");
					net.addEdge(names[1], names[0]);
				}
			}
			catch(BNException e)
			{
				throw new ParserException(e.getMessage());
			}
			return false;
		}		
		StaticBayesianNetwork net;
	}
	
	public static class StaticRunner implements LineHandler
	{
		public StaticRunner(StaticBayesianNetwork bn){this.net = bn;}
		
		StaticBayesianNetwork net;

		public String getPrompt() {return null;}

		public boolean parseLine(String line) throws ParserException {
			long start = System.currentTimeMillis();
			try {
				net.validate();
				net.run(100, 1e-6);
			} catch(BNException e) {
				throw new ParserException("Error running net : " + e.getMessage());
			}
			long end = System.currentTimeMillis();
			System.out.println("Serial running of static network finished in " + ((double)(end-start))/1000.0 + " seconds.");
			return false;
		}
	}
	
	public static class StaticCPDHandler implements LineHandler
	{
		public StaticCPDHandler(StaticBayesianNetwork bn){this.net = bn;}
		
		public String getPrompt() {return "CPD " + type + " Parameter: ";}

		public boolean parseLine(String line) throws ParserException {
			if(builder==null) // We're just starting a build, will need to change these conditions with nondiscrete nodes/cpds
			{
				String[] bits = line.split("~");
				String name = bits[0]; String cpdtype = bits[1];
				BNNode node = net.getNode(name);
				if((node instanceof DiscreteBNNode))
				{
					dnode = (DiscreteBNNode)node;
					int[] dims = new int[dnode.ds_parents.size()];
					for(int i = 0; i < dnode.ds_parents.size(); i++)
						dims[i] = dnode.ds_parents.get(i).getCardinality();	
					try
					{
						this.builder = DiscreteDistribution.getDistributionBuilder(cpdtype, dnode.getCardinality(), dims.length, dims);
						this.type = cpdtype;
					} catch(BNException exc) {
						throw new ParserException("Error creating CPD: " + exc.getMessage());
					}
				}
				return true;
			}
			else
			{
				if(line.matches("\\s*\\*+\\s*"))
				{
					try
					{
						this.dnode.setDistribution(this.builder.getFinished());
					} catch(BNException e) {
						throw new ParserException("Failed to add CPD: " + e.getMessage());
					}
					this.builder = null;
					this.dnode = null;
					return false;
				}
				else
				{
					try
					{
						boolean cont = this.builder.addLine(line);
						if(!cont)
						{
							try { this.dnode.setDistribution(this.builder.getFinished());}
							catch(BNException e) {throw new ParserException("Failed to add CPD: " + e.getMessage());}
							this.builder = null;
							this.dnode = null;
						}
						return cont;
					} catch(BNException e) {throw new ParserException("Error: " + e.getMessage());}
				}
			}
		}

		DiscreteBNNode dnode = null;
		String type;
		DiscreteDistribution.DiscreteDistributionBuilder builder = null;
		StaticBayesianNetwork net;
	}
	
	public static void main(String[] args)
	{
		interactiveStaticNetwork();
	}
	
	public static void interactiveStaticNetwork()
	{
		Parser parser = new Parser(	new BufferedReader(new InputStreamReader(System.in)),
									new BufferedWriter(new OutputStreamWriter(System.out)),false,true);
		
		StaticBayesianNetwork bn = new StaticBayesianNetwork();
		parser.setPrompt(">>");
		parser.setCommentString("\\s*%\\s*");
		parser.addHandler(staticNodeRegex, new StaticNewNodeHandler(bn));
		parser.addHandler(staticEdgeRegex1, new EdgeHandler(bn));
		parser.addHandler(staticEdgeRegex2, new EdgeHandler(bn));
		parser.addHandler(staticCPDRegex, new StaticCPDHandler(bn));
		parser.addHandler("^run$", new StaticRunner(bn));
		
		parser.go();
	}
	
	
	public static StaticBayesianNetwork loadStaticNetworkFile(String filename)
	{
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
