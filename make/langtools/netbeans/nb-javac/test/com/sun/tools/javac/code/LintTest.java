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
package openjdk.tools.javac.code;

import openjdk.source.util.JavacTask;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import jdkx.tools.Diagnostic;
import jdkx.tools.DiagnosticListener;
import jdkx.tools.JavaCompiler;
import jdkx.tools.JavaFileObject;
import jdkx.tools.SimpleJavaFileObject;
import jdkx.tools.ToolProvider;
import junit.framework.TestCase;

/**
 *
 * @author Jan Lahoda
 */
public class LintTest extends TestCase {
    
    public LintTest(String testName) {
        super(testName);
    }

    static class MyFileObject extends SimpleJavaFileObject {
        private String code;
        public MyFileObject(String code) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.code = code;
        }

        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
    
    public void test126218() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        String code = "class Test {java.util.List l;}";
        final AtomicInteger count = new AtomicInteger();
        DiagnosticListener<JavaFileObject> dl = new DiagnosticListener<JavaFileObject>() {
            public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                if ("compiler.warn.raw.class.use".equals(diagnostic.getCode())) {
                    count.incrementAndGet();
                }
            }
        };
        final JavaCompiler tool18 = ToolProvider.getSystemJavaCompiler();
        assert tool18 != null;
        final JavacTask ct18 = (JavacTask) tool18.getTask(null, null, dl, Arrays.asList("-bootclasspath", bootPath, "-Xlint:rawtypes", "-source", "1.8", "-XDide"), null, Arrays.asList(new MyFileObject(code)));
        ct18.analyze();
        assertEquals(1, count.get());
        count.set(0);

        final JavaCompiler tool13 = ToolProvider.getSystemJavaCompiler();
        assert tool13 != null;
        final JavacTask ct13 = (JavacTask) tool13.getTask(null, null, dl, Arrays.asList("-bootclasspath", bootPath, "-Xlint:rawtypes", "-source", "1.3", "-XDide"), null, Arrays.asList(new MyFileObject(code)));
        ct13.analyze();
        assertEquals(0, count.get());
    }

}
