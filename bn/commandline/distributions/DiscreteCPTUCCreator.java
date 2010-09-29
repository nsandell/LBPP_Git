package bn.commandline.distributions;

import java.io.PrintStream;
import java.util.HashMap;

import java.util.regex.Pattern;

import util.Parser.ParserException;
import util.Parser.ParserFunction;
import bn.BNException;
import bn.distributions.DiscreteCPTUC;
import bn.distributions.Distribution;
import bn.commandline.distributions.CPDCreator.ICPDCreator;

class DiscreteCPTUCCreator implements ICPDCreator
{
	static DiscreteCPTUCCreator getFactory()
	{
		return new DiscreteCPTUCCreator(null,null,0);
	}
	
	private DiscreteCPTUCCreator(HashMap<String, Distribution> distmap, String name, int cardinality)
	{
		this.distmap = distmap;
		this.name = name;
		this.cardinality = cardinality;
	}
	
	public void finish() throws ParserException{}
	
	public ParserFunction parseLine(String[] args, PrintStream str) throws ParserException
	{
		try {
			String [] ps = args[0].split("\\s+");
			if(ps.length!=this.cardinality)
				throw new ParserException("Expected " + this.cardinality + " probabilities");
			double[] vec = new double[this.cardinality];
			try
			{
				for(int i = 0; i < this.cardinality; i++)
					vec[i] = Double.parseDouble(ps[i]);
			} catch(NumberFormatException e) {
				throw new ParserException("Invalid probability...");
			}
			DiscreteCPTUC dist = new DiscreteCPTUC(vec);	
			this.distmap.put(name, dist);
			return null;
		} catch(BNException e) {
			throw new ParserException(e.getMessage());
		}
	}
	
	public ICPDCreator newCreator(String name, String argstr, HashMap<String, Distribution> distMap) throws ParserException {
		try{
			return new DiscreteCPTUCCreator(distMap,name,Integer.parseInt(argstr));
		} catch(NumberFormatException e) {
			throw new ParserException("Expected argument to probability vector to be the cardinality.");
		}
	}
	
	private String name;
	private int cardinality;
	public final int[] getGroups() {return groups;}
	public final Pattern getRegEx() {return patt;}
	public final String getPrompt() {return "Enter probability vector: ";}			
	private HashMap<String,Distribution> distmap;
	private static int[] groups = new int[]{1};
	private static Pattern patt = Pattern.compile("\\[?\\s*(([\\.e\\-0-9]+\\s*)+)\\]?");
}