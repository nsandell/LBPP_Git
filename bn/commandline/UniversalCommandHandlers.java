package bn.commandline;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.regex.Pattern;

import util.Parser.MethodWrapperHandler;
import util.Parser.ParserException;
import util.Parser.ParserFunction;
import bn.BNException;
import bn.IBayesNet;
import bn.IBayesNet.RunResults;
import bn.distributions.Distribution;

public class UniversalCommandHandlers {
	
	//TODO Clear this out
	static class CPDAssigner extends MethodWrapperHandler<Distribution>
	{
		CPDAssigner(IBayesNet<?> net,HashMap<String, Distribution> distMap) throws Exception
		{
			super(net,IBayesNet.class.getMethod("setDistribution", 
													 new Class<?>[]{String.class,Distribution.class}),
				     new String[]{"node name","distribution name"},distMap);
		}
		
		public String name(){return "~";}
		public String description(){return "Set the distribution for a node.  For example, X~A sets node X" +
				"is distributed as distribution A.  If this is a dynamic network, and there exists" +
				" an initial distribution, this distribution governs all time t>0.";}
		
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		private static int[] groups = new int[]{1,2};
		private static Pattern patt = Pattern.compile("^\\s*(\\w+)\\s*~\\s*(\\w+)$");
	}
	
	static class BNResetter extends MethodWrapperHandler<Object>
	{
		public BNResetter(IBayesNet<?> net) throws Exception
		{
			super(net,IBayesNet.class.getMethod("resetMessages", new Class<?>[]{}),new String[]{},null);
		}
		
		public String name(){return "reset";}
		public String description(){return "Reset all the messages from the network back to initial values.";}
		
		public int[] getGroups(){return groups;}
		public Pattern getRegEx(){return patt;}
		public String getPrompt(){return null;}
		
		private static Pattern patt = Pattern.compile("^\\s*reset\\s*$");
		private static int[] groups = new int[]{};
	}
	
	static class EvidenceClearer extends MethodWrapperHandler<Object>
	{
		public EvidenceClearer(IBayesNet<?> net) throws Exception
		{
			super(net,IBayesNet.class.getMethod("clearAllEvidence", new Class<?>[]{}),new String[]{},null);
		}
		
		//TODO Figure out why sometimes running converges early when evidence changes.
		public String name(){return "clearAllEvidence";}
		public String description(){return "Clear all evidence from the network.";}
		
		public int[] getGroups(){return groups;}
		public Pattern getRegEx(){return patt;}
		public String getPrompt(){return null;}
		
		private static Pattern patt = Pattern.compile("^\\s*clearAllEvidence\\s*$");
		private static int[] groups = new int[]{};
	}
	
	static class BNSaver implements ParserFunction
	{
		public BNSaver(IBayesNet<?> net)
		{
			this.net = net;
		}
		
		public String name(){return "save";}
		public String description(){return "Print a list of commands for generating this network." +
				"  These commands may be redirected to a file, e.g. 'save >> /home/johndoe/bayesNet'.";}
		
		private IBayesNet<?> net;
		@Override
		public Pattern getRegEx() {return regex;}
		@Override
		public int[] getGroups() {return groups;}
		@Override
		public String getPrompt() {return null;}
		@Override
		public void finish() throws ParserException{}
		@Override
		public ParserFunction parseLine(String[] args, PrintStream output)
				throws ParserException {
			net.print(output);
			return null;
		}
		
		private int[] groups = new int[]{};
		private Pattern regex = Pattern.compile("^\\s*save\\s*$");
	}
	
	static class BNValidate extends MethodWrapperHandler<Object>
	{
		BNValidate(IBayesNet<?> net) throws Exception
		{
			super(net,IBayesNet.class.getMethod("validate", new Class<?>[]{}),new String[]{},null);
		}
		
		public String name(){return "validate";}
		public String description(){return "Validate the structure of the network.  This will ensure all" +
				" nodes have distributions, and that those distributions match the cardinality of the node" +
				" and its parents.  Also ensures acyclicity of the network.";}
		
		public int[] getGroups(){return groups;}
		public Pattern getRegEx(){return patt;}
		public String getPrompt(){return null;}
		
		private static Pattern patt = Pattern.compile("^\\s*validate\\s*$");
		private static int[] groups = new int[]{};
	}
	
	static class BNRunner extends MethodWrapperHandler<Object>
	{
		BNRunner(IBayesNet<?> net) throws Exception
		{
			super(net,IBayesNet.class.getMethod("run",
					new Class[]{int.class, double.class}),
					new String[]{"Number of iterations","Convergence tolerance"},
					null);
		}
	
		public String name(){return "run";}
		public String description(){return "Run belief propagation over the network that has been created.  Two parameters are to be used, " +
				"the first dictates the maximum number of iterations of belief propagation are run.  The second is a convergence condition" +
				" for early termination.  This condition is met when the change in all values of every marginal distribution is under the " +
				"specified value.  For example, run(100,1e-8) will run the network until marginal value changes are under 1e-8 or 100 iterations," +
				" whichever comes first.";}
		
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		
		@Override
		protected void handleReturn(PrintStream ps)
		{
			RunResults res = (RunResults)this.retObj;
			ps.println("Converged after " + res.numIts + " iterations with an error of " + res.error + " in " + res.timeElapsed + " seconds.");
		}
		
