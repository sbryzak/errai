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

package org.jboss.errai.bus.client.tests;

import org.jboss.errai.bus.client.ErraiBus;
import org.jboss.errai.bus.client.framework.ClientMessageBus;
import org.jboss.errai.bus.client.framework.ClientMessageBusImpl;
import org.jboss.errai.bus.client.framework.LogAdapter;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;

/**
 * Base test class for testing ErraiBus-based code. Located in the main distribution so it can be extended
 * by other modules.
 */
public abstract class AbstractErraiTest extends GWTTestCase {
  protected static ClientMessageBus bus;

  static {
    System.out.println("REMEMBER! Bus tests will not succeed if: \n" +
        "1. You do not run the unit tests with the flag: -Dorg.jboss.errai.bus.do_long_poll=false \n" +
        "2. You do not have the main and test source directories in the runtime classpath");

  }

  @Override
  protected void gwtSetUp() throws Exception {

    System.out.println("set-up");
    if (bus == null) {
      System.out.println("GET()");
      bus = (ClientMessageBusImpl) ErraiBus.get();
      bus.setLogAdapter(new LogAdapter() {
        public void warn(String message) {
          System.out.println("WARN: " + message);
        }

        public void info(String message) {
          System.out.println("INFO: " + message);
        }

        public void debug(String message) {
          System.out.println("DEBUG: " + message);
        }

        public void error(String message, Throwable t) {
          System.out.println("ERROR: " + message);
          if (t != null) t.printStackTrace();
        }
      });
    }
    else {
      if (!bus.isInitialized()) {
        System.out.println("reinit-bus");
        bus.init();
      }
      else {
        System.out.println("bus-already-initialized");
      }
    }
  }

  @Override
  protected void gwtTearDown() throws Exception {
     bus.stop(true);
  }

  protected void runAfterInit(final Runnable r) {
    final Timer t = new Timer() {

      @Override
      public void run() {
        ClientMessageBus b = (ClientMessageBus) bus;
        if (b != null && b.isInitialized()) {
          r.run();
        } else {
          // poll again later
          schedule(500);
        }
      }
    };
    t.schedule(100);
    delayTestFinish(90000);
  }
}
