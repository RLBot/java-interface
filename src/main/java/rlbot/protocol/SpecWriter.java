package rlbot.protocol;

import com.google.flatbuffers.FlatBufferBuilder;
import rlbot.flat.InterfaceMessageUnion;
import rlbot.flat.InterfacePacket;
import rlbot.flat.InterfacePacketT;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Logger;

public class SpecWriter {

    private static final int USHORT_MAX = 65535;

    private final Logger logger = Logger.getLogger(SpecWriter.class.getName());
    private final FlatBufferBuilder builder = new FlatBufferBuilder(1 << 12);

    private final WritableByteChannel channel;

    public SpecWriter(OutputStream output) {
        channel = Channels.newChannel(output);
    }

    public synchronized void write(InterfaceMessageUnion msg) throws IOException {
        var packet = new InterfacePacketT();
        packet.setMessage(msg);

        builder.clear();
        builder.finish(InterfacePacket.pack(builder, packet));

        var bb = builder.dataBuffer();
        int size = bb.remaining(); // FlatbufferBuilder fills from the back, so remaining == size

        if (size == 0) {
            return;
        } else if (size > USHORT_MAX) {
            logger.severe("Cannot send message because size of payload (" + size + ") exceeds maximum representable by two bytes");
        }

        // Prepend size, big endian
        bb.position(bb.position() - 2);
        bb.put(bb.position(), (byte) (size >> 8 & 0xFF));
        bb.put(bb.position() + 1, (byte) (size & 0xFF));

        channel.write(bb);
    }
}
