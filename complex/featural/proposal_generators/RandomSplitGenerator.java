package complex.featural.proposal_generators;

import java.util.HashSet;

import util.MathUtil;
import complex.IParentProcess;
import complex.featural.FeaturalModelController;
import complex.featural.IFeaturalChild;
import complex.featural.MHProposalGenerator;
import complex.featural.ProposalAction.SplitAction;

public class RandomSplitGenerator implements MHProposalGenerator
{
	public RandomSplitGenerator(double pmerge, double psplit)
	{
		this.pmerge = pmerge;
		this.psplit = psplit;
	}
	
	public String name()
	{
		return "Random splits";
	}
	
	public MHProposal generate(FeaturalModelController cont)
	{
		int N = cont.getLatentNodes().size();
		if(N < 1)
			return null;
		IParentProcess lat = cont.randomLatent();
		HashSet<IFeaturalChild> children = cont.getChildren(lat);
		HashSet<IFeaturalChild> splits = new HashSet<IFeaturalChild>();
		for(IFeaturalChild child : children)
			if(MathUtil.rand.nextDouble() < .5)
				splits.add(child);
		
		if(splits.size()==0 || splits.size()==children.size())
			return null;
		
		double fp = 2*this.psplit*Math.pow(.5, splits.size())/((double)N);
		double bp = this.pmerge/((double)(N*(N-1)));
		
		cont.log("Proposing split of node " + lat.getName());
		
		return new MHProposal(fp, bp, new SplitAction(lat,splits,true));
	}

	
	double pmerge, psplit;
}
