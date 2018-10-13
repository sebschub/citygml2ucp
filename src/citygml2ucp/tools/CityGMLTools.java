package citygml2ucp.tools;

import java.util.List;

import org.citygml4j.model.gml.geometry.primitives.LinearRing;
import org.citygml4j.model.gml.geometry.primitives.OrientableSurface;
import org.citygml4j.model.gml.geometry.primitives.Polygon;
import org.citygml4j.model.gml.geometry.primitives.SurfaceProperty;

public abstract class CityGMLTools {

	
	public static List<Double> coordinatesFromSurfaceProperty(SurfaceProperty surfaceProperty) {
		if (surfaceProperty.getSurface() instanceof OrientableSurface) {
			OrientableSurface orientableSurface = (OrientableSurface) surfaceProperty.getSurface();
			return coordinatesFromSurfaceProperty(orientableSurface.getBaseSurface());
		} else if (surfaceProperty.getSurface() instanceof Polygon) {
			Polygon polygon = (Polygon) surfaceProperty.getSurface();
			if (polygon.getExterior().getRing() instanceof LinearRing) {
				LinearRing lRing = (LinearRing) polygon.getExterior().getRing();
				if (lRing.isSetPosList()) {
					return lRing.getPosList().getValue(); 
				}
				throw new IllegalArgumentException(
						"Linear ring is no PosList, handle this case!");
			}
			throw new IllegalArgumentException(
					"Polygon is no linear ring, handle this case!");
		}
		throw new IllegalArgumentException(
				"SurfaceProperty is neither OrientableSurface nor Polygon, handle this case!");
	}

	
}
