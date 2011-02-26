package bn.commandline;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.regex.Pattern;

import util.Parser;
import util.Parser.MethodWrapperHandler;
import util.Parser.ParserException;
import util.Parser.ParserFunction;
import bn.BNException;
import bn.IBayesNet;
import bn.IBayesNet.RunResults;
import bn.distributions.Distribution;
import bn.dynamic.IFDiscDBNNode;
import bn.dynamic.IDynamicBayesNet;
import bn.messages.FiniteDiscreteMessage;

public class DynamicCommandHandlers
{
	static class InterEdgeHandler extends Parser.MethodWrapperHandler<Object>
	{
		InterEdgeHandler(IDynamicBayesNet bn) throws Exception
		{
			super(bn,IDynamicBayesNet.class.getMethod("addInterEdge", new Class<?>[]{String.class,String.class}),new String[]{"from node","to node"},null);
		}
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		private static int[] groups = new int[]{1,2};
		private static Pattern patt = Pattern.compile("^\\s*(\\w+)\\s*=>\\s*(\\w+)\\s*$");
		
		public String name(){return "=>";}
		public String description(){return "This command creates an 'inter-slice' edge between two nodes. For example, 'X=>Y' makes " +
				" node X at times t influences node Y at times t+1\n";}
	}
	
	static class InterEdgeRemover extends Parser.MethodWrapperHandler<Object>
	{
		InterEdgeRemover(IDynamicBayesNet bn) throws Exception
		{
			super(bn,IDynamicBayesNet.class.getMethod("removeInterEdge", new Class<?>[]{String.class,String.class}),new String[]{"from node","to node"},null);
		}
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		private static int[] groups = new int[]{1,2};
		private static Pattern patt = Pattern.compile("^\\s*(\\w+)\\s*!=>\\s*(\\w+)\\s*$");
		
		public String name(){return "!=>";}
		public String description(){return "This command removes an existing interslice edge between nodes, e.g. 'X!=>Y'.";};
	}

	static class IntraEdgeHandler extends Parser.MethodWrapperHandler<Object>
	{
		IntraEdgeHandler(IDynamicBayesNet bn) throws Exception
		{
			super(bn,IDynamicBayesNet.class.getMethod("addIntraEdge", new Class<?>[]{String.class,String.class}),new String[]{"from node","to node"},null);
		}
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		private static int[] groups = new int[]{1,2};
		private static Pattern patt = Pattern.compile("^\\s*(\\w+)\\s*->\\s*(\\w+)\\s*$");
		
		public String name(){return "->";}
		public String description(){return "This command creates an intraslice edge between nodes, e.g. 'X->Y' means node X " +
				"at times t influences node Y at times t.";}
	}
	
	static class IntraEdgeRemover extends Parser.MethodWrapperHandler<Object>
	{
		IntraEdgeRemover(IDynamicBayesNet bn) throws Exception
		{
			super(bn,IDynamicBayesNet.class.getMethod("removeIntraEdge", new Class<?>[]{String.class,String.class}),new String[]{"from node","to node"},null);
		}
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		private static int[] groups = new int[]{1,2};
		private static Pattern patt = Pattern.compile("^\\s*(\\w+)\\s*!->\\s*(\\w+)\\s*$");
		
		public String name(){return "!->";}
		public String description(){return "This command removes an existing 'intraslice edge between nodes, e.g. 'X!->Y'.";};
	}

	static class DiscreteNodeAdder extends Parser.MethodWrapperHandler<Object>
	{
		DiscreteNodeAdder(IDynamicBayesNet bn) throws Exception
		{
			super(bn,IDynamicBayesNet.class.getMethod("addDiscreteNode", new Class<?>[]{String.class,int.class}),new String[]{"node name","node cardinality"},null);
		}
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		private static int[] groups = new int[]{1,2};
		private static Pattern patt = Pattern.compile("^\\s*(\\w+)\\s*:\\s*Discrete\\((\\d+)\\)\\s*$");
		
		public String name(){return "Discrete";}
		public String description(){return "Creates a discrete node with specified cardinality, e.g. X:Discrete(3) creates" +
				" a discrete node with cardinality 3, named X";}
	}
	

