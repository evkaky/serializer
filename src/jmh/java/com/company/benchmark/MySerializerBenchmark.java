package com.company.benchmark;

import com.company.MySerializer;
import com.company.Person;
import org.openjdk.jmh.annotations.*;

import java.io.ByteArrayOutputStream;

public class MySerializerBenchmark {

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        Person person;
        ByteArrayOutputStream output;
        MySerializer mySerializer;

        @Setup
        public void prepare() {
            person = new Person();
            person.name = "evkaky";
            person.age = 18;

            output = new ByteArrayOutputStream();
            mySerializer = new MySerializer();
        }
    }

    @Benchmark
    public void mySerializerEncoding(BenchmarkState state) throws Exception {
        state.mySerializer.encode(state.person, state.output);
    }
}




