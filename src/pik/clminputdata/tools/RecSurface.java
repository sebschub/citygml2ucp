package pik.clminputdata.tools;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.vecmath.*;

import org.citygml4j.impl.jaxb.gml._3_1_1.DirectPositionListImpl;
import org.citygml4j.impl.jaxb.gml._3_1_1.ExteriorImpl;
import org.citygml4j.impl.jaxb.gml._3_1_1.LinearRingImpl;
import org.citygml4j.impl.jaxb.gml._3_1_1.PolygonImpl;
import org.citygml4j.impl.jaxb.gml._3_1_1.SurfacePropertyImpl;
import org.citygml4j.model.gml.*;

/**
 * Class for planar polygons.
 * 
 * @author Sebastian Schubert
 * 
 */
public class RecSurface {

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
	// List<Point3d> points2;
	/**
	 * 3d coordinates relative uv1, uv2 and pos
	 */
	Point2d[] points2d;

	/**
	 * Centroid already calculated?
	 */
	private boolean isSetCentroid = false;
	/**
	 * Centroid of the polygon
	 */
	private Point3d centroid;

	/**
	 * Area of the polygon
	 */
	private double area;
	/**
	 * Area already calculated?
	 */
	private boolean isSetArea = false;

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
	 *            Descibes the polygon
	 */
	public RecSurface(SurfaceProperty surfaceProperty) {
		if (surfaceProperty.getSurface() instanceof Polygon) {
			Polygon polygon = (Polygon) surfaceProperty.getSurface();
			if (polygon.getExterior().getRing() instanceof LinearRing) {
				LinearRing lRing = (LinearRing) polygon.getExterior().getRing();
				if (lRing.isSetPosList()) {
					List<Double> coord = lRing.getPosList().getValue();

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

					// uvn = new Vector3d(uv1);
					//
					// uvn.cross(uv1, uv2);
					// // just to be sure
					// uvn.normalize();

					// System.out.println(checkCoplanarity(points));

					pos = points.get(0);

					// get 2d points
					points2d = new Point2d[points.size()];
					for (int i = 0; i < points.size(); i++) {

						Point3d pvs = new Point3d(points.get(i));
						pvs.sub(pos);

						points2d[i] = new Point2d(uv1.dot(pvs), uv2.dot(pvs));
						// System.out.println(points2d[i]);
					}

					// if (!checkCoplanarity(points)) {
					// for (Point3d point3d : points) {
					// System.out.println(point3d);
					// }
					// System.out.println(uvn);
					// System.out.println(imax);
					// System.out.println(jmax);
					// throw new UnsupportedOperationException("Not planar!");
					// }

				} else {
					throw new IllegalArgumentException(
							"Linear ring is no PosList, handle this case!");
				}
			} else {
				throw new IllegalArgumentException(
						"Polygon is no linear ring, handle this case!");
			}
		} else {
			throw new IllegalArgumentException(
					"Surface is no Polygon, handle this case!");
		}
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

	/**
	 * Is the polygon horizontal?
	 * 
	 * @return true for horizontal
	 */
	public boolean isHorizontal() {
		getAngle();
		return this.isHorizontal;
	}

	// from openjava in Polygon
	public boolean contains(Point3d p1, Point3d p2) {

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

		// System.out.println(scaleFactor);
		if (scaleFactor < 0 || scaleFactor > 1) {
			return false;
		}

		temp.scale(scaleFactor);

		planePoint.add(temp);

		planePoint.sub(pos);

		// System.out.println(planePoint);

		// coressponding point2d in plane

		double x = uv1.dot(planePoint);
		double y = uv2.dot(planePoint);

		int hits = 0;

		double lastx = points2d[points2d.length - 1].x;
		double lasty = points2d[points2d.length - 1].y;
		double curx, cury;

		// Walk the edges of the polygon
		for (int i = 0; i < points2d.length; lastx = curx, lasty = cury, i++) {
			curx = points2d[i].x;
			cury = points2d[i].y;

			if (cury == lasty) {
				continue;
			}

			double leftx;
			if (curx < lastx) {
				if (x >= lastx) {
					continue;
				}
				leftx = curx;
			} else {
				if (x >= curx) {
					continue;
				}
				leftx = lastx;
			}

			double test1, test2;
			if (cury < lasty) {
				if (y < cury || y >= lasty) {
					continue;
				}
				if (x < leftx) {
					hits++;
					continue;
				}
				test1 = x - curx;
				test2 = y - cury;
			} else {
				if (y < lasty || y >= cury) {
					continue;
				}
				if (x < leftx) {
					hits++;
					continue;
				}
				test1 = x - lastx;
				test2 = y - lasty;
			}

			if (test1 < (test2 / (lasty - cury) * (lastx - curx))) {
				hits++;
			}
		}

		return ((hits & 1) != 0);
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
					// System.out.println(max);
					// System.out.println(j);
					// System.out.println(i);
				}
			}
		}

		if (max > diffAngle) {
			return false;
		}
		return true;
	}

	public double getArea() {
		if (isSetArea) {
			return area;
		}
		area = 0;

		for (int i = 0; i < points2d.length - 1; i++) {
			// if(points2d[i]==null) System.out.println("1");
			// if(points2d[i+1]==null) System.out.println("2");
			area += (points2d[i].x * points2d[i + 1].y)
					- (points2d[i + 1].x * points2d[i].y);
			// System.out.println(area);
		}
		area = 0.5 * Math.abs(area);

		isSetArea = true;

		/*
		 * if (area < 1e-12) { for (int i = 0; i < points2.size(); i++) {
		 * System.out.println(points2.get(i)); }
		 * System.out.println("++++++++++++++++++++"); for (int i = 0; i <
		 * points.size(); i++) { System.out.println(points.get(i)); }
		 * System.out.println("===================="); }
		 */
		return area;
	}

	public Point3d getCentroid() {
		if (isSetCentroid) {
			return centroid;
		}
		double x = 0, y = 0;
		double iarea;
		iarea = 1. / (6. * getArea());

		for (int i = 0; i < points2d.length - 1; i++) {
			x += (points2d[i].x + points2d[i + 1].x)
					* (points2d[i].x * points2d[i + 1].y - points2d[i + 1].x
							* points2d[i].y);
			y += (points2d[i].y + points2d[i + 1].y)
					* (points2d[i].x * points2d[i + 1].y - points2d[i + 1].x
							* points2d[i].y);
		}

		x *= iarea;
		y *= iarea;

		// convert to 3d

		Vector3d a = new Vector3d(uv1);
		Vector3d b = new Vector3d(uv2);

		a.scale(x);
		b.scale(y);

		a.add(b);
		a.add(pos);

		centroid = new Point3d(a);

		isSetCentroid = true;

		return centroid;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getCentroid().toString();
	}

	public static void main(String[] args) {

		// test2 t = new test2();
		//				
		// t.test=10;
		//		
		// List<test2> list = new LinkedList<test2>();
		// list.add(t);
		//		
		// System.out.println(list.get(0).test);
		//		
		// t.test=15;
		// System.out.println(list.get(0).test);

		RecSurface s1;
		LinearRing l1;

		l1 = new LinearRingImpl();

		DirectPositionList dpl = new DirectPositionListImpl();
		List<Double> l = new ArrayList<Double>();

		// l.add(0.);
		// l.add(0.);
		// l.add(0.);
		// l.add(1.);
		// l.add(0.);
		// l.add(0.);
		// l.add(1.);
		// l.add(1.);
		// l.add(0.);
		// l.add(0.);
		// l.add(1.);
		// l.add(0.);
		// l.add(26048.0730771502);
		// l.add(31065.1755088699);
		// l.add(50.189998626709);
		// l.add(26048.0730771502);
		// l.add(31065.1755088699);
		// l.add(55.5244444127672);
		// l.add(26052.787018);
		// l.add(31053.422852);
		// l.add(55.5244444127672);
		// l.add(26052.787018);
		// l.add(31053.422852);
		// l.add(50.189998626709);
		// l.add(26048.0730771502);
		// l.add(31065.1755088699);
		// l.add(50.189998626709);

		// l.add(26024.7699070815);
		// l.add(31082.2300183635);
		// l.add(51.4799995422363);
		// l.add(26024.7699070815);
		// l.add(31082.2300183635);
		// l.add(54.4529152209556);
		// l.add(26019.795074);
		// l.add(31075.037231);
		// l.add(54.4529152209556);
		// l.add(26019.795074);
		// l.add(31075.037231);
		// l.add(51.4799995422363);
		// l.add(26024.7699070815);
		// l.add(31082.2300183635);
		// l.add(51.4799995422363);

		// test for removing point
		l.add(19988.1035003783);
		l.add(15354.6110825843);
		l.add(65.47);
		l.add(19982.7416654217);
		l.add(15357.6904089219);
		l.add(65.47);
		l.add(19983.2982464282);
		l.add(15353.0810249);
		l.add(65.14);
		l.add(19986.0124784483);
		l.add(15351.5222290375);
		l.add(65.14);
		l.add(19986.0139221508);
		l.add(15351.5243616719);
		l.add(65.1410377179733);
		l.add(19988.1035003783);
		l.add(15354.6110825843);
		l.add(65.47);

		dpl.setValue(l);

		l1.setPosList(dpl);

		Exterior ext = new ExteriorImpl();

		ext.setRing(l1);

		Polygon poly = new PolygonImpl();
		poly.setExterior(ext);

		SurfaceProperty surfaceProperty = new SurfacePropertyImpl();
		surfaceProperty.setSurface(poly);

		s1 = new RecSurface(surfaceProperty);

		System.out.println("==============");

		System.out.println(s1.uv1);
		System.out.println(s1.uv2);

		System.out.println(s1.uvn);
		System.out.println(s1.getArea());
		System.out.println(s1.getCentroid());

		System.out.println(s1.contains(new Point3d(26050.4300475751,
				31059.299180434948, 52.857221519738104), new Point3d(
				26043.7517713251, 31063.49275318495, 53.16715849443683)));

		//
		// public RecSurface(SurfaceProperty surfaceProperty) {
		// if (surfaceProperty.getSurface() instanceof Polygon) {
		// Polygon polygon = (Polygon) surfaceProperty.getSurface();
		// if (polygon.getExterior().getRing() instanceof LinearRing) {
		// LinearRing lRing = (LinearRing) polygon.getExterior();
		//	
	}
}
