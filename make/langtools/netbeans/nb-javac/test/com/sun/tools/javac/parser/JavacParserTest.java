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
package openjdk.tools.javac.parser;

import openjdk.source.tree.BinaryTree;
import openjdk.source.tree.BlockTree;
import openjdk.source.tree.ClassTree;
import openjdk.source.tree.CompilationUnitTree;
import openjdk.source.tree.ExpressionStatementTree;
import openjdk.source.tree.ExpressionTree;
import openjdk.source.tree.LambdaExpressionTree;
import openjdk.source.tree.MethodInvocationTree;
import openjdk.source.tree.MethodTree;
import openjdk.source.tree.ModifiersTree;
import openjdk.source.tree.StatementTree;
import openjdk.source.tree.Tree;
import openjdk.source.tree.Tree.Kind;
import openjdk.source.tree.VariableTree;
import openjdk.source.tree.WhileLoopTree;
import openjdk.source.util.SourcePositions;
import openjdk.source.util.TreePathScanner;
import openjdk.source.util.TreeScanner;
import openjdk.source.util.Trees;
import openjdk.tools.javac.api.JavacTaskImpl;
import openjdk.tools.javac.tree.JCTree;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import jdkx.lang.model.element.Modifier;
import jdkx.tools.Diagnostic;
import jdkx.tools.DiagnosticCollector;
import jdkx.tools.DiagnosticListener;
import jdkx.tools.JavaCompiler;
import jdkx.tools.JavaFileObject;
import jdkx.tools.SimpleJavaFileObject;
import jdkx.tools.ToolProvider;
import junit.framework.TestCase;

public class JavacParserTest extends TestCase {

    public JavacParserTest(String testName) {
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

    public void testPositionForSuperConstructorCalls() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; public class Test {public Test() {super();}}";

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();
        SourcePositions pos = Trees.instance(ct).getSourcePositions();

        MethodTree method = (MethodTree) ((ClassTree) cut.getTypeDecls().get(0)).getMembers().get(0);
        ExpressionStatementTree es = (ExpressionStatementTree) method.getBody().getStatements().get(0);

        assertEquals(72 - 24, pos.getStartPosition(cut, es));
        assertEquals(80 - 24, pos.getEndPosition(cut, es));

        MethodInvocationTree mit = (MethodInvocationTree) es.getExpression();

        assertEquals(72 - 24, pos.getStartPosition(cut, mit));
        assertEquals(79 - 24, pos.getEndPosition(cut, mit));

        assertEquals(72 - 24, pos.getStartPosition(cut, mit.getMethodSelect()));
        assertEquals(77 - 24, pos.getEndPosition(cut, mit.getMethodSelect()));

    }

    public void testPositionForEnumModifiers() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; public enum Test {A;}";

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();
        SourcePositions pos = Trees.instance(ct).getSourcePositions();

        ClassTree clazz = (ClassTree) cut.getTypeDecls().get(0);
        ModifiersTree mt = clazz.getModifiers();

        assertEquals(38 - 24, pos.getStartPosition(cut, mt));
        assertEquals(44 - 24, pos.getEndPosition(cut, mt));
    }

