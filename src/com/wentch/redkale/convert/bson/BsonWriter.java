/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wentch.redkale.convert.bson;

import com.wentch.redkale.convert.*;
import com.wentch.redkale.util.*;
import java.nio.*;

/**
 *
 * @author zhangjx
 */
public class BsonWriter implements Writer {

    private static final int defaultSize = Integer.getInteger("convert.bson.writer.buffer.defsize", 1024);

    private byte[] content;

    protected int count;

    protected boolean tiny;

    public static ObjectPool<BsonWriter> createPool(int max) {
        return new ObjectPool<>(max, (Object... params) -> new BsonWriter(), null, (x) -> x.recycle());
    }

    public byte[] toArray() {
        if (count == content.length) return content;
        byte[] newdata = new byte[count];
        System.arraycopy(content, 0, newdata, 0, count);
        return newdata;
    }

    public ByteBuffer toBuffer() {
        return ByteBuffer.wrap(content, 0, count);
    }

    public void toBuffer(ByteBuffer buffer) {
        buffer.put(content, 0, count);
    }

    public int toBuffer(int offset, ByteBuffer buffer) {
        int len = Math.min(count - offset, buffer.remaining());
        if (len < 1) return 0;
        buffer.put(content, offset, len);
        return len;
    }

    protected BsonWriter(byte[] bs) {
        this.content = bs;
    }

    public BsonWriter() {
        this(defaultSize);
    }

    public BsonWriter(int size) {
        this.content = new byte[size > 128 ? size : 128];
    }

    @Override
    public final boolean isTiny() {
        return tiny;
    }

