/**
 * 
 */
package pik.clminputdata.tools;

import pik.clminputdata.configuration.UrbanCLMConfiguration;
import static java.lang.Math.min;

/**
 * @author Sebastian Schubert
 * 
 */
public class GroundSkySVF extends UrbanSkyViewFactor implements Integrable {

	private final double[] fgs;

	private final Integrator itg;

	private final UrbanCLMConfiguration uclm;

	private int roofIndex;

	public GroundSkySVF(int iurb, int id, int jindex, int iindex,
			UrbanCLMConfiguration uclm, Integrator itg) {
		super(iurb, id, jindex, iindex, uclm);
		this.itg = itg;
		this.uclm = uclm;
		fgs = new double[height.length];
	}

	@Override
	public void run() {
		// height of the building in middle
		for (int i = 0; i < fgs.length; i++) {
			roofIndex = i;
			// partly visible length
			double sPartVis;
			if (height[i] < height[height.length - 1]) {
				sPartVis = min(height[i]
						/ (height[height.length - 1] - height[i]) * (ws + bs),
						ws);
			} else {
				sPartVis = ws;
			}
			// only the part that has varying visibility of
			// receiving wall has to be integrated (sending area of that
			// part is already included)
			try {
				fgs[i] = itg.integral(this, ws - sPartVis, ws);
			} catch (NoConvergenceException e) {
				// use result anyway
				fgs[i] = e.getResult();
				System.out
						.printf(
								"Integration of FGS at uc=%i, nd=%i,j=%i, i=%i and rheight=%i exceeded maximum number of steps.",
								this.iurb, this.id, this.jindex, this.iindex, i);
			}
			// plus part that is fully visible (sending area of that
			// part is included)
			fgs[i] += fprl134(ws - sPartVis, 2 * ws + bs,
					height[height.length - 1], ls);
			// /sending * sending/receiving
			fgs[i] *= 1. / (2 * ws + bs);
		}
		saveToGlobal();
	}

	/*
	 * Skyview factor has to be devided by the sending surface area at the end
	 * 
	 * (non-Javadoc)
	 * 
	 * @see pik.clminputdata.tools.Integrable#f(double)
	 */
	@Override
	public double f(double x) {
		double skyvis;
		if (height[roofIndex] > 0) {
			skyvis = height[height.length - 1] / height[roofIndex] * (ws - x);
		} else {
			skyvis = 2 * ws + bs - x;
		}
		return PrlLRec(x, ls, height[height.length - 1])
				+ PrlLRec(skyvis, ls, height[height.length - 1]);

	}

	@Override
	protected void saveToGlobal() {
		// for (int i = 0; i < fgow.length; i++) {
		// for (int j = 0; j < fgow[i].length; j++) {
		// System.out.println(fgow[i][j]);
		// }
		// }
		uclm.setFgs(iurb, id, jindex, iindex, fgs);
	}
}
