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

package org.jboss.errai.bus.server.io.buffers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Brock
 */
public interface Buffer {
  public void write(InputStream inputStream, BufferColor bufferColor) throws IOException;

  public void write(int writeSize, InputStream inputStream, BufferColor bufferColor) throws IOException;

  public boolean read(OutputStream outputStream, BufferColor bufferColor) throws IOException;

  public boolean read(OutputStream outputStream, BufferColor bufferColor, BufferCallback callback) throws IOException;

  public boolean read(OutputStream outputStream, BufferColor bufferColor, BufferCallback callback, long sequence) throws IOException;
  

  public boolean readWait(OutputStream outputStream, BufferColor bufferColor) throws IOException, InterruptedException;

  public boolean readWait(TimeUnit unit, long time, OutputStream outputStream, BufferColor bufferColor) throws IOException, InterruptedException;

  public boolean readWait(OutputStream outputStream, BufferColor bufferColor, BufferCallback callback) throws IOException, InterruptedException;

  public boolean readWait(TimeUnit unit, long time, OutputStream outputStream, BufferColor bufferColor, BufferCallback callback) throws IOException, InterruptedException;

  public long getHeadSequence();
  
  public int getHeadPositionBytes();
  
  public int getBufferSize();
  
  public int getTotalSegments();
  
  public int getSegmentSize();
  
}
