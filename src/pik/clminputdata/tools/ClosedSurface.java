package pik.clminputdata.tools;

/**
 * An abstract closed surface with calculation of its area and a centroid.
 * 
 * @author Sebastian Schubert
 * 
 */
public abstract class ClosedSurface<T> {

	/**
	 * Centroid already calculated?
	 */
	private boolean isSetCentroid = false;
	/**
	 * Centroid of the polygon
	 */
	private T centroid;

	/**
	 * Area of the polygon
	 */
	private double area;
	/**
	 * Area already calculated?
	 */
	private boolean isSetArea = false;

	public double getArea() {
		if (!isSetArea) {
			area = calcArea();
		}
		return area;
	}

	protected abstract double calcArea();

	public T getCentroid() {
		if (!isSetCentroid) {
			centroid = calcCentroid();
		}
		return centroid;
	}

	protected abstract T calcCentroid();

	@Override
	public String toString() {
		return getCentroid().toString();
	}
}