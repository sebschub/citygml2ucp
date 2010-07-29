/**
 * 
 */
package pik.clminputdata.tools;

import pik.clminputdata.configuration.UrbanCLMConfiguration;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Class for the calculation of skyview factor from wall to sky of two adjacent
 * street canyon.
 * 
 * @author Sebastian Schubert
 * 
 */
public class WallOtherSkySVF extends UrbanSkyViewFactor implements Integrable {

	private final double[][] fws;

	private final Integrator itg;

	private final UrbanCLMConfiguration uclm;

	private int roofIndex;

	public WallOtherSkySVF(int iurb, int id, int jindex, int iindex,
			UrbanCLMConfiguration uclm, Integrator itg) {
		super(iurb, id, jindex, iindex, uclm);
		this.itg = itg;
		this.uclm = uclm;
		fws = new double[heightLength - 1][heightLength];
	}

	@Override
	public void run() {
		// height of the building in middle, case treated extra
		for (int i = 0; i < heightLength - 1; i++) {
			roofIndex = i;
			// partly visible length
			double sFullVis;
			sFullVis = max(((2 * ws + bs) * height[i] - ws
					* height[heightLength - 1])
					/ (ws + bs), 0.);
			for (int j = 0; j < heightLength - 1; j++) {
				if (sFullVis > height[j]) {
					// only the part that has varying visibility of
					// receiving wall has to be integrated (sending area of that
					// part is already included)
					try {
						fws[j][i] = itg.integral(this, height[j], min(
								height[j + 1], sFullVis));
					} catch (NoConvergenceException e) {
						// use result anyway
						fws[j][i] = e.getResult();
						System.out
								.printf(
										"Integration of FWS at uc=%d, nd=%d,j=%d, i=%d , rheight=%d and wheight=%d exceeded maximum number of steps.%n",
										this.iurb, this.id, this.jindex,
										this.iindex, i, j);
					}
				}
				if (sFullVis < height[j + 1]) {
					// plus part that is fully visible (sending area of that
					// part is included)
					fws[j][i] += fnrm13(height[heightLength - 1]
							- max(sFullVis, height[j]),
							height[heightLength - 1] - height[j + 1], 2 * ws
									+ bs, ls);
				}
				// /sending * sending/receiving
				fws[j][i] *= 1. / (2 * ws + bs);
			}
		}
		for (int j = 0; j < heightLength - 1; j++) {
			fws[j][heightLength - 1] = fnrm13(height[heightLength - 1]
					- height[j], height[heightLength - 1] - height[j + 1], ws,
					ls);
			fws[j][heightLength - 1] *= 1. / (2 * ws + bs);
		}
		saveToGlobal();
	}

	/*
	 * Skyview factor has to be divided by the sending surface area at the end
	 * 
	 * (non-Javadoc)
	 * 
	 * @see pik.clminputdata.tools.Integrable#f(double)
	 */
	@Override
	public double f(double x) {
		double l = ws * (height[heightLength - 1] - x)
				/ (height[roofIndex] - x);
		return nrmLRec(l, ls, height[heightLength - 1] - x);

	}

	@Override
	protected void saveToGlobal() {
		uclm.setFwos(iurb, id, jindex, iindex, fws);
	}

	public static void main(String[] args) {
		UrbanCLMConfiguration uclm = new UrbanCLMConfiguration(40., -170., 0.025, 0.025, 1., 0., 90, 90, 1, new double[] {-45., 0., 45., 90.}, new int[] {10}, new double[] {0., 5., 10., 15., 20., 30., 40., 55., 70., 80.}, false);
		uclm.initalizeSVFFields();
		Integrator itg = new Integrator();
		uclm.setBuildingWidth(0, 0, 56, 89, 7.20629);
		uclm.setStreetWidth(0, 0, 56, 89, 13.9001);
		WallOtherSkySVF svf = new WallOtherSkySVF(0, 0, 56, 89, uclm, itg);
		svf.run();
		System.out.println(svf.ls);
		System.out.println(svf.ws);
		System.out.println(svf.bs);
		for (int i = 0; i < uclm.getHeightA().length - 1; i++) {
			for (int j = 0; j < uclm.getHeightA().length; j++) {
				System.out.println(i + "  " + j + "  " + "  " + svf.fws[i][j]);
			}
		}
	}
}
