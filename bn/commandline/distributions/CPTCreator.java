package bn.commandline.distributions;

import java.util.ArrayList;
import bn.distributions.SparseDiscreteCPT.Entry;
import java.util.HashMap;

import java.util.regex.Pattern;

import util.Parser.ParserException;
import util.Parser.ParserFunction;
import bn.BNException;
import bn.distributions.DiscreteCPT;
import bn.distributions.Distribution;
import bn.commandline.distributions.CPDCreator.ICPDCreator;

class CPTCreator implements ICPDCreator
{
	static CPTCreator getFactory()
	{
		return new CPTCreator(null,null,0,null);
	}
	
	private CPTCreator(HashMap<String, Distribution> distmap, String name, int cardinality, int[] dimensions)
	{
		this.distmap = distmap;
		this.name = name;
		this.cardinality = cardinality;
		this.dimensions = dimensions;
		this.entries = new ArrayList<Entry>();
	}
	
	public void finish() throws ParserException
	{
		try {
			this.distmap.put(name, new DiscreteCPT(dimensions, cardinality, entries));
		} catch(BNException e){throw new ParserException(e.getMessage());}
	}
	
	public ParserFunction parseLine(String[] args) throws ParserException
	{
		try {
			String [] conds = args[0].split("\\s+");
			if(conds.length!=this.dimensions.length)
				throw new ParserException("Incorrect number of dimensions.");
			int[] indices = new int[this.dimensions.length];
			for(int i= 0; i < this.dimensions.length; i++)
				indices[i] = Integer.parseInt(conds[i]);
			
			int value = Integer.parseInt(args[1]);
			double p = Double.parseDouble(args[2]);
			Entry ent = new Entry();
			ent.conditional_indices = indices;
			ent.p = p;
			ent.value_index = value;
			this.entries.add(ent);
			return this;
		} catch(NumberFormatException e) {
			throw new ParserException("Expected conditional indexes, followed by variable value, followed by probability.");
		}
	}
	
	public ICPDCreator newCreator(String name, String argstr, HashMap<String, Distribution> distMap) throws ParserException {
		try{
			String[] bits = argstr.split("\\s*,\\s*");
			int[] dimensions = new int[bits.length-1];
			int card = Integer.parseInt(bits[0]);
			for(int i= 0; i < dimensions.length; i++)
				dimensions[i] = Integer.parseInt(bits[i+1]);
			return new CPTCreator(distMap,name,card,dimensions);
		} catch(NumberFormatException e) {
			throw new ParserException("Expected argument to be cardinality, followed by dimensions of each dependency.");
		}
	}
	
	private ArrayList<Entry> entries;
	private String name;
	private int cardinality;
	private int[] dimensions;
	public final int[] getGroups() {return groups;}
	public final Pattern getRegEx() {return patt;}
	public final String getPrompt() {return "Enter CPT Entry: ";}			
	private HashMap<String,Distribution> distmap;
	private static int[] groups = new int[]{1,3,4};
	private static Pattern patt = Pattern.compile("^\\s*((\\d+\\s*)+)\\s+(\\d+)\\s*([\\.e\\-0-9]+)$");
}