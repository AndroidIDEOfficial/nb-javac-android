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

import openjdk.source.tree.ClassTree;
import openjdk.source.tree.CompilationUnitTree;
import openjdk.source.tree.MethodTree;
import openjdk.source.tree.VariableTree;
import openjdk.source.util.TreePathScanner;
import openjdk.source.util.Trees;
import openjdk.tools.javac.api.JavacTaskImpl;
import global.AnnotationProcessingTest;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import jdkx.tools.DiagnosticCollector;
import jdkx.tools.JavaCompiler;
import jdkx.tools.JavaFileObject;
import jdkx.tools.SimpleJavaFileObject;
import jdkx.tools.ToolProvider;
import junit.framework.TestCase;

/**
 *
 * @author lahvac
 */
public class MemberEnterTest extends TestCase {

    public MemberEnterTest(String testName) {
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

    public void testCouplingAPInteraction() throws Exception {
        String code = "package test;\n" +
                      "@Deprecated\n" +
                      "public class Test {\n" +
                      "    @Deprecated public void test() {}\n" +
                      "    @Deprecated public int testF;\n" +
                      "}\n";
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        URL myself = AnnotationProcessingTest.class.getProtectionDomain().getCodeSource().getLocation();
        List<String> options = new LinkedList<String>();
        options.addAll(Arrays.asList("-bootclasspath",  bootPath, "-source", "1.8", "-classpath", myself.toExternalForm()));
        options.addAll(Arrays.asList("-processor", "global.ap1.ErrorProducingAP"));

        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, null, options, null, Arrays.asList(new MyFileObject(code)));
        Iterable<? extends CompilationUnitTree> cuts = ct.parse();

        ct.analyze();

        final int[] found = new int[1];

        new TreePathScanner<Void, Void>() {
            @Override public Void visitMethod(MethodTree node, Void p) {
                if (node.getName().contentEquals("test")) {
                    assertDeprecated();
                    found[0]++;
                }
                return super.visitMethod(node, p);
            }
            @Override public Void visitVariable(VariableTree node, Void p) {
                assertDeprecated();
                found[0]++;
                return super.visitVariable(node, p);
            }
            @Override public Void visitClass(ClassTree node, Void p) {
                assertDeprecated();
                found[0]++;
                return super.visitClass(node, p);
            }
            private void assertDeprecated() {
                assertTrue(ct.getElements().isDeprecated(Trees.instance(ct).
                        getElement(getCurrentPath())));
            }
        }.scan(cuts, null);

        assertEquals(3, found[0]);
    }

    public void testVeryBrokenLambdaNoException() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; public class Test { private void t() { Iterable<Integer> map = null; Integer reduce = map.reduce(0, (o, t) -); } }";

        DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, dc, Arrays.asList("-bootclasspath",  bootPath, "-Xjcov", "-XDshouldStopPolicy=FLOW"), null, Arrays.asList(new openjdk.tools.javac.comp.AttrTest.MyFileObject(code)));
        ct.analyze();
    }
}
