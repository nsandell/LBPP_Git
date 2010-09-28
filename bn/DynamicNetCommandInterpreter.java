package bn;

import java.io.BufferedReader;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.regex.*;

import util.Parser;
import util.Parser.ParserFunction;
import util.Parser.ParserException;

import bn.distributions.Distribution;
import bn.interfaces.IDynBayesNet;

public class DynamicNetCommandInterpreter
{
	static class InterEdgeHandler extends Parser.MethodWrapperHandler<Object>
	{
		InterEdgeHandler(IDynBayesNet bn) throws Exception
		{
			super(bn,IDynBayesNet.class.getMethod("addInterEdge", new Class<?>[]{String.class,String.class}),new String[]{"from node","to node"},null);
		}
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		private static int[] groups = new int[]{1,2};
		private static Pattern patt = Pattern.compile("^\\s*(\\w+)=>(\\w+)\\s*$");
	}
	
	static class IntraEdgeHandler extends Parser.MethodWrapperHandler<Object>
	{
		IntraEdgeHandler(IDynBayesNet bn) throws Exception
		{
			super(bn,IDynBayesNet.class.getMethod("addIntraEdge", new Class<?>[]{String.class,String.class}),new String[]{"from node","to node"},null);
		}
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		private static int[] groups = new int[]{1,2};
		private static Pattern patt = Pattern.compile("^\\s*(\\w+)->(\\w+)\\s*$");
	}
	
	static class DiscreteNodeAdder extends Parser.MethodWrapperHandler<Object>
	{
		DiscreteNodeAdder(IDynBayesNet bn) throws Exception
		{
			super(bn,IDynBayesNet.class.getMethod("addDiscreteNode", new Class<?>[]{String.class,int.class}),new String[]{"node name","node cardinality"},null);
		}
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		private static int[] groups = new int[]{1,2};
		private static Pattern patt = Pattern.compile("^(\\w+):Discrete\\((\\d+)\\)$");
	}
	
	static class InitialDistSetter  extends Parser.MethodWrapperHandler<Distribution>
	{
		InitialDistSetter(IDynBayesNet bn, HashMap<String,Distribution> distMap) throws Exception
		{
			super(bn,IDynBayesNet.class.getMethod("setInitialDistribution", new Class<?>[]{String.class,Distribution.class}),new String[]{"node name","distribution name"},distMap);
		}
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		private static int[] groups = new int[]{1,2};
		private static Pattern patt = Pattern.compile("^\\s*(\\w+)~~(\\w+)\\s*$");
	}
	
	static class ParallelRunner extends Parser.MethodWrapperHandler<Object>
	{
		public ParallelRunner(IDynBayesNet bn) throws Exception
		{
			super(bn,IDynBayesNet.class.getMethod("run_parallel_block", new Class<?>[]{int.class,double.class}),new String[]{"max iterations","tolerance"},null);
		}
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		private static int[] groups = new int[]{1,2};
		private static Pattern patt = Pattern.compile("^runp\\((\\d+),([\\.e\\-0-9]+)\\)$");
	}

	static class MarginalHandler implements Parser.ParserFunction
	{
		
		public MarginalHandler(DynamicBayesianNetwork net)
		{
			this.net = net;
		}

		public String getPrompt() {return null;}
		public void finish(){}
		public int[] getGroups(){return groups;}
		public Pattern getRegEx(){return patt;}
		
		private static Pattern patt = Pattern.compile("^query\\((\\w+)(,(\\d+),(\\d+))?\\)");
		private static int[] groups = new int[]{1,3,4};
		
		public ParserFunction parseLine(String[] args) throws ParserException {
			String nodeName = args[0];
			int t0 = 0, te = this.net.getT()-1;
			if(args[1]!=null)
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
			return null;
		}

		DynamicBayesianNetwork net;
	}
	
	public static void main(String[] args)
	{
		interactiveDynamicNetwork();
	}
	
	private static Parser getParser(BufferedReader input, BufferedWriter output, BufferedWriter error, boolean breakOnExc, boolean printLineOnError, DynamicBayesianNetwork bn)
	{
		try
		{
			HashMap<String, Distribution> distMap = new HashMap<String, Distribution>();
			Parser parser = new Parser(input,output,error,breakOnExc,printLineOnError);
			parser.setCommentString("\\s*%\\s*");
			parser.addHandler(new StaticNetCommandInterpreter.CPDCreator(distMap));
			parser.addHandler(new StaticNetCommandInterpreter.BNValidate(bn));
			parser.addHandler(new StaticNetCommandInterpreter.BNRunner(bn));
			parser.addHandler(new StaticNetCommandInterpreter.CPDAssigner(bn, distMap));
			parser.addHandler(new InterEdgeHandler(bn));
			parser.addHandler(new IntraEdgeHandler(bn));
			parser.addHandler(new DiscreteNodeAdder(bn));
			parser.addHandler(new InitialDistSetter(bn, distMap));
			parser.addHandler(new ParallelRunner(bn));
			parser.addHandler(new MarginalHandler(bn));
		
		return parser;
		}
		catch(Exception e) {
			System.err.println("Error initializing parser: " + e.getMessage());
			return null;
		}
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
	
}
