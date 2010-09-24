package bn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.regex.*;
import util.Parser;
import util.Parser.LineHandler;
import util.Parser.ParserException;

import bn.distributions.DiscreteDistribution;

public class StaticNetCommandInterpreter
{
	static String staticNodeRegex = "^\\w+\\s*:\\s*\\w+(\\((\\w+(,\\w+)*)?\\))?\\s*$";
	static String staticEdgeRegex1 = "\\w+\\s*->\\s*\\w+";
	static String staticEdgeRegex2 = "\\w+\\s*<-\\s*\\w+";
	static String staticCPDRegex = "\\w+~\\w+";
	static String staticRunRegex = "^run\\(\\d+,[\\d\\.e-]+\\)$";
	static String marginalRegex = "^query\\(\\w+\\)$";

	static String nodeEvidenceRegex = "\\+\\w+\\(\\d+\\)";

	static class EvidenceHandler implements Parser.LineHandler
	{
		public EvidenceHandler(StaticBayesianNetwork net)
		{
			this.net = net;
		}
		
		public String getPrompt(){return null;}
		
		public boolean parseLine(String line) throws ParserException {
			String[] bits = line.split("\\(");
			String nodeName = bits[0].substring(1,bits[0].length());
			String evidence = bits[1].substring(0,bits[1].length()-1);
			
			BNNode node = net.getNode(nodeName);
			if(node==null)
				throw new ParserException("Node " + nodeName + " not found in network.");
			
			if(node instanceof DiscreteBNNode)
			{
				int evidencei;
				try{evidencei = Integer.parseInt(evidence);}
				catch(NumberFormatException e){throw new ParserException("Invalid evidence specified.");}
				try{((DiscreteBNNode)node).setValue(evidencei);}
				catch(BNException e){throw new ParserException("Failed to set evidence: " + e.getMessage());}
			}
			else
				throw new ParserException("Operation unsupported for this node type.");
			
			return false;
		}
		
		StaticBayesianNetwork net;
	}
	
	static class MarginalHandler implements Parser.LineHandler
	{
		public MarginalHandler(StaticBayesianNetwork net)
		{
			this.net = net;
		}

		StaticBayesianNetwork net;

		public String getPrompt() {return null;}
		public boolean parseLine(String line) throws ParserException {
			Pattern namePatt = Pattern.compile("\\(\\w+\\)");
			Matcher match = namePatt.matcher(line);
			match.find();
			String nodeName = line.substring(match.start()+1,match.end()-1);
			BNNode node = net.getNode(nodeName);
			if(!(node instanceof DiscreteBNNode))
				throw new ParserException("Node specified is non-discrete, cannot print marginal.");
			DiscreteBNNode dnode = (DiscreteBNNode)node;
			System.out.print(nodeName + " Marginal: ");
			for(int i = 0; i < dnode.getCardinality(); i++)
				System.out.print(dnode.getMarginal().getValue(i) + " ");
			System.out.println();
			return false;
		}
	}

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
			try
			{
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
			} catch( IllegalArgumentException e) {
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

			Pattern argsPatt = Pattern.compile("\\(.+\\)");
			Matcher matcher = argsPatt.matcher(line);
			matcher.find();
			String[] args = line.substring(matcher.start()+1, matcher.end()-1).split(",");

			long start = System.currentTimeMillis();
			try {
				net.validate();
				net.run(Integer.parseInt(args[0]),Double.parseDouble(args[1]));
			} catch(BNException e) {
				throw new ParserException("Error running net : " + e.getMessage());
			} catch(NumberFormatException e) {
				throw new ParserException("Invalid argument to run command.");
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
					int[] dims = dnode.getParentDimensions();
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

	private static Parser getParser(BufferedReader input, BufferedWriter output, BufferedWriter error, boolean breakOnExc, boolean printLineOnError, StaticBayesianNetwork bn)
	{
		Parser parser = new Parser(input,output,error,breakOnExc,printLineOnError);
		parser.setCommentString("\\s*%\\s*");
		parser.addHandler(staticNodeRegex, new StaticNewNodeHandler(bn));
		parser.addHandler(staticEdgeRegex1, new EdgeHandler(bn));
		parser.addHandler(staticEdgeRegex2, new EdgeHandler(bn));
		parser.addHandler(staticCPDRegex, new StaticCPDHandler(bn));
		parser.addHandler(staticRunRegex, new StaticRunner(bn));
		parser.addHandler(marginalRegex, new MarginalHandler(bn));
		parser.addHandler(nodeEvidenceRegex, new EvidenceHandler(bn));
		return parser;
	}

	public static StaticBayesianNetwork loadNetwork(String file) throws BNException
	{
		try
		{
			StaticBayesianNetwork bn = new StaticBayesianNetwork();
			Parser parser = getParser(new BufferedReader(new FileReader(file)), null, null, true, true, bn);
			parser.go();
			return bn;
		} catch(FileNotFoundException e) {
			throw new BNException("Could not find file " + file);
		}
	}

	public static void interactiveStaticNetwork()
	{
		StaticBayesianNetwork bn = new StaticBayesianNetwork();
		Parser parser = getParser(	new BufferedReader(new InputStreamReader(System.in)),
									new BufferedWriter(new OutputStreamWriter(System.out)),
									new BufferedWriter(new OutputStreamWriter(System.err)), false, true, bn);
		parser.setPrompt("S>>");
		parser.go();
	}

	public static enum NodeTypes
	{
		Discrete,
	}
}
