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

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTaskImpl;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import junit.framework.TestCase;

/**
 *
 * @author Jan Lahoda
 */
public class AnnotateTest extends TestCase {

    public AnnotateTest(String testName) {
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

    public void testNotImportedAnnotationsAttributed() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; @Retention(RetentionPolicy.CLASS) public class Test {}";

        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();

        ct.analyze();

        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitIdentifier(IdentifierTree node, Void p) {
                if (node.getName().toString().startsWith("Retention")) {
                    assertNotNull(node.toString(), Trees.instance(ct).getElement(getCurrentPath()));
                }
                return super.visitIdentifier(node, p);
            }
        }.scan(cut, null);
    }

    public void testNotImportedAnnotationsAttributed156131() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; @TransformationSet({@Transformation(displayName=\"\")}) public class Test {}";

        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();

        ct.analyze();

        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitIdentifier(IdentifierTree node, Void p) {
                if (node.getName().toString().startsWith("Transformation")) {
                    assertNotNull(node.toString(), Trees.instance(ct).getElement(getCurrentPath()));
                }
                return super.visitIdentifier(node, p);
            }
        }.scan(cut, null);
    }
    
    public void testUnresolvableAttributeType() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String codeAnnotation = "package test; @interface A { E e(); } @interface E { }";

        final JavacTaskImpl annotation = (JavacTaskImpl)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov", "-d", workingDir.getAbsolutePath()), null, Arrays.asList(new MyFileObject(codeAnnotation)));
        
        annotation.generate();

        assertTrue(new File(workingDir, "test/E.class").delete());
        
        String code = "package test; @A(e = @Transformation(displayName=\"\") ) class Test {}";

        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov", "-classpath", workingDir.getAbsolutePath()), null, Arrays.asList(new MyFileObject(code)));
        
        CompilationUnitTree cut = ct.parse().iterator().next();

        ct.analyze();

        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitIdentifier(IdentifierTree node, Void p) {
                if (node.getName().toString().startsWith("Transformation")) {
                    assertNotNull(node.toString(), Trees.instance(ct).getElement(getCurrentPath()));
                }
                return super.visitIdentifier(node, p);
            }
        }.scan(cut, null);
    }

    public void test199020a() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; @Undefined1(v=\"\", {@Undefined2}) public class Test {}";

        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();

        ct.analyze();
        
        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitIdentifier(IdentifierTree node, Void p) {
                if (node.getName().toString().startsWith("Undefined")) {
                    assertNotNull(node.toString(), Trees.instance(ct).getElement(getCurrentPath()));
                }
                return super.visitIdentifier(node, p);
            }
        }.scan(cut, null);
    }
    
    private File workingDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        workingDir = File.createTempFile("AnnotateTest", "");

        workingDir.delete();
        workingDir.mkdirs();
    }

    @Override
    protected void tearDown() throws Exception {
        deleteRecursively(workingDir);
        super.tearDown();
    }

    private void deleteRecursively(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                deleteRecursively(c);
            }
        }
        
        f.delete();
    }
    
}
