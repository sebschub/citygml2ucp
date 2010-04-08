/**
 * 
 */
package pik.clminputdata.tools;

import static java.lang.Math.sqrt;
import static java.lang.Math.log;
import static java.lang.Math.atan;
import static java.lang.Math.pow;
import static java.lang.Math.PI;
import pik.clminputdata.configuration.UrbanCLMConfiguration;

/**
 * @author Sebastian Schubert
 * 
 */
public abstract class UrbanSkyViewFactor implements Runnable {

	public final double ws;
	public final double bs;
	public final double ls;
	public final double[] height;

	public final int iurb;
	public final int id;
	public final int iindex;
	public final int jindex;

	public UrbanSkyViewFactor(int iurb, int id, int jindex, int iindex,
			UrbanCLMConfiguration uclm) {
		this.ws = uclm.getStreetWidth(iurb, id, jindex, iindex);
//		System.out.println(ws);
		this.bs = uclm.getBuildingWidth(iurb, id, jindex, iindex);
//		System.out.println(bs);
		this.ls = uclm.getStreetLength(id, jindex) * 1000;
//		System.out.println(ls);
		this.height = uclm.getHeightA();
		this.iurb = iurb;
		this.id = id;
		this.iindex = iindex;
		this.jindex = jindex;
	}

	protected abstract void saveToGlobal();

	public static double PrlLRec(double a, double b, double c) {
		return 0.;
	}

	/**
	 * Skyview factor line element normal to a rectangle.
	 * 
	 * The line element is at a distance c to the side b of the rectangle.
	 * 
	 * @param a
	 *            Height of the rectangle
	 * @param b
	 *            Width of the rectangle and length of the line element
	 * @param c
	 *            Distance of line element to rectangle
	 * @return Skyview factor
	 */
	public static double NrmLRec(double a, double b, double c) {
		if (b == 0) {
			return 0.;
		}
		if (c <= 1.e-13) {
			return 0.5;
		}
		double x = a / b;
		double y = c / b;
		double x2 = pow(x, 2);
		double y2 = pow(y, 2);
		double z2 = x2 + y2;
		double z = sqrt(z2);

		return 1.
				/ PI
				* (atan(1. / y) + 0.5 * y
						* log((y2 * (z2 + 1.)) / ((y2 + 1.) * z2)) - y / z
						* atan(1 / z));

	}

	public static double fnrms(double a, double b, double c) {
		if (b <= 1.e-13) {
			return 0.;
		}
		double x = a / b;
		double x2 = x * x;
		double y = c / b;
		double y2 = y * y;
		double z2 = x2 + y2;
		double z = sqrt(z2);

		if (y <= 1.e-13 || x <= 1.e-13) {
			return 0;
		}

		double a1 = log((1. + x2) * (1. + y2) / (1. + z2));
		double a2 = y2 * log(y2 * (1. + z2) / z2 / (1. + y2));
		double a3 = x2 * log(x2 * (1. + z2) / z2 / (1. + x2));
		double a4 = y * atan(1. / y);
		double a5 = x * atan(1. / x);
		double a6 = z * atan(1. / z);
		return (0.25 * (a1 + a2 + a3) + a4 + a5 - a6) / (PI * y);
	}

	public static double fnrm14(double z1, double z2, double h1, double h2,
			double ls) {
		return h2 * (fnrms(z2, ls, h2) - fnrms(z1, ls, h2)) + h1
				* (fnrms(z1, ls, h1) - fnrms(z2, ls, h1));
	}
}
