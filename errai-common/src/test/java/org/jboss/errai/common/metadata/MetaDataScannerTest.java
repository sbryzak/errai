/*
 * Copyright 2011 JBoss, a division of Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.common.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.reflections.vfs.Vfs;
import org.reflections.vfs.ZipDir;

/**
 * Tests for {@link MetaDataScanner}.
 *
 * @author Christian Sadilek <csadilek@redhat.com>
 */
public class MetaDataScannerTest {

  @Test
  public void testJarUrlDecodingWithSimpleName() throws Exception {
    testJarUrlDecoding("test");
  }

  @Test
  public void testJarUrlDecodingWithSpecialCharacters() throws Exception {
    testJarUrlDecoding("testjar-\u00e4\u00f6\u00e9\u00f8 +abc!@$%^&()_+}{");
  }

  private void testJarUrlDecoding(String jarFileName) throws IOException {
    File erraiAppPropertiesFile = new File(System.getProperty("java.io.tmpdir"), "ErraiApp.properties");
    erraiAppPropertiesFile.createNewFile();

    File jarFile = File.createTempFile(jarFileName, ".jar");
    JavaArchive jarArchive = ShrinkWrap.create(JavaArchive.class)
        .addClass(getClass())
        .addAsResource(erraiAppPropertiesFile);
    jarArchive.as(ZipExporter.class).exportTo(jarFile, true);

    erraiAppPropertiesFile.delete();

    URL testJarURL = jarFile.toURI().toURL();
    assertNotNull("Jar file not found: " + jarFile, testJarURL);

    ClassLoader loader = URLClassLoader.newInstance(new URL[] { testJarURL }, getClass().getClassLoader());
    List<URL> urls = MetaDataScanner.getConfigUrls(loader);
    assertFalse("No URLs returned", urls.isEmpty());
    String[] segments = urls.get(0).getPath().split("/");

    assertTrue("No path segments found in URL", segments.length > 0);
    assertEquals("URL not properly decoded", jarFile.getName() +"!", segments[segments.length - 1]);
    assertNotNull("Could not open jar", new ZipDir(urls.get(0)).getFiles());

    Set<String> zipContents = new TreeSet<String>();
    for (Vfs.File path : new ZipDir(urls.get(0)).getFiles()) {
      if (!path.getRelativePath().endsWith("/")) {
        zipContents.add(path.getRelativePath());
      }
    }

    Set<String> expectedContents = new TreeSet<String>();
    expectedContents.add("ErraiApp.properties");
    expectedContents.add("org/jboss/errai/common/metadata/MetaDataScannerTest.class");

    assertEquals("Wrong file contents", expectedContents, zipContents);

    // delete if test passes (otherwise, we may want to inspect its contents)
    jarFile.delete();
  }
}
