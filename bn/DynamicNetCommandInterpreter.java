package bn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.regex.*;

import util.Parser;
import util.Parser.LineHandler;
import util.Parser.ParserException;

import bn.distributions.DiscreteDistribution;
import bn.interfaces.IDynBayesNet;
import bn.interfaces.IDynBayesNet.ParallelInferenceCallback;

public class DynamicNetCommandInterpreter
{
	static String dynamicNodeRegex = "^\\w+\\s*:\\s*\\w+(\\((\\w+(,\\w+)*)?\\))?\\s*$";
	static String dynamicInterEdgeRegex1 = "\\w+\\s*->\\s*\\w+";
	static String dynamicInterEdgeRegex2 = "\\w+\\s*<-\\s*\\w+";
	static String dynamicIntraEdgeRegex1 = "\\w+\\s*=>\\s*\\w+";
	static String dynamicIntraEdgeRegex2 = "\\w+\\s*<=\\s*\\w+";
	static String dynamicInitialCPDRegex = "\\w+~~\\w+";
	static String dynamicSubseqCPDRegex = "\\w+~\\w+";
	static String dynamicRunRegex = "^run\\(\\d+,[\\d\\.e-]+\\)$";
	static String dynamicRunParallelRegex = "^runp\\(\\d+,[\\d\\.e-]+\\)$";
	static String marginalRegex = "^query\\(\\w+(,[0-9]+,[0-9]+)?\\)$";

	static class MarginalHandler implements Parser.LineHandler
	{
		public MarginalHandler(DynamicBayesianNetwork net)
		{
			this.net = net;
		}

		DynamicBayesianNetwork net;

		public String getPrompt() {return null;}
		public boolean parseLine(String line) throws ParserException {
			Pattern namePatt = Pattern.compile("\\(.+\\)");
			Matcher match = namePatt.matcher(line);
			match.find();
			String arguments = line.substring(match.start()+1,match.end()-1);
			String[] args = arguments.split(",");
			String nodeName = args[0];
			int t0 = 0, te = this.net.getT()-1;
			if(args.length > 1)
			{
				t0 = Integer.parseInt(args[1]);
				te = Integer.parseInt(args[2]);
				if(te < t0)
					throw new ParserException("End time earlier than start time.");
				if(te >= net.getT() || t0 < 0)
					throw new ParserException("Requested range outside of [0,"+net.getT()+"]");
			}
			DBNNode<?> node = net.getNode(nodeName);
			if(!(node instanceof DiscreteDBNNode))
				throw new ParserException("Node specified is non-discrete, cannot print marginal.");
			DiscreteDBNNode dnode = (DiscreteDBNNode)node;
			for(int i = 0; i < dnode.getCardinality(); i++)
			{
				System.out.print(nodeName + " Marginal("+i+"): ");
				for(int t = t0; t <= te; t++)
				{
					try {
						System.out.print(dnode.getMarginal(t).getValue(i) + " ");
					} catch(BNException e) {
						throw new ParserException("Problem extracting marginal : " + e.getMessage());
					}
				}
				System.out.println();
			}
			System.out.println();
			return false;
		}
	}

