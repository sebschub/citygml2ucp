package citygml2ucp.tools;

/**
 * An abstract closed surface with calculation of its area and a centroid.
 * 
 * @author Sebastian Schubert
 * 
 */
public abstract class ClosedSurface<T> {

	/**
	 * Centroid of the polygon
	 */
	protected T centroid;

	/**
	 * Area of the polygon
	 */
	protected double signedArea;

	public double getArea() {
		return Math.abs(this.signedArea);
	}

	public double getSignedArea() {
		return this.signedArea;
	}
	
	protected abstract double calcSignedArea();

	public T getCentroid() {
		return this.centroid;
	}

	protected abstract T calcCentroid();

	@Override
	public String toString() {
		return getCentroid().toString();
	}
}