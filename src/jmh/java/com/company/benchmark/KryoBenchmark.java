package com.company.benchmark;

import com.company.Person;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.io.ByteArrayOutputStream;

public class KryoBenchmark {

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        Person person;
        Output output;
        Kryo kryoSerializer;

        @Setup
        public void prepare() {
            person = new Person();
            person.name = "evkaky";
            person.age = 18;

            output = new Output(new ByteArrayOutputStream());
            kryoSerializer = new Kryo();
        }
    }

    @Benchmark
    public void kryoSerializerEncoding(KryoBenchmark.BenchmarkState state) {
        state.kryoSerializer.writeObject(state.output, state.person);
    }
}
