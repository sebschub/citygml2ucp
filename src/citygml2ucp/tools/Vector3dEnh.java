package citygml2ucp.tools;

import javax.vecmath.Tuple3d;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

public class Vector3dEnh extends Vector3d {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// one custom constructor
	public Vector3dEnh(Tuple3d t1, Tuple3d t2) {
		x = t2.x - t1.x;
		y = t2.y - t1.y;
		z = t2.z - t1.z;
	}

	// standard constructors
	public Vector3dEnh() {
	}

	public Vector3dEnh(double[] v) {
		super(v);
	}

	public Vector3dEnh(Vector3d v1) {
		super(v1);
	}

	public Vector3dEnh(Vector3f v1) {
		super(v1);
	}

	public Vector3dEnh(Tuple3f t1) {
		super(t1);
	}

	public Vector3dEnh(Tuple3d t1) {
		super(t1);
	}

	public Vector3dEnh(double x, double y, double z) {
		super(x, y, z);
	}

	
	public final double dot(Tuple3d v1) {
		return (this.x * v1.x + this.y * v1.y + this.z * v1.z);
	}
	
}
