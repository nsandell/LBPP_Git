package complex.featural.proposal_generators;

import java.util.HashSet;

import util.MathUtil;
import complex.IParentProcess;
import complex.featural.FeaturalModelController;
import complex.featural.IFeaturalChild;
import complex.featural.ProposalAction;
import complex.featural.ProposalGenerator;

public class RandomAdderGenerator<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> implements ProposalGenerator<ChildProcess,ParentProcess>
{
	public RandomAdderGenerator(double pmerge, double psplit)
	{
		this.pmerge = pmerge;
		this.psplit = psplit;
	}
	
	public String name()
	{
		return "Random coparenter";
	}
	
	public Proposal<ChildProcess,ParentProcess> generate(FeaturalModelController<ChildProcess,ParentProcess> cont)
	{
		int N = cont.getLatentNodes().size();
		if(N < 1)
			return null;
		ParentProcess lat = cont.randomLatent();
		HashSet<ChildProcess> children = cont.getChildren(lat);
		HashSet<ChildProcess> splits = new HashSet<ChildProcess>();
		for(ChildProcess child : children)
			if(MathUtil.rand.nextDouble() < .5)
				splits.add(child);
		
		if(splits.size()==0 || splits.size()==children.size())
			return null;
		
		double fp = this.psplit*Math.pow(.5, splits.size())/((double)N);
		double bp = this.pmerge/((double)(N*(N-1)));
		
		cont.log("Proposing adding coparent to node " + lat.getName());
		
		return new Proposal<ChildProcess,ParentProcess>(fp, bp, new ProposalAction.MultiUniqueParentAddAction<ChildProcess,ParentProcess>(splits,true));
	}

	
	double pmerge, psplit;
}
