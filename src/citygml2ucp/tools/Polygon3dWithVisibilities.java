/**
 * 
 */
package citygml2ucp.tools;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.citygml4j.model.gml.geometry.primitives.SurfaceProperty;

/**
 * @author Sebastian Schubert
 *
 */
public class Polygon3dWithVisibilities extends Polygon3d {

	
	public List<Polygon3dWithVisibilities> visibilities;
	
	/**
	 * @param surfaceProperty
	 */
	public Polygon3dWithVisibilities(String id, SurfaceProperty surfaceProperty) {
		super(id, surfaceProperty);
		// we want to add elements in parallel so to be sure, use synchronizedList
		this.visibilities = Collections.synchronizedList(new LinkedList<>());
	}
	
	public List<Polygon3dVisibility> generateVisibilityList(boolean eff) {
		List<Polygon3dVisibility> visibilityList = new LinkedList<>();
		for (Polygon3d receiving : visibilities) {
			visibilityList.add(new Polygon3dVisibility(this, receiving, eff));
		}
		return visibilityList;
	}
	
}
