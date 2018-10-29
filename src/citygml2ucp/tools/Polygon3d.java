package citygml2ucp.tools;

import java.util.LinkedList;
import java.util.List;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.citygml4j.model.gml.geometry.primitives.SurfaceProperty;

/**
 * Planar polygon in 3d space.
 * 
 * It included a Polygon2d which represents the polygon in its plane and the two
 * base vectors for these 2d coordinates. Also a vector normal to the surface is
 * calculated.
 * 
 * @author Sebastian Schubert
 * 
 */
public class Polygon3d extends ClosedSurface<Point3d> {
	
	public final String id;

	/**
	 * points with this distance are supposed to be equal and one is remove
	 */
	private static double equalDistance = 0.00001;

	/**
	 * the angle that the angle between normal and one in the "plane" is allowed
	 * to differ
	 */
	private static double diffAngle = 5. / 180. * Math.PI;

	/**
	 * Vector normal to polygon
	 */
	Vector3d uvn;

	/**
	 * base vector of plane of polygon
	 */
	Vector3d uv1, uv2;
	/**
	 * point in plane
	 */
	Point3d pos;
	/**
	 * 3d points of polygon
	 */
	List<Point3d> points;

	/**
	 * 2d coordinates relative uv1, uv2 and pos
	 */
	Polygon2d polygon2d;

	/**
	 * Angle of the polygon in the horizontal plane (x-y) relative to the x axis
	 */
	private double angle;
	/**
	 * Angle already calculated?
	 */
	private boolean isSetAngle = false;
	/**
	 * Polygon is horizontal so {@code angle} is not defined?
	 */
	private boolean isHorizontal = false;

	/**
	 * Constructor.
	 * 
	 * @param surfaceProperty
	 *            Describes the polygon
	 */
	public Polygon3d(String id, SurfaceProperty surfaceProperty) {
		this.id = id;
		
		List<Double> coord = CityGMLTools.coordinatesFromSurfaceProperty(surfaceProperty);

		points = new LinkedList<Point3d>();
		// System.out.println(coord.size()/3);
		for (int i = 0; i < coord.size(); i += 3) {
			points.add(new Point3d(coord.get(i), coord.get(i + 1),
					coord.get(i + 2)));
		}

		/*
		 * // for testing points2 = new LinkedList<Point3d>(points);
		 */

		// remove points which are very near to each other. Since
		// the points ought to be planar, these vector between one
		// correct and one "faulty" point is not in the plane

		// the last one is always the first one, don't delete it
		{
			int i = 0;
			int j = 0;

			while (i < points.size() - 1) {
				while (j < points.size() - 1) {
					if (i == j) {
						j++;
						continue;
					}
					double dist = (new Vector3d(points.get(i),
							points.get(j))).length();
					// System.out.println("i: " + i + "  j: " + j);
					// System.out.println(dist);
					if (dist < equalDistance) {
						points.remove(j);
					} else {
						j++;
					}
				}
				i++;
				j = 0;
			}
		}

		uvn = new Vector3d();

		// find most rectangular vector
		double min = Double.MAX_VALUE;
		int imax = 0, jmax = 0;
		Vector3d a, b;
		for (int j = 0; j < points.size() - 1; j++) {
			a = new Vector3d(points.get(j), points.get(j + 1));
			for (int i = j + 1; i < points.size(); i++) {
				if (i + 1 == points.size()) {
					b = new Vector3d(points.get(i), points.get(0));
				} else {
					b = new Vector3d(points.get(i), points
							.get(i + 1));
				}

				Vector3d temp = new Vector3d(a);
				temp.cross(a, b);

				uvn.add(temp);

				double dp = Math.abs(a.dot(b)) / a.length()
						/ b.length();
				if (dp < min) {
					min = dp;
					imax = i;
					jmax = j;
				}
			}
		}

		uvn.normalize();

		a = new Vector3d(points.get(jmax), points.get(jmax + 1));
		if (imax + 1 == points.size()) {
			b = new Vector3d(points.get(imax), points.get(0));
		} else {
			b = new Vector3d(points.get(imax), points.get(imax + 1));
		}

		a.normalize();
		uv1 = new Vector3d(a);

		// get perpendenicular second vector;

		a.scale(-a.dot(b));
		a.add(b);
		uv2 = new Vector3d(a);
		uv2.normalize();

		pos = points.get(0);

		// calculate 2d points relative to base vectors
		double[] xcoord = new double[points.size()];
		double[] ycoord = new double[points.size()];
		for (int i = 0; i < points.size(); i++) {
			Point3d pvs = new Point3d(points.get(i));
			pvs.sub(pos);
			xcoord[i] = uv1.dot(pvs);
			ycoord[i] = uv2.dot(pvs);
		}

		polygon2d = new Polygon2d(xcoord, ycoord);
	}
	
