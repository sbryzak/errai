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

package org.jboss.errai.codegen.framework.exception;

import org.jboss.errai.codegen.framework.meta.MetaClass;
import org.jboss.errai.codegen.framework.util.GenUtil;

/**
 * @author Christian Sadilek <csadilek@redhat.com>
 */
public class UndefinedMethodException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private MetaClass declaringClass;
  private String methodName;
  private MetaClass[] parameterTypes;

  public UndefinedMethodException() {
    super();
  }

  public UndefinedMethodException(String msg) {
    super(msg);
  }

  public UndefinedMethodException(MetaClass declaringClass, String methodName, MetaClass... parameterTypes) {
    super("undefined method: " + methodName + "(" + GenUtil.classesAsStrings(parameterTypes) + "); in class: "
        + declaringClass.getFullyQualifiedName());
    this.declaringClass = declaringClass;
    this.methodName = methodName;
    this.parameterTypes = parameterTypes;
  }

  @Override
  public String toString() {
    return getMessage();
  }

  public MetaClass getDeclaringClass() {
    return declaringClass;
  }

  public String getMethodName() {
    return methodName;
  }

  public MetaClass[] getParameterTypes() {
    return parameterTypes;
  }
}
