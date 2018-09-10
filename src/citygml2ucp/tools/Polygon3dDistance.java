package citygml2ucp.tools;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import static java.lang.Math.abs;

/**
 * Class for the distance between two polygons, which can be compared and,
 * therefore, sorted.
 * 
 * @author Sebastian Schubert
 * 
 */
public class Polygon3dDistance implements Comparable<Polygon3dDistance> {

	/**
	 * The sending surface
	 */
	public final Polygon3d sending;
	/**
	 * The receiving surface
	 */
	public final Polygon3d receiving;

	/**
	 * The distance between sending and receiving surface defined by the
	 * distance between their centroids.
	 */
	public final double distance;

	private double cosAngle;
	private boolean isSetCosAngle = false;

	/**
	 * Constructor.
	 * 
	 * The constructor calculates the distance between the centroids of the
	 * sending and receiving polygon. If {@code eff=true}, the distance is
	 * multiplied by the absolute value of the cosine of the angle between the
	 * sending's normal and the connection between the centroids. This is the
	 * distance it would be if the centroid was on the direction of the normal
	 * of the sending surface.
	 * 
	 * @param sending
	 *            Sending surface
	 * @param receiving
	 *            Receiving surface
	 * @param eff
	 *            use effective distance?
	 */
	public Polygon3dDistance(Polygon3d sending, Polygon3d receiving, boolean eff) {
		this.sending = sending;
		this.receiving = receiving;

		Point3d p1 = sending.getCentroid();
		Point3d p2 = receiving.getCentroid();

		if (eff) {
			distance = p1.distance(p2) * abs(getCosAngle());
		} else {
			distance = p1.distance(p2);
		}
	}

	@Override
	public int compareTo(Polygon3dDistance arg0) {
		if (arg0 == null) {
			throw new NullPointerException();
		}

		if (arg0.distance > this.distance)
			return -1;
		if (arg0.distance < this.distance)
			return 1;

		return 0;
	}

	public double getCosAngle() {
		if (isSetCosAngle) {
			return cosAngle;
		}

		Vector3d connect = new Vector3d();

		connect.sub(receiving.getCentroid(), sending.getCentroid());
		double l1 = connect.length();
		double l2 = sending.uvn.length();

		if (l1 < 1.e-12 || l2 < 1.e-12) {
			cosAngle = 0.;
		} else {
			cosAngle = connect.dot(sending.uvn) / l1 / l2;
		}
		isSetCosAngle = true;
		return cosAngle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Polygon3dDistance)) {
			return false;
		}

		if (((Polygon3dDistance) obj).distance == this.distance) {
			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return (int) distance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return sending.toString() + "  " + receiving.toString() + "  "
				+ Double.toString(distance);
	}
}
