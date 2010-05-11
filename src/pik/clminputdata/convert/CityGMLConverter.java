package pik.clminputdata.convert;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List; //import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement; //import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.proj4.Proj4;
import org.citygml4j.CityGMLContext;
import org.citygml4j.factory.CityGMLFactory;
import org.citygml4j.model.citygml.core.CityModel;

import pik.clminputdata.configuration.UrbanCLMConfiguration;
import pik.clminputdata.tools.GMLFilenameFilter;
import pik.clminputdata.tools.GroundOtherWallSVF;
import pik.clminputdata.tools.GroundOtherSkySVF;
import pik.clminputdata.tools.Integrator;
import pik.clminputdata.tools.WallOtherSkySVF;
import pik.clminputdata.tools.WallOtherWallSVF;
import ucar.ma2.InvalidRangeException;

/**
 * Main programme.
 * 
 * @author Sebastian Schubert
 * 
 */
public class CityGMLConverter {

	/**
	 * Read data of impervious surfaces.
	 * 
	 * @param conf
	 *            Configuration of this run (includes file information)
	 * @param uclm
	 *            Urban configuration to store the data to
	 * @throws IOException
	 *             File from conf not found
	 */
	private static void readImpSurfaceFile(CityGMLConverterConf conf,
			UrbanCLMConfiguration uclm) throws IOException {
		Scanner scanner = new Scanner(new File(conf.impSurfFile));
		for (int i = 0; i < conf.skipLines; i++) {
			if (scanner.hasNextLine()) {
				scanner.nextLine();
			} else {
				throw new IOException(
						"Impervious Surface has less lines then skipped at beginning.");
			}
		}
		while (scanner.hasNextLine()) {
			Scanner lScanner = new Scanner(scanner.nextLine())
					.useDelimiter(conf.sepString);
			List<Double> values = new LinkedList<Double>();
			while (lScanner.hasNextDouble()) {
				values.add(lScanner.nextDouble());
			}
			int lat = uclm.getRLatIndex(values.get(conf.rowLat - 1));
			int lon = uclm.getRLonIndex(values.get(conf.rowLon - 1));
			uclm.setUrbanFrac(lat, lon, values.get(conf.rowImpSurf - 1) / 100.);
			lScanner.close();
		}
		scanner.close();

	}

