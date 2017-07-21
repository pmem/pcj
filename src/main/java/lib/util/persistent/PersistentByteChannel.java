package lib.util.persistent;

import lib.util.persistent.spi.PersistentMemoryProvider;
import lib.util.persistent.types.LongField;
import lib.util.persistent.types.ObjectField;
import lib.util.persistent.types.ObjectType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.InterruptibleChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/**
 * Created by pprados on 20/07/17.
 */
// TODO: faire une version avec des Heap et non des bytearrays. Permet de gerer les tailles long et est plus rapide
public class PersistentByteChannel extends PersistentObject // AbstractFileChannel
        implements
        SeekableByteChannel,
        InterruptibleChannel,
        GatheringByteChannel,
        ScatteringByteChannel {
    private static final ObjectField<PersistentByteArray> BUF = new ObjectField<>(PersistentByteArray.TYPE);
    private static final LongField SIZE = new LongField();
    private static final LongField POS = new LongField();
    public static final ObjectType<PersistentByteChannel> TYPE =
            ObjectType.fromFields(PersistentByteChannel.class, BUF,SIZE,POS);

    private static final int ROUND_BUF_SIZE =1; // FIXME 1024*5;

    private int roundSize(long size) {
        return (int)((size+ ROUND_BUF_SIZE)/ ROUND_BUF_SIZE)* ROUND_BUF_SIZE;
    }
    private PersistentByteChannel(ObjectPointer<? extends PersistentByteChannel> p) {
        super(p);
    }
    private PersistentByteChannel(ObjectType<? extends PersistentByteChannel> type) {
        super(type);
    }

    public PersistentByteChannel() { this(ROUND_BUF_SIZE); }
    public PersistentByteChannel(long initialeBufSize) {
        super(TYPE);
        Transaction.run(() -> {
            size(initialeBufSize);
            buf(new PersistentByteArray(roundSize(initialeBufSize)));
            position(0L);
        });
    }
    private synchronized PersistentByteArray buf() {
        return getObjectField(BUF);
    }
    private synchronized PersistentByteChannel buf(PersistentByteArray buf) {
        setObjectField(BUF,buf);
        return this;
    }
    private synchronized PersistentByteChannel size(long size) {
        setLongField(SIZE,size);
        return this;
    }

    private synchronized int maxsize() { return buf().length();}
    private void memcpy(MemoryRegion srcRegion, long srcOffset, MemoryRegion destRegion, long destOffset, long length) {
        heap.memcpy(srcRegion, srcOffset,destRegion,destOffset,length);
    }

    private synchronized void extendbuf(long size) {
        Transaction.run(() -> {
            long newSize = Math.max(roundSize(size),size()*2);
            if (newSize<maxsize()) return;
            PersistentByteArray newBuf=new PersistentByteArray((int)newSize);
            MemoryRegion srcRegion=(buf().getPointer().region());
            MemoryRegion dstRegion=(newBuf.getPointer().region());
            memcpy(srcRegion,buf().elementOffset(0),dstRegion,buf().elementOffset(0),maxsize());
            buf(newBuf);
        },this);
    }

    @Override
    public int read(ByteBuffer dst) {
        return (int)read(new ByteBuffer[]{dst},0,Integer.MAX_VALUE);
    }
    @Override
    public long read(ByteBuffer[] dsts) {
        return read(dsts,0,Integer.MAX_VALUE);
    }

    /**
     * @see java.nio.channels.FileChannel#read(ByteBuffer,long)
     * @param dst
     * @param position
     * @return
     */
    public synchronized int read(ByteBuffer dst, long position) {
        if ((position<0) || (position>size())) throw new IndexOutOfBoundsException();
        long oldposition=position();
        Transaction.run(() -> {
            position(position);
            read(dst);
        });
        int bytesRead=(int)(position()-position);
        position(oldposition);
        return bytesRead;
    }

    @Override
    public synchronized long read(ByteBuffer[] dsts, int offset, final int length) {
        long pos=position()+offset;
        if (offset<0 || pos>size()) throw new IndexOutOfBoundsException();
        Transaction.run(() -> {
            int l=length;
            long p=pos;
            PersistentByteArray buf=buf();
            long end=p;
            for (ByteBuffer dst:dsts) {
                int bytesToRead=Math.min(l,dst.remaining());
                end=Math.min(size(),p+bytesToRead);
                // TODO: optimize with jni memcpy ?
                for (long i = p; i < end; i++) dst.put(buf.getByteElement((int)i));
                l-=bytesToRead;
                p=end;
            }
            position(end);
        },this); // TODO: onAbort pour repositionner les positions des dsts
        return (int)position()-pos;
    }


    @Override
    public int write(ByteBuffer src) {
        return (int)write(new ByteBuffer[]{src},0,Integer.MAX_VALUE);
    }

    @Override
    public long write(ByteBuffer[] srcs) {
        return write(srcs,0,Integer.MAX_VALUE);
    }

    public int write(ByteBuffer src, long position) {
        if ((position<0) || (position>size())) throw new IndexOutOfBoundsException();
        long oldposition=position();
        Transaction.run(() -> {
            position(position);
            write(src);
        });
        int bytesRead=(int)(position()-position);
        position(oldposition);
        return bytesRead;
    }

    @Override
    public synchronized long write(ByteBuffer[] srcs, int offset, int length) {
        long pos=position()+offset;
        if (offset<0 || pos>size()) throw new IndexOutOfBoundsException();
        Transaction.run(() -> {
            int l=length;
            long p=pos;
            long newMinimumSize=p;
            for (int i=0;i<srcs.length;++i) newMinimumSize += srcs[i].remaining();
            if (size() < newMinimumSize) {
                extendbuf((int)newMinimumSize);
            }
            PersistentByteArray buf=buf();
            long end=p;
            for (ByteBuffer src:srcs) {
                int bytesToWrite = src.remaining();
                end = p + bytesToWrite;
                // TODO: optimize
                for (long i = p; i < end; i++) buf.setByteElement((int)i, src.get());
                p=end;
            }
            position(end);
            if (end>size()) size(end);
        },this);
        return (int)position()-pos;
    }

    public long transferFrom(FileChannel src,
                             long position, long count)
            throws IOException {
        ByteBuffer buf=ByteBuffer.allocate((int)src.size());
        src.read(buf);
        return write(buf);
    }

    public long transferTo(long position, long count,
                           FileChannel target)
            throws IOException {
        ByteBuffer buf=ByteBuffer.allocate((int)count);
        long rc=read(buf);
        buf.rewind();
        target.write(buf,position);
        return rc;
    }
    @Override
    synchronized public long position() {
        return getLongField(POS);
    }

    @Override
    synchronized public PersistentByteChannel position(long newPosition) {
        Transaction.run(() -> {
            setLongField(POS, newPosition);
        },this);
        return this;
    }

    @Override
    public long size() {
        return getLongField(SIZE);
    }

    @Override
    public SeekableByteChannel truncate(long size) {
        Transaction.run(() -> {
            int newSize = roundSize((int)size);
            if (newSize<maxsize()) {
                PersistentByteArray newBuf = new PersistentByteArray(newSize);
                MemoryRegion srcRegion = (buf().getPointer().region());
                MemoryRegion dstRegion = (newBuf.getPointer().region());
                memcpy(srcRegion,buf().elementOffset(0), dstRegion, buf().elementOffset(0), size);
                buf(newBuf);
            }
            if (position()>size) {
                position(size);
            }
            size(size);
        },this);
        return this;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() {
        Transaction.run(() -> {
            buf(new PersistentByteArray(0)); // Replace buffer with empty
            position(0);
        });
    }

    private static byte[] toArray(PersistentByteChannel channel) {
        channel.position(0);
        ByteBuffer buf=ByteBuffer.allocate((int)channel.size());
        channel.read(buf);
        return buf.array();
    }

    private static byte[] toArray(ByteBuffer buf) {
        return toArray(new ByteBuffer[] { buf});
    }
    private static byte[] toArray(ByteBuffer[] bufs) {
        int size=0;
        for (ByteBuffer buf:bufs) {
            buf.rewind();
            size+=buf.remaining();
        }
        byte[] rc=new byte[size];
        int offset=0;
        for (ByteBuffer buf:bufs) {
            buf.rewind();
            buf.get(rc,offset,buf.remaining());
            buf.rewind();
            offset+=buf.remaining();
        }
        return rc;
    }

    private static void dumpChannel(PersistentByteChannel channel) {
        System.out.print("channel=");
        long p=channel.position();
        channel.position(0);
        ByteBuffer buf=ByteBuffer.allocate((int)channel.size());
        channel.read(buf);
        buf.rewind();
        int len=buf.remaining();
        for (int i=0;i<len;++i) System.out.print(buf.get()+", ");
        System.out.println();
        channel.position(p);
    }
    private static void dumpBufs(ByteBuffer[] bufs) {
        System.out.print("bufs=");
        for (ByteBuffer buf:bufs) {
            buf.rewind();
            int len=buf.remaining();
            for (int i=0;i<len;++i) System.out.print(buf.get(i)+", ");
            buf.rewind();
        }
        System.out.println();
    }
    // TODO: faire des vrais TU, après avoir réorganiser le build en gradle par exemple
    public static void main(String[] args) throws IOException {
        boolean debug=true;
        long rc;
        final int initialSize=1;
        Trace.enable(true);
        Stats.enable(true);
        PersistentMemoryProvider.getDefaultProvider().getHeap().open();
        PersistentByteChannel channel;
        if (debug || ObjectDirectory.get("channel", PersistentByteChannel.class) == null) {
            channel = new PersistentByteChannel(initialSize);
            ObjectDirectory.put("channel", channel);
        } else {
            channel = ObjectDirectory.get("channel", PersistentByteChannel.class);
            assert(Arrays.equals(toArray(channel),new byte[]{1, 2, 3, 4, 5}));
            channel.truncate(initialSize);
            dumpChannel(channel);
            channel.position(0);
        }
        dumpChannel(channel); // Need 0,

        ByteBuffer src = ByteBuffer.allocate(5).put(new byte[]{1, 2, 3, 4, 5});
        src.rewind();
        rc = channel.write(src);
        assert(rc==5);
        assert(Arrays.equals(toArray(channel),new byte[]{1, 2, 3, 4, 5}));
        dumpChannel(channel); // Need 1, 2, 3, 4, 5

        int SizeBuf = 2;
        ByteBuffer[] bufs = new ByteBuffer[]{ByteBuffer.allocate(SizeBuf), ByteBuffer.allocate(SizeBuf), ByteBuffer.allocate(SizeBuf)};
        assert(Arrays.equals(toArray(bufs),new byte[]{0, 0, 0, 0, 0, 0}));
        dumpBufs(bufs); // Need 0,0,0,0,0,0
        channel.position(0);
        rc=channel.read(bufs, 0, 3);
        assert(rc==3);
        assert(Arrays.equals(toArray(bufs),new byte[]{1, 2, 3, 0, 0, 0}));
        dumpBufs(bufs); // Need 1,2,3,0,0,0
        channel.position(0);
        rc=channel.read(bufs);
        assert(rc==5);
        assert(Arrays.equals(toArray(bufs),new byte[]{1, 2, 3, 4, 5, 0}));
        dumpBufs(bufs); // Need 1,2,3,4,5,0

        channel.position(0);
        rc=channel.write(bufs);
        assert(rc==6);
        assert(Arrays.equals(toArray(channel),new byte[]{1, 2, 3, 4, 5, 0}));
        dumpChannel(channel); // Need 1,2,3,4,5,0

        channel.truncate(2);
        dumpChannel(channel); // Need 1,2,
        channel.position(0);
        ByteBuffer dst = ByteBuffer.allocate(5);
        rc=channel.read(dst);
        assert(rc==2);
        assert(Arrays.equals(toArray(dst),new byte[]{1, 2, 0, 0, 0}));
        dumpBufs(new ByteBuffer[]{dst}); // Need 1,2,0,0,0
        dumpChannel(channel);

        //Path temp=File.createTempFile("pmem",".tmp").toPath();
        // Reset channel
        channel.position(0);
        src = ByteBuffer.allocate(5).put(new byte[]{1, 2, 3, 4, 5});
        src.rewind();
        channel.write(src);

        Path temp= Paths.get("/tmp/test.tmp");
        FileChannel fchannel=FileChannel.open(temp, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        channel.position(0);
        channel.transferTo(0,5,fchannel);
        fchannel.close();
        fchannel=FileChannel.open(temp, StandardOpenOption.READ);
        channel.position(0);
        channel.transferFrom(fchannel,0,5);
        dumpChannel(channel);
    }
}
