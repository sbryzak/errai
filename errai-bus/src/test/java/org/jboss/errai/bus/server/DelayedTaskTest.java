package org.jboss.errai.bus.server;

import java.util.Timer;
import java.util.TimerTask;

import org.jboss.errai.bus.client.api.AbstractAsyncTaskTest;
import org.jboss.errai.bus.client.api.AsyncTask;
import org.jboss.errai.bus.client.api.base.TimeUnit;
import org.jboss.errai.bus.server.async.scheduling.PooledExecutorService;

/**
 * Tests the DelayedTask implementation of AsyncTask.
 *
 * @author Jonathan Fuerth <jfuerth@gmail.com>
 */
public class DelayedTaskTest extends AbstractAsyncTaskTest {

  private PooledExecutorService service;
  private CountingRunnable latestRunnable;

  @Override
  protected AsyncTask getTaskUnderTest(CountingRunnable task) {
    latestRunnable = task;
    assertEquals("Hey, you can't pass me a used runnable!", 0, task.getRunCount());
    return service.schedule(task, TimeUnit.MILLISECONDS, 500);
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    service = new PooledExecutorService(10);
  }

  @Override
  protected void gwtTearDown() throws Exception {
    service.shutdown();
    super.gwtTearDown();
  }

  @Override
  protected void runAfterTaskFinished(final Runnable r) {
    final Timer t = new Timer();
    TimerTask tt = new TimerTask() {

      @Override
      public void run() {
        if (latestRunnable.getRunCount() > 0) {
          service.shutdown();
          r.run();
        } else {
          t.schedule(this, 100);
        }
      }
    };
    t.schedule(tt, 120);
  }

}
