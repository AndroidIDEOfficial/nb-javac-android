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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;
import org.junit.Ignore;

/**
 *
 * @author lahvac
 */
public class FlagsTest extends TestCase {
    
    public FlagsTest(String testName) {
        super(testName);
    }

    private static final Set<String> ignoredFields = new HashSet<String>(Arrays.asList("ACC_SUPER", "ACC_BRIDGE", "ACC_VARARGS", "ACC_MODULE", "ACC_DEFENDER", "BAD_OVERRIDE", "ReceiverParamFlags", "BODY_ONLY_FINALIZE"));
    @Ignore
    public void testCheckFlagsNotClashing() throws Exception {
        Map<Long, String> value2Name = new HashMap<Long, String>();

        for (Field f : Flags.class.getDeclaredFields()) {
            if (   !Modifier.isStatic(f.getModifiers())
                || !Modifier.isPublic(f.getModifiers())
                || ignoredFields.contains(f.getName())
                || Long.bitCount(f.getLong(null)) != 1) {
                continue;
            }

            long value = f.getLong(null);

            if (value2Name.containsKey(value)) {
                throw new IllegalStateException("Value clash between " + value2Name.get(value) + " and " + f.getName());
            }

            value2Name.put(value, f.getName());
        }
    }
}
