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

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javap.DisassemblerTool.DisassemblerTask;
import com.sun.tools.javap.JavapTask;
import global.ap1.AP;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.*;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject.Kind;
import junit.framework.TestCase;

/**
 *
 * @author Jan Lahoda
 */
public class ErrorToleranceTest extends TestCase {

    public void testSimple1() throws Exception {
        final String code = "package test;\n" +
                      "public class Test {\n" +
                      "    private void method(Unknown u) {\n" +
                      "    }\n" +
                      "}\n";

        final String golden = "package test;\n" +
                      "public class Test {\n" +
                      "    private void method(Unknown u) {\n" +
                      "        throw new RuntimeException(\"Uncompilable source code - cannot find symbol\\n  symbol:   class Unknown\\n  location: class test.Test\");" +
                      "    }\n" +
                      "}\n";

        compareResults(golden, code);
    }

    public void testSimple2() throws Exception {
        final String code = "package test;\n" +
                      "public class Test {\n" +
                      "    private void method(Object u) {\n" +
                      "        bflmpsvz" +
                      "    }\n" +
                      "}\n";

        final String golden = "package test;\n" +
                      "public class Test {\n" +
                      "    private void method(Object u) {\n" +
                      "        throw new RuntimeException(\"Uncompilable source code - not a statement\");" +
                      "    }\n" +
                      "}\n";

        compareResults(golden, code);
    }

    public void testSimple3() throws Exception {
        final String code = "package test;\n" +
                      "public class Test {\n" +
                      "    private void method(Object o) {\n" +
                      "    }\n" +
                      "    private void method(Unknown u) {\n" +
                      "    }\n" +
                      "}\n";

        final String golden = "package test;\n" +
                      "public class Test {\n" +
                      "    private void method(Object o) {\n" +
                      "    }\n" +
                      "    private void method(Unknown u) {\n" +
                      "        throw new RuntimeException(\"Uncompilable source code - cannot find symbol\\n  symbol:   class Unknown\\n  location: class test.Test\");" +
                      "    }\n" +
                      "}\n";

        compareResults(golden, code);
    }

    public void testInvalidFieldInit() throws Exception {
        final String code = "package test;\n" +
                      "public class Test {\n" +
                      "    public Test() {\n" +
                      "    }\n" +
                      "    public Test(Object o) {\n" +
                      "    }\n" +
                      "    private String s = bflmpsvz;\n" +
                      "}\n";

        final String golden = "package test;\n" +
                      "public class Test {\n" +
                      "    public Test() {\n" +
                      "        throw new RuntimeException(\"Uncompilable source code - cannot find symbol\\n  symbol:   variable bflmpsvz\\n  location: class test.Test\");" +
                      "    }\n" +
                      "    public Test(Object o) {\n" +
                      "        throw new RuntimeException(\"Uncompilable source code - cannot find symbol\\n  symbol:   variable bflmpsvz\\n  location: class test.Test\");" +
                      "    }\n" +
                      "    private String s;\n" +
                      "}\n";

        compareResults(golden, code);
    }

    public void testInvalidStaticFieldInit() throws Exception {
        final String code = "package test;\n" +
                      "public class Test {\n" +
                      "    private static String s = bflmpsvz;\n" +
                      "}\n";

        final String golden = "package test;\n" +
                      "public class Test {\n" +
                      "    static {\n" +
                      "        throw new RuntimeException(\"Uncompilable source code - cannot find symbol\\n  symbol:   variable bflmpsvz\\n  location: class test.Test\");" +
                      "    }\n" +
                      "    private static String s;\n" +
                      "}\n";

        compareResults(golden, code);
    }

    public void testInvalidCase() throws Exception {
        final String code = "package test;\n" +
                      "public class Test {\n" +
                      "    private void method(int i) {\n" +
                      "        switch(i) {\n" +
                      "            case Unknown.CONSTANT:\n" +
                      "                break;\n" +
                      "        }\n" +
                      "    }\n" +
                      "}\n";

        final String golden = "package test;\n" +
                      "public class Test {\n" +
                      "    private void method(int i) {\n" +
                      "        throw new RuntimeException(\"Uncompilable source code - cannot find symbol\\n  symbol:   variable Unknown\\n  location: class test.Test\");" +
                      "    }\n" +
                      "}\n";

        compareResults(golden, code);
    }

    public void testInvalidImport() throws Exception {
        final String code = "package test;\n" +
                      "import a.b.c.List;\n" +
                      "public class Test {\n" +
                      "}\n";

        final String golden = "package test;\n" +
                      "public class Test {\n" +
                      "    static {\n" +
                      "        throw new RuntimeException(\"Uncompilable source code - package a.b.c does not exist\");\n" +
                      "    }\n" +
                      "}\n";

        compareResults(golden, code);
    }

