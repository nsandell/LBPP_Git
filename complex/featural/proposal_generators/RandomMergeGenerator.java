package complex.featural.proposal_generators;

import java.util.HashSet;

import complex.IParentProcess;
import complex.featural.FeaturalModelController;
import complex.featural.IFeaturalChild;
import complex.featural.FeaturalModelController.LatentPair;
import complex.featural.ProposalGenerator;
import complex.featural.ProposalAction.MergeAction;

public class RandomMergeGenerator<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> implements ProposalGenerator<ChildProcess,ParentProcess>
{
	public RandomMergeGenerator(double pmerge, double psplit)
	{
		this.pmerge = pmerge;
		this.psplit = psplit;
	}
	
	public String name()
	{
		return "Random merges";
	}
	
	public Proposal<ChildProcess,ParentProcess> generate(FeaturalModelController<ChildProcess,ParentProcess> cont)
	{
		int N = cont.getLatentNodes().size();
		if(N < 2)
			return null;
		LatentPair<ChildProcess,ParentProcess> pair = cont.randomLatentPair();
		if(pair==null)
			return null;
		
		cont.log("Proposing the merge of nodes " + pair.l1.getName() + " and " + pair.l2.getName());
		
		HashSet<ChildProcess> totalChildren = new HashSet<ChildProcess>();
		totalChildren.addAll(cont.getChildren(pair.l1));
		totalChildren.addAll(cont.getChildren(pair.l2));
		
		double fp = this.pmerge/((double)(N*(N-1)));
		double bp = 2*Math.pow(.5,totalChildren.size())*this.psplit/((double)(N-1));		//Really rough but eh
		
		return new Proposal<ChildProcess,ParentProcess>(fp, bp, new MergeAction<ChildProcess,ParentProcess>(pair.l1,pair.l2,true));
	}
	
	double pmerge, psplit;
}
