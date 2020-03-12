/*
 * Copyright (c) 2020 Eike Stepper (Loehne, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.net4j.ws.jetty;

import org.eclipse.net4j.Net4jUtil;
import org.eclipse.net4j.buffer.IBuffer;
import org.eclipse.net4j.channel.ChannelException;
import org.eclipse.net4j.connector.ConnectorException;
import org.eclipse.net4j.internal.ws.WSAcceptor;
import org.eclipse.net4j.internal.ws.WSAcceptorManager;
import org.eclipse.net4j.internal.ws.WSConnector;
import org.eclipse.net4j.internal.ws.bundle.OM;
import org.eclipse.net4j.protocol.IProtocol;
import org.eclipse.net4j.util.HexUtil;
import org.eclipse.net4j.util.concurrent.ISynchronizer;
import org.eclipse.net4j.util.concurrent.SynchronizingCorrelator;
import org.eclipse.net4j.util.concurrent.TimeoutRuntimeException;
import org.eclipse.net4j.util.om.OMPlatform;
import org.eclipse.net4j.util.om.trace.ContextTracer;
import org.eclipse.net4j.ws.IWSConnector;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.spi.net4j.InternalChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Eike Stepper
 */
public class Net4jWebSocket implements WebSocketListener
{
  private static final long SESSION_IDLE_TIMEOUT = OMPlatform.INSTANCE.getProperty("org.eclipse.net4j.ws.jetty.Net4jWebSocket.sessionIdleTimeout", 30000);

  private static final String MAGIC_PREFIX = HexUtil.bytesToHex("stepper".getBytes());

  private static final int MAGIC_PREFIX_LENGTH = MAGIC_PREFIX.length();

  private static final byte OPCODE_NEGOTIATION = 1;

  private static final byte OPCODE_REGISTRATION = 2;

  private static final byte OPCODE_REGISTRATION_ACK = 3;

  private static final byte OPCODE_DEREGISTRATION = 4;

  private static final Pattern REGISTRATION_PATTERN = Pattern.compile("([^|]+)\\|([^|]+)\\|(.*)");

