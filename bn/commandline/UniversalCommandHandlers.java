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
		private static Pattern patt = Pattern.compile("^\\s*(\\w+)\\s*~\\s*(\\w+)$");
	}
	
	static class BNResetter extends MethodWrapperHandler<Object>
	{
		public BNResetter(IBayesNet<?> net) throws Exception
		{
			super(net,IBayesNet.class.getMethod("resetMessages", new Class<?>[]{}),new String[]{},null);
		}
		
		public int[] getGroups(){return groups;}
		public Pattern getRegEx(){return patt;}
		public String getPrompt(){return null;}
		
		private static Pattern patt = Pattern.compile("^\\s*reset\\s*$");
		private static int[] groups = new int[]{};
	}
	
	static class BNSaver implements ParserFunction
	{
		public BNSaver(IBayesNet<?> net)
		{
			this.net = net;
		}
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
	
	static class BNSampler extends MethodWrapperHandler<Object>
	{
		public BNSampler(IBayesNet<?> net) throws Exception
		{
			super(net,IBayesNet.class.getMethod("sample", new Class<?>[]{}),new String[]{},null);
		}
		
		public int[] getGroups(){return groups;}
		public Pattern getRegEx(){return patt;}
		public String getPrompt(){return null;}
		
		private static Pattern patt = Pattern.compile("^\\s*samplenet\\s*$");
		private static int[] groups = new int[]{};
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

	/*static class LLGetter extends MethodWrapperHandler<Object>
	{
		LLGetter(IBayesNet<?> net) throws Exception
		{
			super(net,IBayesNet.class.getMethod("nodeLogLikelihood",new Class[]{String.class}),
					new String[]{"node name"}, null);
		}
		
		@Override
		protected void handleReturn(PrintStream pr)
		{
			pr.println((Double)this.retObj);
		}
		
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		
		private static Pattern patt = Pattern.compile("^\\s*ll\\(\\s*(\\w+)\\s*\\)\\s*$");
		private static int[] groups = new int[]{1};
	}*/
	
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
		
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		
		private static Pattern patt = Pattern.compile("^\\s*remove\\(\\s*(\\w+)\\s*\\)\\s*$");
		private static int[] groups = new int[]{1};
	}
	
	/*static class NetLLGetter extends MethodWrapperHandler<Object>
	{
		NetLLGetter(IBayesNet<?> net) throws Exception
		{
			super(net,IBayesNet.class.getMethod("logLikelihood",new Class[]{}),
					new String[]{}, null);
		}
		
		@Override
		protected void handleReturn(PrintStream pr)
		{
			pr.println((Double)this.retObj);
		}
		
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		
		private static Pattern patt = Pattern.compile("^\\s*ll\\s*$");
		private static int[] groups = new int[]{};
	}*/
	
}
