package complex.featural;

import complex.IChildProcess;
import complex.IParentProcess;

public interface IFeaturalChild extends IChildProcess {

	public void addParent(IParentProcess parent);
	public void killParent(IParentProcess parent);
}
