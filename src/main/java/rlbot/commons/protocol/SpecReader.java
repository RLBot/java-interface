package rlbot.commons.protocol;

import rlbot.flat.CorePacket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * An InputStream wrapper that follows the RLBot socket protocol.
 */
public class SpecReader {

    private final ByteBuffer buffer = ByteBuffer.allocate(1 << 16);
    private final ReadableByteChannel channel;
    private final InputStream input;

    public SpecReader(InputStream input) {
        this.input = input;
        this.channel = Channels.newChannel(input);
    }

    /**
     * Read one {@link CorePacket}. Blocking.
     * @return the read {@link CorePacket}.
     * @throws IOException if the input stream cannot be read from.
     */
    public CorePacket readOne() throws IOException {
        buffer.clear();
        buffer.limit(2);
        channel.read(buffer);
        buffer.flip();
        buffer.order(ByteOrder.BIG_ENDIAN);
        int size = buffer.getShort();
        if (size < 0) {
            // Fix signedness
            size += 1 << 16;
        }

        buffer.clear();
        buffer.limit(size);
        channel.read(buffer);
        buffer.flip();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return CorePacket.getRootAsCorePacket(buffer);
    }

    /**
     * @return {@code true} if an incoming message is available.
     * @throws IOException
     */
    public boolean anyAvailable() throws IOException {
        return input.available() > 0;
    }
}
