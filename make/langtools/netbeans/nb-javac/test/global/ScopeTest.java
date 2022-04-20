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

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import junit.framework.TestCase;

public class ScopeTest extends TestCase {

    public ScopeTest(String name) {
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

    public void testPositionForSuperConstructorCalls() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test;\n" +
                      "public class Test {\n" +
                      "    private void x() {\n" +
                      "        RequestProcessor.post(new Runnable() {\n" +
                      "            public void run() {\n" +
                      "                String test = null;\n" +
                      "                test.length();\n" +
                      "            }\n" +
                      "        });\n" +
                      "    }\n" +
                      "}\n";

        JavacTask ct = (JavacTask)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));

        Iterable<? extends CompilationUnitTree> trees = ct.parse();

        ct.analyze();

        final Trees t = Trees.instance(ct);

        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitIdentifier(IdentifierTree node, Void p) {
                if ("RequestProcessor".equals(node.getName().toString())) {
                    t.getScope(getCurrentPath());
                }
                return super.visitIdentifier(node, p);
            }
        }.scan(trees.iterator().next(), null);

        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitVariable(VariableTree node, Void p) {
                Element e = t.getElement(getCurrentPath());

                assertNotNull(t.getPath(e));
                return super.visitVariable(node, p);
            }
        }.scan(trees.iterator().next(), null);
    }

    public void test142924() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test;\n" +
                      "public class Test {\n" +
                      "    public Unresolved field;" +
                      "}\n";

        JavacTask ct = (JavacTask)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
        ct.parse();
        ct.analyze();

        TypeElement te = ct.getElements().getTypeElement("test.Test");
        for (VariableElement field : ElementFilter.fieldsIn(te.getEnclosedElements())) {
            TypeMirror type = field.asType();
            if (type.getKind() == TypeKind.ERROR) {
                Type.ErrorType et = (Type.ErrorType)type;
                for (Symbol symbol : et.tsym.members().getSymbolsByName(et.tsym.name.table.fromString("XYZ")));
            }
        }
    }

    public void testScope180164() throws IOException {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final String version = System.getProperty("java.vm.specification.version"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test;\n" +
                      "public class Test {\n" +
                       "    private final Object o = new Runnable() {\n" +
                       "        public void run() {\n" +
                       "            String u = null;\n" +
                       "            String n = u;\n" +
                       "        }\n" +
                       "    };\n" +
                      "}\n";

        final JavacTask ct = (JavacTask)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-source", version, "-Xjcov"), null, Arrays.asList(new MyFileObject(code)));
	Iterable<? extends CompilationUnitTree> cut = ct.parse();
        ct.analyze();

	final TreePath[] paths = new TreePath[1];
	final Element[] el = new Element[1];
	
	new TreePathScanner<Void, Void>() {
	    @Override
	    public Void visitIdentifier(IdentifierTree node, Void p) {
		if (node.getName().contentEquals("u")) {
		    paths[0] = getCurrentPath();
		    el[0] = Trees.instance(ct).getElement(getCurrentPath());
		}
		return super.visitIdentifier(node, p);
	    }
	}.scan(cut, null);

	assertNotNull(paths[0]);
	
	assertNotNull(Trees.instance(ct).getPath(el[0]));

	Trees.instance(ct).getScope(paths[0]);

	assertNotNull(Trees.instance(ct).getPath(el[0]));
    }

}
