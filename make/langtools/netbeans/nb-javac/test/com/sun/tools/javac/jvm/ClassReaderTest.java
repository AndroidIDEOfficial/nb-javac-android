/*
 * Copyright 2003-2004 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package openjdk.tools.javac.jvm;

import openjdk.source.util.JavacTask;
import openjdk.tools.javac.code.Symbol.ClassSymbol;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import jdkx.lang.model.element.TypeElement;
import jdkx.lang.model.util.ElementFilter;
import jdkx.tools.FileObject;
import jdkx.tools.ForwardingJavaFileManager;
import jdkx.tools.JavaCompiler;
import jdkx.tools.JavaFileManager;
import jdkx.tools.JavaFileObject;
import jdkx.tools.JavaFileObject.Kind;
import jdkx.tools.SimpleJavaFileObject;
import jdkx.tools.StandardLocation;
import jdkx.tools.ToolProvider;
import junit.framework.TestCase;

/**
 *
 * @author Jan Lahoda
 */
public class ClassReaderTest extends TestCase {

    public ClassReaderTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

//    public void testX() throws Exception {
//        byte[] array = "01234567890123456789012345678901234567890123456789".getBytes();
//        InputStream ins = new TestInputStream(new ByteArrayInputStream(array));
//        byte[] read = ClassReader.readInputStream(new byte[30], ins);
//        byte[] readCanonical = new byte[array.length];
//
//        System.arraycopy(read, 0, readCanonical, 0, array.length);
//        assertTrue(Arrays.toString(read) + "vs." + Arrays.toString(array), Arrays.equals(array, readCanonical));
//    }

    private static final class TestInputStream extends InputStream {

        private InputStream delegateTo;

        public TestInputStream(InputStream delegateTo) {
            this.delegateTo = delegateTo;
        }

        public int read() throws IOException {
            //not used by ClassReader.readInputStream:
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return delegateTo.read(b, off, Math.min(10, len));
        }

        @Override
        public int available() throws IOException {
            return 30;
        }

    }

    public void testOrderOnClassPathIsSignificant() throws Exception {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        JFM fileManager = new JFM(tool.getStandardFileManager(null, null, null), ClassJFO.create("Test1", "Test", 1000), ClassJFO.create("Test2", "Test", 2000));
        JavacTask ct = (JavacTask)tool.getTask(null, fileManager, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.<JavaFileObject>asList());
        ct.analyze();

        TypeElement pack = ct.getElements().getTypeElement("Test");

        URI source = ((ClassSymbol) pack).classfile.toUri();

        assertTrue(source.toASCIIString(), source.getPath().endsWith("Test1.class"));
        assertEquals(1, pack.getEnclosedElements().size());
    }

    public void testV48ClassFileWithGenericInfo() throws Exception {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        JFM fileManager = new JFM(tool.getStandardFileManager(null, null, null), ClassJFO.create("V48gen", "V48gen", 1000));
        JavacTask ct = (JavacTask)tool.getTask(null, fileManager, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-XDide"), null, Arrays.<JavaFileObject>asList());
        ct.analyze();

        TypeElement v48gen = ct.getElements().getTypeElement("V48gen");

        assertNotNull(v48gen);
        assertEquals(1, v48gen.getTypeParameters().size());
    }
    
    public void testMethodParamAnnotations() throws Exception {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        JFM fileManager = new JFM(tool.getStandardFileManager(null, null, null));
        JavacTask ct = (JavacTask)tool.getTask(null, fileManager, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-XDide"), null, Arrays.<JavaFileObject>asList(new SourceFileObject("public class Test { public static void t(@Deprecated int p) { } }")));

        ct.generate();
        
        JFM readingFileManager = new JFM(tool.getStandardFileManager(null, null, null), new ClassJFO(new URI("mem://Test.class"), "Test", 0, fileManager.writtenClasses.get("Test")));
        JavacTask readCT = (JavacTask)tool.getTask(null, readingFileManager, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-XDide"), null, null);
        readCT.analyze();
        
        TypeElement test = readCT.getElements().getTypeElement("Test");
        
        assertNotNull(ElementFilter.methodsIn(test.getEnclosedElements()).get(0).getParameters().get(0).getAnnotation(Deprecated.class));
    }

    private static final class JFM extends ForwardingJavaFileManager<JavaFileManager> {

        private final Iterable<JavaFileObject> classes;
        public JFM(JavaFileManager delegate, JavaFileObject... classes) {
            super(delegate);
            this.classes = Arrays.asList(classes);
        }

        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
            if (StandardLocation.CLASS_PATH == location && "".equals(packageName) && kinds.contains(Kind.CLASS)) {
                return classes;
            }

            Iterable<JavaFileObject> list = super.list(location, packageName, kinds, recurse);

            return list;
        }

        @Override
        public String inferBinaryName(Location location, JavaFileObject file) {
            if (file instanceof ClassJFO) {
                return ((ClassJFO) file).binaryName;
            }

            return super.inferBinaryName(location, file);
        }

        private final Map<String, byte[]> writtenClasses = new HashMap<String, byte[]>();
        
        @Override
        public JavaFileObject getJavaFileForOutput(Location location, final String className, Kind kind, FileObject sibling) throws IOException {
            if (location.isOutputLocation() && kind == Kind.CLASS) {
                return new SimpleJavaFileObject(URI.create("myfo:/" + className), kind) {
                    @Override
                    public OutputStream openOutputStream() throws IOException {
                        return new ByteArrayOutputStream() {
                            @Override public void close() throws IOException {
                                super.close();
                                writtenClasses.put(className, toByteArray());
                            }
                        };
                    }
                };
            } else {
                return super.getJavaFileForOutput(location, className, kind, sibling);
            }
        }
    }

    private static final class ClassJFO extends SimpleJavaFileObject {

        private final String binaryName;
        private final long lastModified;
        private final byte[] data;

        public ClassJFO(URI uri, String binaryName, long lastModified) {
            this(uri, binaryName, lastModified, null);
        }
        
        public ClassJFO(URI uri, String binaryName, long lastModified, byte[] data) {
            super(uri, Kind.CLASS);
            this.binaryName = binaryName;
            this.lastModified = lastModified;
            this.data = data;
        }

        public static final ClassJFO create(String name, String binName, long lastModified) throws URISyntaxException {
            return new ClassJFO(ClassReaderTest.class.getResource(name + ".class").toURI(), binName, lastModified);
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return data != null ? new ByteArrayInputStream(data) : uri.toURL().openStream();
        }

        @Override
        public long getLastModified() {
            return lastModified;
        }

    }

    private static class SourceFileObject extends SimpleJavaFileObject {
        private String text;
        public SourceFileObject(String text) {
            this("Test", text);
        }
        public SourceFileObject(String name, String text) {
            super(URI.create("myfo:/" + name + ".java"), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }
}
