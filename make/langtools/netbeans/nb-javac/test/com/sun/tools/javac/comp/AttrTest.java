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

import openjdk.source.tree.CompilationUnitTree;
import openjdk.source.tree.LambdaExpressionTree;
import openjdk.source.tree.LiteralTree;
import openjdk.source.tree.MethodTree;
import openjdk.source.tree.NewClassTree;
import openjdk.source.tree.Tree;
import openjdk.source.tree.VariableTree;
import openjdk.source.util.SourcePositions;
import openjdk.source.util.TreePath;
import openjdk.source.util.TreePathScanner;
import openjdk.source.util.TreeScanner;
import openjdk.source.util.Trees;
import openjdk.tools.javac.api.JavacScope;
import openjdk.tools.javac.api.JavacTaskImpl;
import openjdk.tools.javac.api.JavacTrees;
import openjdk.tools.javac.tree.JCTree;
import openjdk.tools.javac.tree.JCTree.JCLambda;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jdkx.lang.model.element.Element;
import jdkx.lang.model.element.ElementKind;
import jdkx.lang.model.type.TypeKind;
import jdkx.lang.model.type.TypeMirror;
import jdkx.tools.Diagnostic;
import jdkx.tools.DiagnosticCollector;
import jdkx.tools.FileObject;
import jdkx.tools.ForwardingJavaFileManager;
import jdkx.tools.JavaCompiler;
import jdkx.tools.JavaFileManager;
import jdkx.tools.JavaFileManager.Location;
import jdkx.tools.JavaFileObject;
import jdkx.tools.JavaFileObject.Kind;
import jdkx.tools.SimpleJavaFileObject;
import jdkx.tools.StandardJavaFileManager;
import jdkx.tools.ToolProvider;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import junit.framework.TestCase;
import org.junit.Ignore;

/**
 *
 * @author Jan Lahoda
 */
public class AttrTest extends TestCase {

    public AttrTest(String testName) {
        super(testName);
    }

    static class MyFileObject extends SimpleJavaFileObject {
        private String text;
        public MyFileObject(String text) {
            this("Test", text);
        }
        public MyFileObject(String name, String text) {
            super(URI.create("myfo:/" + name + ".java"), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }

    public void testExceptionParameterCorrectKind() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; public class Test { { try { } catch (NullPointerException ex) {} } }";

        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();

        ct.analyze();

        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitVariable(VariableTree node, Void p) {
                Element el = Trees.instance(ct).getElement(getCurrentPath());

                assertNotNull(el);
                assertEquals(ElementKind.EXCEPTION_PARAMETER, el.getKind());

                return super.visitVariable(node, p);
            }
        }.scan(cut, null);
    }