	static class InitialDistSetter  extends Parser.MethodWrapperHandler<Distribution>
	{
		InitialDistSetter(IDynamicBayesNet bn, HashMap<String,Distribution> distMap) throws Exception
		{
			super(bn,IDynamicBayesNet.class.getMethod("setInitialDistribution", new Class<?>[]{String.class,Distribution.class}),new String[]{"node name","distribution name"},distMap);
		}
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		private static int[] groups = new int[]{1,2};
		private static Pattern patt = Pattern.compile("^\\s*(\\w+)\\s*~~\\s*(\\w+)\\s*$");
		
		public String name(){return "~~";}
		public String description(){return "Assign a distribution to be the 'initial' distribution for a variable.  This is" +
				" necessary when a node has inter-slice parents that do not exist at time 0.  For example X~~pi assigns " +
				"distribution pi to dictate the value of X at time 0.";}
	}

	static class ParallelRunner extends Parser.MethodWrapperHandler<Object>
	{
		public ParallelRunner(IDynamicBayesNet bn) throws Exception
		{
			super(bn,IDynamicBayesNet.class.getMethod("run_parallel_block", new Class<?>[]{int.class,double.class}),new String[]{"max iterations","tolerance"},null);
		}

		@Override
		protected void handleReturn(PrintStream pr)
		{
			RunResults res = (RunResults)this.retObj;
			pr.println("Converged after " + res.numIts + " iterations with an error of " + res.error + " in " + res.timeElapsed + " seconds in parallel run mode.");
		}

		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		private static int[] groups = new int[]{1,2};
		private static Pattern patt = Pattern.compile("^\\s*runp\\(\\s*(\\d+)\\s*,\\s*([\\.e\\-0-9]+)\\s*\\)\\s*$");
		
		public String name(){return "runp";}
		public String description(){return "The same as run, but will split the network time-wise into a number of regions" +
				" (one for each available CPU core) and perform belief propagation.";}
	}

	static class ParallelOptimizer extends MethodWrapperHandler<Object>
	{
		public ParallelOptimizer(IBayesNet<?> net) throws Exception {
			super(net,IDynamicBayesNet.class.getMethod("optimize_parallel", new Class[]{int.class,double.class,int.class,double.class}),
					new String[]{"maximum EM iterations", "EM convergence criterion",
				"maximum BP iterations", "BP convergence criterion"},null);
		}

		@Override
		protected void handleReturn(PrintStream pr)
		{
			RunResults res = (RunResults)this.retObj;
			pr.println("Optimized in " + res.numIts + " iterations of EM, with an error of " + res.error + " and in " + res.timeElapsed + " seconds.");
		}


		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}

		private static Pattern patt = Pattern.compile("^\\s*learnp\\(\\s*(.*)\\s*,\\s*(.*)\\s*,\\s*(.*)\\s*,\\s*(.*)\\s*\\)\\s*$");
		private static int[] groups = new int[]{1,2,3,4};
		
