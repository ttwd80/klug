package pom;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.junit.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @version 1.0 - 2016-01-29
 * @version 1.1 - 2016-06-19
 * @author ttwangsa
 *
 */

public class MavenLatestVersionIT {
	public static class Dependency {
		private String groupId;
		private String artifactId;
		private String version;

		public String getGroupId() {
			return groupId;
		}

		public void setGroupId(final String groupId) {
			this.groupId = groupId;
		}

		public String getArtifactId() {
			return artifactId;
		}

		public void setArtifactId(final String artifactId) {
			this.artifactId = artifactId;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(final String version) {
			this.version = version;
		}
	}

	public static class Plugin extends Dependency {
	}

	@XmlRootElement
	public static class Dependencies {
		private List<Dependency> dependencies;

		@XmlElement(name = "dependency")
		public List<Dependency> getDependencies() {
			return dependencies;
		}

		public void setDependencies(final List<Dependency> dependencies) {
			this.dependencies = dependencies;
		}

	}

	@XmlRootElement
	public static class Plugins {
		private List<Plugin> plugins;

		@XmlElement(name = "plugin")
		public List<Plugin> getPlugins() {
			return plugins;
		}

		public void setPlugins(final List<Plugin> plugins) {
			this.plugins = plugins;
		}

	}

	@Test
	public void testDependencies() throws Exception {
		final Dependencies dependencies = createDependencies();
		final Map<String, String> map = createMap();
		merge(dependencies.getDependencies(), map);
		check(dependencies.getDependencies());
	}

	@Test
	public void testPlugin() throws Exception {
		final Plugins plugins = createPlugins();
		final Map<String, String> map = createMap();
		merge(plugins.getPlugins(), map);
		check(plugins.getPlugins());
	}

	public static class DependencyComparator implements Comparator<Dependency> {

		@Override
		public int compare(final Dependency o1, final Dependency o2) {
			if (!o1.getGroupId().equals(o2.getGroupId())) {
				return o1.getGroupId().compareTo(o2.getGroupId());
			} else {
				return o1.getArtifactId().compareTo(o2.getArtifactId());
			}
		}
	}

	private void check(final List<? extends Dependency> list) throws IOException {
		Collections.sort(list, new DependencyComparator());
		final List<String> messages = new ArrayList<String>();
		for (final Dependency dependency : list) {
			final String version = getLatestVersion(dependency);
			if (!version.equals(dependency.getVersion())) {
				messages.add("[" + dependency.getArtifactId() + ":" + dependency.getGroupId() + ":"
						+ dependency.getVersion() + "] - [" + version + "] - BAD");

			} else {
				System.out.println("[" + dependency.getArtifactId() + ":" + dependency.getGroupId() + ":"
						+ dependency.getVersion() + "] - OK");
			}
		}
		if (messages.size() > 0) {
			for (final String message : messages) {
				System.out.println(message);
			}
			fail();
		}

	}

	private String getLatestVersion(final Dependency dependency) throws IOException {
		// http://mvnrepository.com/artifact/org.jsoup/jsoup
		final String url = "http://mvnrepository.com/artifact/" + dependency.getGroupId() + "/"
				+ dependency.getArtifactId();
		final org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
		return doc.select("a.vbtn.release:not(.candidate)").first().text();
	}

	private void merge(final List<? extends Dependency> list, final Map<String, String> map) {
		for (final Dependency dependency : list) {
			final String version = dependency.getVersion();
			if (version.startsWith("$")) {
				final String resolved = map.get(version);
				if (resolved == null) {
					throw new RuntimeException("unknown:" + version);
				}
				dependency.setVersion(resolved);
			}
		}
	}

	private Dependencies createDependencies() throws Exception {
		final String segment = getSegment("<dependencies>", "</dependencies>");
		final Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(Dependency.class, Dependencies.class);
		marshaller.afterPropertiesSet();
		final Source source = new StreamSource(new ByteArrayInputStream(segment.getBytes()));
		return (Dependencies) marshaller.unmarshal(source);
	}

	private Map<String, String> createMap() throws IOException, ParserConfigurationException, SAXException {
		final String segment = getSegment("<properties>", "</properties>");
		return toMap(segment);
	}

	private String getContent() throws IOException {
		final File file = new File("pom.xml");
		final FileInputStream fis = new FileInputStream(file);
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IOUtils.copy(fis, baos);
		final String content = new String(baos.toByteArray());
		return content;
	}

	private String getSegment(final String beginMarker, final String endMarker) throws IOException {
		final String content = getContent();
		final int begin = content.indexOf(beginMarker);
		final int end = content.indexOf(endMarker) + endMarker.length();
		final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		final String segment = XML_HEADER + content.substring(begin, end);
		return segment;
	}

	private Plugins createPlugins() throws Exception {
		final String content = getContent();
		final String ELEMENT_BEGIN = "<plugin>";
		final String ELEMENT_END = "</plugin>";
		int begin = 0;
		String xmlString = "<plugins>";
		while (begin >= 0) {
			begin = content.indexOf(ELEMENT_BEGIN, begin);
			if (begin == -1) {
				break;
			}
			final int end = content.indexOf(ELEMENT_END, begin) + ELEMENT_END.length();
			xmlString += content.substring(begin, end);
			begin = end;
		}
		xmlString += "</plugins>";
		final Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(Plugins.class, Plugin.class);
		marshaller.afterPropertiesSet();
		final Source source = new StreamSource(new ByteArrayInputStream(xmlString.getBytes()));
		return (Plugins) marshaller.unmarshal(source);

	}

	private Map<String, String> toMap(final String segment)
			throws ParserConfigurationException, SAXException, IOException {

		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document doc = db.parse(new ByteArrayInputStream(segment.getBytes()));
		final Node nodeProperties = doc.getChildNodes().item(0);
		final NodeList childNodes = nodeProperties.getChildNodes();
		final Map<String, String> map = new LinkedHashMap<String, String>();
		for (int i = 0; i < childNodes.getLength(); i++) {
			final Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				final String key = "${" + node.getNodeName() + "}";
				final String value = node.getTextContent();
				map.put(key, value);
			}
		}
		return map;
	}
}
