package bn;

import java.io.BufferedReader;


import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.regex.*;

import util.Parser;
import util.Parser.ParserFunction;
import util.Parser.MethodWrapperHandler;
import util.Parser.ParserException;

import bn.distributions.Distribution;
import bn.interfaces.IBayesNet;
import bn.interfaces.IStaticBayesNet;

public class StaticNetCommandInterpreter
{
	
	static class DiscreteNodeAdder extends Parser.MethodWrapperHandler<Object>
	{
		DiscreteNodeAdder(IStaticBayesNet bn) throws Exception
		{
			super(bn,IStaticBayesNet.class.getMethod("addDiscreteNode", new Class<?>[]{String.class,int.class}),new String[]{"node name","node cardinality"},null);
		}
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		private static int[] groups = new int[]{1,2};
		private static Pattern patt = Pattern.compile("^(\\w+):Discrete\\((\\d+)\\)$");
	}
	
	static class StaticEdgeHandler extends MethodWrapperHandler<Object>
	{
		
		StaticEdgeHandler(StaticBayesianNetwork net) throws Exception
		{
			super(net,net.getClass().getInterfaces()[0].getMethod("addEdge",new Class[]{String.class,String.class}),argnames,null);
		}

		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}

		private static int[] groups = new int[]{1,2};
		private static String[] argnames = new String[]{"from node","to node"};
		private static Pattern patt = Pattern.compile("(\\w+)\\s*->\\s*(\\w+)");
	}
	
	static class CPDAssigner extends MethodWrapperHandler<Distribution>
	{
		CPDAssigner(IBayesNet<?> net,HashMap<String, Distribution> distMap) throws Exception
		{
			super(net,IBayesNet.class.getMethod("setDistribution", 
													 new Class<?>[]{String.class,Distribution.class}),
				     new String[]{"node name","distribution name"},distMap);
		}
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		private static int[] groups = new int[]{1,2};
		private static Pattern patt = Pattern.compile("\\s*(\\w+)\\s*~\\s*(\\w+)");
	}

	static class CPDCreator implements ParserFunction
	{
		public CPDCreator(HashMap<String, Distribution> distMap)
		{
			this.distMap = distMap;
			this.patt = Pattern.compile("(\\w+)=(\\w+)\\((\\w+(,\\w+)*)\\)");
		}

		public void finish(){};
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return this.patt;}
		public String getPrompt() {return null;}

		public ParserFunction parseLine(String[] args) throws ParserException
		{
			return Distribution.getDistributionBuilder(args[1], args[0], args[2], distMap);
		}
		
		private int[] groups = new int[]{1,2,3};
		private HashMap<String, Distribution> distMap;
		private Pattern patt;
	}
	

	static class BNValidate extends MethodWrapperHandler<Object>
	{
		BNValidate(IBayesNet<?> net) throws Exception
		{
			super(net,IBayesNet.class.getMethod("validate", new Class<?>[]{}),new String[]{},null);
		}
		
		public int[] getGroups(){return groups;}
		public Pattern getRegEx(){return patt;}
		public String getPrompt(){return null;}
		
		private static Pattern patt = Pattern.compile("^validate$");
		private static int[] groups = new int[]{};
	}
		
	static class BNRunner extends MethodWrapperHandler<Object>
	{
		BNRunner(IBayesNet<?> net) throws Exception
		{
			super(net,IStaticBayesNet.class.getMethod("run",
					new Class[]{int.class, double.class}),
					new String[]{"Number of iterations","Convergence tolerance"},
					null);
		}
		
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		
		private static Pattern patt = Pattern.compile("^run\\((\\d+)(,([\\.0-9\\+\\-e]+))?\\)");
		private static int[] groups = new int[]{1,3};
	}
	
	static class MarginalHandler implements ParserFunction
	{
		public MarginalHandler(StaticBayesianNetwork net)
		{
			this.net = net;
		}

		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		public void finish() throws ParserException {}
		public ParserFunction parseLine(String[] args) throws ParserException
		{
			BNNode node = net.getNode(args[0]);
			if(node==null)
				throw new ParserException("Unknown node: " + args[0]);
			if(node instanceof DiscreteBNNode)
			{
				DiscreteBNNode dnode = (DiscreteBNNode)node;
				for(int i = 0; i < dnode.getCardinality(); i++)
					System.out.println(dnode.getMarginal().getValue(i)+" ");
			}
			else
				throw new ParserException("Don't know how to print marginal for node.");
			return null;
		}
		
		private StaticBayesianNetwork net;
		private static Pattern patt = Pattern.compile("^query\\((\\w+)\\)\\s*$");
		private static int[] groups = new int[]{1};
	}

	public static void main(String[] args)
	{
		interactiveStaticNetwork();
	}

	private static Parser getParser(BufferedReader input, BufferedWriter output, BufferedWriter error, boolean breakOnExc, boolean printLineOnError, StaticBayesianNetwork bn)
	{
		try
		{
			Parser parser = new Parser(input,output,error,breakOnExc,printLineOnError);
			HashMap<String, Distribution> distMap = new HashMap<String, Distribution>();
			parser.setCommentString("\\s*%\\s*");
			parser.addHandler(new DiscreteNodeAdder(bn));
			parser.addHandler(new StaticEdgeHandler(bn));
			parser.addHandler(new CPDCreator(distMap));
			parser.addHandler(new CPDAssigner(bn, distMap));
			parser.addHandler(new MarginalHandler(bn));
			parser.addHandler(new BNRunner(bn));
			parser.addHandler(new BNValidate(bn));
			return parser;
		} catch(Exception e) {
			System.err.println("Error loading parser : " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public static StaticBayesianNetwork loadNetwork(String file) throws BNException
	{
		try
		{
			StaticBayesianNetwork bn = new StaticBayesianNetwork();
			Parser parser = getParser(new BufferedReader(new FileReader(file)), null, null, true, true, bn);
			if(parser==null) return null;
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
		if(parser==null)
			return;
		parser.setPrompt("S>>");
		parser.go();
	}

	public static enum NodeTypes
	{
		Discrete,
	}
}
