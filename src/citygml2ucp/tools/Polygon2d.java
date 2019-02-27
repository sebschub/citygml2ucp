/**
 * 
 */
package citygml2ucp.tools;

import javax.vecmath.Point2d;

/**
 * A 2d polygon based on {@link javax.vecmath.Point2d Point2d}.
 * 
 * @author Sebastian Schubert
 * 
 */
public class Polygon2d extends ClosedSurface<Point2d> {

	/**
	 * Coordinates of the polygon
	 */
	private double[] xcoord, ycoord;

	public static Polygon2d xyProjectedPolygon2d(Polygon3d polygon) {

		double[] xcoord = new double[polygon.points.size()];
		double[] ycoord = new double[polygon.points.size()];

		for (int i = 0; i < polygon.points.size(); i++) {
			xcoord[i] = polygon.points.get(i).x;
			ycoord[i] = polygon.points.get(i).y;
		}
		return new Polygon2d(xcoord, ycoord); 
	}
	
	/**
	 * Constructor.
	 * 
	 * @param x
	 *            x coordinates
	 * @param y
	 *            y coordinates
	 * @throws IllegalArgumentException
	 *             x and y do not have the same length
	 */
	public Polygon2d(double[] x, double[] y) throws IllegalArgumentException {
		if (x.length != y.length) {
			throw new IllegalArgumentException(
					"x and y arrays have to be of the same size.");
		}
		this.xcoord = x;
		this.ycoord = y;
		
		this.signedArea = this.calcSignedArea();
		this.centroid = this.calcCentroid();
	}

	/*
	 * (non-Javadoc) Surveyor's Formula:
	 * http://en.wikipedia.org/wiki/Polygon#Area_and_centroid
	 * 
	 * @see citygml2ucp.tools.ClosedSurface#calcArea()
	 */
	@Override
	protected double calcSignedArea() {
		double area = 0;
		for (int i = 0; i < xcoord.length - 1; i++) {
			area += (xcoord[i] * ycoord[i + 1]) - (xcoord[i + 1] * ycoord[i]);
		}
		return 0.5 * area;
	}

	public double[] getxcoord(){
		return this.xcoord;
	}
	
	public double[] getycoord(){
		return this.ycoord;
	}
	
	/*
	 * (non-Javadoc) http://en.wikipedia.org/wiki/Polygon#Area_and_centroid
	 * 
	 * @see citygml2ucp.tools.ClosedSurface#calcCentroid()
	 */
	@Override
	protected Point2d calcCentroid() {
		double x = 0, y = 0;
		double iarea;
		iarea = 1. / (6. * getSignedArea());

		for (int i = 0; i < xcoord.length - 1; i++) {
			x += (xcoord[i] + xcoord[i + 1])
					* (xcoord[i] * ycoord[i + 1] - xcoord[i + 1] * ycoord[i]);
			y += (ycoord[i] + ycoord[i + 1])
					* (xcoord[i] * ycoord[i + 1] - xcoord[i + 1] * ycoord[i]);
		}

		x *= iarea;
		y *= iarea;

		return new Point2d(x, y);
	}

	// no idea how to inherit from java.awt.polygon (uses integers!) or other
	// classes instead of reusing the code
	/**
	 * Is a point inside the polygon?
	 * 
	 * Code taken from java.awt.polygon (openjava).
	 * 
	 * @param x
	 *            x coordinate of the point
	 * @param y
	 *            y coordinate of the point
	 * @return (x,y) inside the polygon?
	 */
	public boolean contains(double x, double y) {
		int hits = 0;

		double lastx = xcoord[xcoord.length - 1];
		double lasty = ycoord[xcoord.length - 1];
		double curx, cury;

		// Walk the edges of the polygon
		for (int i = 0; i < xcoord.length; lastx = curx, lasty = cury, i++) {
			curx = xcoord[i];
			cury = ycoord[i];

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

}
