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
	
	private final CityGMLConverterConf conf;
	
	private final int start, end;
	
	private final int chunkIndex, nChunks;
		
	/**
	 * 
	 */
	public CityGMLVisibilityRunnable(CityGMLConverterData citydata, int start, int end, int chunkIndex, int nChunks, CityGMLConverterConf conf) {
		this.citydata = citydata;
		this.start = start;
		this.end = end;
		
		this.chunkIndex = chunkIndex;
		this.nChunks = nChunks;
		
		this.conf = conf;
	}

	@Override
	public void run() {
		
		// length of maximum number in message
		int outputLength;
		if (nChunks == 1) {
			// write number of building in case of only one chunk
			outputLength = (int) (Math.log10(citydata.buildings.size()) + 1);
		} else {
			// write of chunk
			outputLength = (int) (Math.log10(nChunks) + 1);
			System.out.println(" Started chunk  " + String.format("%" + outputLength + "d", chunkIndex + 1)
			+ "/" + nChunks);
		}
		
		for (int iBuildingSending = this.start; iBuildingSending < this.end; iBuildingSending++) {
			if (nChunks == 1) {
				System.out.println(" Building " + String.format("%" + outputLength + "d", iBuildingSending + 1)
						+ "/" + citydata.buildings.size());
			}
			SimpleBuilding buildingSending = citydata.buildings.get(iBuildingSending);

//			int iWallSendingStart;
//			if (conf.saveMemory) {
//				iWallSendingStart = buildingSending.walls.size();
//			} else {
//				iWallSendingStart = buildingSending.walls.size() - 1;
//			}
			for (int iWallSending = 0; iWallSending < buildingSending.walls.size(); iWallSending++) {
				Polygon3dWithVisibilities wallSending = buildingSending.walls.get(iWallSending);

				// check other buildings, skip current
				int iBuildReceivingStart;
				if (conf.saveMemory) {
					iBuildReceivingStart = 0;
				} else {
					iBuildReceivingStart = iBuildingSending + 1;
				}
					
				for (int iBuildingReceiving = iBuildReceivingStart ; iBuildingReceiving < citydata.buildings
						.size(); iBuildingReceiving++) {
					
					if (iBuildingSending == iBuildingReceiving)	continue;
					
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
								// skip check surface if it is sending or receiving
								if (wallChecking == wallSending || wallChecking == wallReceiving) continue;
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
						
						if (vis) {
							wallSending.visibilities.add(wallReceiving);
							if (!conf.saveMemory) {
								wallReceiving.visibilities.add(wallSending);
							}
						}

					}
				}
			}
			
			if (conf.saveMemory) {
				citydata.calcStreetPropertiesForBuilding(buildingSending);
				// remove stored visibilities
				for (Polygon3dWithVisibilities sending : buildingSending.walls) {
					sending.visibilities = null;
				}
			}
			
		}
		if (nChunks > 1) {
			System.out.println(
					" Finished chunk " + String.format("%" + outputLength + "d", chunkIndex + 1) + "/" + nChunks);
		}
	}

}