	/**
	 * Main routine of the converter.
	 * 
	 * @param args
	 *            Path to properties of the run
	 * @throws IOException
	 * @throws JAXBException
	 * @throws IllegalArgumentException
	 * @throws InvalidRangeException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, JAXBException,
			IllegalArgumentException, InvalidRangeException,
			InterruptedException {

		long startTime = new Date().getTime();

		CityGMLConverterConf conf;

		if (args.length == 1) {
			conf = new CityGMLConverterConf(args[0]);
		} else {
			conf = new CityGMLConverterConf();
		}

		conf.outputConf();

		// create new urban configuration
		UrbanCLMConfiguration uclm = new UrbanCLMConfiguration(conf.pollat,
				conf.pollon, conf.dlat, conf.dlon, conf.startlat_tot,
				conf.startlon_tot, conf.ie_tot, conf.je_tot, conf.nuclasses,
				conf.streetdir, conf.ke_urban, conf.height);

		Proj4 soldner = new Proj4(conf.proj4code, "+init=epsg:4326 +latlong");

		// set up citygml4j context
		CityGMLContext ctx = new CityGMLContext();
		CityGMLFactory citygml = ctx.createCityGMLFactory();

		// create a JAXBContext, an Unmarshaller and unmarshal input file
		JAXBContext jaxbCtx = ctx.createJAXBContext();

		File[] flist;
		File folder = new File(conf.inputGMLFolder);
		if (folder.isDirectory()) {
			flist = new File(conf.inputGMLFolder)
					.listFiles(new GMLFilenameFilter());
		} else {
			flist = new File[1];
			flist[0] = folder;
		}

		CityGMLConverterStats stats = new CityGMLConverterStats(flist, conf);

		readImpSurfaceFile(conf, uclm);

		ThreadPoolExecutor exec = new ThreadPoolExecutor(conf.nThreads,
				conf.nThreads, Long.MAX_VALUE, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(conf.nThreadsQueue),
				new ThreadPoolExecutor.CallerRunsPolicy());

		// here loop over citygmlfiles
		for (int i = 0; i < flist.length; i++) {

			File file = flist[i];

			System.out.println("Processing file " + (i + 1) + "/"
					+ flist.length + ": " + file);

			// unmarshaller has to be in loop, otherwise memory is not freed
			// (reference in unmarshaller?)

			Unmarshaller um = jaxbCtx.createUnmarshaller();
			JAXBElement<?> featureElem = (JAXBElement<?>) um.unmarshal(file);

			// map the JAXBElement class to the citygml4j object model
			CityModel cityModel = (CityModel) citygml.jaxb2cityGML(featureElem);

			CityGMLConverterThread cgmlct = new CityGMLConverterThread(uclm,
					conf, soldner, stats, cityModel, i, file.toString());

			// everything that is need is now in cgmlct, rest can be deleted
			cityModel = null;
			featureElem = null;
			um = null;

			if (conf.nThreads > 1) {
				exec.execute(cgmlct);
			} else {
				cgmlct.run();
			}
		}

		exec.shutdown();
		exec.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

		System.out.println("Largest Building: " + uclm.maxHeight + " m");
		System.out.println("Smallest Building: " + uclm.minHeight + " m");

		uclm.fakeUrbanClassFrac();
		uclm.normBuildingFrac();
		uclm.normBuildProbAndCalcStreetFraction();
		uclm.normStreetWidth();
		uclm.calculateBuildingWidth();
		if (conf.consistentOutput) {
			uclm.defineMissingData();
		}
		if (conf.doHeightReduction) {
			uclm.reduceHeight(conf.heightReductionP);
		}

		long lasted = new Date().getTime() - startTime;
		System.out.printf("Urban parameter calculation took %.3f minutes.%n",
				lasted / 1000. / 60.);

		if (conf.calcSVF) {

			System.out.println("Starting calculation of Skyview factors.");
			startTime = new Date().getTime();

			uclm.initalizeSVFFields();
			
			Integrator itg = new Integrator();

			exec = new ThreadPoolExecutor(conf.nThreads, conf.nThreads,
					Long.MAX_VALUE, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>(conf.nThreadsQueue),
					new ThreadPoolExecutor.CallerRunsPolicy());

			for (int iurb = 0; iurb < uclm.getNuclasses(); iurb++) {
				for (int id = 0; id < uclm.getNstreedir(); id++) {
					for (int j = 0; j < uclm.getJe_tot(); j++) {
						for (int i = 0; i < uclm.getIe_tot(); i++) {
							if (uclm.getUrbanFrac(j, i) > 1.e-12) {
								GroundOtherWallSVF gow = new GroundOtherWallSVF(
										iurb, id, j, i, uclm, itg);
								GroundOtherSkySVF gs = new GroundOtherSkySVF(iurb, id, j,
										i, uclm, itg);
								WallOtherWallSVF wws = new WallOtherWallSVF(iurb, id, j,
										i, uclm, itg);
								WallOtherSkySVF wss = new WallOtherSkySVF(iurb, id, j,
										i, uclm, itg);
								System.out
										.println("SVF Calculation for iurb = "
												+ iurb + ", id = " + id
												+ ", j = " + j + ", i = " + i);
								if (conf.nThreads > 1) {
									exec.execute(gow);
									exec.execute(gs);
									exec.execute(wws);
									exec.execute(wss);
								} else {
									gow.run();
									gs.run();
									wws.run();
									wss.run();
								}
							}
						}
					}
				}
			}

			exec.shutdown();
			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			uclm.defineMissingDataSVF();

			lasted = new Date().getTime() - startTime;
			System.out.printf(
					"Urban skyview factor calculation took %.3f minutes.%n",
					lasted / 1000. / 60.);
		}

		uclm.toNetCDFfile(conf.outputFile);

		stats.writeLogs();
		stats.toNetCDFfile(conf.statsFile);

	}
}
