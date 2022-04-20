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

import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import junit.framework.TestCase;

public class TestAnnotationPackageInfo extends TestCase {

    public TestAnnotationPackageInfo(String name) {
        super(name);
    }

    static class MyFileObject extends SimpleJavaFileObject {
        private String text;
        public MyFileObject(String fileName, String text) {
            super(URI.create("myfo:/" + fileName), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }

    public void test178452() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String codeAnnotation = "package test; public @interface Test {\n" +
                                "    public int mandatory();\n" +
                                "}\n";
        String codePackageInfo = "@Test package test;";
        DiagnosticCollector<JavaFileObject> coll = new DiagnosticCollector<JavaFileObject>();
        JavacTask ct = (JavacTask)tool.getTask(null, null, coll, Arrays.asList("-bootclasspath",  bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject("test/Test.java", codeAnnotation), new MyFileObject("test/package-info.java", codePackageInfo)));
        ct.analyze();

        for (Diagnostic<? extends JavaFileObject> d : coll.getDiagnostics()) {
            if (d.getKind() != Diagnostic.Kind.ERROR) continue;

            return;
        }

        fail(coll.getDiagnostics().toString());
    }
    
    public void testMutualAnnotations() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String codeAnnotationFoo = "package test; @Bar @java.lang.annotation.Target(java.lang.annotation.ElementType.CLASS) public @interface Foo {}\n";
        String codeAnnotationBar = "package test; @Foo @java.lang.annotation.Target(java.lang.annotation.ElementType.CLASS) public @interface Bar {}\n";

        JavacTask ct = (JavacTask)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-Xjcov"), null, Arrays.asList(new MyFileObject("test/Foo.java", codeAnnotationFoo), new MyFileObject("test/Bar.java", codeAnnotationBar)));
        ct.analyze();
    }
}
