package complex;

import bn.dynamic.IDBNNode;
import complex.metrics.FinDiscMarginalDivergence.FinDiscMarginalHolder;

public interface IParentProcess extends FinDiscMarginalHolder {
	String getName();
	
	void backupParameters();
	void restoreParameters();
	
	double parameterLL();
	
	int id();
	
	IDBNNode hook();
}
