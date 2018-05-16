package com.company;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

// грамматика двоичного формата:
// obj: ( PRIMITIVE_CLASSNAME NULL_SENTINEL VAL | CLASSNAME ( FIELDNAME obj )* )
// NULL_SENTINEL 1 - не null, 0 - null

public class MySerializer {
    private final Set<Class<?>> simpleTypes = new HashSet<>();

    public MySerializer() {
        simpleTypes.add(Boolean.class);
        simpleTypes.add(Character.class);
        simpleTypes.add(Byte.class);
        simpleTypes.add(Short.class);
        simpleTypes.add(Integer.class);
        simpleTypes.add(Long.class);
        simpleTypes.add(Float.class);
        simpleTypes.add(Double.class);
        simpleTypes.add(String.class);
    }

    public void encode(Object obj, OutputStream output) throws Exception {
        HashSet<Object> visitedRefs = new HashSet<>();
        DataOutputStream dataOutput = new DataOutputStream(new BufferedOutputStream(output));
        encode(obj, dataOutput, visitedRefs);
        dataOutput.flush();
    }

    // dfs traverse
    private void encode(Object obj, DataOutputStream dataOutput, Set<Object> visitedRefs) throws Exception {
        Class<?> clazz = obj.getClass();
        String className = clazz.getName();
        dataOutput.writeUTF(className);
        writeNullSentinel(NullSentinel.NON_NULL_VALUE, dataOutput);

        if (isTypeSimple(clazz)) {
            writePrimitiveValue(className, obj, dataOutput);
        } else {
            ensureGraphIsAcycled(obj, visitedRefs);
            for (Field field : clazz.getFields()) {
                dataOutput.writeUTF(field.getName());
                Object fieldRef = field.get(obj);
                if (fieldRef == null) {
                    dataOutput.writeUTF(field.getClass().getName());
                    writeNullSentinel(NullSentinel.NULL_VALUE, dataOutput);
                } else {
                    encode(fieldRef, dataOutput, visitedRefs);
                }
            }
        }
    }

    private void writeNullSentinel(NullSentinel nullSentinel, DataOutputStream dataOutput) throws IOException {
        switch (nullSentinel) {
            case NULL_VALUE: dataOutput.writeByte(0); break;
            case NON_NULL_VALUE: dataOutput.writeByte(1); break;
        }
    }

    private void ensureGraphIsAcycled(Object ref, Set<Object> visitedRefs) {
        if (visitedRefs.contains(ref)) {
            throw new CycledGraphException();
        }
        visitedRefs.add(ref);
    }

    private boolean isTypeSimple(Class<?> clazz) {
        return simpleTypes.contains(clazz);
    }

    private void writePrimitiveValue(String className, Object value, DataOutputStream dataOutput) throws Exception {
        switch (className) {
            case "java.lang.Integer": dataOutput.writeInt((int) value); break;
            case "java.lang.Long": dataOutput.writeLong((long) value); break;
            case "java.lang.Short": dataOutput.writeShort((short) value); break;
            case "java.lang.Byte": dataOutput.writeByte((byte) value); break;
            case "java.lang.Double": dataOutput.writeDouble((double) value); break;
            case "java.lang.Float": dataOutput.writeFloat((float) value); break;
            case "java.lang.Character": dataOutput.writeChar((char) value); break;
            case "java.lang.Boolean": dataOutput.writeBoolean((boolean) value); break;
            case "java.lang.String": dataOutput.writeUTF((String) value); break;
        }
    }

    public Object decode(InputStream in) throws Exception {
        DataInputStream dataInput = new DataInputStream(new BufferedInputStream(in));
        return innerDecode(dataInput);
    }

    // dfs traverse
    private Object innerDecode(DataInputStream inputData) throws Exception {
        String className = inputData.readUTF();
        NullSentinel nullSentinel = readNullSentinel(inputData);
        if (nullSentinel == NullSentinel.NULL_VALUE) {
            return null;
        }
        Class<?> clazz = Class.forName(className);
        if (isTypeSimple(clazz)) {
            return createPrimitiveObject(className, inputData);
        }

        Constructor<?> ctor = clazz.getConstructor();
        Object obj = ctor.newInstance();
        for (Field _ : clazz.getFields()) {
            String fieldName = inputData.readUTF();
            Field field = clazz.getDeclaredField(fieldName);
            field.set(obj, innerDecode(inputData));
        }
        return obj;
    }

    private Object createPrimitiveObject(String className, DataInputStream dataInput) throws IOException {
        switch (className) {
            case "java.lang.Integer": return dataInput.readInt();
            case "java.lang.Long": return dataInput.readLong();
            case "java.lang.Short": return dataInput.readShort();
            case "java.lang.Byte": return dataInput.readByte();
            case "java.lang.Double": return dataInput.readDouble();
            case "java.lang.Float": return dataInput.readFloat();
            case "java.lang.Character": return dataInput.readChar();
            case "java.lang.Boolean": return dataInput.readBoolean();
            case "java.lang.String": return dataInput.readUTF();
        }
        throw new RuntimeException("unknown primitive type: " + className);
    }

    private NullSentinel readNullSentinel(DataInputStream dataInput) throws IOException {
        return (dataInput.readByte() == 0) ? NullSentinel.NULL_VALUE : NullSentinel.NON_NULL_VALUE;
    }
}

