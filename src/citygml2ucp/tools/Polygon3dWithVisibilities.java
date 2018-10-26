/**
 * 
 */
package citygml2ucp.tools;

import java.util.ArrayList;
import java.util.List;

import org.citygml4j.model.gml.geometry.primitives.SurfaceProperty;

/**
 * @author Sebastian Schubert
 *
 */
public class Polygon3dWithVisibilities extends Polygon3d {

	
	public List<Polygon3dVisibility> visibilities;
	
	/**
	 * @param surfaceProperty
	 */
	public Polygon3dWithVisibilities(SurfaceProperty surfaceProperty) {
		super(surfaceProperty);
		this.visibilities = new ArrayList<>();
	}
	
}
