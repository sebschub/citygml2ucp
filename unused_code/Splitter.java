package pik.clminputdata.convert;

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshallerHandler;

import org.citygml4j.factory.CityGMLFactory;
import org.citygml4j.model.citygml.core.CityGMLBase;
import org.citygml4j.model.citygml.core.CityObject;
import org.citygml4j.model.citygml.core.CoreModule;
import org.proj4.Proj4;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.NamespaceSupport;
import org.xml.sax.helpers.XMLFilterImpl;

import pik.clminputdata.configuration.Soldner;
import pik.clminputdata.configuration.UrbanCLMConfiguration;

public class Splitter extends XMLFilterImpl {
	private final JAXBContext jaxbCtx;
	private final CityGMLFactory citygml;
	private final Proj4 proj4;
	private final UrbanCLMConfiguration uclm;
	private final ExecutorService exec;
	private final LinkedList<CityGMLConverterThread> lThreads;
	private final int nThreads;
	private final int nThreadsQueue;

	private int depth;
	private Locator locator;
	private UnmarshallerHandler unmarshallerHandler;
	private NamespaceSupport namespaces = new NamespaceSupport();

	public Splitter(JAXBContext jaxbCtx, CityGMLFactory citygml,
			UrbanCLMConfiguration uclm, Proj4 proj4, ExecutorService exec,
			int nThreads) {
		this.jaxbCtx = jaxbCtx;
		this.citygml = citygml;
		this.uclm = uclm;
		this.proj4 = proj4;
		this.exec = exec;
		lThreads = new LinkedList<CityGMLConverterThread>();
		this.nThreads = nThreads;
		nThreadsQueue = 10 * nThreads;
	}

	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {

		if (depth != 0) {
			// we are in the middle of forwarding events.
			// continue to do so.
			depth++;
			super.startElement(namespaceURI, localName, qName, atts);
			return;
		}

		if ((namespaceURI.equals(CoreModule.v0_4_0.getNamespaceUri()) || namespaceURI
				.equals(CoreModule.v1_0_0.getNamespaceUri()))
				&& localName.equals("cityObjectMember")) {

			// we want to start working on the feature embraced by
			// <cityObjectMember>.
			// so we first forward the event of the <cityObjectMember> element.
			super.startElement(namespaceURI, localName, qName, atts);

			// start a new unmarshaller
			Unmarshaller unmarshaller;

			try {
				unmarshaller = jaxbCtx.createUnmarshaller();
			} catch (JAXBException je) {
				// there's no way to recover from this error.
				// we will abort the processing.
				throw new SAXException(je);
			}

			unmarshallerHandler = unmarshaller.getUnmarshallerHandler();

			// set it as the content handler so that it will receive
			// SAX events from now on.
			setContentHandler(unmarshallerHandler);

			// fire SAX events to emulate the start of a new document.
			unmarshallerHandler.startDocument();
			unmarshallerHandler.setDocumentLocator(locator);

			Enumeration e = namespaces.getPrefixes();
			while (e.hasMoreElements()) {
				String prefix = (String) e.nextElement();
				String uri = namespaces.getURI(prefix);

				unmarshallerHandler.startPrefixMapping(prefix, uri);
			}

			String defaultURI = namespaces.getURI("");
			if (defaultURI != null)
				unmarshallerHandler.startPrefixMapping("", defaultURI);

			// count the depth of elements and we will know when to stop.
			depth = 1;
		}
	}

	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {

		if (depth != 0) {
			depth--;

			if (depth == 0) {
				// just finished sending one chunk.

				// emulate the end of a document.
				Enumeration e = namespaces.getPrefixes();
				while (e.hasMoreElements()) {
					String prefix = (String) e.nextElement();
					unmarshallerHandler.endPrefixMapping(prefix);
				}

				String defaultURI = namespaces.getURI("");
				if (defaultURI != null)
					unmarshallerHandler.endPrefixMapping("");
				unmarshallerHandler.endDocument();

				// stop forwarding events by setting a dummy handler.
				// XMLFilter doesn't accept null, so we have to give it
				// something,
				// hence a DefaultHandler, which does nothing.
				setContentHandler(new DefaultHandler());

				// then retrieve the fully unmarshalled object
				try {
					JAXBElement<?> result = (JAXBElement<?>) unmarshallerHandler
							.getResult();

					// process the element contained by the cityObjectMember
					// element
					CityGMLConverterThread cgmlct = new CityGMLConverterThread(
							uclm, proj4, citygml.jaxb2cityGML(result));
					
					exec.execute(cgmlct);
					lThreads.add(cgmlct);
					
					if (lThreads.size()==nThreadsQueue) {
						for (int i = 0; i < nThreads; i++) {
							lThreads.get(i).join();
						}
						lThreads.subList(0, nThreads).clear();
					}
//					cgmlct.run();
					
					
				} catch (JAXBException je) {
					// error was found during the unmarshalling.
					// you can either abort the processing by throwing a
					// SAXException,
					// or you can continue processing by returning from this
					// method.
					System.err
							.println("unable to process an <cityObjectMember> at line "
									+ locator.getLineNumber());
					return;
				} catch (InterruptedException ie) {
					ie.printStackTrace();
					System.err
					.println("problem with thread");
					return;
				}

				unmarshallerHandler = null;
			}
		}

		// forward this event
		super.endElement(namespaceURI, localName, qName);
	}

	// public void process(CityGMLBase base) {
	// if (base instanceof CityObject)
	// System.out.println("Found a " + ((CityObject)base).getCityGMLClass() +
	// " instance.");
	// else
	// System.out.println("Cannot interpret content of <cityObjectMember> element");
	// }

	public void setDocumentLocator(Locator locator) {
		super.setDocumentLocator(locator);
		this.locator = locator;
	}

	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		namespaces.pushContext();
		namespaces.declarePrefix(prefix, uri);

		super.startPrefixMapping(prefix, uri);
	}

	public void endPrefixMapping(String prefix) throws SAXException {
		namespaces.popContext();

		super.endPrefixMapping(prefix);
	}
}