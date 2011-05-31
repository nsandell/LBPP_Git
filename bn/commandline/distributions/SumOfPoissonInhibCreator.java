package bn.commandline.distributions;

import java.io.PrintStream;

import java.util.HashMap;

import java.util.regex.Pattern;

import util.Parser.ParserException;
import util.Parser.ParserFunction;
import bn.distributions.Distribution;
import bn.distributions.InhibitedSumOfPoisson;
import bn.commandline.distributions.CPDCreator.ICPDCreator;

class SumOfPoissonInhibCreator implements ICPDCreator
{
	static SumOfPoissonInhibCreator getFactory()
	{
		return new SumOfPoissonInhibCreator(null,null,0,0);
	}
	
	private SumOfPoissonInhibCreator(HashMap<String, Distribution> distmap, String name, int nump, double inhib)
	{
		this.distmap = distmap;
		this.name = name;
		this.nump = nump;
		this.inhibp = inhib;
	}
	double inhibp;
	
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
		InhibitedSumOfPoisson sop;
		if(allZ!=-1)
			sop = new InhibitedSumOfPoisson(vec, allZ, this.inhibp);
		else
			sop = new InhibitedSumOfPoisson(vec, this.inhibp);
		this.distmap.put(name, sop);
		return null;
	}

	public ICPDCreator newCreator(String name, String argstr, HashMap<String, Distribution> distMap) throws ParserException {
		try{
			String[] argbits = argstr.split(",");
			if(argbits.length!=2) throw new ParserException("Require both the number of parents and the probability of being inhibited.");
			return new SumOfPoissonInhibCreator(distMap,name,Integer.parseInt(argbits[0]),Double.parseDouble(argbits[1]));
		} catch(NumberFormatException e) {
			throw new ParserException("Expected argument to be the number of parents supported.");
		}
	}
	
	public String name(){return "AdditivePoissonInhib";}
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