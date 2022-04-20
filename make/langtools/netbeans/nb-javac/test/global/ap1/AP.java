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

package global.ap1;

import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

/**
 *
 * @author lahvac
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AP extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(Ann.class)) {
            //XXX: JavacFiler.getElementURLs expects that e.classfile != null
            //workarounding here for the time being
            ClassSymbol s = (ClassSymbol) e;
            if (s.classfile == null) s.classfile = s.sourcefile;
            //XXX end
            
            Ann ann = e.getAnnotation(Ann.class);
            Writer w = null;
            try {
                JavaFileObject jfo = processingEnv.getFiler().createSourceFile(ann.fqnToGenerate(), e);

                w = jfo.openWriter();
                w.write(ann.content());
            } catch (IOException ex) {
                Logger.getLogger(AP.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (w != null) {
                    try {
                        w.close();
                    } catch (IOException ex) {
                        Logger.getLogger(AP.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        return false;
    }

    private static final String ANNOTATION = Ann.class.getName();
    private static final Set<String> SUPPORTED_ANNOTATIONS = new HashSet<String>(Arrays.asList(ANNOTATION));

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return SUPPORTED_ANNOTATIONS;
    }

}
