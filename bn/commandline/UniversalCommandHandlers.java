package bn.commandline;

import java.util.HashMap;
import java.util.regex.Pattern;

import util.Parser.MethodWrapperHandler;
import bn.IBayesNet;
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
		
		private static Pattern patt = Pattern.compile("^\\s*run\\(\\s*(\\d+)\\s*(,\\s*([\\.0-9\\+\\-e]+)\\s*)?\\s*\\)\\s*$");
		private static int[] groups = new int[]{1,3};
	}

	static class LLGetter extends MethodWrapperHandler<Object>
	{
		LLGetter(IBayesNet<?> net) throws Exception
		{
			super(net,IBayesNet.class.getMethod("nodeLogLikelihood",new Class[]{String.class}),
					new String[]{"node name"}, null);
		}
		
		@Override
		protected void handleReturn()
		{
			System.out.println((Double)this.retObj);
		}
		
		public int[] getGroups() {return groups;}
		public Pattern getRegEx() {return patt;}
		public String getPrompt() {return null;}
		
		private static Pattern patt = Pattern.compile("^\\s*ll\\(\\s*(\\w+)\\s*\\)\\s*$");
		private static int[] groups = new int[]{1};
	}
}
