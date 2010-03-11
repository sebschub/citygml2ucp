package pik.clminputdata.convert;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List; //import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement; //import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.proj4.Proj4;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.citygml4j.CityGMLContext;
import org.citygml4j.factory.CityGMLFactory;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.building.Building;
import org.citygml4j.model.citygml.core.CityGMLBase;
import org.citygml4j.model.citygml.core.CityModel;
import org.citygml4j.model.citygml.core.CityObject;
import org.citygml4j.model.citygml.core.CityObjectMember; //import org.citygml4j.model.citygml.core.CoreModule;
import org.citygml4j.model.gml.Length;
import org.citygml4j.model.gml.MultiSurface;
import org.citygml4j.model.gml.MultiSurfaceProperty;

import pik.clminputdata.configuration.UrbanCLMConfiguration;
import pik.clminputdata.tools.GMLFilenameFilter;
import ucar.ma2.InvalidRangeException;

public class CityGMLConverter {

	// public final static int nThreads = 12;
	// public final static int nThreadsQueue = 30;
	static List<CityGMLConverterThread> lThreads = new LinkedList<CityGMLConverterThread>();

	/**
	 * @param args
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws JAXBException
	 * @throws InvalidRangeException
	 * @throws IllegalArgumentException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, SAXException,
			ParserConfigurationException, JAXBException,
			IllegalArgumentException, InvalidRangeException,
			InterruptedException {

		long startTime = new Date().getTime();

		CityGMLConverterConf conf;
		// if (args.length>=1) {
		// conf = new CityGMLConverterConf(args[0]);
		// } else {
		// conf = new CityGMLConverterConf();
		// }
		// conf.writeConf();

		if (args.length == 1) {
			conf = new CityGMLConverterConf(args[0]);
		} else {
			conf = new CityGMLConverterConf();
		}

		conf.outputConf();

		// create new urban configuration
		UrbanCLMConfiguration uclm = new UrbanCLMConfiguration(conf.pollat,
				conf.pollon, conf.dlat, conf.dlon, conf.startlat_tot,
				conf.startlon_tot, conf.ie_tot, conf.je_tot, conf.ke_tot,
				conf.nuclasses, conf.streetdir, conf.ke_urban, conf.height);

		// uclm.toNetCDFfile("/home/schubert/temp/test2.nc");

		// // new converter of soldner <=> lat lon
		// Soldner bsold = new Soldner();

		Proj4 soldner = new Proj4(conf.proj4code, "+init=epsg:4326 +latlong");

		// set up citygml4j context
		CityGMLContext ctx = new CityGMLContext();
		CityGMLFactory citygml = ctx.createCityGMLFactory();

		// // create a new XML parser
		// SAXParserFactory factory = SAXParserFactory.newInstance();
		// factory.setNamespaceAware(true);
		// XMLReader reader = factory.newSAXParser().getXMLReader();

		// create a JAXBContext, an Unmarshaller and unmarshal input file
		JAXBContext jaxbCtx = ctx.createJAXBContext();

		// Following is done in splitter

		// Unmarshaller um = jaxbCtx.createUnmarshaller();
		// JAXBElement<?> cityModelElem = (JAXBElement<?>) um.unmarshal(new
		// File(
		// "../datasets/SimpleLOD1Buildings.gml"));

		File[] flist;
		File folder = new File(conf.inputGMLFolder);
		if (folder.isDirectory()) {
			flist = new File(conf.inputGMLFolder)
					.listFiles(new GMLFilenameFilter());
		} else {
			flist = new File[1];
			flist[0] = folder;
		}

		CityGMLConverterStats stats = new CityGMLConverterStats(flist.length,
				flist, new File(conf.logNonPlanar), new File(
						conf.logNoSurfButBuildFrac));

		// ExecutorService exec = Executors.newFixedThreadPool(nThreads);
		ThreadPoolExecutor exec = new ThreadPoolExecutor(conf.nThreads,
				conf.nThreads, Long.MAX_VALUE, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(conf.nThreadsQueue),
				new ThreadPoolExecutor.CallerRunsPolicy());

		// final Runtime rt = Runtime.getRuntime();

		// here loop over citygmlfiles
		for (int i = 0; i < flist.length; i++) {

			File file = flist[i];

			System.out.println(file);

			// File file = new File("../datasets/2000020000.gml");
			// File file = new File("../datasets/gml/2600032000.gml");

			// unmarshaller has to be in loop, otherwise memory is not freed
			// (reference in unmarshaller?)
			Unmarshaller um = jaxbCtx.createUnmarshaller();
			// read the CityGML dataset and unmarshal it into a JAXBElement
			// instance
			// JAXBElement<?> featureElem = (JAXBElement<?>)
			// um.unmarshal(new
			// File(
			// "../datasets/2000020000.gml"));

			JAXBElement<?> featureElem = (JAXBElement<?>) um.unmarshal(file);

			// map the JAXBElement class to the citygml4j object model
			CityModel cityModel = (CityModel) citygml.jaxb2cityGML(featureElem);

			CityGMLConverterThread cgmlct = new CityGMLConverterThread(uclm,
					soldner, stats, cityModel, i, file.toString());

			if (conf.nThreads > 1) {
				exec.execute(cgmlct);
				// lThreads.add(cgmlct);
				// System.out.println(lThreads.size());
				// if (lThreads.size() == nThreadsQueue) {
				// for (int i = 0; i < nThreads; i++) {
				// lThreads.get(i).join();
				// }
				// lThreads.subList(0, nThreads).clear();
				// System.out.println("removed, so: " + lThreads.size());
				// }
			} else {
				cgmlct.run();
			}
		}
		// wait to finish for all threads
		// if (multithreaded) {
		// for (int i = 0; i < lThreads.size(); i++) {
		// lThreads.get(i).join();
		// }
		// }

		exec.shutdown();
		exec.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

		System.out.println(uclm.maxHeight);
		System.out.println(uclm.minHeight);

		uclm.normBuildProb();
		uclm.normStreetWidth();

		uclm.toNetCDFfile(conf.outputFile);

		stats.writeLogs();
		stats.toNetCDFfile(conf.statsFile);

		long lasted = new Date().getTime() - startTime;

		System.out.println("Program took " + lasted / 1000. + " seconds.");

	}
}
