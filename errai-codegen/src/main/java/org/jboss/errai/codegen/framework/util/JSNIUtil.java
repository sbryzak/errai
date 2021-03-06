/*
 * Copyright 2011 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.codegen.framework.util;

import org.jboss.errai.codegen.framework.meta.MetaField;
import org.jboss.errai.codegen.framework.meta.MetaMethod;
import org.jboss.errai.codegen.framework.meta.MetaParameter;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
public class JSNIUtil {
  public static String fieldAccess(MetaField field) {
    return "instance.@" + field.getDeclaringClass().getFullyQualifiedName().replaceAll("\\$", "\\.") + "::"
            + field.getName();
  }

  public static String methodAccess(MetaMethod method) {
    StringBuilder buf = new StringBuilder(
            (method.getReturnType().isVoid() ? "" : "return ") +
                    "instance.@"
                    + method.getDeclaringClass().getFullyQualifiedName().replaceAll("\\$", "\\.") + "::"
                    + method.getName() + "(");

    for (MetaParameter parm : method.getParameters()) {
      buf.append(parm.getType().getInternalName());
    }
    buf.append(")(");

    int length = method.getParameters().length;

    for (int i = 0; i < length; i++) {
      buf.append("a").append(i);
      if (i + 1 < length) buf.append(",");
    }
    buf.append(")");

    return buf.toString();
  }
}
