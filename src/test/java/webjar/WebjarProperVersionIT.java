package webjar;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @version 1.0 - 2016-06-19
 * @author ttwangsa
 *
 */

public class WebjarProperVersionIT {

	@Test
	public void testWebjarProperVersion() throws IOException {
		final String filename = "webjar.properties";
		final String prefix = "/webjars/";
		final WebjarProperVersionTester tester = new WebjarProperVersionTester(filename, prefix);
		final List<String> list = tester.listEntries();
		if (list.size() == 0) {
			fail("nothing in " + filename);
		}
		final List<String> listInvalid = new ArrayList<String>();
		for (final String value : list) {
			if (tester.isValid(value)) {
				System.out.println(value + " OK");
			} else {
				listInvalid.add(value);
			}
		}
		if (listInvalid.size() > 0) {
			fail(StringUtils.join(listInvalid, ", "));
		}
	}

	private static class WebjarProperVersionTester {
		private final String filename;
		private final String prefix;

		public WebjarProperVersionTester(final String filename, final String prefix) {
			super();
			this.filename = filename;
			this.prefix = prefix;
		}

		public boolean isValid(final String value) {
			if (value.startsWith(prefix)) {
				try {
					final String location = "/META-INF/resources" + value;
					final long length = new ClassPathResource(location).contentLength();
					if (length < 0) {
						return false;
					} else {
						return true;
					}
				} catch (final IOException e) {
					return false;
				}
			} else {
				return false;
			}
		}

		public List<String> listEntries() throws IOException {
			final List<String> list = new ArrayList<>();
			final Properties properties = new Properties();
			final Resource resource = new ClassPathResource(filename);
			final InputStream inputStream = resource.getInputStream();
			properties.load(inputStream);
			final Enumeration<?> enumeration = properties.propertyNames();
			while (enumeration.hasMoreElements()) {
				final Object next = enumeration.nextElement();
				if (next != null) {
					final String value = properties.getProperty(next.toString());
					if (StringUtils.trimToNull(value) != null) {
						list.add(value);
					}
				}
			}
			IOUtils.closeQuietly(inputStream);
			Collections.sort(list);
			return list;
		}
	}
}
