package bn.commandline.distributions;

import java.io.PrintStream;

import java.util.HashMap;

import java.util.regex.Pattern;

import util.Parser.ParserException;
import util.Parser.ParserFunction;
import bn.distributions.Distribution;
import bn.distributions.SumOfPoisson;
import bn.commandline.distributions.CPDCreator.ICPDCreator;

class SumOfPoissonCreator implements ICPDCreator
{
	static SumOfPoissonCreator getFactory()
	{
		return new SumOfPoissonCreator(null,null,0);
	}
	
	private SumOfPoissonCreator(HashMap<String, Distribution> distmap, String name, int nump)
	{
		this.distmap = distmap;
		this.name = name;
		this.nump = nump;
	}
	
	public void finish() throws ParserException{}
	
	public ParserFunction parseLine(String[] args, PrintStream str) throws ParserException
	{
		String [] ps = args[0].split("\\s+");
		if(ps.length!=this.nump && ps.length!=(this.nump+1))
			throw new ParserException("Expected number of means either equal to parents or parents plus one.");
		double allZ = -1;
		if(ps.length==(this.nump+1))
			allZ = Double.parseDouble(ps[0]);

		double[] vec = new double[this.nump];
		try
		{
			for(int i = 0; i < this.nump; i++)
				vec[i] = Double.parseDouble(ps[i+(allZ!=-1?1:0)]);
		} catch(NumberFormatException e) {
			throw new ParserException("Invalid probability...");
		}
		SumOfPoisson sop;
		if(allZ!=-1)
			sop = new SumOfPoisson(vec, allZ);
		else
			sop = new SumOfPoisson(vec);
		this.distmap.put(name, sop);
		return null;
	}

	public ICPDCreator newCreator(String name, String argstr, HashMap<String, Distribution> distMap) throws ParserException {
		try{
			return new SumOfPoissonCreator(distMap,name,Integer.parseInt(argstr));
		} catch(NumberFormatException e) {
			throw new ParserException("Expected argument to be the number of parents supported.");
		}
	}
	
	public String name(){return "AdditivePoisson";}
	public String description(){return "Creates a sum of poisson distribution.";}
	
	private String name;
	private int nump;
	public final int[] getGroups() {return groups;}
	public final Pattern getRegEx() {return patt;}
	public final String getPrompt() {return "Enter means vector: ";}
	private HashMap<String,Distribution> distmap;
	private static int[] groups = new int[]{1};
	private static Pattern patt = Pattern.compile("\\[?\\s*(([\\.e\\-0-9E]+\\s*)+)\\]?");
}