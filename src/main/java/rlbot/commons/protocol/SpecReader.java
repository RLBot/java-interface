package rlbot.commons.protocol;

import rlbot.flat.CorePacket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class SpecReader {

    private final ByteBuffer buffer = ByteBuffer.allocate(1 << 16);
    private final ReadableByteChannel channel;
    private final InputStream input;

    public SpecReader(InputStream input) {
        this.input = input;
        this.channel = Channels.newChannel(input);
    }

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

    public boolean anyAvailable() throws IOException {
        return input.available() > 0;
    }
}