		public String name(){return "learnp";}
		public String description(){return "Same as 'learn command, but will slice the network time-wise into as many segments" +
				" as there are available cores on the machine.  Belief propagation will then be performed in parallel for each" +
				" iteration of learning.";}
	}

	static class DiscreteEvidenceNodeAdder implements Parser.ParserFunction
	{
		public DiscreteEvidenceNodeAdder(IDynamicBayesNet net)
		{
			this.net = net;
		}
		IDynamicBayesNet net;
		
		public String getPrompt() {return null;}
		public void finish(){}
		public int[] getGroups(){return groups;}
		public Pattern getRegEx(){return patt;}

		private static Pattern patt = Pattern.compile("^\\s*(\\w+)\\s*:\\s*DiscreteEvidenceNode\\(\\)\\s*$");
		private static int[] groups = new int[]{1};
		
		public String name(){return "DiscreteEvidenceNode";}
		public String description(){return "Create a discrete evidence node -- this will then prompt for the entire observation sequence of this node.";};
		
		public ParserFunction parseLine(String[] args, PrintStream str) throws ParserException {
			return new EvidenceGrabber(this.net,args[0], this.net.getT());
		}
		
		private static class EvidenceGrabber implements Parser.ParserFunction
		{
			
			public EvidenceGrabber(IDynamicBayesNet net, String name, int T)
			{
				this.net = net;
				this.name = name;
				this.T = T;
			}
			IDynamicBayesNet net;
			String name;
			int T;
			public String getPrompt() {return "Enter evidence sequence : ";}
			public void finish(){}
			public int[] getGroups(){return groups;}
			public Pattern getRegEx(){return patt;}
			
			private static Pattern patt = Pattern.compile("^\\s*(\\d+)((\\s+\\d+)+)\\s*$");
			private static int[] groups = new int[]{1,2};
			public String name(){return "";}
			public String description(){return "";}
			
			@Override
			public ParserFunction parseLine(String[] args, PrintStream str) throws ParserException {
				int[] evidence = new int[T];
				try {
					evidence[0] = Integer.parseInt(args[0]);
					String[] splits = args[1].split("\\s+");
					int count = 1;
					for(int i = 0; i < splits.length; i++)
					{
						if(!splits[i].equals(""))
						{
							evidence[count] = Integer.parseInt(splits[i]);
							count++;
						}
					}
					if(count!=T)
					{
						System.out.println("Expected 5 pieces of evidence!");
						return this;
					}

					try {
						net.addDiscreteEvidenceNode(this.name, evidence);
					} catch(BNException e) {
						throw new ParserException("Failed to add evidence node : " + e.toString());
					}
					return null;
				} catch(NumberFormatException e) {
					throw new ParserException("Failed to load evidence for evidence node : "+ e.toString());
				}
			}
		}
	}

	static class MarginalHandler implements Parser.ParserFunction
	{

		public MarginalHandler(IDynamicBayesNet net)
		{
			this.net = net;
		}

		public String getPrompt() {return null;}
		public void finish(){}
		public int[] getGroups(){return groups;}
		public Pattern getRegEx(){return patt;}

		private static Pattern patt = Pattern.compile("^\\s*query\\(\\s*(\\w+)\\s*(,\\s*(\\d+)\\s*,\\s*(\\d+))?\\s*\\)\\s*$");
		private static int[] groups = new int[]{1,3,4};
		
		public String name(){return "query";}
		public String description(){return "Request the marginal distribution of a node, for all time or a subset of times. " +
				"As examples, query(X) requests the marginals for node X for all time.  query(X,4,10) requests the marginals " +
				"for node X from time steps 4 to 10.";}

		public ParserFunction parseLine(String[] args, PrintStream str) throws ParserException {
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

			try
			{
				IFDiscDBNNode nd = (IFDiscDBNNode) net.getNode(nodeName);
				if(nd==null)
					throw new BNException("Coudln't find node " + nodeName);
				FiniteDiscreteMessage dmsg = nd.getMarginal(0);
				int card =  dmsg.getCardinality();
				for(int i = 0; i < card; i++)
				{
					for(int t = t0; t <= te; t++)
					{
						dmsg = nd.getMarginal(t);
						str.print(dmsg.getValue(i) + " ");
					}
					str.println();
				}
				str.println();
			} catch(BNException e) {
				throw new ParserException("Problem extracting marginal : " + e.getMessage());
			}
			return null;
		}

		IDynamicBayesNet net;
	}

	static class ObservationHandler implements ParserFunction
	{
		public ObservationHandler(IDynamicBayesNet bn) throws Exception
		{
			this.bn = bn;
		}

		public ParserFunction parseLine(String[] args, PrintStream str) throws ParserException
		{
			try
			{
				IFDiscDBNNode nd = (IFDiscDBNNode)bn.getNode(args[0]);
				int t0 = Integer.parseInt(args[1]);
				args[2] = args[2].trim();
				String [] obsStr = args[2].split("\\s+");
				int[] data = new int[obsStr.length];
				for(int i = 0; i < obsStr.length; i++)
					data[i] = Integer.parseInt(obsStr[i]);
				nd.setValue(data,t0);
			} catch(NumberFormatException e) {throw new ParserException("Number formating error.");}
			catch(BNException e){throw new ParserException(e.getMessage());}
			return null;
		}
		
		public String name(){return "=";}
		public String description(){return "Evidence setting operator. Set the value of a variable for " +
				"a subset of times.  For example X(4) = 1 2 3 4 5 sets X at time 4 to 1, X at time 5 to 2, etc.";}

		public void finish(){}
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}

		private static int[] groups = new int[]{1,3,4};
		private static Pattern patt = Pattern.compile("^\\s*(\\w+)(\\((\\d+)\\))?\\s*=(.*)");

		IDynamicBayesNet bn;
	}
}
