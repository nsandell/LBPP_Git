package complex.featural.proposal_generators;

import java.util.Vector;

import complex.IParentProcess;
import complex.featural.FeaturalModelController;
import complex.featural.IFeaturalChild;
import complex.featural.ProposalAction;
import complex.featural.ProposalGenerator;
import complex.featural.FeaturalModelController.LatentPair;

public class RandomExpungeGenerator<ChildProcess extends IFeaturalChild, ParentProcess extends IParentProcess> implements ProposalGenerator<ChildProcess,ParentProcess> {
	
	public RandomExpungeGenerator(double pexp, double pabs)
	{
		this.pexp = pexp;
		this.pabs = pabs;
	}
	
	public String name()
	{
		return "Random expunges";
	}
	
	@Override
	public Proposal<ChildProcess,ParentProcess> generate(FeaturalModelController<ChildProcess,ParentProcess> cont)
	{
		int N = cont.getLatentNodes().size();
		if(N < 2)
			return null;
		
		LatentPair<ChildProcess,ParentProcess> pair = cont.randomLatentPair();
		if(pair==null)
			return null;
		
		
		double fp = pexp/((double)(N*(N-1)));
		double bp = pabs/((double)(N*(N-1)));
		
		Vector<ChildProcess> expells = new Vector<ChildProcess>();
		for(ChildProcess child : cont.getChildren(pair.l2))
			if(cont.getChildren(pair.l1).contains(child))
					expells.add(child);
	
		if(expells.size()==0)
			return null;

		cont.log("Attempting to expel the children of node " + pair.l2.getName() + " from node " + pair.l1.getName());
	
		return new Proposal<ChildProcess,ParentProcess>(fp, bp, new ProposalAction.DisconnectAction<ChildProcess,ParentProcess>(pair.l1,expells));
	}

	private double pexp, pabs;
}
