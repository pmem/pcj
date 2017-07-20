package lib.util.persistent;

import lib.util.persistent.spi.PersistentMemoryProvider;
import lib.util.persistent.types.ArrayType;
import lib.util.persistent.types.DoubleField;
import lib.util.persistent.types.IntField;
import lib.util.persistent.types.LongField;
import lib.util.persistent.types.ObjectField;
import lib.util.persistent.types.ObjectType;
import lib.xpersistent.XHeap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Created by pprados on 20/07/17.
 */
public class PersistentByteChannel extends PersistentObject
        implements SeekableByteChannel,WritableByteChannel,ReadableByteChannel {
    private static final ObjectField<PersistentByteArray> BUF = new ObjectField<>(PersistentByteArray.TYPE);
    private static final IntField MAXSIZE = new IntField();
    private static final LongField POS = new LongField();
    public static final ObjectType<PersistentByteChannel> TYPE =
            ObjectType.fromFields(PersistentByteChannel.class, BUF,MAXSIZE,POS);

    private static final int BUF_SIZE=1024*10;

    public PersistentByteChannel(int initialeSize) {
        super(TYPE);
        Transaction.run(() -> {
            maxsize((initialeSize+BUF_SIZE)/BUF_SIZE);
            buf(new PersistentByteArray(maxsize()));
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
    private synchronized int maxsize() { return getIntField(MAXSIZE);}
    private synchronized PersistentByteChannel maxsize(int newSize) {
        setIntField(MAXSIZE,newSize);
        return this;
    }
    private void memcpy(MemoryRegion srcRegion, long srcOffset, MemoryRegion destRegion, long destOffset, long length) {
        heap.memcpy(srcRegion, srcOffset,destRegion,destOffset,length);
    }

    private synchronized void extendbuf(int size) {
        Transaction.run(() -> {
            int newSize = (size+BUF_SIZE)/BUF_SIZE;
            if (newSize<maxsize()) return;
            PersistentByteArray newBuf=new PersistentByteArray(newSize);
            MemoryRegion srcRegion=(buf().getPointer().region());
            MemoryRegion dstRegion=(newBuf.getPointer().region());
            memcpy(srcRegion,0,dstRegion,0,maxsize());
            buf(newBuf);
            maxsize(newSize);
        },this);
    }

    @Override
    synchronized public int read(ByteBuffer dst) {
        int pos=(int)position();
        Transaction.run(() -> {
            int bytesToRead=dst.remaining();
            PersistentByteArray buf=buf();
            int end=Math.min(buf.length(),pos+bytesToRead);
            // TODO: optimize
            for (int i = pos; i < end; i++) dst.put(buf.getByteElement(i));
            position(end);
        },this);
        return (int)position()-pos;
    }

    @Override
    synchronized public int write(ByteBuffer src) {
        int pos=(int)position();
        Transaction.run(() -> {
            int bytesToWrite = src.remaining();
            int newMinimumSize=(int)position()+bytesToWrite;
            if (size()<newMinimumSize) {
                extendbuf(newMinimumSize);
            }
            PersistentByteArray buf=buf();
            int end=pos+bytesToWrite;
            // TODO: optimize
            for (int i = pos; i < end; i++) buf.setByteElement(i,src.get());
            position(end);
        },this);
        return (int)position()-pos;
    }

    @Override
    synchronized public long position() {
        return getLongField(POS);
    }

    @Override
    synchronized public SeekableByteChannel position(long newPosition) {
        Transaction.run(() -> {
            setLongField(POS, newPosition);
        },this);
        return this;
    }

    @Override
    public long size() {
        return buf().length();
    }

    @Override
    public SeekableByteChannel truncate(long size) {
        Transaction.run(() -> {
            int newSize = ((int)size+BUF_SIZE)/BUF_SIZE;
            if (newSize<maxsize()) return;
            PersistentByteArray newBuf=new PersistentByteArray(newSize);
            MemoryRegion srcRegion=(buf().getPointer().region());
            MemoryRegion dstRegion=(newBuf.getPointer().region());
            memcpy(srcRegion,0,dstRegion,0,size);
            buf(newBuf);
            maxsize(newSize);
            if (position()>newSize)
                position(newSize);
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
            buf(new PersistentByteArray(0));
        });
    }

    public static void main(String[] args) {
        PersistentByteChannel channel=new PersistentByteChannel(100);
        System.out.println(channel.position());
        System.out.println(channel.maxsize());
    }
}
