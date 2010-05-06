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
public class GroundOtherWallSVF extends UrbanSkyViewFactor implements
		Integrable {

	private final double[][] fgow;

	private final Integrator itg;

	private final UrbanCLMConfiguration uclm;

	private int wallIndex, roofIndex;

	public GroundOtherWallSVF(int iurb, int id, int jindex, int iindex,
			UrbanCLMConfiguration uclm, Integrator itg) {
		super(iurb, id, jindex, iindex, uclm);
		this.itg = itg;
		this.uclm = uclm;
		fgow = new double[heightLength][heightLength - 1];
	}

	@Override
	public void run() {
		// height of the building in middle
		for (int i = 0; i < fgow.length; i++) {
			roofIndex = i;
			for (int j = 0; j < fgow[i].length; j++) {
				// end of non visible fraction
				double sNonVis;
				if (height[i] > 0) {
					if (height[i] < height[j + 1]) {
						sNonVis = min(
								(height[i] / (height[j + 1] - height[i]) + 1)
										* (ws + bs), 2 * ws + bs);
					} else {
						sNonVis = 2 * ws + bs;
					}
				} else {
					sNonVis = ws + bs;
				}

				if (sNonVis < 2 * ws + bs) {
					// System.out.println(height[i]);
					// System.out.println(height[j] + " " + height[j + 1]);
					// System.out.println("=============================");
					wallIndex = j;
					// begin of fully visible fraction
					double sFullVis;
					if (height[i] > 0) {
						if (height[i] < height[j]) {
							sFullVis = min(
									(height[i] / (height[j] - height[i]) + 1)
											* (ws + bs), 2 * ws + bs);
						} else {
							sFullVis = 2 * ws + bs;
						}
					} else {
						sFullVis = ws + bs;
					}
					// only the part that has varying visibility of
					// receiving wall has to be integrated (sending area of that
					// part is already included)
					try {
						fgow[i][j] = itg.integral(this, sNonVis, sFullVis);
					} catch (NoConvergenceException e) {
						// use result anyway
						fgow[i][j] = e.getResult();
						System.out
								.printf(
										"Integration of FGOW at uc=%d, nd=%d,j=%d, i=%d, wheight=%d and rheight=%d exceeded maximum number of steps.%n",
										this.iurb, this.id, this.jindex,
										this.iindex, j, i);
					}
					// plus part that is fully visible (sending area of that
					// part is included)
					fgow[i][j] += fnrm14(sFullVis, 2 * ws + bs, height[j],
							height[j + 1], ls);
					// /sending * sending/receiving
					fgow[i][j] *= 1. / (height[j + 1] - height[j]);
				}
			}
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
		double rec = Math.max(height[wallIndex + 1]
				- Math.max(x * height[roofIndex] / (x - ws - bs),
						height[wallIndex]), 0.);
		return nrmLRec(height[wallIndex + 1], ls, x)
				- nrmLRec(height[wallIndex + 1] - rec, ls, x);
	}

	@Override
	protected void saveToGlobal() {
		// for (int i = 0; i < fgow.length; i++) {
		// for (int j = 0; j < fgow[i].length; j++) {
		// System.out.println(fgow[i][j]);
		// }
		// }
		uclm.setFgow(iurb, id, jindex, iindex, fgow);
	}

	public static void main(String[] args) {
		UrbanCLMConfiguration uclm = new UrbanCLMConfiguration();
		Integrator itg = new Integrator();
		uclm.setBuildingWidth(0, 0, 20, 30, 10.);
		uclm.setStreetWidth(0, 0, 20, 30, 20.);
		GroundOtherWallSVF svf = new GroundOtherWallSVF(0, 0, 20, 30, uclm, itg);
		svf.run();
		System.out.println(uclm.getStreetLength(0, 20));
		for (int i = 0; i < uclm.getHeightA().length; i++) {
			for (int j = 0; j < uclm.getHeightA().length - 1; j++) {
				System.out.println(i + "  " + j + "  " + svf.fgow[i][j]);
			}
		}
	}

}
