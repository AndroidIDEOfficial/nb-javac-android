/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * Defines the Language Model, Annotation Processing, and Java Compiler APIs.
 * <p>
 * These APIs model declarations and types of the Java programming language,
 * and define interfaces for tools such as compilers which can be invoked
 * from a program.
 * <p>
 * This module is upgradeable.
 *
 * @moduleGraph
 * @since 9
 */
module java.compiler {
    exports com.itsaky.androidide.config;
    exports com.itsaky.androidide.zipfs2;
    exports javac.internal;
    exports javac.internal.jimage;
    exports javac.internal.jimage.decompressor;
    exports javac.internal.jmod;
    exports javac.internal.jrtfs;
    exports jdkx.annotation.processing;
    exports jdkx.lang.model;
    exports jdkx.lang.model.element;
    exports jdkx.lang.model.type;
    exports jdkx.lang.model.util;
    exports jdkx.tools;

    uses jdkx.tools.DocumentationTool;
    uses jdkx.tools.JavaCompiler;

    opens com.itsaky.androidide.config to jdk.compiler,jdk.jdeps;
}