	/**
	 * Calculate the angle of normal vector projected on horizontal plane.
	 * 
	 * @return angle in degrees
	 */
	public double getAngle() {
		if (isSetAngle) {
			return angle;
		}
		if (Math.abs(uvn.x) < 1.e-12) {
			if (Math.abs(uvn.y) < 1.e-12) {
				angle = -1000.;
				isHorizontal = true;
			} else {
				angle = 90.;
			}
		} else {
			angle = Math.toDegrees(Math.atan(uvn.y / uvn.x));
		}
		isSetAngle = true;
		return angle;
	}

	
	public List<Point3d> getPoints(){
		return this.points;		
	}
	
	/**
	 * Is the polygon horizontal?
	 * 
	 * @return true for horizontal
	 */
	public boolean isHorizontal() {
		getAngle();
		return this.isHorizontal;
	}

	public boolean checkCoplanarity() {

		double max = Double.MIN_VALUE;
		Vector3d a;
		for (int j = 0; j < points.size(); j++) {
			for (int i = 0; i < points.size(); i++) {
				if (i == j) {
					continue;
				}
				a = new Vector3d(points.get(i), points.get(j));

				double dot = Math.acos(a.dot(uvn) / a.length()) - Math.PI / 2.;

				if (dot > max) {
					max = dot;
				}
			}
		}

		if (max > diffAngle) {
			return false;
		}
		return true;
	}

	@Override
	protected double calcSignedArea() {
		return polygon2d.getSignedArea();
	}

	@Override
	protected Point3d calcCentroid() {
		return get3dFrom2d(polygon2d.getCentroid());
	}

	protected Point3d get3dFrom2d(Point2d point) {
		Vector3d a = new Vector3d(uv1);
		Vector3d b = new Vector3d(uv2);

		a.scale(point.x);
		b.scale(point.y);

		a.add(b);
		a.add(pos);

		return new Point3d(a);
	}
	
	
	/**
	 * Does the line between two points hit the polygon?
	 * 
	 * @param p1
	 *            One point
	 * @param p2
	 *            Other point
	 * @return hit?
	 */
	public boolean isHitBy(Point3d p1, Point3d p2) {
		// vector of direction
		Vector3d rv = new Vector3d(p1, p2);

		double dprv = uvn.dot(rv);
		// System.out.println(dprv);

		if (Math.abs(dprv) < 1e-10) {
			return false;
		}

		// calc point which is on the plane

		Point3d planePoint = new Point3d(p1);
		Point3d temp = new Point3d(rv);

		double scaleFactor = (uvn.dot(pos) - uvn.dot(p1)) / dprv;

		if (scaleFactor < 0 || scaleFactor > 1) {
			return false;
		}

		temp.scale(scaleFactor);

		planePoint.add(temp);

		planePoint.sub(pos);

		// Corresponding point2d in plane

		double x = uv1.dot(planePoint);
		double y = uv2.dot(planePoint);

		return polygon2d.contains(x, y);
	}

}
