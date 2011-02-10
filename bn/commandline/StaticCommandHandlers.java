package bn.commandline;

import java.io.PrintStream;

import java.util.regex.Pattern;

import util.Parser;
import util.Parser.MethodWrapperHandler;
import util.Parser.ParserException;
import util.Parser.ParserFunction;
import bn.BNException;
import bn.messages.FiniteDiscreteMessage;
import bn.statc.IFDiscBNNode;
import bn.statc.IStaticBayesNet;

class StaticCommandHandlers {

	static class DiscreteNodeAdder extends Parser.MethodWrapperHandler<Object>
	{
		DiscreteNodeAdder(IStaticBayesNet bn) throws Exception
		{
			super(bn,IStaticBayesNet.class.getMethod("addDiscreteNode", new Class<?>[]{String.class,int.class}),new String[]{"node name","node cardinality"},null);
		}
		
		public String name(){return "Discrete";}
		public String description(){return "Creates a discrete node with specified cardinality, e.g. X:Discrete(3) creates" +
				" a discrete node with cardinality 3, named X";}
		
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		private static int[] groups = new int[]{1,2};
		private static Pattern patt = Pattern.compile("^\\s*(\\w+)\\s*:\\s*Discrete\\(\\s*(\\d+)\\s*\\)\\s*$");
	}
	
	static class StaticEdgeHandler extends MethodWrapperHandler<Object>
	{
		StaticEdgeHandler(IStaticBayesNet net) throws Exception
		{
			super(net,IStaticBayesNet.class.getMethod("addEdge",new Class[]{String.class,String.class}),argnames,null);
		}

		public String name(){return "->";}
		public String description(){return "Edge addition operator.  This operator adds an edge between two variables, e.g. 'X->Y'";}
		
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}

		private static int[] groups = new int[]{1,2};
		private static String[] argnames = new String[]{"from node","to node"};
		private static Pattern patt = Pattern.compile("^\\s*(\\w+)\\s*->\\s*(\\w+)\\s*$");
	}
	
	static class StaticEdgeRemover extends MethodWrapperHandler<Object>
	{
		StaticEdgeRemover(IStaticBayesNet net) throws Exception
		{
			super(net,IStaticBayesNet.class.getMethod("removeEdge",new Class[]{String.class,String.class}),argnames,null);
		}
		
		public String name(){return "!->";}
		public String description(){return "Edge removal operator.  This operator removes an existing edge between two variables, e.g. 'X!->Y'";}

		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}

		private static int[] groups = new int[]{1,2};
		private static String[] argnames = new String[]{"from node","to node"};
		private static Pattern patt = Pattern.compile("^\\s*(\\w+)\\s*!->\\s*(\\w+)\\s*$");
	}
	
	static class MarginalHandler implements ParserFunction
	{
		public MarginalHandler(IStaticBayesNet net)
		{
			this.net = net;
		}
		
		public String name(){return "query";}
		public String description(){return "Request the marginal distribution of a node, e.g. query(X)";}

		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		public void finish() throws ParserException {}
		public ParserFunction parseLine(String[] args, PrintStream str) throws ParserException
		{
			try
			{//TODO This is so hacky we need to move to the new command system
				IFDiscBNNode nd =(IFDiscBNNode)net.getNode(args[0]);
				FiniteDiscreteMessage msg = nd.getMarginal();
				for(int i = 0; i < msg.getCardinality(); i++)
					System.out.println(msg.getValue(i)+" ");
			} catch(BNException e) {
				throw new ParserException("Error printing marginal : " + e.getMessage());
			}
			return null;
		}
		
		private IStaticBayesNet net;
		private static Pattern patt = Pattern.compile("^\\s*query\\((\\w+)\\)\\s*$");
		private static int[] groups = new int[]{1};
	}
	
	static class ObservationHandler implements ParserFunction
	{
		public ObservationHandler(IStaticBayesNet bn) throws Exception
		{
			this.net = bn;
		}
		
		public ParserFunction parseLine(String[] args, PrintStream str) throws ParserException
		{
			try
			{
				IFDiscBNNode nd =(IFDiscBNNode)net.getNode(args[0]);
				nd.setValue(Integer.parseInt(args[1]));
			}	catch(BNException e) {
				throw new ParserException("Error setting value for node " + args[0] + ": " + e.getMessage());
			}
			return null;
		}
	
		public void finish() throws ParserException{}
		
		public String name(){return "=";}
		public String description(){return "Evidence operator.  Set the value of a node, e.g. 'X=1'";}
		
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}

		private IStaticBayesNet net;
		private static int[] groups = new int[]{1,2};
		private static Pattern patt = Pattern.compile("\\s*(\\w+)\\s*=\\s*(\\d+)\\s*");
	}
}
