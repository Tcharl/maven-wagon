package org.apache.maven.wagon;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.events.*;
import org.apache.maven.wagon.repository.Repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Implementation of common facilties for Wagon providers.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public abstract class AbstractWagon
    implements Wagon
{
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    protected Repository source;

    protected SessionEventSupport sessionEventSupport = new SessionEventSupport();

    protected TransferEventSupport transferEventSupport = new TransferEventSupport();

    // ----------------------------------------------------------------------
    // Repository
    // ----------------------------------------------------------------------

    public Repository getRepository()
    {
        return source;
    }

    // ----------------------------------------------------------------------
    // Connection
    // ----------------------------------------------------------------------

    public void connect( Repository source ) throws 
      ConnectionException, AuthenticationException
    {
        if ( source == null )
        {
            throw new IllegalStateException( "The repository specified cannot be null." );
        }

        this.source = source;

        fireSessionOpening();

        openConnection();

        fireSessionOpened();
    }

    public void disconnect()
        throws ConnectionException
    {
        fireSessionDisconnecting();

        closeConnection();

        fireSessionDisconnected();
    }
    
    protected abstract void closeConnection() throws ConnectionException;

    // ----------------------------------------------------------------------
    // Stream i/o
    // ----------------------------------------------------------------------

    protected void getTransfer( String resource, File destination, InputStream input, OutputStream output )
        throws TransferFailedException
    {
        fireGetStarted( resource, destination );

        try
        {
            transfer( resource, input, output, TransferEvent.REQUEST_GET );

        }
        catch ( IOException e )
        {
            fireTransferError( resource, e );

            if ( destination.exists() )
            {
                boolean deleted = destination.delete();

                if ( ! deleted )
                {
                    destination.deleteOnExit();
                }
            }

            String msg = "GET request of: " + resource + " from " + source.getName() + "failed";

            throw new TransferFailedException( msg, e );

        }
        finally
        {
            shutdownStream( input );

            shutdownStream( output );

        }

        fireGetCompleted( resource, destination );
    }

    protected void putTransfer(String resource, File source, InputStream input, OutputStream output, boolean closeOutput )
        throws TransferFailedException
    {
        firePutStarted( resource, source );

        try
        {
            transfer( resource, input, output, TransferEvent.REQUEST_PUT );

        }
        catch ( IOException e )
        {
            fireTransferError( resource, e );

            String msg = "PUT request for: " + resource + " to " + source.getName() + "failed";

            throw new TransferFailedException( msg, e );
        }
        finally
        {
            shutdownStream( input );

            if ( closeOutput )
            {
                shutdownStream( output );
            }
        }
        firePutCompleted( resource, source );
    }

    protected void transfer( String resource, InputStream input, OutputStream output, int requestType )
        throws IOException
    {
        byte[] buffer = new byte[ DEFAULT_BUFFER_SIZE ];

        TransferEvent transferEvent = new TransferEvent( this, resource, TransferEvent.TRANSFER_PROGRESS, requestType );

        while ( true )                                               
        {
            int n = input.read( buffer ) ;
            
            if ( n == -1 )
            {
               break;    
            }
            
            // @todo probably new event should be created!!
            
            transferEvent.setData( buffer, n );

            fireTransferProgress( transferEvent );
            
            output.write( buffer, 0, n );

            
        }
    }




    protected void shutdownStream( InputStream inputStream )
    {
        if ( inputStream != null )
        {
            try
            {
                inputStream.close();
            }
            catch ( Exception e )
            {
            }
        }
    }

    protected void shutdownStream( OutputStream outputStream )
    {
        if ( outputStream != null )
        {
            try
            {
                outputStream.close();
            }
            catch ( Exception e )
            {
            }
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected void fireTransferProgress( TransferEvent transferEvent )
    {
        transferEventSupport.fireTransferProgress( transferEvent );
    }

    protected void fireGetCompleted( String resource, File localFile )
    {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent =
            new TransferEvent( this, resource, TransferEvent.TRANSFER_COMPLETED, TransferEvent.REQUEST_GET );

        transferEvent.setTimestamp( timestamp );

        transferEvent.setLocalFile( localFile );
        
        transferEventSupport.fireTransferCompleted( transferEvent );
    }

    protected void fireGetStarted( String resource, File localFile )
    {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent =
            new TransferEvent( this, resource, TransferEvent.TRANSFER_STARTED, TransferEvent.REQUEST_GET );

        transferEvent.setTimestamp( timestamp );
        
        transferEvent.setLocalFile( localFile );

        transferEventSupport.fireTransferStarted( transferEvent );
    }

    protected void firePutCompleted( String resource, File localFile )
    {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent =
            new TransferEvent( this, resource, TransferEvent.TRANSFER_COMPLETED, TransferEvent.REQUEST_PUT );

        transferEvent.setTimestamp( timestamp );
        
        transferEvent.setLocalFile( localFile );

        transferEventSupport.fireTransferCompleted( transferEvent );
    }

    protected void firePutStarted( String resource, File localFile )
    {
        long timestamp = System.currentTimeMillis();

        TransferEvent transferEvent =
            new TransferEvent( this, resource, TransferEvent.TRANSFER_STARTED, TransferEvent.REQUEST_PUT );

        transferEvent.setTimestamp( timestamp );
        
        transferEvent.setLocalFile( localFile );

        transferEventSupport.fireTransferStarted( transferEvent );
    }

    protected void fireSessionDisconnected()
    {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent( this, SessionEvent.SESSION_DISCONNECTED );

        sessionEvent.setTimestamp( timestamp );

        sessionEventSupport.fireSessionDisconnected( sessionEvent );
    }

    protected void fireSessionDisconnecting()
    {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent( this, SessionEvent.SESSION_DISCONNECTING );

        sessionEvent.setTimestamp( timestamp );

        sessionEventSupport.fireSessionDisconnecting( sessionEvent );
    }

    protected void fireSessionLoggedIn()
    {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent( this, SessionEvent.SESSION_LOGGED_IN );

        sessionEvent.setTimestamp( timestamp );

        sessionEventSupport.fireSessionLoggedIn( sessionEvent );
    }

    protected void fireSessionLoggedOff()
    {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent( this, SessionEvent.SESSION_LOGGED_OFF );

        sessionEvent.setTimestamp( timestamp );

        sessionEventSupport.fireSessionLoggedOff( sessionEvent );
    }

    protected void fireSessionOpened()
    {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent( this, SessionEvent.SESSION_OPENED );

        sessionEvent.setTimestamp( timestamp );

        sessionEventSupport.fireSessionOpened( sessionEvent );
    }

    protected void fireSessionOpening()
    {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent( this, SessionEvent.SESSION_OPENING );

        sessionEvent.setTimestamp( timestamp );

        sessionEventSupport.fireSessionOpening( sessionEvent );
    }

    protected void fireSessionConnectionRefused()
    {

        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent( this, SessionEvent.SESSION_CONNECTION_REFUSED );

        sessionEvent.setTimestamp( timestamp );

        sessionEventSupport.fireSessionConnectionRefused( sessionEvent );
    }

    protected void fireSessionError( Exception exception )
    {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent( this, exception );

        sessionEvent.setTimestamp( timestamp );

        sessionEventSupport.fireSessionError( sessionEvent );

    }

    protected void fireTransferDebug( String message )
    {
        transferEventSupport.fireDebug( message );
    }

    protected void fireSessionDebug( String message )
    {
        sessionEventSupport.fireDebug( message );
    }

    public boolean hasTransferListener( TransferListener listener )
    {
        return transferEventSupport.hasTransferListener( listener );
    }

    public void addTransferListener( TransferListener listener )
    {
        transferEventSupport.addTransferListener( listener );
    }

    public void removeTransferListener( TransferListener listener )
    {
        transferEventSupport.removeTransferListener( listener );
    }

    public void addSessionListener( SessionListener listener )
    {
        sessionEventSupport.addSessionListener( listener );
    }

    public boolean hasSessionListener( SessionListener listener )
    {
        return sessionEventSupport.hasSessionListener( listener );
    }

    public void removeSessionListener( SessionListener listener )
    {
        sessionEventSupport.removeSessionListener( listener );
    }

    protected void fireTransferError( String resource, Exception e )
    {
        TransferEvent transferEvent = new TransferEvent( this, resource, e );

        transferEventSupport.fireTransferError( transferEvent );
    }


    public SessionEventSupport getSessionEventSupport()
    {
        return sessionEventSupport;
    }

    public void setSessionEventSupport( SessionEventSupport sessionEventSupport )
    {
        this.sessionEventSupport = sessionEventSupport;
    }

    public TransferEventSupport getTransferEventSupport()
    {
        return transferEventSupport;
    }

    public void setTransferEventSupport( TransferEventSupport transferEventSupport )
    {
        this.transferEventSupport = transferEventSupport;
    }
}
