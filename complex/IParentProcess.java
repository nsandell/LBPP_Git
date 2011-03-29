package complex;

import complex.metrics.FinDiscMarginalDivergence.FinDiscMarginalHolder;

public interface IParentProcess extends FinDiscMarginalHolder {
	String getName();
	void backupParameters();
	void restoreParameters();
}
