package com.company.benchmark;

import com.company.Person;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class JacksonBenchamark {

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        Person person;
        ByteArrayOutputStream output;
        ObjectMapper jacksonSerializer;

        @Setup
        public void prepare() {
            person = new Person();
            person.name = "evkaky";
            person.age = 18;

            output = new ByteArrayOutputStream();
            jacksonSerializer = new ObjectMapper();
        }
    }

    @Benchmark
    public void jacksonSerializerEncoding(JacksonBenchamark.BenchmarkState state) throws IOException {
        state.jacksonSerializer.writeValue(state.output, state.person);
    }
}
