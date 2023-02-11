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

import openjdk.source.util.JavacTask;
import openjdk.tools.javac.Main;
import openjdk.tools.javac.api.JavacTaskImpl;
import global.ap1.AP;
import global.ap1.ClassBasedAP;
import global.ap1.ErrorProducingAP;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import jdkx.lang.model.element.Element;
import jdkx.lang.model.element.TypeElement;
import jdkx.tools.Diagnostic;
import jdkx.tools.DiagnosticCollector;
import jdkx.tools.JavaCompiler;
import jdkx.tools.JavaFileObject;
import jdkx.tools.SimpleJavaFileObject;
import jdkx.tools.ToolProvider;
import junit.framework.TestCase;
import org.junit.Ignore;

/**
 *
 * @author lahvac
 */
public class AnnotationProcessingTest extends TestCase {

    public AnnotationProcessingTest(String name) {
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

    @Ignore
    public void testNoFalseEnterErrors() throws IOException {
        String code = "package test; @global.ap1.Ann(fqnToGenerate=\"test.G\", content=\"package test; public class G {}\") public class Test extends G {}";

        performErrorsTest(code, 0);
    }
    
    public void testNoFalseEnterWarnings() throws IOException {
        String code = "package test; public class Test { @SuppressWarnings(\"deprecation\") public java.rmi.server.Skeleton t() { return null; } }";

        performErrorsTest(code, Arrays.asList("-Xlint"), 0);
    }

    @Ignore
    public void testCorrectEnterErrors() throws IOException {
        String code = "package test; @global.ap1.Ann(fqnToGenerate=\"test.H\", content=\"package test; public class H {}\") public class Test extends Undefined {}";

        performErrorsTest(code, 1);
    }
    
    @Ignore
    public void testDuplicatedErrorsReported() throws IOException {
        String code = "package test; @global.ap1.Ann(fqnToGenerate=\"test.H\", content=\"package test; public class H {}\") public class Test {}";

        performAPErrorsTest(code, ErrorProducingAP.class.getName(), new FileContent[0], "14-117:message 1", "14-117:message 2", "14-117:message 3");
    }

    @Ignore
    public void testDependentAP() throws IOException {
        String code = "package test; public class Test { Auxiliary aux; }";
        String auxiliary = "package test; @global.ap1.Ann(fqnToGenerate=\"test.G\", content=\"package test; public class G {}\") public class Auxiliary extends G { private Unknown t; private Aux a; }";
        String aux = "package test; @global.ap1.Ann(fqnToGenerate=\"test.H\", content=\"package test; public class H {}\") public class Aux extends H { private Unknown t; }";

        performAPErrorsTest(code, AP.class.getName(), 
                new FileContent[] {
                    new FileContent("test/Auxiliary.java", auxiliary),
                    new FileContent("test/Aux.java", aux)
                },
                "140-140:cannot find symbol\n  symbol:   class Unknown\n  location: class test.Auxiliary",
                "134-134:cannot find symbol\n  symbol:   class Unknown\n  location: class test.Aux");
    }

    @Ignore
    public void testNoAP() throws IOException {
        String code = "package test; public class Test { Auxiliary aux; }";
        String auxiliary = "package test; public class Auxiliary { private Unknown t; private Aux a; }";
        String aux = "package test; public class Aux { private Unknown t; }";

        performAPErrorsTest(code, null,
                new FileContent[] {
                    new FileContent("test/Auxiliary.java", auxiliary),
                    new FileContent("test/Aux.java", aux)
                },
                "47-47:cannot find symbol\n  symbol:   class Unknown\n  location: class test.Auxiliary",
                "41-41:cannot find symbol\n  symbol:   class Unknown\n  location: class test.Aux");
    }

    private void performErrorsTest(String code, int expectedErrors) throws IOException {
        performErrorsTest(code, new ArrayList<String>(), expectedErrors);
    }
    
    private void performErrorsTest(String code, Collection<? extends String> extraOptions, int expectedErrors) throws IOException {
        File sourceOutput = File.createTempFile("NoFalseErrorsFromAP", "");
        sourceOutput.delete();
        assertTrue(sourceOutput.mkdirs());

        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        URL myself = AnnotationProcessingTest.class.getProtectionDomain().getCodeSource().getLocation();
        DiagnosticCollector<JavaFileObject> diagnostic = new DiagnosticCollector<JavaFileObject>();
        List<String> options = new ArrayList<String>(Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-classpath", myself.toExternalForm(), "-processor", AP.class.getName(), "-s", sourceOutput.getAbsolutePath(), "-XDbackgroundCompilation"));
        options.addAll(extraOptions);
        JavacTask ct = (JavacTask)tool.getTask(null, null, diagnostic, options, null, Arrays.asList(new MyFileObject(code)));
        ct.analyze();
        assertEquals(diagnostic.getDiagnostics().toString(), expectedErrors, diagnostic.getDiagnostics().size());

        //intentionally not deleting thwn the test fails to simply diagnostic
        delete(sourceOutput);
    }

    private void performAPErrorsTest(String code, String apName, FileContent[] auxiliary, String... goldenErrors) throws IOException {
        File temp = File.createTempFile("NoFalseErrorsFromAP", "");
        temp.delete();
        assertTrue(temp.mkdirs());

        File sourceOutput = new File(temp, "out");

        assertTrue(sourceOutput.mkdirs());

        File source = new File(temp, "src");

        assertTrue(source.mkdirs());

        if (auxiliary != null) {
            for (FileContent fc : auxiliary) {
                File aux = new File(source, fc.path);

                aux.getParentFile().mkdirs();

                FileWriter w = new FileWriter(aux);

                w.write(fc.content);
                w.close();
            }
        }

        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        URL myself = AnnotationProcessingTest.class.getProtectionDomain().getCodeSource().getLocation();
        DiagnosticCollector<JavaFileObject> diagnostic = new DiagnosticCollector<JavaFileObject>();
        List<String> options = new LinkedList<String>();
        options.addAll(Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-classpath", myself.toExternalForm()));
        if (apName != null) {
            options.addAll(Arrays.asList("-processor", apName));
        } else {
            options.add("-proc:none");
        }
        options.addAll(Arrays.asList("-s", sourceOutput.getAbsolutePath(), "-XDbackgroundCompilation", "-sourcepath", source.getAbsolutePath()));
        JavacTask ct = (JavacTask)tool.getTask(null, null, diagnostic, options, null, Arrays.asList(new MyFileObject(code)));
        ct.analyze();

        List<String> actualErrors = new ArrayList<String>();

        for (Diagnostic<? extends JavaFileObject> d : diagnostic.getDiagnostics()) {
            actualErrors.add(d.getStartPosition() + "-" + d.getEndPosition() + ":" + d.getMessage(null));
        }

        assertEquals(Arrays.asList(goldenErrors), actualErrors);

        //intentionally not deleting thwn the test fails to simply diagnostic
        delete(sourceOutput);
    }

    public void testAPNoSources() throws IOException {
        File sourceOutput = File.createTempFile("APNoSources", "");
        
        sourceOutput.delete();
        assertTrue(sourceOutput.mkdirs());

        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        URL myself = AnnotationProcessingTest.class.getProtectionDomain().getCodeSource().getLocation();
        
        Main.compile(new String[] {
            "-bootclasspath",  bootPath,
            "-source", version,
            "-target", version,
            "-classpath", myself.toExternalForm(),
            "-processor", ClassBasedAP.class.getName(),
            "-s", sourceOutput.getAbsolutePath(),
            "java.lang.String"
        });

        assertTrue(new File(sourceOutput, "java.lang.String.txt").canRead());
        
        //intentionally not deleting thwn the test fails to simply diagnostic
        delete(sourceOutput);
    }
    
    private static void delete(File d) {
        if (d.isDirectory()) {
            for (File f : d.listFiles()) {
                delete(f);
            }
        }
        d.delete();
    }

    private static class FileContent {
        final String path;
        final String content;

        public FileContent(String path, String content) {
            this.path = path;
            this.content = content;
        }
    }

    public void testKeepBrokenAttributes228628() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; @Test(NonExistent.class) public @interface Test { public Class<?> value(); }";

        JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov", "-XDshouldStopPolicy=GENERATE"), null, Arrays.asList(new global.Test116436.MyFileObject(code)));
        Iterable<? extends Element> classes = ct.enter();
        TypeElement test = (TypeElement) classes.iterator().next();
        
        assertEquals(1, test.getAnnotationMirrors().get(0).getElementValues().size());
    }
    
}