		private static Pattern patt = Pattern.compile("^\\s*run\\(\\s*(\\d+)\\s*(,\\s*([\\.0-9\\+\\-e]+)\\s*)?\\s*\\)\\s*$");
		private static int[] groups = new int[]{1,3};
	}
	
	static class BNRunnerDefault extends MethodWrapperHandler<Object>
	{
		BNRunnerDefault(IBayesNet<?> net) throws Exception
		{
			super(net,IBayesNet.class.getMethod("run",
					new Class[]{}),
					new String[]{},
					null);
		}
		
		public String name(){return "run";}
		public String description(){return "With no parameters provided, run belief propagation with default parameters of (100,0).";}
		
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		
		@Override
		protected void handleReturn(PrintStream ps)
		{
			RunResults res = (RunResults)this.retObj;
			ps.println("Converged after " + res.numIts + " iterations with an error of " + res.error + " in " + res.timeElapsed + " seconds, using default parameters (100,0)");
		}
		
		private static Pattern patt = Pattern.compile("^\\s*run\\s*$");
		private static int[] groups = new int[]{};
	}
	
	static class Optimizer extends MethodWrapperHandler<Object>
	{
		public Optimizer(IBayesNet<?> net) throws Exception {
			super(net,IBayesNet.class.getMethod("optimize", new Class[]{int.class,double.class,int.class,double.class}),
					new String[]{"maximum EM iterations", "EM convergence criterion",
								 "maximum BP iterations", "BP convergence criterion"},null);
		}
		
		@Override
		protected void handleReturn(PrintStream pr)
		{
			RunResults res = (RunResults)this.retObj;
			pr.println("Optimized in " + res.numIts + " iterations of EM, with an error of " + res.error + " and in " + res.timeElapsed + " seconds.");
		}
		
		public String name(){return "learn";}
		public String description(){return "Peform parameter optimization via expectation-maximzation.  Takes 4 parameters, i.e. learn(10,1e-3,50,1e-6)." +
				"  This command will perform expectation maximization by iteratively using belief propagation with paramters (50,1e-6), and after each" +
				" iteration of belief propagation maximize the parameter set.  This will be done a maximum of 10 times, and will terminate early if every " +
				"parameter changes by less than 1e-3.";}


		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}

		private static Pattern patt = Pattern.compile("^\\s*learn\\(\\s*(.*)\\s*,\\s*(.*)\\s*,\\s*(.*)\\s*,\\s*(.*)\\s*\\)\\s*$");
		private static int[] groups = new int[]{1,2,3,4};
	}
	
	static class NodeDistPrinter implements ParserFunction
	{
		public NodeDistPrinter(IBayesNet<?> net)
		{
			this.net = net;
		}
		
		public String name(){return "distInfo";}
		public String description(){return "This command gets the distribution information for a node.";}
		
		private IBayesNet<?> net;
		@Override
		public Pattern getRegEx() {return regex;}
		@Override
		public int[] getGroups() {return groups;}
		@Override
		public String getPrompt() {return null;}
		@Override
		public void finish() throws ParserException{}
		@Override
		public ParserFunction parseLine(String[] args, PrintStream output)
				throws ParserException {
			try {net.printDistributionInfo(args[0], output);}
			catch(BNException e) { throw new ParserException(e.getMessage());}
			return null;
		}
		
		private int[] groups = new int[]{1};
		private Pattern regex = Pattern.compile("^\\s*distInfo\\((\\w+)\\)\\s*$");
	}

	static class NodeRemover extends MethodWrapperHandler<Object>
	{
		NodeRemover(IBayesNet<?> net) throws Exception
		{
			super(net,IBayesNet.class.getMethod("removeNode",new Class[]{String.class}),
					new String[]{"node name"}, null);
		}
		
		@Override
		protected void handleReturn(PrintStream pr)
		{
			pr.println((Double)this.retObj);
		}
		
		public String name(){return "remove";}
		public String description(){return "Remove a node from the netowrk, e.g. remove(X).";}
		
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		
		private static Pattern patt = Pattern.compile("^\\s*remove\\(\\s*(\\w+)\\s*\\)\\s*$");
		private static int[] groups = new int[]{1};
	}
	
	/*
	static class DefinitionPrinter extends MethodWrapperHandler<Object>
	{
		DefinitionPrinter(IBayesNet<?> net) throws Exception
		{
			super(net,IBayesNet.class.getMethod("getDefinition", new Class[]{}),
					new String[]{},null);
		}
		
		@Override 
		protected void handleReturn(PrintStream pr)
		{
			pr.println((String)this.retObj);
		}
		
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		
		private static Pattern patt = Pattern.compile("^\\s*print\\s*$");
		private static int[] groups = new int[]{};
	}*/
	
	static class NetLLGetter extends MethodWrapperHandler<Object>
	{
		NetLLGetter(IBayesNet<?> net) throws Exception
		{
			super(net,IBayesNet.class.getMethod("getLogLikelihood",new Class[]{}),
					new String[]{}, null);
		}
		
		@Override
		protected void handleReturn(PrintStream pr)
		{
			pr.println((Double)this.retObj);
		}
		
		public String name(){return "ll";}
		public String description(){return "Get the approximate log likelihood of the evidence given the network.  The approximation" +
				" is the 'Bethe free energy' approximation.";}
		
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		
		private static Pattern patt = Pattern.compile("^\\s*ll\\s*$");
		private static int[] groups = new int[]{};
	}
	
}