    public void testNPEFromNCTWithUnboundWildcard() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; public class Test { { new java.util.ArrayList<java.util.List<?>>() {}; } }";

        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        ct.analyze();
    }
    
    public void testErrorReturnType1() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String api = "package test; public class API { public static Undef call() { return null; } }";
        String use = "package test; public class Use { public void t() { Object str = API.call(); } }";
        DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, dc, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject("API", api), new MyFileObject("Use", use)));
        
        ct.analyze();
        
        Set<String> diagnostics = new HashSet<String>();
        
        for (Diagnostic<? extends JavaFileObject> d : dc.getDiagnostics()) {
            diagnostics.add(d.getSource().getName() + ":" + d.getStartPosition() + "-" + d.getEndPosition() + ":" + d.getCode());
        }
        
        assertEquals(new HashSet<String>(Arrays.asList("/API.java:47-52:compiler.err.cant.resolve.location", "/Use.java:64-72:compiler.err.type.error")), diagnostics);
    }
    
    public void testErrorReturnType2() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String api = "package test; public class API { public static String call() { return null; } }";
        String use = "package test; public class Use { public void t() { Object str = API.; } }";
        DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, dc, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject("API", api), new MyFileObject("Use", use)));
        
        ct.analyze();
        
        Set<String> diagnostics = new HashSet<String>();
        
        for (Diagnostic<? extends JavaFileObject> d : dc.getDiagnostics()) {
            diagnostics.add(d.getSource().getName() + ":" + d.getStartPosition() + "-" + d.getEndPosition() + ":" + d.getCode());
        }
        
        assertEquals(new HashSet<String>(Arrays.asList("/Use.java:68-68:compiler.err.expected")), diagnostics);
    }
    
    public void testErrorReturnType3() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String api = "package test; public class API { public static String call() { return null; } }";
        String use = "package test; public class Use { public void t() { Object str = API.undef(1); } }";
        DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, dc, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject("API", api), new MyFileObject("Use", use)));
        
        ct.analyze();
        
        Set<String> diagnostics = new HashSet<String>();
        
        for (Diagnostic<? extends JavaFileObject> d : dc.getDiagnostics()) {
            diagnostics.add(d.getSource().getName() + ":" + d.getStartPosition() + "-" + d.getEndPosition() + ":" + d.getCode());
        }
        
        assertEquals(new HashSet<String>(Arrays.asList("/Use.java:64-73:compiler.err.cant.resolve.location.args")), diagnostics);
    }

    public void testErrorReturnType4() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String api = "package test; public class API { public static Undef call() { return null; } }";
        String use = "package test; import static test.API.*; public class Use { public void t() { Object str = call(); } }";
        DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, dc, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject("API", api), new MyFileObject("Use", use)));
        
        ct.analyze();
        
        Set<String> diagnostics = new HashSet<String>();
        
        for (Diagnostic<? extends JavaFileObject> d : dc.getDiagnostics()) {
            diagnostics.add(d.getSource().getName() + ":" + d.getStartPosition() + "-" + d.getEndPosition() + ":" + d.getCode());
        }
        
        assertEquals(new HashSet<String>(Arrays.asList("/API.java:47-52:compiler.err.cant.resolve.location", "/Use.java:90-94:compiler.err.type.error")), diagnostics);
    }

    public void testErrorReturnType5() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String use = "package test; public class Use { public static Undef call() { return null; } public void t() { Object str = call(); } }";
        DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, dc, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject("Use", use)));
        
        ct.analyze();
        
        Set<String> diagnostics = new HashSet<String>();
        
        for (Diagnostic<? extends JavaFileObject> d : dc.getDiagnostics()) {
            diagnostics.add(d.getSource().getName() + ":" + d.getStartPosition() + "-" + d.getEndPosition() + ":" + d.getCode());
        }
        
        assertEquals(new HashSet<String>(Arrays.<String>asList("/Use.java:47-52:compiler.err.cant.resolve.location")), diagnostics);
    }
    
    public void testErrorReturnType6() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String api = "package test; public class API { public static Undef VAR; }";
        String use = "package test; public class Use { public void t() { Object str = API.VAR; } }";
        DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, dc, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject("API", api), new MyFileObject("Use", use)));
        
        ct.analyze();
        
        Set<String> diagnostics = new HashSet<String>();
        
        for (Diagnostic<? extends JavaFileObject> d : dc.getDiagnostics()) {
            diagnostics.add(d.getSource().getName() + ":" + d.getStartPosition() + "-" + d.getEndPosition() + ":" + d.getCode());
        }
        
        assertEquals(new HashSet<String>(Arrays.asList("/API.java:47-52:compiler.err.cant.resolve.location", "/Use.java:64-71:compiler.err.type.error")), diagnostics);
    }
    
    public void testErrorReturnType7() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String api = "package test; public class API { public static Undef VAR; }";
        String use = "package test; public class Use { public void t() { Object str = API.UNDEF; } }";
        DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, dc, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject("API", api), new MyFileObject("Use", use)));
        
        ct.analyze();
        
        Set<String> diagnostics = new HashSet<String>();
        
        for (Diagnostic<? extends JavaFileObject> d : dc.getDiagnostics()) {
            diagnostics.add(d.getSource().getName() + ":" + d.getStartPosition() + "-" + d.getEndPosition() + ":" + d.getCode());
        }
        
        assertEquals(new HashSet<String>(Arrays.asList("/API.java:47-52:compiler.err.cant.resolve.location", "/Use.java:64-73:compiler.err.cant.resolve.location")), diagnostics);
    }
    
    public void testErrorReturnType8() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String use = "package test; public class Use { public void t() { Object str = Undef.UNDEF; } }";
        DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, dc, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject("Use", use)));
        
        ct.analyze();
        
        Set<String> diagnostics = new HashSet<String>();
        
        for (Diagnostic<? extends JavaFileObject> d : dc.getDiagnostics()) {
            diagnostics.add(d.getSource().getName() + ":" + d.getStartPosition() + "-" + d.getEndPosition() + ":" + d.getCode());
        }
        
        assertEquals(new HashSet<String>(Arrays.asList("/Use.java:64-69:compiler.err.cant.resolve.location")), diagnostics);
    }

    public void testAnonymous() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String use = "package test; import java.util.*; public class Use { public void t() { List<String> ll = new ArrayList<>() { }; } }";
        DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, dc, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject("Use", use)));
        
        ct.analyze();
        
        Set<String> diagnostics = new HashSet<String>();
        
        for (Diagnostic<? extends JavaFileObject> d : dc.getDiagnostics()) {
            diagnostics.add(d.getSource().getName() + ":" + d.getStartPosition() + "-" + d.getEndPosition() + ":" + d.getCode());
        }
        
        assertEquals(new HashSet<String>(Arrays.asList("/Use.java:93-104:compiler.err.cant.apply.diamond.1")), diagnostics);
    }

    public void testErrorConstructor1() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String api = "package test; public class API { public API(Undef p) { } }";
        String use = "package test; public class Use { public void t() { Object str = new API(null); } }";
        DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, dc, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject("API", api), new MyFileObject("Use", use)));
        
        ct.analyze();
        
        Set<String> diagnostics = new HashSet<String>();
        
        for (Diagnostic<? extends JavaFileObject> d : dc.getDiagnostics()) {
            diagnostics.add(d.getSource().getName() + ":" + d.getStartPosition() + "-" + d.getEndPosition() + ":" + d.getCode());
        }
        
        assertEquals(new HashSet<String>(Arrays.asList("/API.java:44-49:compiler.err.cant.resolve.location", "/Use.java:64-77:compiler.err.type.error")), diagnostics);
    }

    public void testLambda1() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; public class Test { public static void main(String[] args) { Task<String> t = (String c) -> { System.err.println(\"Lambda!\"); return ; }; } public interface Task<C> { public void run(C c); } }";

        DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, dc, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        ct.analyze();

        assertEquals(dc.getDiagnostics().toString(), 0, dc.getDiagnostics().size());
    }

    public void testLambda2() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; public class Test { Task<String> t = (String c) -> { System.err.println(\"Lambda!\"); return ; }; public interface Task<C> { public void run(C c); } }";

        DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, dc, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        ct.analyze();

        assertEquals(dc.getDiagnostics().toString(), 0, dc.getDiagnostics().size());
    }
    
    public void testNonVoidReturnType() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; public class Test { private void t() { r(() -> { return 1; }); } private int r(Task t) { return t.run(); } public interface Task { public int run(); } }";

        DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, dc, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        ct.analyze();

        assertEquals(dc.getDiagnostics().toString(), 0, dc.getDiagnostics().size());
    }
    
    @Ignore
    public void testBreakAttrDuringLambdaAttribution() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; public class Test { public void t(Comparable c) { } }";

        DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, dc, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();
        
        ct.analyze();
        
        final JavacScope[] scope = new JavacScope[1];
        
        new TreePathScanner<Void, Void>() {
            @Override public Void visitMethod(MethodTree node, Void p) {
                if (node.getName().contentEquals("t"))
                    scope[0] = JavacTrees.instance(ct.getContext()).getScope(new TreePath(getCurrentPath(), node.getBody()));
                return super.visitMethod(node, p); //To change body of generated methods, choose Tools | Templates.
            }
        }.scan(cut, null);
        
        JCTree.JCStatement statement = ct.parseStatement("t((other) -> {return 0;})", new SourcePositions[1], new DiagnosticCollector<JavaFileObject>());
        
        final JCTree[] attributeTo = new JCTree[1];
        final JCLambda[] lambdaTree = new JCLambda[1];
        
        new TreeScanner<Void, Void>() {
            @Override public Void visitVariable(VariableTree node, Void p) {
                attributeTo[0] = (JCTree) node;
                return super.visitVariable(node, p); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Void visitLambdaExpression(LambdaExpressionTree node, Void p) {
                lambdaTree[0] = (JCLambda) node;
                return super.visitLambdaExpression(node, p); //To change body of generated methods, choose Tools | Templates.
            }            
        }.scan(statement, null);
                
        ct.attributeTreeTo(statement, scope[0].getEnv(), attributeTo[0]);
        assertNotNull(lambdaTree[0].type);
    }

    public void testCheckMethodNPE() throws Exception {
        String code = "public class Test { class Inner { Inner(int i) {} } public static void main(String[] args) { int i = 1; Test c = null; c.new Inner(i++) {}; } }";
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;
        
        JavaFileManager fm = new MemoryOutputJFM(tool.getStandardFileManager(null, null, null));
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, fm, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();
        
        ct.analyze();
        
        new TreePathScanner<Void, Void>() {
            @Override public Void visitNewClass(NewClassTree node, Void p) {
                assertNotNull(node.getEnclosingExpression());
                assertEquals(1, node.getArguments().size());
                return super.visitNewClass(node, p);
            }
        }.scan(cut, null);
        
        ct.generate(); //verify no exceptions during generate
    }
    
    public void test208454() throws Exception {
        String code = "public class Test { public static void main(String[] args) { String.new Runnable() { public void run() {} }; } }";
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;
        
        JavaFileManager fm = new MemoryOutputJFM(tool.getStandardFileManager(null, null, null));
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, fm, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();
        
        ct.analyze();
        
        new TreePathScanner<Void, Void>() {
            @Override public Void scan(Tree tree, Void p) {
                if (tree == null) return null;
                
                TreePath path = new TreePath(getCurrentPath(), tree);
                
                Trees.instance(ct).getScope(path);
                return super.scan(tree, p);
            }
        }.scan(cut, null);
        
        ct.generate(); //verify no exceptions during generate
    }

    public void testNewClassWithEnclosingNoAnonymous() throws Exception {
        String code = "public class Test { class Inner { Inner(int i) {} } public static void main(String[] args) { int i = 1; Test c = null; c.new Inner(i++); } }";
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;
        
        JavaFileManager fm = new MemoryOutputJFM(tool.getStandardFileManager(null, null, null));
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, fm, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();
        
        ct.analyze();
        
        new TreePathScanner<Void, Void>() {
            @Override public Void visitNewClass(NewClassTree node, Void p) {
                assertNotNull(node.getEnclosingExpression());
                assertEquals(1, node.getArguments().size());
                return super.visitNewClass(node, p);
            }
        }.scan(cut, null);
        
        ct.generate(); //verify no exceptions during generate
    }
    
    public void testNewClassWithoutEnclosingAnonymous() throws Exception {
        String code = "public class Test { class Inner { Inner(int i) {} } public static void main(String[] args) { int i = 1; new Inner(i++) {}; } }";
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;
        
        JavaFileManager fm = new MemoryOutputJFM(tool.getStandardFileManager(null, null, null));
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, fm, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();
        
        ct.analyze();
        
        new TreePathScanner<Void, Void>() {
            @Override public Void visitNewClass(NewClassTree node, Void p) {
                assertNull(node.getEnclosingExpression());
                assertEquals(1, node.getArguments().size());
                return super.visitNewClass(node, p);
            }
        }.scan(cut, null);
        
        ct.generate(); //verify no exceptions during generate
    }
    
    public void testNewClassWithoutEnclosingNoAnonymous() throws Exception {
        String code = "public class Test { class Inner { Inner(int i) {} } public static void main(String[] args) { int i = 1; new Inner(i++); } }";
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;
        
        JavaFileManager fm = new MemoryOutputJFM(tool.getStandardFileManager(null, null, null));
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, fm, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();
        
        ct.analyze();
        
        new TreePathScanner<Void, Void>() {
            @Override public Void visitNewClass(NewClassTree node, Void p) {
                assertNull(node.getEnclosingExpression());
                assertEquals(1, node.getArguments().size());
                return super.visitNewClass(node, p);
            }
        }.scan(cut, null);
        
        ct.generate(); //verify no exceptions during generate
    }

    public void testNPEForEmptyTargetOfTypeAnnotation() throws Exception {
        String code = "class Test { private void t(@NonNull String a) {} } @java.lang.annotation.Target() @interface NonNull { }";
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;
        
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov", "-XDshouldStopPolicy=FLOW"), null, Arrays.asList(new MyFileObject(code)));
        
        ct.analyze();
    }
    
    public void testAssignmentToError() throws Exception {
        String code = "public class Test { public static void main(String[] args) { bbb = 0; } }";
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;
        
        JavaFileManager fm = new MemoryOutputJFM(tool.getStandardFileManager(null, null, null));
        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, fm, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        final Trees trees = Trees.instance(ct);
        CompilationUnitTree cut = ct.parse().iterator().next();
        
        ct.analyze();
        
        new TreePathScanner<Void, Void>() {
            @Override public Void visitLiteral(LiteralTree node, Void p) {
                TypeMirror type = trees.getTypeMirror(getCurrentPath());
                
                assertNotNull(type);
                assertEquals(TypeKind.INT, type.getKind());
                
                return super.visitLiteral(node, p);
            }
        }.scan(cut, null);
    }
    private static class MemoryOutputJFM extends ForwardingJavaFileManager<StandardJavaFileManager> {

        private final Map<String, byte[]> writtenClasses = new HashMap<String, byte[]>();
        
        public MemoryOutputJFM(StandardJavaFileManager m) {
            super(m);
        }

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

}