  private static final Pattern REGISTRATION_ACK_PATTERN = Pattern.compile("([^|]+)\\|(.*)");

  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG, Net4jWebSocket.class);

  private static final String SUCCESS = "Success";

  /**
   * Has next to no impact on performance.
   */
  private static final boolean ASYNC = true;

  private static final boolean TEXT = false;

  private static final short CONTROL_CHANNEL_INDEX = 0;

  private static Timer timer;

  private static int timerClients;

  private final SynchronizingCorrelator<Short, String> acknowledgements = new SynchronizingCorrelator<Short, String>();

  private volatile WSConnector connector;

  private volatile Session session;

  private TimerTask pongTask;

  public Net4jWebSocket()
  {
  }

  public Net4jWebSocket(IWSConnector connector)
  {
    this.connector = (WSConnector)connector;
  }

  public IWSConnector getConnector()
  {
    return connector;
  }

  public Session getSession()
  {
    return session;
  }

  public boolean isClient()
  {
    return pongTask != null;
  }

  @Override
  public void onWebSocketConnect(Session session)
  {
    this.session = session;
    session.setIdleTimeout(SESSION_IDLE_TIMEOUT);

    boolean client = connector != null;
    if (client)
    {
      pongTask = new TimerTask()
      {
        @Override
        public void run()
        {
          try
          {
            Session session = getSession();
            if (session != null)
            {
              session.getRemote().sendPong(null);
            }
          }
          catch (IOException ex)
          {
            OM.LOG.warn(ex);
            // TODO Close the connector?
          }
        }
      };

      acquireTimer().scheduleAtFixedRate(pongTask, 20000, 20000);
    }
    else
    {
      // Only create/wire a WSConnector on the server side.
      String acceptorName = session.getUpgradeRequest().getHeader(WSConnector.ACCEPTOR_NAME_HEADER);
      WSAcceptor acceptor = WSAcceptorManager.INSTANCE.getAcceptor(acceptorName);
      if (acceptor == null)
      {
        if (TRACER.isEnabled())
        {
          TRACER.format("Acceptor {0} not found", acceptorName); //$NON-NLS-1$
        }

        session.close(1011, "Acceptor not found");
        return;
      }

      connector = acceptor.handleAccept(this);
    }

    if (TRACER.isEnabled())
    {
      TRACER.format("Connection established: {0}", connector); //$NON-NLS-1$
    }
  }

  public void close()
  {
    if (session != null)
    {
      session.close();
      session = null;
    }
  }

  @Override
  public void onWebSocketClose(int statusCode, String reason)
  {
    session = null;

    if (TRACER.isEnabled())
    {
      TRACER.format("Connection closed: {0}", connector); //$NON-NLS-1$
    }

    if (pongTask != null)
    {
      pongTask.cancel();
      pongTask = null;

      releaseTimer();
    }

    if (connector != null)
    {
      connector.inverseClose();
      connector = null;
    }
  }

  private synchronized Timer acquireTimer()
  {
    if (timer == null)
    {
      timer = new Timer(true);
    }

    ++timerClients;
    return timer;
  }

  private synchronized void releaseTimer()
  {
    if (--timerClients == 0)
    {
      timer.cancel();
      timer = null;
    }
  }

  private synchronized void sendString(String message) throws IOException
  {
    if (ASYNC)
    {
      session.getRemote().sendString(message, new WriteCallback()
      {
        @Override
        public void writeSuccess()
        {
          // Do nothing.
        }

        @Override
        public void writeFailed(Throwable ex)
        {
          OM.LOG.error(ex);
          // TODO Close the channel?
        }
      });
    }
    else
    {
      session.getRemote().sendString(message);
    }
  }

  private synchronized void sendBytes(IBuffer buffer) throws IOException
  {
    ByteBuffer byteBuffer = buffer.getByteBuffer();

    if (ASYNC)
    {
      session.getRemote().sendBytes(byteBuffer, new WriteCallback()
      {
        @Override
        public void writeSuccess()
        {
          buffer.release();
        }

        @Override
        public void writeFailed(Throwable ex)
        {
          OM.LOG.error(ex);
          buffer.release();
          // TODO Close the channel?
        }
      });
    }
    else
    {
      session.getRemote().sendBytes(byteBuffer);
      buffer.release();
    }
  }

  public void registerChannel(short channelID, long timeout, IProtocol<?> protocol) throws IOException
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Registering channel {0} with protocol {1}", channelID, protocol); //$NON-NLS-1$
    }

    assertValidChannelID(channelID);
    ISynchronizer<String> acknowledgement = acknowledgements.correlate(channelID);

    int protocolVersion = Net4jUtil.getProtocolVersion(protocol);
    String protocolID = Net4jUtil.getProtocolID(protocol);

    if (TEXT)
    {
      sendString(MAGIC_PREFIX + OPCODE_REGISTRATION + channelID + "|" + protocolVersion + "|" + protocolID);
    }
    else
    {
      IBuffer buffer = provideBuffer();
      ByteBuffer byteBuffer = buffer.startPutting(CONTROL_CHANNEL_INDEX);
      byteBuffer.put(OPCODE_REGISTRATION);
      byteBuffer.putShort(channelID);
      byteBuffer.putInt(protocolVersion);
      org.eclipse.internal.net4j.buffer.BufferUtil.putString(byteBuffer, protocolID, false);
      sendBuffer(buffer);
    }

    String error = acknowledgement.get(timeout);
    if (error == null)
    {
      throw new TimeoutRuntimeException(MessageFormat.format("Registration timeout after {0} milliseconds", timeout)); //$NON-NLS-1$
    }

    if (error != SUCCESS)
    {
      throw new ChannelException("Failed to register channel with peer: " + error); //$NON-NLS-1$
    }
  }

  private void onRegistration(short channelID, int protocolVersion, String protocolID)
  {
    assertConnected();
    assertValidChannelID(channelID);
    String error = "";

    try
    {
      InternalChannel channel = connector.inverseOpenChannel(channelID, protocolID, protocolVersion);
      if (channel == null)
      {
        throw new ConnectorException("Could not open channel");
      }
    }
    catch (Exception ex)
    {
      error = ex.getMessage();
      if (error == null)
      {
        error = "Unknown error";
      }

      if (TRACER.isEnabled())
      {
        TRACER.trace("Problem during channel registration", ex); //$NON-NLS-1$
      }
    }

    acknowledgeRegistration(channelID, error);
  }

  private void acknowledgeRegistration(short channelID, String error)
  {
    try
    {
      if (TEXT)
      {
        sendString(MAGIC_PREFIX + OPCODE_REGISTRATION_ACK + channelID + "|" + error);
      }
      else
      {
        IBuffer buffer = provideBuffer();
        ByteBuffer byteBuffer = buffer.startPutting(CONTROL_CHANNEL_INDEX);
        byteBuffer.put(OPCODE_REGISTRATION_ACK);
        byteBuffer.putShort(channelID);
        org.eclipse.internal.net4j.buffer.BufferUtil.putString(byteBuffer, error, true);
        sendBuffer(buffer);
      }
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
  }

  private void onRegistrationAck(short channelID, String error)
  {
    assertConnected();
    assertValidChannelID(channelID);

    if (error != null && error.isEmpty())
    {
      error = SUCCESS;
    }

    acknowledgements.put(channelID, error);
  }

  public void deregisterChannel(short channelID) throws IOException
  {
    if (session == null)
    {
      // This is an inverse deregistration.
      return;
    }

    if (TRACER.isEnabled())
    {
      TRACER.format("Deregistering channel {0}", channelID); //$NON-NLS-1$
    }

    assertValidChannelID(channelID);

    if (TEXT)
    {
      sendString(MAGIC_PREFIX + OPCODE_DEREGISTRATION + channelID);
    }
    else
    {
      IBuffer buffer = provideBuffer();
      ByteBuffer byteBuffer = buffer.startPutting(CONTROL_CHANNEL_INDEX);
      byteBuffer.put(OPCODE_DEREGISTRATION);
      byteBuffer.putShort(channelID);
      sendBuffer(buffer);
    }
  }

  private void onDeregistration(short channelID)
  {
    try
    {
      assertConnected();
      assertValidChannelID(channelID);
      connector.inverseCloseChannel(channelID);
    }
    catch (Throwable ex)
    {
      ex.printStackTrace();
      if (TRACER.isEnabled())
      {
        TRACER.trace("Problem during channel deregistration", ex); //$NON-NLS-1$
      }
    }
  }

  public void sendBuffer(IBuffer buffer)
  {
    try
    {
      ByteBuffer byteBuffer = buffer.getByteBuffer();
      int position = byteBuffer.position();
      if (position <= IBuffer.HEADER_SIZE)
      {
        buffer.release();

        if (position == IBuffer.HEADER_SIZE)
        {
          boolean eos = buffer.isEOS();
          if (eos)
          {
            // TODO No payload but EOS!
            throw new Error("No payload but EOS!");
          }

          return;
        }

        throw new IllegalArgumentException("Illegal buffer size: " + position);
      }

      int payloadSize = position - IBuffer.HEADER_SIZE;
      if (buffer.isEOS())
      {
        payloadSize = -payloadSize;
      }

      byteBuffer.putShort(IBuffer.CHANNEL_ID_POS, buffer.getChannelID());
      byteBuffer.putShort(IBuffer.PAYLOAD_SIZE_POS, (short)payloadSize);
      byteBuffer.flip();

      sendBytes(buffer);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }

  private void onReceiveBuffer(byte[] payload, int offset, int len)
  {
    if (len < IBuffer.HEADER_SIZE)
    {
      if (len != 0)
      {
        throw new IllegalArgumentException("Payload length: " + len);
      }

      if (TRACER.isEnabled())
      {
        TRACER.trace("Received empty buffer!"); //$NON-NLS-1$
      }

      return;
    }

    IBuffer buffer = provideBuffer();
    ByteBuffer byteBuffer = buffer.getByteBuffer();
    byteBuffer.put(payload, offset, len);
    byteBuffer.flip();

    short channelID = byteBuffer.getShort();

    if (!TEXT && channelID == CONTROL_CHANNEL_INDEX)
    {
      byteBuffer.position(IBuffer.HEADER_SIZE);

      try
      {
        byte opcode = byteBuffer.get();
        switch (opcode)
        {
        case OPCODE_REGISTRATION:
        {
          channelID = buffer.getShort();
          int protocolVersion = buffer.getInt();
          String protocolID = buffer.getString();
          onRegistration(channelID, protocolVersion, protocolID);
          break;
        }

        case OPCODE_REGISTRATION_ACK:
        {
          channelID = buffer.getShort();
          String error = buffer.getString();
          onRegistrationAck(channelID, error);
          break;
        }

        case OPCODE_DEREGISTRATION:
        {
          channelID = buffer.getShort();
          onDeregistration(channelID);
          break;
        }

        default:
          break;
        }
      }
      finally
      {
        buffer.release();
      }

      return;
    }

    short payloadSize = byteBuffer.getShort();
    if (payloadSize < 0)
    {
      buffer.setEOS(true);
    }

    ((org.eclipse.internal.net4j.buffer.Buffer)buffer).setChannelID(channelID);
    ((org.eclipse.internal.net4j.buffer.Buffer)buffer).setState(org.eclipse.net4j.buffer.BufferState.PUTTING);
    byteBuffer.position(IBuffer.HEADER_SIZE);

    InternalChannel channel = connector.getChannel(channelID);
    if (channel != null)
    {
      channel.handleBufferFromMultiplexer(buffer);
    }
    else
    {
      if (TRACER.isEnabled())
      {
        TRACER.trace("Discarding buffer from unknown channel"); //$NON-NLS-1$
      }

      buffer.release();
    }
  }

  @Override
  public void onWebSocketText(String message)
  {
    if (!TEXT)
    {
      return;
    }

    if (message.length() < MAGIC_PREFIX_LENGTH + 1 || !message.startsWith(MAGIC_PREFIX))
    {
      return;
    }

    char opcode = message.charAt(MAGIC_PREFIX_LENGTH);
    message = message.substring(MAGIC_PREFIX_LENGTH + 1);

    switch (opcode)
    {
    case '0' + OPCODE_NEGOTIATION:
      onNegotiation(message);
      break;

    case '0' + OPCODE_REGISTRATION:
    {
      Matcher matcher = REGISTRATION_PATTERN.matcher(message);
      if (!matcher.matches())
      {
        throw new IllegalArgumentException("Invalid channel registration message: " + message);
      }

      short channelID = Short.parseShort(matcher.group(1));
      int protocolVersion = Integer.parseInt(matcher.group(2));
      String protocolID = matcher.group(3);
      onRegistration(channelID, protocolVersion, protocolID);
      break;
    }

    case '0' + OPCODE_REGISTRATION_ACK:
    {
      Matcher matcher = REGISTRATION_ACK_PATTERN.matcher(message);
      if (!matcher.matches())
      {
        throw new IllegalArgumentException("Invalid channel registration ack message: " + message);
      }

      short channelID = Short.parseShort(matcher.group(1));
      String error = matcher.group(2);
      onRegistrationAck(channelID, error);
      break;
    }

    case '0' + OPCODE_DEREGISTRATION:
    {
      short channelID = Short.parseShort(message);
      onDeregistration(channelID);
      break;
    }

    default:
      break;
    }
  }

  @Override
  public void onWebSocketBinary(byte[] payload, int offset, int len)
  {
    onReceiveBuffer(payload, offset, len);
  }

  @Override
  public void onWebSocketError(Throwable cause)
  {
    cause.printStackTrace();
  }

  private void onNegotiation(String message)
  {
  }

  private void assertNegotiating()
  {
    if (!connector.isNegotiating())
    {
      connector.deactivate();
      throw new IllegalStateException("Connector is not negotiating");
    }
  }

  private void assertConnected()
  {
    if (!connector.isConnected())
    {
      throw new IllegalStateException("Connector is not connected");
    }
  }

  private void assertValidChannelID(short channelID)
  {
    if (channelID < 1)
    {
      throw new IllegalArgumentException("Bad channelID " + channelID);
    }
  }

  protected IBuffer provideBuffer()
  {
    return connector.getConfig().getBufferProvider().provideBuffer();
  }
}