    public void testInvalidImportWithStaticInit() throws Exception {
        final String code = "package test;\n" +
                      "import a.b.c.List;\n" +
                      "public class Test {\n" +
                      "    static {\n" +
                      "        System.out.println();\n" +
                      "    }\n" +
                      "}\n";

        final String golden = "package test;\n" +
                      "public class Test {\n" +
                      "    static {\n" +
                      "        throw new RuntimeException(\"Uncompilable source code - package a.b.c does not exist\");\n" +
                      "    }\n" +
                      "}\n";

        compareResults(golden, code);
    }

    public void testInvalidCodeBeforePackage() throws Exception {
        final String code = "xyz\n" +
                      "package test;\n" +
                      "public class Test {\n" +
                      "}\n";

        final String golden = "package test;\n" +
                      "public class Test {\n" +
                      "}\n";

        compareResults(golden, code);
    }

    public void testInvalidCodeAfterClass() throws Exception {
        final String code = "package test;\n" +
                      "public class Test {\n" +
                      "}\n" +
                      "xyz\n";

        final String golden = "package test;\n" +
                      "public class Test {\n" +
                      "}\n";

        compareResults(golden, code);
    }

    public void testMethodWithoutBody1() throws Exception {
        final String code = "package test;\n" +
                      "public class Test {\n" +
                      "     public void test();\n" +
                      "}\n";

        final String golden = "package test;\n" +
                      "public class Test {\n" +
                      "     public void test() {" +
                      "        throw new RuntimeException(\"Uncompilable source code - missing method body, or declare abstract\");\n" +
                      "     }\n" +
                      "}\n";

        compareResults(golden, code);
    }
    
    public void testMethodWithoutBody2() throws Exception {
        final String code = "package test;\n" +
                      "public abstract class Test {\n" +
                      "     public abstract void test() {}\n" +
                      "}\n";

        final String golden = "package test;\n" +
                      "public abstract class Test {\n" +
                      "     public void test() {" +
                      "        throw new RuntimeException(\"Uncompilable source code - abstract methods cannot have a body\");\n" +
                      "     }\n" +
                      "}\n";

        compareResults(golden, code);
    }

    public void testDuplicateMethods() throws Exception {
        final String code = "package test;\n" +
                      "public class Test {\n" +
                      "    public void test() {\n" +
                      "    };\n" +
                      "    public void test() {\n" +
                      "    };\n" +
                      "}\n";

        final String golden = "package test;\n" +
                      "public class Test {\n" +
                      "    static {\n" +
                      "        throw new RuntimeException(\"Uncompilable source code - method test() is already defined in class test.Test\");\n" +
                      "    }\n" +
                      "    public void test() {" +
                      "    }\n" +
                      "}\n";

        compareResults(golden, code);
    }

    public void testDuplicateClasses() throws Exception {
        final String code = "package test;\n" +
                      "public class Test {\n" +
                      "    public static class Nested {\n" +
                      "    }\n" +
                      "    public static class Nested {\n" +
                      "    }\n" +
                      "}\n";

        final String golden = "package test;\n" +
                      "public class Test {\n" +
                      "    static {\n" +
                      "        throw new RuntimeException(\"Uncompilable source code - class test.Test.Nested is already defined in class test.Test\");\n" +
                      "    }\n" +
                      "    public static class Nested {\n" +
                      "    }\n" +
                      "}\n";

        compareResults(golden, code);
    }

    public void testIssue147516() throws Exception {
        final String code = "package test;\n" +
                      "public class Test {\n" +
                      "    public static final Nested NESTED = new Nested();\n" +
                      "    public static class Nested implements Runnable {\n" +
                      "    }\n" +
                      "}\n";

        final String golden = "package test;\n" +
                      "public class Test {\n" +
                      "    static {\n" +
                      "        throw new RuntimeException(\"Uncompilable source code - test.Test.Nested is not abstract and does not override abstract method run() in java.lang.Runnable\");\n" +
                      "    }\n" +
                      "    public static final Nested NESTED = new Nested();\n" +
                      "    public static class Nested implements Runnable {\n" +
                      "        static {\n" +
                      "            throw new RuntimeException(\"Uncompilable source code - test.Test.Nested is not abstract and does not override abstract method run() in java.lang.Runnable\");\n" +
                      "        }\n" +
                      "    };\n" +
                      "}\n";

        compareResults(golden, code);
    }
    
