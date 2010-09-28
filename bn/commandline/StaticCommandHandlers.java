package bn.commandline;

import java.util.regex.Pattern;

import util.Parser;
import util.Parser.MethodWrapperHandler;
import util.Parser.ParserException;
import util.Parser.ParserFunction;
import bn.interfaces.IBayesNode;
import bn.interfaces.IDiscreteBayesNode;
import bn.interfaces.IStaticBayesNet;

class StaticCommandHandlers {
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
		private static Pattern patt = Pattern.compile("^\\s*(\\w+)\\s*:\\s*Discrete\\(\\s*(\\d+)\\s*\\)\\s*$");
	}
	
	static class StaticEdgeHandler extends MethodWrapperHandler<Object>
	{
		StaticEdgeHandler(IStaticBayesNet net) throws Exception
		{
			super(net,IStaticBayesNet.class.getMethod("addEdge",new Class[]{String.class,String.class}),argnames,null);
		}

		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}

		private static int[] groups = new int[]{1,2};
		private static String[] argnames = new String[]{"from node","to node"};
		private static Pattern patt = Pattern.compile("^\\s*(\\w+)\\s*->\\s*(\\w+)\\s*$");
	}
	
	static class MarginalHandler implements ParserFunction
	{
		public MarginalHandler(IStaticBayesNet net)
		{
			this.net = net;
		}

		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		public void finish() throws ParserException {}
		public ParserFunction parseLine(String[] args) throws ParserException
		{
			IBayesNode node = net.getNode(args[0]);
			if(node==null)
				throw new ParserException("Unknown node: " + args[0]);
			if(node instanceof IDiscreteBayesNode)
			{
				IDiscreteBayesNode dnode = (IDiscreteBayesNode)node;
				for(int i = 0; i < dnode.getCardinality(); i++)
					System.out.println(dnode.getMarginal().getValue(i)+" ");
			}
			else
				throw new ParserException("Don't know how to print marginal for node.");
			return null;
		}
		
		private IStaticBayesNet net;
		private static Pattern patt = Pattern.compile("^\\s*query\\((\\w+)\\)\\s*$");
		private static int[] groups = new int[]{1};
	}
	
	static class ObservationHandler extends MethodWrapperHandler<Object>
	{
		public ObservationHandler(IStaticBayesNet bn) throws Exception
		{
			super(bn,IStaticBayesNet.class.getMethod("addDiscreteEvidence", new Class<?>[]{String.class,int.class}),
					new String[]{"node name","observation"},null);
		}
		
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}

		private static int[] groups = new int[]{1,2};
		private static Pattern patt = Pattern.compile("\\s*(\\w+)\\s*=\\s*(\\d+)\\s*");
	}
}
