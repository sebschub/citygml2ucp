/**
 * 
 */
package citygml2ucp.convert;

import citygml2ucp.tools.Polygon3d;
import citygml2ucp.tools.Polygon3dWithVisibilities;
import citygml2ucp.tools.SimpleBuilding;

/**
 * @author sebschub
 *
 */
public class CityGMLVisibilityRunnable implements Runnable {

	private final CityGMLConverterData citydata;
	
	private final int start, end;
	
	private final int chunkIndex, numberChunks;
	
	/**
	 * 
	 */
	public CityGMLVisibilityRunnable(CityGMLConverterData citydata, int start, int end, int chunkIndex, int numberChunks) {
		this.citydata = citydata;
		this.start = start;
		this.end = end;
		
		this.chunkIndex = chunkIndex;
		this.numberChunks = numberChunks;
	}

	@Override
	public void run() {
		
		int numberChunksLength = (int) (Math.log10(numberChunks) + 1);
		System.out.println(" Started chunk  " + String.format("%" + numberChunksLength + "d", chunkIndex + 1)
		+ "/" + numberChunks);
		for (int iBuildingSending = this.start; iBuildingSending < this.end; iBuildingSending++) {
			SimpleBuilding buildingSending = citydata.buildings.get(iBuildingSending);

			for (int iWallSending = 0; iWallSending < buildingSending.walls.size() - 1; iWallSending++) {
				Polygon3dWithVisibilities wallSending = buildingSending.walls.get(iWallSending);

				// check other buildings, skip current
				for (int iBuildingReceiving = iBuildingSending + 1; iBuildingReceiving < citydata.buildings
						.size(); iBuildingReceiving++) {
					SimpleBuilding buildingReceiving = citydata.buildings.get(iBuildingReceiving);

					// if buildings are too far away, skip:
					double distanceSendiungReceiving = buildingSending.location.distance(buildingReceiving.location);
					if (distanceSendiungReceiving > citydata.conf.maxbuild_radius) {
						continue;
					}

					// distance is ok, so check every other surface
					for (int iWallReceiving = 0; iWallReceiving < buildingReceiving.walls.size(); iWallReceiving++) {
						Polygon3dWithVisibilities wallReceiving = buildingReceiving.walls.get(iWallReceiving);

						//							if (iBuildingSending == iBuildingReceiving && iWallSending >= iWallReceiving)
						//								continue;

						boolean vis = true;

						// which to check
						for (int iBuildingChecking = 0; iBuildingChecking < citydata.buildings.size(); iBuildingChecking++) {
							SimpleBuilding buildingChecking = citydata.buildings.get(iBuildingChecking);

							// in principle, building to check should be on
							// the
							// connection between the starting and end
							// building,
							// so sum of distances - distance of buildings
							// approx. 0, because of buildings larger radius
							double distenceDifference = buildingChecking.location.distance(buildingSending.location)
									+ buildingChecking.location.distance(buildingReceiving.location)
									- distanceSendiungReceiving;
							if (distenceDifference > citydata.conf.maxcheck_radius) {
								continue;
							}

							// check wall surfaces
							for (Polygon3dWithVisibilities wallChecking : buildingChecking.walls) {
								if (wallChecking.isHitBy(wallSending.getCentroid(), wallReceiving.getCentroid())) {
									vis = false;
									break;
								}
							}
							if (!vis) {
								break;
							}

							// check roof surfaces
							for (Polygon3d roofChecking : buildingChecking.roofs) {
								if (roofChecking.isHitBy(wallSending.getCentroid(), wallReceiving.getCentroid())) {
									vis = false;
									break;
								}
							}
							if (!vis) {
								break;
							}
						}
						
						wallSending.visibilities.add(wallReceiving);
						wallReceiving.visibilities.add(wallSending);

					}
				}
			}
		}
		System.out.println(" Finished chunk " + String.format("%" + numberChunksLength + "d", chunkIndex + 1)
		+ "/" + numberChunks);

	}

}
