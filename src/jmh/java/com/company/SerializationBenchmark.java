package com.company;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import org.openjdk.jmh.annotations.*;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
public class SerializationBenchmark {

    private Person person;
    private MySerializer mySerializer;
    private Kryo kryoSerializer;
    private ByteArrayOutputStream outputForMySerialization;
    private Output outputForKryoSerialization;

    @Setup
    public void prepare() {
        person = new Person();
        person.name = "evkaky";
        person.age = 18;

        mySerializer = new MySerializer();
        kryoSerializer = new Kryo();

        outputForMySerialization = new ByteArrayOutputStream();
        outputForKryoSerialization = new Output(new ByteArrayOutputStream());
    }

    @Benchmark
    public void measureMySerialization() throws Exception {
        mySerializer.encode(person, outputForMySerialization);
    }

    @Benchmark
    public void measureKryoSerialization() {
        kryoSerializer.writeObject(outputForKryoSerialization, person);
    }
}