    public void testIssue212342a() throws Exception {
        final String code = "package test;\n" +
                      "@global.ap1.Ann(fqnToGenerate=\"test.G\", content=\"package test; public class G {}\") public class Test { void t1(G g) {} G t2() { return null; } }\n";

        final String golden = "package test;\n" +
                      "public class Test { void t1(G g) {} G t2() { return null; } }\n" +
                      "class G {}\n";

        compile(code, true, AP.class.getName());
        
        Collection<String> codeSig = dumpSignatures("test.Test");
        
        compile(golden, true);
        
        Collection<String> goldenSig = dumpSignatures("test.Test");
        assertEquals(goldenSig, codeSig);
    }
    
    public void testIssue212342b() throws Exception {
        final String code = "package test;\n" +
                      "@global.ap1.Ann(fqnToGenerate=\"test.G\", content=\"package test; @global.ap1.Ann(fqnToGenerate=\\\"test.H\\\", content=\\\"package test; public class H {}\\\") public class G {}\") public class Test { void t(H h) { System.err.println(1); } void t(G g) { System.err.println(2); } }\n";

        final String golden = "package test;\n" +
                      "public class Test {  void t(H h) { System.err.println(1); } void t(G g) { System.err.println(2); } }\n" +
                      "class G {}\n" +
                      "class H {}\n";

        compile(code, true, AP.class.getName());
        
        Collection<String> codeSig = dumpSignatures("test.Test");
        
        compile(golden, true);
        
        Collection<String> goldenSig = dumpSignatures("test.Test");
        assertEquals(goldenSig, codeSig);
    }
    
    public void testIssue212342c() throws Exception {
        final String code = "package test;\n" +
                      "@global.ap1.Ann(fqnToGenerate=\"test.G\", content=\"package test; @global.ap1.Ann(fqnToGenerate=\\\"test.H\\\", content=\\\"package test; public class H {}\\\") public class G {}\") public class Test { void t(G g) { System.err.println(2); } void t(H h) { System.err.println(1); } }\n";

        final String golden = "package test;\n" +
                      "public class Test {  void t(G g) { System.err.println(2); } void t(H h) { System.err.println(1); } }\n" +
                      "class G {}\n" +
                      "class H {}\n";

        compile(code, true, AP.class.getName());
        
        Collection<String> codeSig = dumpSignatures("test.Test");
        
        compile(golden, true);
        
        Collection<String> goldenSig = dumpSignatures("test.Test");
        assertEquals(goldenSig, codeSig);

    }
    public void testIssue212342d() throws Exception {
        final String code = "package test;\n" +
                      "@global.ap1.Ann(fqnToGenerate=\"test.G\", content=\"package test; @global.ap1.Ann(fqnToGenerate=\\\"test.H\\\", content=\\\"package test; public class H {}\\\") public class G {}\") public class Test { void t(G g, String str) { System.err.println(2); } void t(H h, String str) { System.err.println(1); } }\n";

        final String golden = "package test;\n" +
                      "public class Test {  void t(G g, String str) { System.err.println(2); } void t(H h, String str) { System.err.println(1); } }\n" +
                      "class G {}\n" +
                      "class H {}\n";

        compile(code, true, AP.class.getName());
        
        Collection<String> codeSig = dumpSignatures("test.Test");
        
        compile(golden, true);
        
        Collection<String> goldenSig = dumpSignatures("test.Test");
        assertEquals(goldenSig, codeSig);

    }
    
    public void testIssue212342e() throws Exception {
        final String code = "package test;\n" +
                      "@global.ap1.Ann(fqnToGenerate=\"test.G\", content=\"package test; @global.ap1.Ann(fqnToGenerate=\\\"test.H\\\", content=\\\"package test; public class H {}\\\") public class G {}\") public class Test { <T> void t(G g, T t) { System.err.println(2); } <T> void t(H h, T t) { System.err.println(1); } }\n";

        final String golden = "package test;\n" +
                      "public class Test { <T> void t(G g, T t) { System.err.println(2); } <T> void t(H h, T t) { System.err.println(1); } }\n" +
                      "class G {}\n" +
                      "class H {}\n";

        compile(code, true, AP.class.getName());
        
        Collection<String> codeSig = dumpSignatures("test.Test");
        
        compile(golden, true);
        
        Collection<String> goldenSig = dumpSignatures("test.Test");
        assertEquals(goldenSig, codeSig);

    }
    
