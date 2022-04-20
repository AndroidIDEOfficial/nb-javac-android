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

package global;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.api.JavacTaskImpl;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import junit.framework.TestCase;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;

public class PackageInfoTest extends TestCase {

    public PackageInfoTest(String name) {
        super(name);
    }

    static class MyFileObject extends SimpleJavaFileObject {
        private String text;
        public MyFileObject(String text) {
            super(URI.create("myfo:/test/package-info.java"), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }

    public void testPositionForSuperConstructorCalls() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        JFM fm = new JFM(tool.getStandardFileManager(null, null, null));

        JavaFileObject jfo = fm.list(StandardLocation.CLASS_PATH, "test", EnumSet.of(Kind.SOURCE), false).iterator().next();

        assertNotNull(jfo);
        
        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null,fm, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(jfo));

        Iterable<? extends CompilationUnitTree> trees = ct.parse();
        Iterable<? extends Element> types = ct.enter(trees);
        boolean seenType = false;

        for (Element te : types) {
            if (te.getKind() == ElementKind.PACKAGE) {
                assertEquals("test", ((PackageElement)te).getQualifiedName().toString());
                assertFalse(seenType);
                seenType = true;
            }
        }

        assertTrue(seenType);
    }

    private static final class JFM extends ForwardingJavaFileManager<JavaFileManager> {

        public JFM(JavaFileManager delegate) {
            super(delegate);
        }

        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
            if (StandardLocation.CLASS_PATH == location && "test".equals(packageName) && kinds.contains(Kind.SOURCE)) {
                return Arrays.<JavaFileObject>asList(new MyFileObject("package test;\n"));
            }

            return super.list(location, packageName, kinds, recurse);
        }

        @Override
        public String inferBinaryName(Location location, JavaFileObject file) {
            if (file instanceof MyFileObject) {
                return "test.package-info";
            }
            
            return super.inferBinaryName(location, file);
        }
        
    }
    
}
