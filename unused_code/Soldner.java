/**
 * 
 */
package pik.clminputdata.configuration;

import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionPointImpl;

import static java.lang.Math.sqrt;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.cos;

/**
 * @author Sebastian Schubert
 * 
 */
public class Soldner extends ProjectionImpl {

	private static final long serialVersionUID = 1L;

	public final LatLonPoint origin;

	// public final double olat, olon;

	private final double olatrad, olonrad;

	public final double feast, fnorth;

	public final double a, ifl;

	public final double ecc;

	private final double pRho1, ecc2, pMu1, e1, e12, e13, e14, m0;
	private final double p1Phi1, p2Phi1, p3Phi1, p4Phi1;
	private final double p1M, p2M, p3M, p4M;

	public Soldner() {
		this(52. + 25. / 60. + 7.1338 / 3600., 13. + 37. / 60. + 37.9332
				/ 3600., 40000., 10000., 6377397.155, 299.1528128);

	}

	public Soldner(double olat, double olon, double feast, double fnorth,
			double a, double ifl) {
		this(new LatLonPointImpl(olat, olon), feast, fnorth, a, ifl);
	}

	/**
	 * Initilize for from rotated origin point (formally a ProjectionPoint)
	 * @param pp
	 * @param feast
	 * @param fnorth
	 * @param a
	 * @param ifl
	 */
	public Soldner(ProjectionPoint pp, double feast, double fnorth,
			double a, double ifl) {
		this(new LatLonPointImpl(pp.getX(), pp.getY()), feast, fnorth, a, ifl);
	}
	
	public Soldner(LatLonPoint origin, double feast, double fnorth, double a,
			double ifl) {
		this.origin = origin;
		olatrad = Math.toRadians(origin.getLatitude());
		olonrad = Math.toRadians(origin.getLongitude());
		this.feast = feast;
		this.fnorth = fnorth;
		this.a = a;
		this.ifl = ifl;

		addParameter(ATTR_NAME, "soldner");
		addParameter("natural_origin_latitude", origin.getLatitude());
		addParameter("natural_origin_longitude", origin.getLongitude());
		addParameter("false_northing", fnorth);
		addParameter("false_easting", feast);
		addParameter("ellipsoid_semi_major_axis", a);
		addParameter("ellipsoid_inverse_flattening", ifl);

		// private values for calculation
		double ecc4, ecc6;
		ecc2 = 2. / ifl - 1. / (ifl * ifl);
		ecc4 = ecc2 * ecc2;
		ecc6 = ecc2 * ecc4;
		ecc = sqrt(ecc2);
		e1 = (1 - sqrt(1 - ecc2)) / (1 + sqrt(1 - ecc2));
		e12 = e1 * e1;
		e13 = e1 * e12;
		e14 = e1 * e13;

		p1Phi1 = 3. * e1 / 2. - 27 * e13 / 32.;
		p2Phi1 = 21. * e12 / 16. - 55. * e14 / 32.;
		p3Phi1 = 151. * e13 / 96.;
		p4Phi1 = 1097. * e14 / 512.;

		pRho1 = a * (1 - ecc2);

		p1M = (1. - ecc2 / 4. - 3 * ecc4 / 64. - 5. * ecc6 / 256.);
		pMu1 = 1. / (a * p1M);

		p2M = 3. * ecc2 / 8. + 3. * ecc4 / 32. + 45. * ecc6 / 1024.;
		p3M = 15. * ecc4 / 256. + 45. * ecc6 / 1024.;
		p4M = 35. * ecc6 / 3072.;

		m0 = a
				* (p1M * olatrad - p2M * sin(2. * olatrad) + p3M
						* sin(4. * olatrad) - p4M * sin(6. * olatrad));

	}

