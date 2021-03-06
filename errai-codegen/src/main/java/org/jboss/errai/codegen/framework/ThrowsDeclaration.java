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

package org.jboss.errai.codegen.framework;

import org.jboss.errai.codegen.framework.builder.callstack.LoadClassReference;
import org.jboss.errai.codegen.framework.meta.MetaClass;
import org.jboss.errai.codegen.framework.meta.MetaClassFactory;

/**
 * @author Christian Sadilek <csadilek@redhat.com>
 */
public class ThrowsDeclaration extends AbstractStatement {
  private MetaClass[] exceptionTypes;

  private ThrowsDeclaration(MetaClass[] exceptionTypes) {
    this.exceptionTypes = exceptionTypes;
  }

  public static ThrowsDeclaration of(Class<? extends Throwable>... exceptionTypes) {
    return new ThrowsDeclaration(MetaClassFactory.fromClassArray(exceptionTypes));
  }

  public static ThrowsDeclaration of(MetaClass... exceptionTypes) {
    return new ThrowsDeclaration(exceptionTypes);
  }

  public static ThrowsDeclaration none() {
    return new ThrowsDeclaration(new MetaClass[0]);
  }

  public boolean isEmpty() {
    return (exceptionTypes == null) || (exceptionTypes.length == 0);
  }

  public MetaClass[] getExceptionTypes() {
    return exceptionTypes;
  }

  String generatedCache;

  @Override
  public String generate(Context context) {
    if (generatedCache != null) return generatedCache;
    StringBuilder buf = new StringBuilder(128);
    for (int i = 0; i < exceptionTypes.length; i++) {
      if (i == 0) {
        buf.append("throws ");
      }

      buf.append(LoadClassReference.getClassReference(exceptionTypes[i], context));

      if (i + 1 < exceptionTypes.length) {
        buf.append(", ");
      }
    }
    return generatedCache = buf.toString();
  }
}