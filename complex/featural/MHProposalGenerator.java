package complex.featural;


public interface MHProposalGenerator extends ProposalGenerator
{
	public MHProposal generate(FeaturalModelController cont);
	public String name();
	
	public static class MHProposal extends Proposal
	{
		public MHProposal(double fp, double bp, ProposalAction act)
		{
			super(act);
			this.fp = fp;
			this.bp = bp;
		}
		
		double forwardP()
		{
			return this.fp;
		}
		double backwardP()
		{
			return this.bp;
		}
		
		private double fp, bp;
	}
}
