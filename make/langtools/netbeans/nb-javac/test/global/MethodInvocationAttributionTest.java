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
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.net.URI;
import java.util.Arrays;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import junit.framework.TestCase;

public class MethodInvocationAttributionTest extends TestCase {

    public MethodInvocationAttributionTest(String name) {
        super(name);
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

    public void testSimple1() throws Exception {
        final String code = "package test;\n" +
                      "import static java.nio.MappedByteBuffer.*;\n" +
                      "public class Test {\n" +
                      "    private void x() {\n" +
                      "         allocate(4);\n" +
                      "    }\n" +
                      "}\n";

        performTest(code);
    }

    public void testSimple2() throws Exception {
        final String code = "package test;\n" +
                      "import java.nio.MappedByteBuffer;\n" +
                      "public class Test {\n" +
                      "    private void x() {\n" +
                      "         MappedByteBuffer.allocate(4);\n" +
                      "    }\n" +
                      "}\n";
        performTest(code);
    }

    public void testSimple3() throws Exception {
        final String code = "package test;\n" +
                      "import javax.swing.SwingUtilities;\n" +
                      "public class Test {\n" +
                      "    private void x() {\n" +
                      "         int x = SwingUtilities.CENTER;\n" +
                      "    }\n" +
                      "}\n";
        performTest(code);
    }

    public void testSimple4() throws Exception {
        final String code = "package test;\n" +
                      "import static javax.swing.SwingUtilities.*;\n" +
                      "public class Test {\n" +
                      "    private void x() {\n" +
                      "         int x = CENTER;\n" +
                      "    }\n" +
                      "}\n";
        performTest(code);
    }

    private void performTest(String code) throws Exception {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        JavacTask ct = (JavacTask)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));

        Iterable<? extends CompilationUnitTree> trees = ct.parse();

        ct.analyze();

        final Trees t = Trees.instance(ct);

        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
                if (node.getArguments().size() == 0) {
                    return null;
                }

                Element e = t.getElement(getCurrentPath());

                assertEquals("allocate", e.getSimpleName().toString());
                assertEquals("java.nio.ByteBuffer", ((TypeElement) e.getEnclosingElement()).getQualifiedName().toString());
                return null;
            }
            @Override
            public Void visitIdentifier(IdentifierTree node, Void p) {
                if (getCurrentPath().getParentPath().getLeaf().getKind() != Kind.VARIABLE) {
                    return super.visitIdentifier(node, p);
                }

                Element e = t.getElement(getCurrentPath());

                assertEquals("CENTER", e.getSimpleName().toString());
                assertEquals("javax.swing.SwingConstants", ((TypeElement) e.getEnclosingElement()).getQualifiedName().toString());

                return null;
            }
        }.scan(trees.iterator().next(), null);
    }

}
