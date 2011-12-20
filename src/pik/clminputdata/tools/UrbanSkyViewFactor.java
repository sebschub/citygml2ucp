/**
 * 
 */
package pik.clminputdata.tools;

import static java.lang.Math.sqrt;
import static java.lang.Math.log;
import static java.lang.Math.atan;
import static java.lang.Math.pow;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import pik.clminputdata.configuration.UrbanCLMConfiguration;

/**
 * General class for the calculation of skyview factors in urban context.
 * 
 * @author Sebastian Schubert
 * 
 */
public abstract class UrbanSkyViewFactor implements Runnable {

	/**
	 * Width of the street
	 */
	public final double ws;
	/**
	 * Building width
	 */
	public final double bs;
	/**
	 * Length of street
	 */
	public final double ls;
	/**
	 * Height levels of roofs (not necessary all levels are used)
	 */
	public final double[] height;
	/**
	 * Length of height array up to which calculation should be done
	 */
	public final int heightLength;

	/**
	 * Index urban class
	 */
	public final int iurb;
	/**
	 * Index street direction
	 */
	public final int id;
	/**
	 * Lon index
	 */
	public final int iindex;
	/**
	 * Lat index
	 */
	public final int jindex;

	public UrbanSkyViewFactor(int iurb, int id, int jindex, int iindex,
			UrbanCLMConfiguration uclm) {
		this.ws = uclm.getStreetWidth(iurb, id, jindex, iindex);
		// System.out.println(ws);
		this.bs = uclm.getBuildingWidth(iurb, id, jindex, iindex);
		// System.out.println(bs);
		this.ls = uclm.getStreetLength(id, jindex) * 1000;
		// System.out.println(ls);
		this.height = uclm.getHeightA();
		this.heightLength = uclm.getLocalKe_urbanMax(iurb, id, jindex, iindex);
		this.iurb = iurb;
		this.id = id;
		this.iindex = iindex;
		this.jindex = jindex;
	}

	public UrbanSkyViewFactor(double ws, double bs, double ls, double[] height,
			int iurb, int id, int iindex, int jindex) {
		this.ws = ws;
		this.bs = bs;
		this.ls = ls;
		this.height = height;
		this.heightLength = height.length;
		this.iurb = iurb;
		this.id = id;
		this.iindex = iindex;
		this.jindex = jindex;
	}

	/**
	 * Save skyview factor to global configuration class.
	 */
	protected abstract void saveToGlobal();

	/**
	 * Skyview factor from a line element (somehow parallel) to a rectangle
	 * 
	 * @param a
	 *            Parameter of rectangle
	 * @param b
	 *            Parameter of rectangle and length of line element
	 * @param c
	 *            Distance of rectangle to line element
	 * @return Skyview factor
	 */
	public static double prlLRec(double a, double b, double c) {
		if (c == 0.) {
			return 0.5;
		}

		double x = b / c;
		double x2 = x * x;
		double y = a / c;
		double y2 = y * y;

		return 1.
				/ (x * PI)
				* (sqrt(1 + x2) * atan(y / sqrt(1 + x2)) - atan(y) + x * y
						/ sqrt(1 + y2) * atan(x / sqrt(1 + y2)));
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
	public static double nrmLRec(double a, double b, double c) {
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

	/**
	 * Skyview factor of rectangles normal to each other.
	 * 
	 * @param a
	 *            Width of receiving rectangle
	 * @param b
	 *            Common side of rectangle
	 * @param c
	 *            Width of sending rectangle
	 * @return Skyview factor
	 */
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

	/**
	 * Skyview factor of two parallel rectangles of equal size.
	 * 
	 * @param a
	 *            Width of rectangles
	 * @param b
	 *            Height of rectangles
	 * @param c
	 *            Distance of rectangles
	 * @return Skyview factor
	 */
	public static double fprls(double a, double b, double c) {

		if (a <= 1.e-13 || b <= 1.e-13) {
			return 0.;
		}

		if (c <= 1.e-13) {
			return 1.;
		}

		double x = a / c;
		double x2 = x * x;
		double y = b / c;
		double y2 = y * y;

		return 2.
				/ (PI * x * y)
				* (0.5 * log((1. + x2) * (1. + y2) / (1. + x2 + y2)) + y
						* (sqrt(1. + x2)) * atan(y / (sqrt(1. + x2))) + x
						* (sqrt(1. + y2)) * atan(x / (sqrt(1. + y2))) - y
						* atan(y) - x * atan(x));
	}

	/**
	 * Skyview factor * sending area for two normal elements 1 to 4 (1 2 normal
	 * to 34 with 2 connected to 4)
	 * 
	 * @param z1
	 *            Inner boundary of 1
	 * @param z2
	 *            Outer boundary of 1
	 * @param h1
	 *            Inner boundary of 2
	 * @param h2
	 *            Outer boundary of 2
	 * @param ls
	 *            Street length
	 * @return SVF
	 */
	public static double fnrm14(double z1, double z2, double h1, double h2,
			double ls) {
		return h2 * (fnrms(z2, ls, h2) - fnrms(z1, ls, h2)) + h1
				* (fnrms(z1, ls, h1) - fnrms(z2, ls, h1));
	}

	/**
	 * Skyview factor * sending area for two normal elements 1 to 3 (12 normal
	 * to 3)
	 * 
	 * @param h1
	 *            Larger distance from 3
	 * @param h2
	 *            Smaller distance from 3
	 * @param z
	 *            Size of 3
	 * @param ls
	 *            Common side of 12 and 3 (street length)
	 * @return
	 */
	public static double fnrm13(double h1, double h2, double z, double ls) {
		return h1 * fnrms(z, ls, h1) - h2 * fnrms(z, ls, h2);
	}

	/**
	 * Skyview factor * Sending Area for two parallel elements 1 to 34 (1 2
	 * parallel to 3 4).
	 * 
	 * @param g
	 *            Size of sending area (1)
	 * @param s
	 *            Size of receiving area (34)
	 * @param h
	 *            Distance (1) and (34)
	 * @param ls
	 *            Street length
	 * @return SVF
	 */
	public static double fprl134(double g, double s, double h, double ls) {
		return 0.5 * (s * fprls(s, ls, h) + g * fprls(g, ls, h) - (s - g)
				* fprls(s - g, ls, h));
	}

	/**
	 * Skyview factor * Sending Area for two parallel elements 1 to 6 (1 2 3
	 * parallel to 4 5 6).
	 * 
	 * @param s1
	 *            Lower boundary of sending surface (1)
	 * @param s2
	 *            Upper boundary of sending surface (1)
	 * @param r1
	 *            Lower boundary of receiving surface (2)
	 * @param r2
	 *            Upper boundary of receiving surface (2)
	 * @param ls
	 *            Street length
	 * @param d
	 *            Distance of elements
	 * @return SVF
	 */
	public static double fprl16(double s1, double s2, double r1, double r2,
			double ls, double d) {
		return 0.5 * (abs(r2 - s1) * fprls(ls, abs(r2 - s1), d) - abs(r2 - s2)
				* fprls(ls, abs(r2 - s2), d) - abs(r1 - s1)
				* fprls(ls, abs(r1 - s1), d) + abs(r1 - s2)
				* fprls(ls, abs(r1 - s2), d));
	}

}
