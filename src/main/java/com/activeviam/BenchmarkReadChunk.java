/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.activeviam;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.nio.DoubleBuffer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3)
@OutputTimeUnit(NANOSECONDS)
@Fork(value = 2)
@State(Scope.Benchmark)
public class BenchmarkReadChunk {
    interface IChunk{
        Object read(int pos);
        double readDouble(int pos);
    }
    interface ICurrentChunk extends IChunk {
        @Override
        Double read(int pos);
        @Override
        double readDouble(int pos);
    }

    static class ArrayCurrentChunkDouble implements ICurrentChunk {
        /** Data. */
        protected final double[] data;

        public ArrayCurrentChunkDouble(double[] data) {
            this.data = data;
        }
        @Override
        public Double read(int position) {
            return readDouble(position);
        }
        @Override
        public double readDouble(int position) {
            return data[position];
        }
    }

    static class BufferCurrentChunkDouble implements ICurrentChunk {
        /** Buffer of doubles. */
        protected DoubleBuffer buffer;

        public BufferCurrentChunkDouble(DoubleBuffer buffer) {
            this.buffer = buffer;
        }
        @Override
        public Double read(int position) {
            return readDouble(position);
        }
        @Override
        public double readDouble(int position) {
            return buffer.get(position);
        }
    }

    static class RandomCurrentChunkDouble implements ICurrentChunk {

        public RandomCurrentChunkDouble() { }
        @Override
        public Double read(int position) {
            return readDouble(position);
        }
        @Override
        public double readDouble(int position) {
            return Math.random();
        }
    }

    static class StaticValueCurrentChunkDouble implements ICurrentChunk {

        public StaticValueCurrentChunkDouble() { }
        @Override
        public Double read(int position) {
            return readDouble(position);
        }
        @Override
        public double readDouble(int position) {
            return 4d;
        }
    }


    interface INewChunk extends IChunk {
        @Override
        default Double read(int pos) {
            return readDouble(pos);
        }

        @Override
        double readDouble(int pos);
    }

    static class ArrayNewChunkDouble implements INewChunk {
        /** Data. */
        protected final double[] data;

        public ArrayNewChunkDouble(double[] data) {
            this.data = data;
        }

        @Override
        public double readDouble(int position) {
            return data[position];
        }
    }

    static class BufferNewChunkDouble implements INewChunk {
        /** Buffer of doubles. */
        protected DoubleBuffer buffer;

        public BufferNewChunkDouble(DoubleBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public double readDouble(int position) {
            return buffer.get(position);
        }
    }

    static class RandomNewChunkDouble implements INewChunk {

        public RandomNewChunkDouble() { }

        @Override
        public double readDouble(int position) {
            return Math.random();
        }
    }

    static class StaticValueNewChunkDouble implements INewChunk {

        public StaticValueNewChunkDouble() { }

        @Override
        public double readDouble(int position) {
            return 4d;
        }
    }

    IChunk[] chunksetWithLotsOfColumns;

    @Param({ "mono", "mega" })
    private String mode;

    protected static int chunkSetSize = 500;

    @Setup
    public void setup() {
        // Simulate a list of chunksets with 4 chunks different per chunkset.
        chunksetWithLotsOfColumns = new IChunk[chunkSetSize];
        boolean mega = mode.equals("mega");
        if (!mega) {
            // Here we don't expect megamorphism: all implementations containing "current" implement `read(int)`
            for (int i = 0; i < chunkSetSize; i += 4) {
                // data is not important
                double[] data1 = new double[] { i, i + 1, i + 2, i + 3 };
                double[] data2 = new double[] { 4 * i, 4 * i + 1, 4 * i + 2, 4 * i + 3 };
                chunksetWithLotsOfColumns[i] = new ArrayCurrentChunkDouble(data1);
                chunksetWithLotsOfColumns[i + 1] = new BufferCurrentChunkDouble(DoubleBuffer.wrap(data2));
                chunksetWithLotsOfColumns[i + 2] = new RandomCurrentChunkDouble();
                chunksetWithLotsOfColumns[i + 3] = new StaticValueCurrentChunkDouble();
            }
        } else {
            // Here we expect megamorphism: all implementations containing "new" delegate `read(int)` to their underlying interface.
            for (int i = 0; i < chunkSetSize; i += 4) {
                // data is not important
                double[] data1 = new double[] { i, i + 1, i + 2, i + 3 };
                double[] data2 = new double[] { 4 * i, 4 * i + 1, 4 * i + 2, 4 * i + 3 };
                chunksetWithLotsOfColumns[i] = new ArrayNewChunkDouble(data1);
                chunksetWithLotsOfColumns[i + 1] = new BufferNewChunkDouble(DoubleBuffer.wrap(data2));
                chunksetWithLotsOfColumns[i + 2] = new RandomNewChunkDouble();
                chunksetWithLotsOfColumns[i + 3] = new StaticValueNewChunkDouble();
            }
        }
    }

