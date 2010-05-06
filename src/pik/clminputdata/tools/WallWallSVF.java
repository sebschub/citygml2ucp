/**
 * 
 */
package pik.clminputdata.tools;

import static java.lang.Math.min;
import static java.lang.Math.max;
import pik.clminputdata.configuration.UrbanCLMConfiguration;

/**
 * @author Sebastian Schubert
 * 
 */
public class WallWallSVF extends UrbanSkyViewFactor implements Integrable {

	private final double[][][] fww;

	private final Integrator itg;

	private final UrbanCLMConfiguration uclm;

	private int wallIndex, roofIndex;

	public WallWallSVF(int iurb, int id, int jindex, int iindex,
			UrbanCLMConfiguration uclm, Integrator itg) {
		super(iurb, id, jindex, iindex, uclm);
		this.itg = itg;
		this.uclm = uclm;
		fww = new double[heightLength - 1][heightLength][heightLength - 1];
	}

	/**
	 * Constructor for testing purposed
	 * @param ws
	 * @param bs
	 * @param ls
	 * @param height
	 * @param iurb
	 * @param id
	 * @param iindex
	 * @param jindex
	 */
	private WallWallSVF(double ws, double bs, double ls, double[] height,
			int iurb, int id, int iindex, int jindex) {
		super(ws, bs, ls, height, iurb, id, iindex, jindex);
		this.itg = new Integrator();
		this.uclm = new UrbanCLMConfiguration();
		fww = new double[heightLength - 1][heightLength][heightLength - 1];
	}
	
	@Override
	public void run() {
		// height of the building in middle
		for (int i = 0; i < heightLength; i++) {
			roofIndex = i;
			for (int j = 0; j < heightLength - 1; j++) {
				wallIndex = j;
				// height of non visible fraction
				double sNonVis;
				// height of full visible
				double sFullVis;
				if (height[i] > 0) {
					if (height[i] < height[j + 1]) {
						sNonVis = max(((2 * ws + bs) * height[i] - ws
								* height[j + 1])
								/ (ws + bs), 0.);
					} else {
						sNonVis = min((2 * ws + bs) / ws
								* (height[i] - height[j + 1]) + height[j + 1],
								height[heightLength - 1]);
					}
					if (height[i] < height[j]) {
						sFullVis = max(((2 * ws + bs) * height[i] - ws
								* height[j])
								/ (ws + bs), 0.);
					} else {
						sFullVis = min((2 * ws + bs) / ws
								* (height[i] - height[j]) + height[j],
								height[heightLength - 1]);
					}
				} else {
					sNonVis = 0.;
					sFullVis = 0.;
				}

				for (int j2 = j; j2 < heightLength - 1; j2++) {
					if (sNonVis < height[j2 + 1]) {
						// only the part that has varying visibility of
						// receiving wall has to be integrated (sending area of
						// that
						// part is already included)
						if (sFullVis > height[j2]) {
							try {
								fww[j2][i][j] = itg.integral(this, max(sNonVis,
										height[j2]), min(sFullVis, height[j2 + 1]));
							} catch (NoConvergenceException e) {
								// use result anyway
								fww[j2][i][j] = e.getResult();
								System.out
								.printf(
										"Integration of FWW at uc=%d, nd=%d,j=%d, i=%d, wheight=%d, rheight=%d and wheight=%d exceeded maximum number of steps.%n",
										this.iurb, this.id, this.jindex,
										this.iindex, j, i, j2);
							}
						}
						if (sFullVis < height[j2 + 1]) {
							// plus part that is fully visible (sending area of
							// that
							// part is included)
							fww[j2][i][j] += fprl16(height[j], height[j + 1],
									max(sFullVis, height[j2]), height[j2 + 1],
									ls, 2 * ws + bs);
						}
						// /sending * sending/receiving
						fww[j2][i][j] *= 1. / (height[j2 + 1] - height[j2]);
						fww[j][i][j2] = (height[j2 + 1] - height[j2])/(height[j + 1] - height[j])* fww[j2][i][j];
					}
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
		double hvis;
		if (height[roofIndex] <= x) {
			hvis = ((2 * ws + bs) * height[roofIndex] - ws * x) / (ws + bs);
			return prlLRec(x - hvis, ls, 2 * ws + bs)
					- prlLRec(x - height[wallIndex + 1], ls, 2 * ws + bs);
		}
		hvis = x + (height[roofIndex] - x) * (2 * ws + bs) / ws;
		return prlLRec(height[wallIndex] - x, ls, 2 * ws + bs)
				- prlLRec(hvis - x, ls, 2 * ws + bs);
	}

	@Override
	protected void saveToGlobal() {
		// for (int i = 0; i < fgow.length; i++) {
		// for (int j = 0; j < fgow[i].length; j++) {
		// System.out.println(fgow[i][j]);
		// }
		// }
		uclm.setFww(iurb, id, jindex, iindex, fww);
	}

	public static void main(String[] args) {
		UrbanCLMConfiguration uclm = new UrbanCLMConfiguration();
		Integrator itg = new Integrator();
		uclm.setBuildingWidth(0, 0, 20, 30, 10.);
		uclm.setStreetWidth(0, 0, 20, 30, 20.);
		WallWallSVF svf = new WallWallSVF(0, 0, 20, 30, uclm, itg);
		svf.run();
		System.out.println(uclm.getStreetLength(0, 20));
		for (int i = 0; i < uclm.getHeightA().length-1; i++) {
			for (int j = 0; j < uclm.getHeightA().length; j++) {
				for (int k = 0; k < uclm.getHeightA().length-1; k++) {
					System.out.println(i + "  " + j + "  " + k + "  " + svf.fww[i][j][k]);
				}
			}
		}
	}

}