    public BsonWriter setTiny(boolean tiny) {
        this.tiny = tiny;
        return this;
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    /**
     * 返回指定至少指定长度的缓冲区
     *
     * @param len
     * @return
     */
    private byte[] expand(int len) {
        int newcount = count + len;
        if (newcount <= content.length) return content;
        byte[] newdata = new byte[Math.max(content.length * 3 / 2, newcount)];
        System.arraycopy(content, 0, newdata, 0, count);
        this.content = newdata;
        return newdata;
    }

    /**
     * 往指定的位置写入字节
     *
     * @param position
     * @param chs 
     * @return  
     */
    public int rewriteTo(int position, byte... chs) {
        System.arraycopy(chs, 0, content, position, chs.length);
        return position + chs.length;
    }

    public final int rewriteTo(int position, short value) {
        rewriteTo(position, (byte) (value >> 8), (byte) value);
        return position + 2;
    }

    public final int rewriteTo(int position, char value) {
        rewriteTo(position, (byte) ((value & 0xFF00) >> 8), (byte) (value & 0xFF));
        return position + 2;
    }

    public final int rewriteTo(int position, int value) {
        rewriteTo(position, (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value);
        return position + 4;
    }

    public final int rewriteTo(int position, long value) {
        rewriteTo(position, (byte) (value >> 56), (byte) (value >> 48), (byte) (value >> 40), (byte) (value >> 32),
                (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value);
        return position + 8;
    }

    public BsonWriter fillRange(final int len) {
        expand(len);
        count += len;
        return this;
    }

    public void writeTo(final byte ch) {
        expand(1);
        content[count++] = ch;
    }

    public final void writeTo(final byte... chs) {
        writeTo(chs, 0, chs.length);
    }

    public void writeTo(final byte[] chs, final int start, final int len) {
        expand(len);
        System.arraycopy(chs, start, content, count, len);
        count += len;
    }

    protected boolean recycle() {
        this.count = 0;
        if (this.content.length > defaultSize) {
            this.content = new byte[defaultSize];
        }
        return true;
    }

    @Override
    public String toString() {
        return new String(content, 0, count);
    }

    //------------------------------------------------------------------------
    public final int count() {
        return this.count;
    }

    @Override
    public final void writeBoolean(boolean value) {
        writeTo(value ? (byte) 1 : (byte) 0);
    }

    @Override
    public final void writeByte(byte value) {
        writeTo(value);
    }

    @Override
    public final void writeChar(final char value) {
        writeTo((byte) ((value & 0xFF00) >> 8), (byte) (value & 0xFF));
    }

    @Override
    public final void writeShort(short value) {
        writeTo((byte) (value >> 8), (byte) value);
    }

    @Override
    public final void writeInt(int value) {
        writeTo((byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value);
    }

    @Override
    public final void writeLong(long value) {
        writeTo((byte) (value >> 56), (byte) (value >> 48), (byte) (value >> 40), (byte) (value >> 32),
                (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value);
    }

    @Override
    public final void writeFloat(float value) {
        writeInt(Float.floatToIntBits(value));
    }

    @Override
    public final void writeDouble(double value) {
        writeLong(Double.doubleToLongBits(value));
    }

    @Override
    public final void wirteClassName(String clazz) {
        writeSmallString(clazz == null ? "" : clazz);
    }

    @Override
    public final void writeObjectB(int fieldCount, Object obj) {
        writeSmallString("");
        writeShort(BsonReader.SIGN_OBJECTB);
    }

    @Override
    public final void writeObjectE(Object obj) {
        writeByte(BsonReader.SIGN_NONEXT);
        writeShort(BsonReader.SIGN_OBJECTE);
    }

    @Override
    public final void writeField(boolean comma, Attribute attribute) {
        writeByte(BsonReader.SIGN_HASNEXT);
        writeSmallString(attribute.field());
        byte typeval = 127;  //字段的类型值
        final Class type = attribute.type();
        if (type == boolean.class || type == Boolean.class) {
            typeval = 1;
        } else if (type == byte.class || type == Byte.class) {
            typeval = 2;
        } else if (type == short.class || type == Short.class) {
            typeval = 3;
        } else if (type == char.class || type == Character.class) {
            typeval = 4;
        } else if (type == int.class || type == Integer.class) {
            typeval = 5;
        } else if (type == long.class || type == Long.class) {
            typeval = 6;
        } else if (type == float.class || type == Float.class) {
            typeval = 7;
        } else if (type == double.class || type == Double.class) {
            typeval = 8;
        } else if (type == String.class) {
            typeval = 9;
        } else if (type == boolean[].class || type == Boolean[].class) {
            typeval = 101;
        } else if (type == byte[].class || type == Byte[].class) {
            typeval = 102;
        } else if (type == short[].class || type == Short[].class) {
            typeval = 103;
        } else if (type == char[].class || type == Character[].class) {
            typeval = 104;
        } else if (type == int[].class || type == Integer[].class) {
            typeval = 105;
        } else if (type == long[].class || type == Long[].class) {
            typeval = 106;
        } else if (type == float[].class || type == Float[].class) {
            typeval = 107;
        } else if (type == double[].class || type == Double[].class) {
            typeval = 108;
        } else if (type == String[].class) {
            typeval = 109;
        }
        writeByte(typeval);
    }

    /**
     * 对于类的字段名、枚举值这些长度一般不超过255且不会出现双字节字符的字符串采用writeSmallString处理, readSmallString用于读取
     *
     * @param value
     */
    @Override
    public final void writeSmallString(String value) {
        if (value.isEmpty()) {
            writeTo((byte) 0);
            return;
        }
        char[] chars = Utility.charArray(value);
        if (chars.length > 255) throw new ConvertException("'" + value + "' has  very long length");
        byte[] bytes = new byte[chars.length + 1];
        bytes[0] = (byte) chars.length;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] > Byte.MAX_VALUE) throw new ConvertException("'" + value + "'  has  double-word");
            bytes[i + 1] = (byte) chars[i];
        }
        writeTo(bytes);
    }

    @Override
    public final void writeString(String value) {
        if (value == null) {
            writeInt(Reader.SIGN_NULL);
            return;
        } else if (value.isEmpty()) {
            writeInt(0);
            return;
        }
        byte[] bytes = Utility.encodeUTF8(value);
        writeInt(bytes.length);
        writeTo(bytes);
    }

    @Override
    public final void writeNull() {
        writeShort(Reader.SIGN_NULL);
    }

    @Override
    public final void writeArrayB(int size) {
        writeInt(size);
    }

    @Override
    public final void writeArrayMark() {
    }

    @Override
    public final void writeArrayE() {
    }

    @Override
    public void writeMapB(int size) {
        writeArrayB(size);
    }

    @Override
    public final void writeMapMark() {
    }

    @Override
    public final void writeMapE() {
    }

}
