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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdkx.annotation.processing.AbstractProcessor;
import jdkx.annotation.processing.RoundEnvironment;
import jdkx.annotation.processing.SupportedSourceVersion;
import jdkx.lang.model.SourceVersion;
import jdkx.lang.model.element.TypeElement;
import jdkx.lang.model.util.ElementFilter;
import jdkx.tools.FileObject;
import jdkx.tools.StandardLocation;

/**
 *
 * @author lahvac
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ClassBasedAP extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement te : ElementFilter.typesIn(roundEnv.getRootElements())) {
            try {
                FileObject jfo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", te.getQualifiedName() + ".txt");

                jfo.openWriter().close();
            } catch (IOException ex) {
                Logger.getLogger(ClassBasedAP.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    private static final Set<String> SUPPORTED_ANNOTATIONS = new HashSet<String>(Arrays.asList("*"));

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return SUPPORTED_ANNOTATIONS;
    }

}
