package com.company;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class MySerializerTest {

    @Test
    public void should_serialize_deserialize_graph_with_arrays() throws Exception {
        Person p = new Person();
        p.name = "evkaky";
        p.age = 18;
        ArrayList<Integer> nums = new ArrayList<>();
        nums.add(100);
        nums.add(200);
        p.nums = nums;

        MySerializer mySerializer = new MySerializer();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mySerializer.encode(p, output);

        InputStream inStream = new ByteArrayInputStream(output.toByteArray());
        Person deserializedPerson = (Person) mySerializer.decode(inStream);

        assertEquals(p.nums.size(), deserializedPerson.nums.size());
        assertEquals(p.nums.get(0), deserializedPerson.nums.get(0));
        assertEquals(p.nums.get(1), deserializedPerson.nums.get(1));
    }

    @Test
    public void should_serialize_deserialize_simple_type() throws Exception {
        String str = "ekvkay";

        MySerializer mySerializer = new MySerializer();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mySerializer.encode(str, output);

        InputStream inStream = new ByteArrayInputStream(output.toByteArray());
        String deserializedStr = (String) mySerializer.decode(inStream);

        assertEquals(str, deserializedStr);
    }

    @Test
    public void simple_serialize_deserialize_with_flat_object() throws Exception {
        Person p = new Person();
        p.name = "evkaky";
        p.age = 18;

        MySerializer mySerializer = new MySerializer();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mySerializer.encode(p, output);

        InputStream inStream = new ByteArrayInputStream(output.toByteArray());
        Person deserializedPerson = (Person) mySerializer.decode(inStream);

        assertEquals(p.name, deserializedPerson.name);
        assertEquals(p.age, deserializedPerson.age);
    }

    @Test(expected = CycledGraphException.class)
    public void should_fail_on_cycled_graph_serialization() throws Exception {
        Person p = new Person();
        p.name = "evkaky";
        p.age = 18;
        p.anotherPerson = p;

        MySerializer mySerializer = new MySerializer();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mySerializer.encode(p, output);
    }

    @Test
    public void should_serialize_deserialize_simple_tree() throws Exception {
        Person p = new Person();
        p.name = "evkaky";
        p.age = 18;

        Person anotherPerson = new Person();
        anotherPerson.name = "chuvi";
        anotherPerson.age = 18;

        p.anotherPerson = anotherPerson;

        MySerializer mySerializer = new MySerializer();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mySerializer.encode(p, output);

        InputStream inStream = new ByteArrayInputStream(output.toByteArray());
        Person deserializedPerson = (Person) mySerializer.decode(inStream);

        assertEquals(p.name, deserializedPerson.name);
        assertEquals(p.age, deserializedPerson.age);
        assertEquals(p.anotherPerson.name, deserializedPerson.anotherPerson.name);
        assertEquals(p.anotherPerson.age, deserializedPerson.anotherPerson.age);
    }
}