package com.company;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

// грамматика двоичного формата:
// obj: ( PRIMITIVE_CLASSNAME NULL_SENTINEL VAL | CLASSNAME ( FIELDNAME obj )* )
// NULL_SENTINEL 1 - не null, 0 - null

public class MySerializer {
    public static void main(String[] args) throws Exception {
        Object[] arr = {22, 33};
        arr[0] = 100;
        arr[1] = "ivan";
        System.out.println(arr[0]);
        System.out.println(arr[1]);
    }

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
        Set<Object> visitedRefs = new HashSet<>();
        DataOutputStream dataOutput = new DataOutputStream(new BufferedOutputStream(output));
        encode(obj, obj.getClass(), dataOutput, visitedRefs);
        dataOutput.flush();
    }

    // dfs traverse
    private void encode(Object obj, Class<?> clazz, DataOutputStream dataOutput, Set<Object> visitedRefs) throws Exception {
        dataOutput.writeUTF(clazz.getName());

        if (obj == null) {
            writeNullSentinel(NullSentinel.NULL_VALUE, dataOutput);
            return;
        }

        writeNullSentinel(NullSentinel.NON_NULL_VALUE, dataOutput);

        if (isTypeSimple(clazz)) {
            writePrimitiveValue(clazz, obj, dataOutput);
            return;
        }

        ensureGraphIsAcycled(obj, visitedRefs);

        if (clazz.isArray()) {
            Object[] arr = (Object[]) obj;
            dataOutput.writeInt(arr.length);
            for (int i = 0; i < arr.length - 1; i++) {
                Class<?> arrItemType = (arr[i] == null) ? clazz.getComponentType() : arr[i].getClass();
                encode(arr[i], arrItemType, dataOutput, visitedRefs);
            }
            return;
        }

        for (Field field : getAllNonStaticFields(clazz)) {
            field.setAccessible(true);
            dataOutput.writeUTF(field.getName());
            Object fieldRef = field.get(obj);
            Class<?> fieldType = (fieldRef == null) ? field.getType() : fieldRef.getClass();
            encode(fieldRef, fieldType, dataOutput, visitedRefs);
            field.setAccessible(false);
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

    private void writePrimitiveValue(Class<?> clazz, Object value, DataOutputStream dataOutput) throws Exception {
        switch (clazz.getName()) {
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
        Class<?> clazz = Class.forName(className);

        NullSentinel nullSentinel = readNullSentinel(inputData);
        if (nullSentinel == NullSentinel.NULL_VALUE) {
            return null;
        }

        if (isTypeSimple(clazz)) {
            return createPrimitiveObject(clazz, inputData);
        }

        if (clazz.isArray()) {
            int arrLen = inputData.readInt();
            // reflection для создания массивов сломан - полиморфизм не работает
            Object[] arr = new Object[arrLen];
            for (int i = 0; i < arrLen - 1; i++) {
                Object arrItem = innerDecode(inputData);
                arr[i] = arrItem;
            }
            return arr;
        }

        Object obj = clazz.newInstance();
        for (Field _ : getAllNonStaticFields(clazz)) {
            String fieldName = inputData.readUTF();
            Field field = FieldUtils.getField(clazz, fieldName, true);
            field.set(obj, innerDecode(inputData));
        }
        return obj;
    }

    private Object createPrimitiveObject(Class<?> clazz, DataInputStream dataInput) throws IOException {
        switch (clazz.getName()) {
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
        throw new RuntimeException("unknown simple type");
    }

    private NullSentinel readNullSentinel(DataInputStream dataInput) throws IOException {
        return (dataInput.readByte() == 0) ? NullSentinel.NULL_VALUE : NullSentinel.NON_NULL_VALUE;
    }

    private static List<Field> getAllNonStaticFields(Class<?> clazz) {
        return Arrays
                .stream(FieldUtils.getAllFields(clazz))
                .filter(f -> !Modifier.isStatic(f.getModifiers()) && !f.getType().getName().equals("java.lang.Class"))
                .collect(Collectors.toList());
    }
}
