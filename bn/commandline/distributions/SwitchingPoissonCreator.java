package bn.commandline.distributions;

import java.io.PrintStream;

import java.util.ArrayList;
import bn.distributions.SparseDiscreteCPT.Entry;
import java.util.HashMap;

import java.util.regex.Pattern;

import util.Parser.ParserException;
import util.Parser.ParserFunction;
import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.Distribution;
import bn.distributions.SwitchingPoisson;
import bn.commandline.distributions.CPDCreator.ICPDCreator;

class SwitchingPoissonCreator implements ICPDCreator
{
	static SwitchingPoissonCreator getFactory()
	{
		return new SwitchingPoissonCreator(null,null,null);
	}
	
	private SwitchingPoissonCreator(HashMap<String, Distribution> distmap, String name, int[] dimensions)
	{
		this.distmap = distmap;
		this.name = name;
		this.dimensions = dimensions;
		this.entries = new ArrayList<Entry>();
	}
	
	public void finish() throws ParserException
	{
		try {
			this.distmap.put(name, new SwitchingPoisson(dimensions, entries));
		} catch(BNException e){throw new ParserException(e.getMessage());}
	}
	
	public ParserFunction parseLine(String[] args, PrintStream out) throws ParserException
	{
		try {
			String [] conds = args[0].split("\\s+");
			if(conds.length!=this.dimensions.length)
				throw new ParserException("Incorrect number of dimensions.");
			int[] indices = new int[this.dimensions.length];
			for(int i= 0; i < this.dimensions.length; i++)
				indices[i] = Integer.parseInt(conds[i]);
			
			double p = Double.parseDouble(args[1]);
			Entry ent = new Entry();
			ent.conditional_indices = indices;
			ent.p = p;
			this.entries.add(ent);
			return this;
		} catch(NumberFormatException e) {
			throw new ParserException("Expected conditional indexes, followed by variable value, followed by probability.");
		}
	}
	
	public ICPDCreator newCreator(String name, String argstr, HashMap<String, Distribution> distMap) throws ParserException {
		try{
			String[] bits = argstr.split("\\s*,\\s*");
			int[] dimensions = new int[bits.length];
			for(int i= 0; i < dimensions.length; i++)
				dimensions[i] = Integer.parseInt(bits[i]);
			return new SwitchingPoissonCreator(distMap,name,dimensions);
		} catch(NumberFormatException e) {
			throw new ParserException("Expected argument to be cardinality, followed by dimensions of each dependency.");
		}
	}
	
	public String name(){return "SwitchingPoisson";}
	public String description(){return "Create a switching poisson distribution. Parameters provided must be consistent and complete.";}
	
	private ArrayList<Entry> entries;
	private String name;
	private int cardinality;
	private int[] dimensions;
	public final int[] getGroups() {return groups;}
	public final Pattern getRegEx() {return patt;}
	public final String getPrompt() {return "Enter Switching Poisson Mean: ";}			
	private HashMap<String,Distribution> distmap;
	private static int[] groups = new int[]{1,3};
	private static Pattern patt = Pattern.compile("^\\s*((\\d+\\s+)+)\\s*([\\.eE\\-0-9]+)$");
}