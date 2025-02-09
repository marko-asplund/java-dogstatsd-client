package com.timgroup.statsd;

import jnr.unixsocket.UnixSocketAddress;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;

public class NonBlockingStatsDClientBuilder implements Cloneable {

    /**
     * 1400 chosen as default here so that the number of bytes in a message plus the number of bytes required
     * for additional udp headers should be under the 1500 Maximum Transmission Unit for ethernet.
     * See https://github.com/DataDog/java-dogstatsd-client/pull/17 for discussion.
     */

    public int maxPacketSizeBytes = 0;
    public int port = NonBlockingStatsDClient.DEFAULT_DOGSTATSD_PORT;
    public int telemetryPort = NonBlockingStatsDClient.DEFAULT_DOGSTATSD_PORT;
    public int queueSize = NonBlockingStatsDClient.DEFAULT_QUEUE_SIZE;
    public int timeout = NonBlockingStatsDClient.SOCKET_TIMEOUT_MS;
    public int bufferPoolSize = NonBlockingStatsDClient.DEFAULT_POOL_SIZE;
    public int socketBufferSize = NonBlockingStatsDClient.SOCKET_BUFFER_BYTES;
    public int processorWorkers = NonBlockingStatsDClient.DEFAULT_PROCESSOR_WORKERS;
    public int senderWorkers = NonBlockingStatsDClient.DEFAULT_SENDER_WORKERS;
    public boolean blocking = NonBlockingStatsDClient.DEFAULT_BLOCKING;
    public boolean enableTelemetry = NonBlockingStatsDClient.DEFAULT_ENABLE_TELEMETRY;
    public boolean enableAggregation = NonBlockingStatsDClient.DEFAULT_ENABLE_AGGREGATION;
    public int telemetryFlushInterval = Telemetry.DEFAULT_FLUSH_INTERVAL;
    public int aggregationFlushInterval = StatsDAggregator.DEFAULT_FLUSH_INTERVAL;
    public int aggregationShards = StatsDAggregator.DEFAULT_SHARDS;

    public Callable<SocketAddress> addressLookup;
    public Callable<SocketAddress> telemetryAddressLookup;

    public String hostname;
    public String telemetryHostname;
    public String namedPipe;
    public String prefix;
    public String entityID;
    public String[] constantTags;

    public StatsDClientErrorHandler errorHandler;
    public ThreadFactory threadFactory;

    public NonBlockingStatsDClientBuilder() { }