//    public void testErroneousMemberSelectPositions() throws IOException {
//        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
//        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
//        assert tool != null;
//
//        String code = "package test; public class Test { public void test() { new Runnable() {}.   } public Test() {}}";
//
//        JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
//        CompilationUnitTree cut = ct.parse().iterator().next();
//        SourcePositions pos = Trees.instance(ct).getSourcePositions();
//
//        ClassTree clazz = (ClassTree) cut.getTypeDecls().get(0);
//        ExpressionStatementTree est = (ExpressionStatementTree) ((MethodTree) clazz.getMembers().get(0)).getBody().getStatements().get(0);
//
//        assertEquals(79 - 24, pos.getStartPosition(cut, est));
//        assertEquals(97 - 24, pos.getEndPosition(cut, est));
//    }
    public void testNewClassWithEnclosing() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; class Test { class d {} private void method() { Object o = Test.this.new d(); } }";

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();
        SourcePositions pos = Trees.instance(ct).getSourcePositions();

        ClassTree clazz = (ClassTree) cut.getTypeDecls().get(0);
        ExpressionTree est = ((VariableTree) ((MethodTree) clazz.getMembers().get(1)).getBody().getStatements().get(0)).getInitializer();

        assertEquals(97 - 24, pos.getStartPosition(cut, est));
        assertEquals(114 - 24, pos.getEndPosition(cut, est));
    }

    public void testPreferredPositionForBinaryOp() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; public class Test {private void test() {Object o = null; boolean b = o != null && o instanceof String;} private Test() {}}";

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();

        ClassTree clazz = (ClassTree) cut.getTypeDecls().get(0);
        MethodTree method = (MethodTree) clazz.getMembers().get(0);
        VariableTree condSt = (VariableTree) method.getBody().getStatements().get(1);
        BinaryTree cond = (BinaryTree) condSt.getInitializer();

        JCTree condJC = (JCTree) cond;

        assertEquals(117 - 24, condJC.pos);
    }

    public void testPositionBrokenSource126732a() throws IOException {
        String[] commands = new String[]{
            "return Runnable()",
            "do { } while (true)",
            "throw UnsupportedOperationException()",
            "assert true",
            "1 + 1",
            "vartype varname",};

        for (String command : commands) {
            final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
            final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
            assert tool != null;

            String code = "package test;\n"
                    + "public class Test {\n"
                    + "    public static void test() {\n"
                    + "        " + command + " {\n"
                    + "                new Runnable() {\n"
                    + "        };\n"
                    + "    }\n"
                    + "}";

            JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
            CompilationUnitTree cut = ct.parse().iterator().next();

            ClassTree clazz = (ClassTree) cut.getTypeDecls().get(0);
            MethodTree method = (MethodTree) clazz.getMembers().get(0);
            List<? extends StatementTree> statements = method.getBody().getStatements();

            StatementTree ret = statements.get(0);
            StatementTree block = statements.get(1);

            Trees t = Trees.instance(ct);

            assertEquals(command, code.indexOf(command + " {") + (command + " ").length(), t.getSourcePositions().getEndPosition(cut, ret));
            assertEquals(command, code.indexOf(command + " {") + (command + " ").length(), t.getSourcePositions().getStartPosition(cut, block));
        }
    }

    public void testPositionBrokenSource126732b() throws IOException {
        String[] commands = new String[]{
            "break",
            "break A",
            "continue ",
            "continue A",};

        for (String command : commands) {
            final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
            final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
            assert tool != null;

            String code = "package test;\n"
                    + "public class Test {\n"
                    + "    public static void test() {\n"
                    + "        while (true) {\n"
                    + "            " + command + " {\n"
                    + "                new Runnable() {\n"
                    + "        };\n"
                    + "        }\n"
                    + "    }\n"
                    + "}";

            JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
            CompilationUnitTree cut = ct.parse().iterator().next();

            ClassTree clazz = (ClassTree) cut.getTypeDecls().get(0);
            MethodTree method = (MethodTree) clazz.getMembers().get(0);
            List<? extends StatementTree> statements = ((BlockTree) ((WhileLoopTree) method.getBody().getStatements().get(0)).getStatement()).getStatements();

            StatementTree ret = statements.get(0);
            StatementTree block = statements.get(1);

            Trees t = Trees.instance(ct);

            assertEquals(command, code.indexOf(command + " {") + (command + " ").length(), t.getSourcePositions().getEndPosition(cut, ret));
            assertEquals(command, code.indexOf(command + " {") + (command + " ").length(), t.getSourcePositions().getStartPosition(cut, block));
        }
    }

    public void testErrorRecoveryForEnhancedForLoop142381() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; class Test { private void method() { java.util.Set<String> s = null; for (a : s) {} } }";

        final List<Diagnostic<? extends JavaFileObject>> errors = new LinkedList<Diagnostic<? extends JavaFileObject>>();

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, new DiagnosticListener<JavaFileObject>() {
            public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                errors.add(diagnostic);
            }
        }, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));

        CompilationUnitTree cut = ct.parse().iterator().next();

        ClassTree clazz = (ClassTree) cut.getTypeDecls().get(0);
        StatementTree forStatement = ((MethodTree) clazz.getMembers().get(0)).getBody().getStatements().get(1);

        assertEquals(Kind.ENHANCED_FOR_LOOP, forStatement.getKind());
        assertFalse(errors.isEmpty());
    }

    public void testPositionAnnotationNoPackage187551() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "\n@interface Test {}";

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();
        ClassTree clazz = (ClassTree) cut.getTypeDecls().get(0);
        Trees t = Trees.instance(ct);

        assertEquals(1, t.getSourcePositions().getStartPosition(cut, clazz));
    }

    public void testPositionsSane() throws IOException {
        performPositionsSanityTest("package test; class Test { private void method() { java.util.List<? extends java.util.List<? extends String>> l; } }");
        performPositionsSanityTest("package test; class Test { private void method() { java.util.List<? super java.util.List<? super String>> l; } }");
        performPositionsSanityTest("package test; class Test { private void method() { java.util.List<? super java.util.List<?>> l; } }");
        performPositionsSanityTest("package test; class Test { private void method() { java.util.List<String> l = null; l.reduce(null, (String s1, String s2) -> { return s1; }); } }");
    }

    private void performPositionsSanityTest(String code) throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        final List<Diagnostic<? extends JavaFileObject>> errors = new LinkedList<Diagnostic<? extends JavaFileObject>>();

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, new DiagnosticListener<JavaFileObject>() {
            public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                errors.add(diagnostic);
            }
        }, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));

        final CompilationUnitTree cut = ct.parse().iterator().next();
        final Trees trees = Trees.instance(ct);

        new TreeScanner<Void, Void>() {
            private long parentStart = 0;
            private long parentEnd = Integer.MAX_VALUE;

            @Override
            public Void scan(Tree node, Void p) {
                if (node == null) {
                    return null;
                }

                long start = trees.getSourcePositions().getStartPosition(cut, node);

                if (start == (-1)) {
                    return null; //synthetic tree
                }
                assertTrue(node.toString() + ":" + start + "/" + parentStart, parentStart <= start);

                long prevParentStart = parentStart;

                parentStart = start;

                long end = trees.getSourcePositions().getEndPosition(cut, node);

                assertTrue(node.toString() + ":" + end + "/" + parentEnd, end <= parentEnd);

                long prevParentEnd = parentEnd;

                parentEnd = end;

                super.scan(node, p);

                parentStart = prevParentStart;
                parentEnd = prevParentEnd;

                return null;
            }

        }.scan(cut, null);
    }

    public void testCorrectWilcardPositions() throws IOException {
        performWildcardPositionsTest("package test; import java.util.List; class Test { private void method() { List<? extends List<? extends String>> l; } }",
                Arrays.asList("List<? extends List<? extends String>> l;",
                        "List<? extends List<? extends String>>",
                        "List",
                        "? extends List<? extends String>",
                        "List<? extends String>",
                        "List",
                        "? extends String",
                        "String"));
        performWildcardPositionsTest("package test; import java.util.List; class Test { private void method() { List<? super List<? super String>> l; } }",
                Arrays.asList("List<? super List<? super String>> l;",
                        "List<? super List<? super String>>",
                        "List",
                        "? super List<? super String>",
                        "List<? super String>",
                        "List",
                        "? super String",
                        "String"));
        performWildcardPositionsTest("package test; import java.util.List; class Test { private void method() { List<? super List<?>> l; } }",
                Arrays.asList("List<? super List<?>> l;",
                        "List<? super List<?>>",
                        "List",
                        "? super List<?>",
                        "List<?>",
                        "List",
                        "?"));
        performWildcardPositionsTest("package test; import java.util.List; class Test { private void method() { List<? extends List<? extends List<? extends String>>> l; } }",
                Arrays.asList("List<? extends List<? extends List<? extends String>>> l;",
                        "List<? extends List<? extends List<? extends String>>>",
                        "List",
                        "? extends List<? extends List<? extends String>>",
                        "List<? extends List<? extends String>>",
                        "List",
                        "? extends List<? extends String>",
                        "List<? extends String>",
                        "List",
                        "? extends String",
                        "String"));
        performWildcardPositionsTest("package test; import java.util.List; class Test { private void method() { List<? extends List<? extends List<? extends String   >>> l; } }",
                Arrays.asList("List<? extends List<? extends List<? extends String   >>> l;",
                        "List<? extends List<? extends List<? extends String   >>>",
                        "List",
                        "? extends List<? extends List<? extends String   >>",
                        "List<? extends List<? extends String   >>",
                        "List",
                        "? extends List<? extends String   >",
                        "List<? extends String   >",
                        "List",
                        "? extends String",
                        "String"));
    }

    public void performWildcardPositionsTest(final String code, List<String> golden) throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        final List<Diagnostic<? extends JavaFileObject>> errors = new LinkedList<Diagnostic<? extends JavaFileObject>>();

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, new DiagnosticListener<JavaFileObject>() {
            public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                errors.add(diagnostic);
            }
        }, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));

        final CompilationUnitTree cut = ct.parse().iterator().next();
        final List<String> content = new LinkedList<String>();
        final Trees trees = Trees.instance(ct);

        new TreeScanner<Void, Void>() {
            @Override
            public Void scan(Tree node, Void p) {
                if (node == null) {
                    return null;
                }

                long start = trees.getSourcePositions().getStartPosition(cut, node);

                if (start == (-1)) {
                    return null; //synthetic tree
                }
                long end = trees.getSourcePositions().getEndPosition(cut, node);

                content.add(code.substring((int) start, (int) end));

                return super.scan(node, p);
            }

        }.scan(((MethodTree) ((ClassTree) cut.getTypeDecls().get(0)).getMembers().get(0)).getBody().getStatements().get(0), null);

        assertEquals(golden.toString(), content.toString());
    }

    public void testStartPositionForMethodWithoutModifiers() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package t; class Test { <T> void t() {} }";

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();
        ClassTree clazz = (ClassTree) cut.getTypeDecls().get(0);
        MethodTree mt = (MethodTree) clazz.getMembers().get(0);
        Trees t = Trees.instance(ct);
        int start = (int) t.getSourcePositions().getStartPosition(cut, mt);
        int end = (int) t.getSourcePositions().getEndPosition(cut, mt);

        assertEquals("<T> void t() {}", code.substring(start, end));
    }

    public void testStartPositionEnumConstantInit() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package t; enum Test { AAA; }";

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();
        ClassTree clazz = (ClassTree) cut.getTypeDecls().get(0);
        VariableTree enumAAA = (VariableTree) clazz.getMembers().get(0);
        Trees t = Trees.instance(ct);
        int start = (int) t.getSourcePositions().getStartPosition(cut, enumAAA.getInitializer());

        assertEquals("; }", code.substring(start));
    }

    public void testVariableInIfThen1() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package t; class Test { private static void t(String name) { if (name != null) String nn = name.trim(); } }";
        DiagnosticCollector<JavaFileObject> coll = new DiagnosticCollector<JavaFileObject>();
        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, coll, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));

        ct.parse();

        List<String> codes = new LinkedList<String>();

        for (Diagnostic<? extends JavaFileObject> d : coll.getDiagnostics()) {
            codes.add(d.getCode());
        }

        assertEquals(Arrays.<String>asList("compiler.err.variable.not.allowed"), codes);
    }

    public void testVariableInIfThen2() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package t; class Test { private static void t(String name) { if (name != null) class X {} } }";
        DiagnosticCollector<JavaFileObject> coll = new DiagnosticCollector<JavaFileObject>();
        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, coll, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));

        ct.parse();

        List<String> codes = new LinkedList<String>();

        for (Diagnostic<? extends JavaFileObject> d : coll.getDiagnostics()) {
            codes.add(d.getCode());
        }

        assertEquals(Arrays.<String>asList("compiler.err.class.not.allowed"), codes);
    }

    public void testVariableInIfThen3() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package t; class Test { private static void t(String name) { if (name != null) abstract } }";
        DiagnosticCollector<JavaFileObject> coll = new DiagnosticCollector<JavaFileObject>();
        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, coll, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));

        ct.parse();

        List<String> codes = new LinkedList<String>();

        for (Diagnostic<? extends JavaFileObject> d : coll.getDiagnostics()) {
            codes.add(d.getCode());
        }

        assertEquals(Arrays.<String>asList("compiler.err.expected4"), codes);//TODO: was "compiler.err.illegal.start.of.expr" before JDK8 merge
