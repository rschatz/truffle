/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.tools.profiler.impl;

import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.tools.profiler.CPUTracer;
import com.oracle.truffle.tools.profiler.HeapHistogram;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Instrument;

/**
 *
 * @author Tomas Hurka
 */
@TruffleInstrument.Registration(id = HeapHistogramInstrument.ID, name = "Heap Histogram", version = "0.1", services = {HeapHistogram.class})
public class HeapHistogramInstrument extends TruffleInstrument {

    /**
     * Default constructor.
     *
     * @since 0.30
     */
    public HeapHistogramInstrument() {
    }

    /**
     * A string used to identify the tracer, i.e. as the name of the tool.
     *
     * @since 0.30
     */
    public static final String ID = "heaphisto";
    private HeapHistogram histo;
    private static ProfilerToolFactory<HeapHistogram> factory;
    private static Map<Env, HeapHistogram> envs = new HashMap<>();

    /**
     * Sets the factory which instantiates the {@link HeapHistogram}.
     *
     * @param factory the factory which instantiates the {@link HeapHistogram}.
     * @since 0.30
     */
    public static void setFactory(ProfilerToolFactory<HeapHistogram> factory) {
        if (factory == null || !factory.getClass().getName().startsWith("com.oracle.truffle.tools.profiler")) {
            throw new IllegalArgumentException("Wrong factory: " + factory);
        }
        HeapHistogramInstrument.factory = factory;
    }

    static {
        // Be sure that the factory is initialized:
        try {
            Class.forName(HeapHistogram.class.getName(), true, HeapHistogram.class.getClassLoader());
        } catch (ClassNotFoundException ex) {
            // Can not happen
            throw new AssertionError();
        }
    }

    /**
     * Does a lookup in the runtime instruments of the engine and returns an
     * instance of the {@link CPUTracer}.
     *
     * @since 0.33
     */
    public static HeapHistogram getHistogram(Engine engine) {
        Instrument instrument = engine.getInstruments().get(ID);
        if (instrument == null) {
            throw new IllegalStateException("Heap Histogram is not installed.");
        }
        return instrument.lookup(HeapHistogram.class);
    }

    public static Set<HeapHistogram> getAllHeapHistograms() {
        return new HashSet<>(envs.values());
    }

    /**
     * Called to create the Instrument.
     *
     * @param env environment information for the instrument
     * @since 0.30
     */
    @Override
    protected void onCreate(TruffleInstrument.Env env) {
        histo = factory.create(env);
        if (env.getOptions().get(HeapHistogramInstrument.ENABLED)) {
            histo.setCollecting(true);
        }
        env.registerService(histo);
        envs.put(env, histo);
    }

    /**
     * @return A list of the options provided by the {@link HeapHistogram}.
     * @since 0.30
     */
    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return new HeapHistogramInstrumentOptionDescriptors();
    }

    /**
     * Called when the Instrument is to be disposed.
     *
     * @param env environment information for the instrument
     * @since 0.30
     */
    @Override
    protected void onDispose(TruffleInstrument.Env env) {
        histo.close();
        envs.remove(env);
    }

    @Option(name = "", help = "Enable the Heap Histogram.", category = OptionCategory.USER)
    static final OptionKey<Boolean> ENABLED = new OptionKey<>(false);

}
