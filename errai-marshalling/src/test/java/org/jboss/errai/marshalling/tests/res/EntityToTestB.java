package org.jboss.errai.marshalling.tests.res;

import org.jboss.errai.common.client.api.annotations.Portable;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
@Portable
public class EntityToTestB {
  private String foo;
  private int num;

  public EntityToTestB() {
  }

  public EntityToTestB(String foo, int num) {
    this.foo = foo;
    this.num = num;
  }

  public String getFoo() {
    return foo;
  }

  public int getNum() {
    return num;
  }
}