    public void testIssue212342f() throws Exception {
        final String code = "package test;\n" +
                      "@global.ap1.Ann(fqnToGenerate=\"test.G\", content=\"package test; @global.ap1.Ann(fqnToGenerate=\\\"test.H\\\", content=\\\"package test; public class H {}\\\") public class G {}\") public class Test { <T extends H> void t(G g, T t) { System.err.println(2); } <T extends G> void t(H h, T t) { System.err.println(1); } }\n";

        final String golden = "package test;\n" +
                      "public class Test { <T extends H> void t(G g, T t) { System.err.println(2); } <T extends G> void t(H h, T t) { System.err.println(1); } }\n" +
                      "class G {}\n" +
                      "class H {}\n";

        compile(code, true, AP.class.getName());
        
        Collection<String> codeSig = dumpSignatures("test.Test");
        
        compile(golden, true);
        
        Collection<String> goldenSig = dumpSignatures("test.Test");
        assertEquals(goldenSig, codeSig);

    }

    public void testIssue212342g() throws Exception {
        final String code = "package test;\n" +
                      "@global.ap1.Ann(fqnToGenerate=\"test.G\", content=\"package test; @global.ap1.Ann(fqnToGenerate=\\\"test.H\\\", content=\\\"package test; public class H {}\\\") public class G {}\") public class Test { H h = new H(); G g = new G(); }\n";

        final String golden = "package test;\n" +
                      "public class Test {  H h = new H(); G g = new G(); }\n" +
                      "class G {}\n" +
                      "class H {}\n";

        compile(code, true, AP.class.getName());
        
        Collection<String> codeSig = dumpSignatures("test.Test");
        
        compile(golden, true);
        
        Collection<String> goldenSig = dumpSignatures("test.Test");
        assertEquals(goldenSig, codeSig);
    }
    
    //<editor-fold defaultstate="collapsed" desc=" Test Infrastructure ">
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

    private File workingDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        workingDir = File.createTempFile("ErrorToleranceTest", "");

        workingDir.delete();
        workingDir.mkdirs();
    }

    @Override
    protected void tearDown() throws Exception {
        deleteRecursively(workingDir);
        super.tearDown();
    }

    private String[] compile(String code, boolean repair) throws Exception {
        return compile(code, repair, null);
    }
    
    private String[] compile(String code, boolean repair, String apToRun) throws Exception {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        StandardJavaFileManager std = tool.getStandardFileManager(null, null, null);
        MemoryJavaFileManager mjfm = new MemoryJavaFileManager(std);

        std.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(workingDir));
        
        List<String> compilerOptions = new ArrayList<String>();
        compilerOptions.addAll(Arrays.asList("-bootclasspath",  bootPath, "-Xjcov", "-XDshouldStopPolicy=GENERATE", "-XDbackgroundCompilation"));
        if (apToRun != null) {
            URL myself = AnnotationProcessingTest.class.getProtectionDomain().getCodeSource().getLocation();
            File sourceOutput = new File(workingDir, "sourceOutput");
            sourceOutput.mkdirs();
            compilerOptions.addAll(Arrays.asList("-classpath", myself.toExternalForm(), "-processor", AP.class.getName(), "-s", sourceOutput.getAbsolutePath()));
        }
        JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, mjfm, null, compilerOptions, null, Arrays.asList(new MyFileObject(code)));
        com.sun.tools.javac.main.JavaCompiler.instance(ct.getContext()).doRepair = repair;
        ct.parse();
        Iterable<? extends Element> analyze = ct.analyze();
        
        List<String> result = new LinkedList<String>();
        
        for (TypeElement te : ElementFilter.typesIn(analyze)) {
            result.add(ct.getElements().getBinaryName(te).toString());
        }
        
        ct.generate();
        
        return result.toArray(new String[0]);
    }
    
    private Collection<String> dumpSignatures(String... fqns) throws Exception {
        List<String> result = new LinkedList<String>();
        
        for (String fqn : fqns) {
            StringWriter s = new StringWriter();
            PrintWriter w = new PrintWriter(s);
            DisassemblerTask javapTool = new JavapTask(w, null, null, Arrays.<String>asList("-classpath", workingDir.getAbsolutePath(), "-private", "-c"), Collections.singletonList(fqn));
            javapTool.call();
            w.close();
            result.add(s.toString());
        }
        
        return result;
    }
    
    private void compareResults(String golden, String code) throws Exception {
        Collection<String> codeSig = dumpSignatures(compile(code, true));
        Collection<String> goldenSig = dumpSignatures(compile(golden, false));
        assertEquals(goldenSig, codeSig);
    }

    private void deleteRecursively(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                deleteRecursively(c);
            }
        }
        
        f.delete();
    }
    
    private static final class MemoryJavaFileManager extends ForwardingJavaFileManager {

        public MemoryJavaFileManager(JavaFileManager jfm) {
            super(jfm);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
            JavaFileObject jfo = super.getJavaFileForOutput(location, className, kind, sibling);
            
            System.err.println("output=" + jfo);
            
            return jfo;
        }
        
    }
    //</editor-fold>
}
