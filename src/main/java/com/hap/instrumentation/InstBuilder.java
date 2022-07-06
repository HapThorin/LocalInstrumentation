/*
 * Copyright (C) 2021-2021 HapThorin. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hap.instrumentation;

import com.sun.tools.attach.VirtualMachine;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * 获取当前vm的{@link java.lang.instrument.Instrumentation}的构建器
 *
 * @author HapThorin
 * @version 1.0.0
 * @date 2022/7/6
 */
public class InstBuilder {
    /**
     * 当前vm的{@link java.lang.instrument.Instrumentation}
     */
    public static Instrumentation inst;

    /**
     * 当前进程的pid
     *
     * @return 当前进程的pid
     */
    private static String getLocalPid() {
        return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    }

    /**
     * 获取默认的Agent Manifest
     *
     * @param agentClass agent主类
     * @return 默认的Agent Manifest
     */
    private static Manifest getAgentManifest(String agentClass) {
        final Manifest manifest = new Manifest();
        final Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue("Agent-Class", agentClass);
        attributes.putValue("Can-Redefine-Classes", "true");
        attributes.putValue("Can-Retransform-Classes", "true");
        return manifest;
    }

    /**
     * 获取默认的Agent Jar包临时路径
     *
     * @return 默认的Agent Jar包临时路径
     * @throws IOException jar包写出失败
     */
    private static String getAgentJarPath() throws IOException {
        final String agentClass = InstBuilder.class.getName();
        final String agentRef = agentClass.replace('.', '/') + ".class";
        final InputStream is = InstBuilder.class.getClassLoader().getResourceAsStream(agentRef);
        if (is == null) return null;
        final File agentJar = Files.createTempFile("DummyAgent", ".jar").toFile();
        agentJar.deleteOnExit();
        try (JarOutputStream os = new JarOutputStream(new FileOutputStream(agentJar), getAgentManifest(agentClass))) {
            os.putNextEntry(new JarEntry(agentRef));
            IOUtils.copy(is, os);
        }
        return agentJar.getCanonicalPath();
    }

    /**
     * 获取当前vm的{@link java.lang.instrument.Instrumentation}
     *
     * @return 当前vm的Instrumentation
     * @throws Exception 获取失败
     */
    public static Instrumentation getLocalInst() throws Exception {
        if (inst != null) return inst;
        VirtualMachine.attach(getLocalPid()).loadAgent(getAgentJarPath());
        return inst;
    }

    /**
     * 默认agentmain
     *
     * @param agentArgs 空入参
     * @param inst      当前vm的Instrumentation
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        InstBuilder.inst = inst;
    }
}