	static class DynamicNewNodeHandler implements Parser.LineHandler
	{
		public DynamicNewNodeHandler(DynamicBayesianNetwork net)
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
			} catch(IllegalArgumentException e) {
				throw new ParserException("Unrecognized node type '"+nodeType+"'");
			}
			return false;
		}

		DynamicBayesianNetwork net;
		Pattern nodeNamePatt, nodeTypePatt, argsPatt;
	}

	public static class EdgeHandler implements LineHandler
	{

		public EdgeHandler(DynamicBayesianNetwork net)
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
					net.addIntraEdge(names[0], names[1]);
				}
				else if(line.contains("<-"))
				{
					String [] names = line.split("\\s*<-\\s*");
					net.addIntraEdge(names[1], names[0]);
				}
				else if(line.contains("=>"))
				{
					String [] names = line.split("\\s*=>\\s*");
					net.addInterEdge(names[0], names[1]);
				}
				else
				{
					String [] names = line.split("\\s*<=\\s*");
					net.addInterEdge(names[1], names[0]);
				}
			}
			catch(BNException e)
			{
				throw new ParserException(e.getMessage());
			}
			return false;
		}		
		DynamicBayesianNetwork net;
	}

	public static class DynamicRunner implements LineHandler
	{
		public DynamicRunner(DynamicBayesianNetwork bn){this.net = bn;}

		DynamicBayesianNetwork net;

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

	public static class ParallelDynamicRunner implements LineHandler, ParallelInferenceCallback
	{
		public ParallelDynamicRunner(DynamicBayesianNetwork bn){this.net = bn;}

		DynamicBayesianNetwork net;

		public String getPrompt() {return null;}

		public boolean parseLine(String line) throws ParserException {

			Pattern argsPatt = Pattern.compile("\\(.+\\)");
			Matcher matcher = argsPatt.matcher(line);
			matcher.find();
			String[] args = line.substring(matcher.start()+1, matcher.end()-1).split(",");

			long start;
			try {
				net.validate();
				start = System.currentTimeMillis();
				net.run_parallel(Integer.parseInt(args[0]),Double.parseDouble(args[1]),this);
				while(this.done==false)
				{
					try
					{
						Thread.sleep(200);
					} catch(InterruptedException e){}
				}
			} catch(BNException e) {
				throw new ParserException("Error running net : " + e.getMessage());
			} catch(NumberFormatException e) {
				throw new ParserException("Invalid argument to run command.");
			}
			System.out.println("Serial running of static network finished in " + ((double)(doneTime-start))/1000.0 + " seconds.");
			return false;
		}
		
		public boolean done;
		public long doneTime;
		public boolean hadError;
		public String errorMsg;

		public void callback(IDynBayesNet neet) {
			doneTime = System.currentTimeMillis();
			this.done = true;
		}

		public void error(IDynBayesNet net, String error) {
			this.done = true;
			this.hadError = true;
			this.errorMsg = error;
		}
	}
	
	public static class DynamicCPDHandler implements LineHandler
	{
		public DynamicCPDHandler(DynamicBayesianNetwork bn){this.net = bn;}

		public String getPrompt() {return "CPD " + type + " Parameter: ";}

		public boolean parseLine(String line) throws ParserException {
			if(builder==null) // We're just starting a build, will need to change these conditions with nondiscrete nodes/cpds
			{
				this.first = false;
				String[] bits = line.split("~[~]?");
				if(line.contains("~~"))
					this.first = true;
				String name = bits[0]; String cpdtype = bits[1];
				DBNNode<?> node = net.getNode(name);
				if((node instanceof DiscreteDBNNode))
				{
					dnode = (DiscreteDBNNode)node;
					int[] dims;
					if(this.first)
						dims = dnode.getSlice0ParentDim();
					else
						dims = dnode.getSlice1ParentDim();
					
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
						if(this.first)
							this.dnode.setInitialDistribution(this.builder.getFinished());
						else
							this.dnode.setAdvanceDistribution(this.builder.getFinished());
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
							try { 
								if(this.first)
									this.dnode.setInitialDistribution(this.builder.getFinished());
								else
									this.dnode.setAdvanceDistribution(this.builder.getFinished());
							}	catch(BNException e) {throw new ParserException("Failed to add CPD: " + e.getMessage());}
							this.builder = null;
							this.dnode = null;
						}
						return cont;
					} catch(BNException e) {throw new ParserException("Error: " + e.getMessage());}
				}
			}
		}

		boolean first = false;
		DiscreteDBNNode dnode = null;
		String type;
		DiscreteDistribution.DiscreteDistributionBuilder builder = null;
		DynamicBayesianNetwork net;
	}

	public static void main(String[] args)
	{
		interactiveDynamicNetwork();
	}
	
	private static Parser getParser(BufferedReader input, BufferedWriter output, BufferedWriter error, boolean breakOnExc, boolean printLineOnError, DynamicBayesianNetwork bn)
	{
		Parser parser = new Parser(input,output,error,breakOnExc,printLineOnError);
		parser.setCommentString("\\s*%\\s*");
		parser.addHandler(dynamicNodeRegex, new DynamicNewNodeHandler(bn));
		EdgeHandler eh = new EdgeHandler(bn);
		parser.addHandler(dynamicInterEdgeRegex1,eh);
		parser.addHandler(dynamicInterEdgeRegex2,eh);
		parser.addHandler(dynamicIntraEdgeRegex1,eh);
		parser.addHandler(dynamicIntraEdgeRegex2,eh);
		DynamicCPDHandler cpdh = new DynamicCPDHandler(bn);
		parser.addHandler(dynamicInitialCPDRegex,cpdh);
		parser.addHandler(dynamicSubseqCPDRegex,cpdh);
		parser.addHandler(dynamicRunRegex, new DynamicRunner(bn));
		parser.addHandler(dynamicRunParallelRegex, new ParallelDynamicRunner(bn));
		parser.addHandler(marginalRegex, new MarginalHandler(bn));
		
		return parser;
	}

	public static DynamicBayesianNetwork loadNetwork(String file) throws BNException
	{
		try
		{
			BufferedReader input = new BufferedReader(new FileReader(file));
			int T = Integer.parseInt(input.readLine());
			DynamicBayesianNetwork bn = new DynamicBayesianNetwork(T);
			Parser parser = getParser(new BufferedReader(new FileReader(file)), null, null, true, true, bn);
			parser.go();
			return bn;
		} catch(FileNotFoundException e) {
			throw new BNException("Could not find file " + file);
		} catch(IOException e) {
			throw new BNException("Error reading first line for number of slices.");
		} catch(NumberFormatException e) {
			throw new BNException("First line of definition file must be the dynamic network time slice number.");
		}
	}

	public static void interactiveDynamicNetwork()
	{	
		int T = 0;
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		try
		{
			while(T < 2)
			{
				System.out.print("Enter Number of Time Slices : " );
				String firstLine = input.readLine();
				try{T = Integer.parseInt(firstLine);}
				catch(NumberFormatException e){System.err.println("Invalid entry.");}
				if(T < 2)
					System.err.println("Error, number of slices must be at least 2.");
			}
			DynamicBayesianNetwork bn = new DynamicBayesianNetwork(T);
			Parser parser = getParser(input, new BufferedWriter(new OutputStreamWriter(System.out)),
					new BufferedWriter(new OutputStreamWriter(System.err)), false, true, bn);
			parser.setPrompt("D>>");
			parser.go();
		} catch(IOException e) {}
	}
	
	public static enum NodeTypes
	{
		Discrete,
	}
}
