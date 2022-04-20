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
package com.sun.tools.javac.comp;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import junit.framework.TestCase;

/**
 *
 * @author Jan Lahoda
 */
public class FlowTest extends TestCase {
    
    public FlowTest(String testName) {
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

    public void test152088() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; public class Test {public void test() {java.util.List<String> l = null;for (String s : !l.isEmpty()) {}}}";

        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));

        ct.analyze();
    }

    public void test152334() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; public class Test {private void test(final int x) {int x = 0; int y = x;}}";

        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));

        ct.analyze();
    }

    public void test153488a() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test;\n" +
                      "class Test{\n" +
                      "    private final int x;" +
                      "    Test() { x = 0; }" +
                      "    class Inner { int foo() { return x; } }" +
                      "}";

        DiagnosticCollector<JavaFileObject> c = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, c, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();
        
        ct.analyze();

        assertEquals(c.getDiagnostics().toString(), 0, c.getDiagnostics().size());

        ClassTree clazz = (ClassTree) ((ClassTree) cut.getTypeDecls().get(0)).getMembers().get(2);

        Context context = ct.getContext();
        Flow flow = Flow.instance(context);
        TreeMaker make = TreeMaker.instance(context);
        Log l = Log.instance(context);
        l.startPartialReparse();
        JavaFileObject prev = l.useSource(cut.getSourceFile());
        try {
            flow.reanalyzeMethod(make.forToplevel((JCCompilationUnit) cut), (JCClassDecl) clazz);
        } finally {
            l.useSource(prev);
            l.endPartialReparse();
        }        
        assertEquals(c.getDiagnostics().toString(), 0, c.getDiagnostics().size());
    }

    public void test153488b() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test;\n" +
                      "class Test{\n" +
                      "    Test() { }" +
                      "    class Inner { final int y; int foo() { final int x; return x; } }" +
                      "}";

        DiagnosticCollector<JavaFileObject> c = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, c, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();

        ct.analyze();

        assertEquals(c.getDiagnostics().toString(), 2, c.getDiagnostics().size());

        ClassTree clazz = (ClassTree) ((ClassTree) cut.getTypeDecls().get(0)).getMembers().get(1);

        Context context = ct.getContext();
        Flow flow = Flow.instance(context);
        TreeMaker make = TreeMaker.instance(context);
        Log l = Log.instance(context);
        l.startPartialReparse();
        JavaFileObject prev = l.useSource(cut.getSourceFile());
        try {
            flow.reanalyzeMethod(make.forToplevel((JCCompilationUnit) cut), (JCClassDecl) clazz);
        } finally {
            l.useSource(prev);
            l.endPartialReparse();
        }
        assertEquals(c.getDiagnostics().toString(), 4, c.getDiagnostics().size());
    }
    
    public void test194658() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test;\n" +
                      "class Test{\n" +
                      "    public void notify(String message, Throwable exception) {\n" +
                      "        (message != null) || (exception != null);\n" +
                      "        String str = foo.Bar;\n" +
                      "    }\n" +
                      "}";

        DiagnosticCollector<JavaFileObject> c = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, c, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();

        ct.analyze();
    }

    /*requires java.lang.AutoCloseable:
     */
    public void XtestErrorAndAutoCloseable() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; public class Test { { try (InputStream in = new FileInputStream(\"\")) {} }}";

        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));

        assertNotNull("Must run on JDK7 with ARM", ct.getElements().getTypeElement("java.lang.AutoCloseable"));
        ct.analyze();
    }

    public void testReturnInInitializer() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test;\n" +
                      "class Test{\n" +
                      "    {\n" +
                      "        return ;\n" +
                      "    }\n" +
                      "}";

        DiagnosticCollector<JavaFileObject> c = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, c, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov", "-XDshouldStopPolicy=FLOW"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();

        ct.analyze();
    }
    
    public void testBreakContinueUnresolvedTarget() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test;\n" +
                      "class Test{\n" +
                      "    private void test() {\n" +
                      "        while (true) {\n" +
                      "            break undef;\n" +
                      "        }\n" +
                      "    }\n" +
                      "}";

        DiagnosticCollector<JavaFileObject> c = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, c, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov", "-XDshouldStopPolicy=FLOW"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();

        ct.analyze();
    }
    
}
