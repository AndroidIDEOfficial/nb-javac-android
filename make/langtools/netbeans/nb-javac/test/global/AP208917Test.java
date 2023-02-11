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
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import jdkx.annotation.processing.AbstractProcessor;
import jdkx.annotation.processing.RoundEnvironment;
import jdkx.annotation.processing.SupportedAnnotationTypes;
import jdkx.annotation.processing.SupportedSourceVersion;
import jdkx.lang.model.SourceVersion;
import jdkx.lang.model.element.Element;
import jdkx.lang.model.element.TypeElement;
import jdkx.tools.Diagnostic;
import jdkx.tools.DiagnosticCollector;
import jdkx.tools.JavaCompiler;
import jdkx.tools.JavaFileObject;
import jdkx.tools.SimpleJavaFileObject;
import jdkx.tools.ToolProvider;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author lahvac
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("*")
public class AP208917Test extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getRootElements()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "1st warning without source");
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "warning with source", e);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "2nd warning without source");
        }
        return false;
    }

    @Test
    public void performTest() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        DiagnosticCollector<JavaFileObject> diagnostic = new DiagnosticCollector<JavaFileObject>();
        List<String> options = new LinkedList<String>();
        options.addAll(Arrays.asList("-bootclasspath",  bootPath, "-source", "1.8", "-classpath", System.getProperty("java.class.path")));
        options.addAll(Arrays.asList("-processor", AP208917Test.class.getName()));
        JavacTask ct = (JavacTask)tool.getTask(null, null, diagnostic, options, null, Arrays.asList(new MyFileObject("class Test {}")));
        ct.analyze();

        List<String> actualErrors = new ArrayList<String>();

        for (Diagnostic<? extends JavaFileObject> d : diagnostic.getDiagnostics()) {
            String diagnosticSource;
            if (d.getSource() != null) {
                diagnosticSource = d.getSource().toUri().toString();
                diagnosticSource = diagnosticSource.substring(diagnosticSource.lastIndexOf('/') + 1);
            } else {
                diagnosticSource = "<none>";
            }
            actualErrors.add(diagnosticSource + ":" + d.getStartPosition() + "-" + d.getEndPosition() + ":" + d.getMessage(null));
        }

        Assert.assertEquals(Arrays.asList("<none>:-1--1:1st warning without source", "Test.java:0-13:warning with source", "<none>:-1--1:2nd warning without source"), actualErrors);
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
}