//        assertEquals(Arrays.<String>asList("compiler.err.illegal.start.of.expr"), codes);
    }

    //see javac bug #6882235, NB bug #98234:
    public void testMissingExponent() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "\nclass Test { { System.err.println(0e); } }";

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"/*, "-XDshouldStopPolicy=ENTER"*/), null, Arrays.asList(new MyFileObject(code)));

        assertNotNull(ct.parse().iterator().next());
    }

    public void testTryResourcePos() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        final String code = "package t; class Test { { try (java.io.InputStream in = null) { } } }";

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();

        new TreeScanner<Void, Void>() {
            @Override
            public Void visitVariable(VariableTree node, Void p) {
                if ("in".contentEquals(node.getName())) {
                    JCTree.JCVariableDecl var = (JCTree.JCVariableDecl) node;

                    assertEquals("in = null) { } } }", code.substring(var.pos));
                }
                return super.visitVariable(node, p);
            }
        }.scan(cut, null);
    }

    public void testVarPos() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        final String code = "package t; class Test { { java.io.InputStream in = null; } }";

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();

        new TreeScanner<Void, Void>() {
            @Override
            public Void visitVariable(VariableTree node, Void p) {
                if ("in".contentEquals(node.getName())) {
                    JCTree.JCVariableDecl var = (JCTree.JCVariableDecl) node;

                    assertEquals("in = null; } }", code.substring(var.pos));
                }
                return super.visitVariable(node, p);
            }
        }.scan(cut, null);
    }

    public void testLambdaPositions() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        final String code = "package test; class Test { private void method() { java.util.List<String> l = null; l.reduce(null, (String s1, String s2) -> { return s1 + s2; }); } }";

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        final CompilationUnitTree cut = ct.parse().iterator().next();
        final Trees trees = Trees.instance(ct);
        final List<String> content = new ArrayList<String>();

        new TreePathScanner<Void, Void>() {
            boolean record;

            @Override
            public Void scan(Tree node, Void p) {
                if (node == null) {
                    return null;
                }

                if (record) {
                    long start = trees.getSourcePositions().getStartPosition(cut, node);

                    if (start == (-1)) {
                        return null; //synthetic tree
                    }

                    long end = trees.getSourcePositions().getEndPosition(cut, node);

                    content.add(code.substring((int) start, (int) end));
                }

                return super.scan(node, p);
            }

            @Override
            public Void visitLambdaExpression(LambdaExpressionTree node, Void p) {
                boolean old = record;

                record = true;

                try {
                    return super.visitLambdaExpression(node, p);
                } finally {
                    record = old;
                }
            }

        }.scan(cut, null);

        assertEquals(Arrays.asList("String s1", "String", "String s2", "String", "{ return s1 + s2; }", "return s1 + s2;", "s1 + s2", "s1", "s2"), content);
    }

    public void testInfiniteParsing() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        final String code = "111\npackage t; class Test { }";

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();
        assertNotNull(cut);
    }

    public void testShouldNotSkipFirstStrictFP8005931() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        final String code = "strictfp class Test { }";

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, Arrays.asList("-bootclasspath", bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();

        assertTrue(((ClassTree) cut.getTypeDecls().get(0)).getModifiers().getFlags().contains(Modifier.STRICTFP));
    }
}
