package complex.featural;

import complex.IParentProcess;

public interface ProposalGenerator<ChildProcess extends IFeaturalChild,ParentProcess extends IParentProcess>
{
	public Proposal<ChildProcess,ParentProcess> generate(FeaturalModelController<ChildProcess,ParentProcess> cont);
	public String name();
	
	public static class Proposal<ChildProcess extends IFeaturalChild,ParentProcess extends IParentProcess>
	{
		public Proposal(double fp, double bp, ProposalAction<ChildProcess,ParentProcess>act)
		{
			this.fp = fp;
			this.bp = bp;
			this.act = act;
		}
		
		double forwardP()
		{
			return this.fp;
		}
		double backwardP()
		{
			return this.bp;
		}
		ProposalAction<ChildProcess,ParentProcess> action()
		{
			return this.act;
		}
		
		private double fp, bp;
		private ProposalAction<ChildProcess,ParentProcess> act;
	}
}
