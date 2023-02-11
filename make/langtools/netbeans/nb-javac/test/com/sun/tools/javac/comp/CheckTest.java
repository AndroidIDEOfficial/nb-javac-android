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
package openjdk.tools.javac.comp;

import openjdk.tools.javac.api.JavacTaskImpl;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import jdkx.tools.FileObject;
import jdkx.tools.ForwardingJavaFileManager;
import jdkx.tools.JavaCompiler;
import jdkx.tools.JavaFileManager.Location;
import jdkx.tools.JavaFileObject;
import jdkx.tools.JavaFileObject.Kind;
import jdkx.tools.SimpleJavaFileObject;
import jdkx.tools.StandardJavaFileManager;
import jdkx.tools.ToolProvider;
import junit.framework.TestCase;

/**
 *
 * @author Jan Lahoda
 */
public class CheckTest extends TestCase {
    
    public CheckTest(String testName) {
        super(testName);
    }            

    static class MyFileObject extends SimpleJavaFileObject {
        private String text;
        public MyFileObject(String text) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }

    public void testErroneousSuperClassDoesNotStopGenerate() throws Exception {
        String code = "public class Test extends ArrayList {\n" +
                      "}\n";
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;
        
        MemoryOutputJFM m = new MemoryOutputJFM(tool.getStandardFileManager(null, null, null));

        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, m, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov", "-XDshould-stop.at=GENERATE"), null, Arrays.asList(new MyFileObject(code)));
        
        ct.generate();
        
        assertEquals(new HashSet<String>(Arrays.asList("Test")), new HashSet<String>(m.writtenClasses));
    }
    
    private static class MemoryOutputJFM extends ForwardingJavaFileManager<StandardJavaFileManager> {

        private List<String> writtenClasses = new LinkedList<String>();
        
        public MemoryOutputJFM(StandardJavaFileManager m) {
            super(m);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, final String className, Kind kind, FileObject sibling) throws IOException {
            if (location.isOutputLocation() && kind == Kind.CLASS) {
                return new SimpleJavaFileObject(URI.create("myfo:/" + className), kind) {
                    @Override
                    public OutputStream openOutputStream() throws IOException {
                        writtenClasses.add(className);
                        return new ByteArrayOutputStream();
                    }
                };
            } else {
                return super.getJavaFileForOutput(location, className, kind, sibling);
            }
        }
        
    }

}