    @Benchmark
    public void benchmark(Blackhole blackhole) {
        double sum = 0d;
        for (IChunk chunk : chunksetWithLotsOfColumns) {
            sum += (Double) chunk.read(0);
            sum += (Double) chunk.read(1);
            sum += (Double) chunk.read(2);
            sum += (Double) chunk.read(3);
        }
        blackhole.consume(sum);
    }

    /*
    java -XX:LoopUnrollLimit=1 -XX:-TieredCompilation -XX:-Inline -jar target/benchmarks.jar
    With 10 different implementations
Benchmark                     (mode)  Mode  Cnt      Score      Error  Units
BenchmarkReadChunk.benchmark    mono  avgt    6  28188.752 ±  713.779  ns/op
BenchmarkReadChunk.benchmark    mega  avgt    6  32269.078 ± 5307.135  ns/op


    With 4 different implementations
Benchmark                     (mode)  Mode  Cnt      Score      Error  Units
BenchmarkReadChunk.benchmark    mono  avgt    6  34520.308 ±  211.988  ns/op
BenchmarkReadChunk.benchmark    mega  avgt    6  40509.653 ± 1679.530  ns/op


    With 2 different implementations
Benchmark                     (mode)  Mode  Cnt      Score      Error  Units
BenchmarkReadChunk.benchmark    mono  avgt    6  24502.773 ±  180.207  ns/op
BenchmarkReadChunk.benchmark    mega  avgt    6  26793.713 ± 2347.674  ns/op


    With 1 implementation
Benchmark                     (mode)  Mode  Cnt      Score      Error  Units
BenchmarkReadChunk.benchmark    mono  avgt    6  19379.033 ±  803.722  ns/op
BenchmarkReadChunk.benchmark    mega  avgt    6  20792.519 ± 3527.568  ns/op
     */

    /*
    java -jar target/benchmarks.jar

    With 10 different implementations
Benchmark                     (mode)  Mode  Cnt      Score     Error  Units
BenchmarkReadChunk.benchmark    mono  avgt    6  16866.848 ± 100.889  ns/op
BenchmarkReadChunk.benchmark    mega  avgt    6  20861.132 ± 920.708  ns/op

    With 4 different implementations
Benchmark                     (mode)  Mode  Cnt      Score     Error  Units
BenchmarkReadChunk.benchmark    mono  avgt    6  18320.274 ± 268.064  ns/op
BenchmarkReadChunk.benchmark    mega  avgt    6  28160.217 ± 684.585  ns/op

    With 2 different implementations
Benchmark                     (mode)  Mode  Cnt     Score     Error  Units
BenchmarkReadChunk.benchmark    mono  avgt    6  5216.686 ± 427.837  ns/op
BenchmarkReadChunk.benchmark    mega  avgt    6  5358.252 ± 118.637  ns/op

    With 1 implementation
Benchmark                     (mode)  Mode  Cnt     Score    Error  Units
BenchmarkReadChunk.benchmark    mono  avgt    6  1834.698 ± 14.378  ns/op
BenchmarkReadChunk.benchmark    mega  avgt    6  1834.928 ±  5.712  ns/op

     */

}
