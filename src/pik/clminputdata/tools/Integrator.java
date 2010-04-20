/**
 * 
 */
package pik.clminputdata.tools;

import static java.lang.Math.sqrt;
import static java.lang.Math.abs;

/**
 * @author Sebastian Schubert
 * 
 */
public class Integrator {

	public final double eps;

	private final int maxk = 33554432;

	private final double[] xNST;
	private final double[] modx;
	private final double[] w;
	private final int nx;

	public Integrator() {
		this(4e-15);
	}

	public Integrator(double eps) {
		this.eps = eps;

		nx = 5;

		xNST = new double[nx];
		xNST[0] = -1. / 3. * sqrt(5. + 2. * sqrt(10. / 7.));
		xNST[1] = -1. / 3. * sqrt(5. - 2. * sqrt(10. / 7.));
		xNST[2] = 0.;
		xNST[3] = 1. / 3. * sqrt(5. - 2. * sqrt(10. / 7.));
		xNST[4] = 1. / 3. * sqrt(5. + 2. * sqrt(10. / 7.));

		modx = new double[nx];

		w = new double[nx];
		w[0] = (322. - 13. * sqrt(70.)) / 900.;
		w[1] = (322. + 13. * sqrt(70.)) / 900.;
		w[2] = 128. / 225.;
		w[3] = (322. + 13. * sqrt(70.)) / 900.;
		w[4] = (322. - 13. * sqrt(70.)) / 900.;
	}

	public double integral(Integrable f, double a, double b) throws NoConvergenceException {

		if (a == b)
			return 0.;

		double intStepN = 0;
		double intStepN1 = 0;

		double errorFac = Math.pow(2., 2. * nx);

		int k = 1;
		intStepN = intStep(f, a, b, k);

		double integral = 0.;

		while (k < maxk) {
			k *= 2;
			intStepN1 = intStep(f, a, b, k);
			// error is N^(2*nx), so factor 2^(2*nx) between stepN and stepN1
			integral = (errorFac * intStepN1 - intStepN) / (errorFac - 1.);
			if (abs(integral) > 1.e-15) {
				if (abs((integral - intStepN1) / integral) < eps)
					return integral;
			} else {
				if (abs(integral - intStepN1) < eps)
					return integral;
			}
			intStepN = intStepN1;
		}
		throw new NoConvergenceException("Number of integration intervals exceeded.", integral);
	}

	/**
	 * Calculate integral for fixed number of intervals for an arbitrary
	 * interval [a,b].
	 * 
	 * Sum over given x with weight w. Since the x are given for integral from
	 * -1 to 1, a linear shift has to be used. For the complete formula see e.g.
	 * Abramowitz and Stegun p. 887.
	 * 
	 * @param f
	 * @param a
	 * @param b
	 * @param nIntvalls
	 * @return
	 */
	private double intStep(Integrable f, double a, double b, int nIntvalls) {
		double h = (b - a) / nIntvalls;
		double h2 = h / 2;

		for (int i = 0; i < nx; i++) {
			modx[i] = xNST[i] * h2;
		}

		double is = 0.;
		double x = a + h2;
		for (int i = 0; i < nIntvalls; i++) {
			for (int j = 0; j < nx; j++) {
				is += w[j] * f.f(modx[j] + x);
			}
			x += h;
		}

		return h2 * is;
	}

}
