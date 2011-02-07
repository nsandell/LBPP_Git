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
	
	public ParserFunction parseLine(String[] args, PrintStream out) throws ParserException
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
	
	public String name(){return "CPT";}
	public String description(){return "Create a conditional probability table.  Takes a list of in-line arguments.  The first argument is the cardinality" + 
		" of the variable that this CPT determines the distribution of.  Each successive argument defines the cardinality of the CPTs dependencies.  User must then" +
		"supply parameters in a sparse format, e.g. dep1value dep2value dep3value ... variableValue probability.  An asterix will finalize the table.\n\n" +
		"For example:\nA<CPT(3,2,2)\n0 0 0 .8\n0 0 1 .2\n0 1 0 .2\n0 1 1 .8\n1 0 1 .5\n1 0 2 .5\n1 1 0 .3\n 1 1 1 .4\n 1 1 2 .3\n*\n  This " +
		"creates a CPT for a variable of size 3 that depends on two parents, each of size 2.  \"1 0 2 .5\" means that when the first parent is 1, and the" +
		" second parent is 0, the variable will be value 2 with probability .5.  Parameters provided must be consistent and complete.";}
	
	private ArrayList<Entry> entries;
	private String name;
	private int cardinality;
	private int[] dimensions;
	public final int[] getGroups() {return groups;}
	public final Pattern getRegEx() {return patt;}
	public final String getPrompt() {return "Enter CPT Entry: ";}			
	private HashMap<String,Distribution> distmap;
	private static int[] groups = new int[]{1,3,4};
	private static Pattern patt = Pattern.compile("^\\s*((\\d+\\s*)+)\\s+(\\d+)\\s+([\\.eE\\-0-9]+)$");
}