	@Override
	public ProjectionPoint latLonToProj(LatLonPoint latlon,
			ProjectionPointImpl destPoint) {

		double latrad, lonrad;
		latrad = Math.toRadians(latlon.getLatitude());
		lonrad = Math.toRadians(latlon.getLongitude());

		double sinlat, sinlatsq, coslat, coslatsq, tanlat, t;
		sinlat = sin(latrad);
		coslat = cos(latrad);
		sinlatsq = sinlat * sinlat;
		coslatsq = 1. - sinlatsq;
		tanlat = sinlat / coslat;
		t = tanlat * tanlat;

		double m, al, al2, al3, al4, al5, c, nu; // al is the big A
		m = a
				* (p1M * latrad - p2M * sin(2. * latrad) + p3M
						* sin(4. * latrad) - p4M * sin(6. * latrad));
		nu = a / pow(1 - ecc2 * sinlatsq, 0.5);
		c = ecc2 * coslatsq / (1. - ecc2);
		al = (lonrad - olonrad) * coslat;
		al2 = al * al;
		al3 = al2 * al;
		al4 = al2 * al2;
		al5 = al4 * al;
		// x = m - m0 + nu*tanlat*(al2/2. + (5-t+6*c)*al4/24.);

		double east, north;

		east = feast + nu
				* (al - t * al3 / 6. - (8 - t + 8. * c) * t * al5 / 120.);
		north = fnorth + m - m0 + nu * tanlat
				* (al2 / 2. + (5 - t + 6 * c) * al4 / 24.);

		destPoint = new ProjectionPointImpl(east, north);

		return destPoint;
	}

	@Override
	public LatLonPoint projToLatLon(ProjectionPoint ppt,
			LatLonPointImpl destPoint) {

		double north, east;
		north = ppt.getY();
		east = ppt.getX();

		double sinphi1, sinphi1sq, cosphi1inv, tanphi1, phi1, mu1;

		mu1 = (m0 + (north - fnorth)) * pMu1;

		phi1 = mu1 + p1Phi1 * sin(2 * mu1) + p2Phi1 * sin(4 * mu1) + p3Phi1
				* sin(6 * mu1) + p4Phi1 * sin(8 * mu1);
		sinphi1 = sin(phi1);
		sinphi1sq = sinphi1 * sinphi1;
		cosphi1inv = 1. / cos(phi1);
		tanphi1 = sinphi1 * cosphi1inv;

		double rho1, nu1;

		rho1 = pRho1 * pow(1 - ecc2 * sinphi1sq, -1.5);
		nu1 = a * pow(1 - ecc2 * sinphi1sq, -0.5);

		double d, t1, d2, d3, d4, d5;

		d = (east - feast) / nu1;
		d2 = d * d;
		d3 = d2 * d;
		d4 = d2 * d2;
		d5 = d4 * d;
		t1 = tanphi1 * tanphi1;
		// System.out.println(rho1);
		double lat, lon;

		lat = phi1 - (nu1 * tanphi1 / rho1)
				* (d2 / 2. - (1 + 3 * t1) * d4 / 25.);

		lon = olonrad + (d - t1 * d3 / 3. + (1 + 3 * t1) * t1 * d5 / 15.)
				* cosphi1inv;

		destPoint = new LatLonPointImpl(Math.toDegrees(lat), Math
				.toDegrees(lon));
		return destPoint;

	}

	@Override
	public ProjectionImpl constructCopy() {
		return new Soldner(origin,feast,fnorth,a,ifl);
	}

	@Override
	public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean equals(Object proj) {
		if (proj instanceof Soldner) {
			Soldner pr = (Soldner) proj;
			return (a == pr.a) && (ifl == pr.ifl) && origin.equals(pr.origin)
					&& (fnorth == pr.fnorth) && (feast == pr.feast);
		}
		return false;
	}

	@Override
	public String paramsToString() {
		return "origin: " + origin + ", false easting: " + feast
				+ ", false northing: " + fnorth
				+ ", ellipsoid semi-major axis: " + a
				+ ", ellipsoid inverse flattening: " + ifl;
	}

	/**
	 * test with testcase
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		// length are in Clarke's links
		Soldner s = new Soldner(Math.toDegrees(0.182241463), Math
				.toDegrees(-1.070468608), 430000, 325000, 31706587.88,
				294.2606764);

		ProjectionPoint p = s.latLonToProj(10, -62);
		System.out.println(p.getX()); // 66644.94
		System.out.println(p.getY()); // 82536.22

		LatLonPoint l = s.projToLatLon(p);

		// System.out.println(s.olat);
		// System.out.println(s.olon);
		// System.out.println(s.e1);
		// System.out.println(s.ecc2);
		// System.out.println(s.m0);

		System.out.println(l.getLatitude()); // 10
		System.out.println(l.getLongitude()); // -62
	}

}