    public NonBlockingStatsDClientBuilder port(int val) {
        port = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder telemetryPort(int val) {
        telemetryPort = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder queueSize(int val) {
        queueSize = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder timeout(int val) {
        timeout = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder bufferPoolSize(int val) {
        bufferPoolSize = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder socketBufferSize(int val) {
        socketBufferSize = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder maxPacketSizeBytes(int val) {
        maxPacketSizeBytes = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder processorWorkers(int val) {
        processorWorkers = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder senderWorkers(int val) {
        senderWorkers = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder blocking(boolean val) {
        blocking = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder addressLookup(Callable<SocketAddress> val) {
        addressLookup = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder telemetryAddressLookup(Callable<SocketAddress> val) {
        telemetryAddressLookup = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder hostname(String val) {
        hostname = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder telemetryHostname(String val) {
        telemetryHostname = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder namedPipe(String val) {
        namedPipe = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder prefix(String val) {
        prefix = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder entityID(String val) {
        entityID = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder constantTags(String... val) {
        constantTags = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder errorHandler(StatsDClientErrorHandler val) {
        errorHandler = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder enableTelemetry(boolean val) {
        enableTelemetry = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder enableAggregation(boolean val) {
        enableAggregation = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder telemetryFlushInterval(int val) {
        telemetryFlushInterval = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder aggregationFlushInterval(int val) {
        aggregationFlushInterval = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder aggregationShards(int val) {
        aggregationShards = val;
        return this;
    }

    public NonBlockingStatsDClientBuilder threadFactory(ThreadFactory val) {
        threadFactory = val;
        return this;
    }

    /**
     * NonBlockingStatsDClient factory method.
     * @return the built NonBlockingStatsDClient.
     */
    public NonBlockingStatsDClient build() throws StatsDClientException {
        return new NonBlockingStatsDClient(resolve());
    }

    /**
     * Creates a copy of this builder with any implicit elements resolved.
     * @return the resolved copy of the builder.
     */
    protected NonBlockingStatsDClientBuilder resolve() {
        NonBlockingStatsDClientBuilder resolved;

        try {
            resolved = (NonBlockingStatsDClientBuilder) clone();
        } catch (CloneNotSupportedException e) {
            throw new UnsupportedOperationException("clone");
        }

        int packetSize = maxPacketSizeBytes;
        Callable<SocketAddress> lookup = addressLookup;

        if (lookup == null) {
            String namedPipeFromEnv = System.getenv(NonBlockingStatsDClient.DD_NAMED_PIPE_ENV_VAR);
            String resolvedNamedPipe = namedPipe == null ? namedPipeFromEnv : namedPipe;
            
            if (resolvedNamedPipe == null) {
                lookup = staticStatsDAddressResolution(hostname, port);
            } else {
                lookup = staticNamedPipeResolution(resolvedNamedPipe);
            }
        }

        if (packetSize == 0) {
            packetSize = (port == 0) ? NonBlockingStatsDClient.DEFAULT_UDS_MAX_PACKET_SIZE_BYTES :
                NonBlockingStatsDClient.DEFAULT_UDP_MAX_PACKET_SIZE_BYTES;
        }

        Callable<SocketAddress> telemetryLookup = telemetryAddressLookup;
        if (telemetryLookup == null) {
            if (telemetryHostname == null) {
                telemetryLookup = lookup;
            } else {
                telemetryLookup = staticStatsDAddressResolution(telemetryHostname, telemetryPort);
            }
        }

        resolved.maxPacketSizeBytes = packetSize;
        resolved.addressLookup = lookup;
        resolved.telemetryAddressLookup = telemetryLookup;

        return resolved;
    }

    /**
     * Create dynamic lookup for the given host name and port.
     *
     * @param hostname
     *     the host name of the targeted StatsD server. If the environment variable
     *     "DD_AGENT_HOST" is set, this parameter is overwritten by the environment
     *     variable value.
     * @param port
     *     the port of the targeted StatsD server. If the environment variable
     *     "DD_DOGSTATSD_PORT" is set, this parameter is overwritten by the environment
     *     variable value.
     * @return a function to perform the lookup
     */
    public static Callable<SocketAddress> volatileAddressResolution(final String hostname, final int port) {
        return new Callable<SocketAddress>() {
            @Override public SocketAddress call() throws UnknownHostException {
                if (port == 0) { // Hostname is a file path to the socket
                    return new UnixSocketAddress(hostname);
                } else {
                    return new InetSocketAddress(InetAddress.getByName(hostname), port);
                }
            }
        };
    }

    /**
     * Lookup the address for the given host name and cache the result.
     *
     * @param hostname the host name of the targeted StatsD server
     * @param port     the port of the targeted StatsD server
     * @return a function that cached the result of the lookup
     * @throws Exception if the lookup fails, i.e. {@link UnknownHostException}
     */
    public static Callable<SocketAddress> staticAddressResolution(final String hostname, final int port)
            throws Exception {
        final SocketAddress address = volatileAddressResolution(hostname, port).call();
        return new Callable<SocketAddress>() {
            @Override public SocketAddress call() {
                return address;
            }
        };
    }

    protected static Callable<SocketAddress> staticStatsDAddressResolution(String hostname, int port)
            throws StatsDClientException {
        try {
            if (hostname == null) {
                hostname = getHostnameFromEnvVar();
                port = getPortFromEnvVar(port);
            }

            return staticAddressResolution(hostname, port);
        } catch (final Exception e) {
            throw new StatsDClientException("Failed to lookup StatsD host", e);
        }
    }

    protected static Callable<SocketAddress> staticNamedPipeResolution(String namedPipe) {
        final NamedPipeSocketAddress socketAddress = new NamedPipeSocketAddress(namedPipe);
        return new Callable<SocketAddress>() {
            @Override public SocketAddress call() {
                return socketAddress;
            }
        };
    }

    /**
     * Retrieves host name from the environment variable "DD_AGENT_HOST".
     *
     * @return host name from the environment variable "DD_AGENT_HOST"
     *
     * @throws StatsDClientException if the environment variable is not set
     */
    private static String getHostnameFromEnvVar() {
        final String hostname = System.getenv(NonBlockingStatsDClient.DD_AGENT_HOST_ENV_VAR);
        if (hostname == null) {
            throw new StatsDClientException("Failed to retrieve agent hostname from environment variable", null);
        }
        return hostname;
    }

    /**
     * Retrieves dogstatsd port from the environment variable "DD_DOGSTATSD_PORT".
     *
     * @return dogstatsd port from the environment variable "DD_DOGSTATSD_PORT"
     *
     * @throws StatsDClientException if the environment variable is an integer
     */
    private static int getPortFromEnvVar(final int defaultPort) {
        final String statsDPortString = System.getenv(NonBlockingStatsDClient.DD_DOGSTATSD_PORT_ENV_VAR);
        if (statsDPortString == null) {
            return defaultPort;
        } else {
            try {
                final int statsDPort = Integer.parseInt(statsDPortString);
                return statsDPort;
            } catch (final NumberFormatException e) {
                throw new StatsDClientException("Failed to parse "
                        + NonBlockingStatsDClient.DD_DOGSTATSD_PORT_ENV_VAR + "environment variable value", e);
            }
        }
    }


}

