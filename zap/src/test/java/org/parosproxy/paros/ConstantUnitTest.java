/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2018 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.parosproxy.paros;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Unit test for {@link Constant}. */
public class ConstantUnitTest {

    @ClassRule public static TemporaryFolder tempDir = new TemporaryFolder();
    private Path zapInstallDir;
    private Path zapHomeDir;

    @Before
    public void before() throws Exception {
        Path parentDir = tempDir.newFolder().toPath();
        zapInstallDir = Files.createDirectories(parentDir.resolve("install"));
        zapHomeDir = Files.createDirectories(parentDir.resolve("home"));
    }

    @Test
    public void shouldInitialiseHomeDirFromInstallDir() throws IOException {
        // Given
        String configContents = "<config><version>0</version></config>";
        installationFile("xml/config.xml", configContents);
        String log4jContents = "log4j.rootLogger...";
        installationFile("xml/log4j.properties", log4jContents);
        Constant.setZapInstall(zapInstallDir.toString());
        Constant.setZapHome(zapHomeDir.toString());
        // When
        new Constant();
        // Then
        assertHomeFile("config.xml", configContents);
        assertHomeFile("log4j.properties", log4jContents);
        assertHomeDirs();
        assertThat(Files.walk(zapHomeDir).count(), is(equalTo(7L)));
    }

    @Test
    public void shouldInitialiseHomeDirFromBundledFiles() throws IOException {
        // Given
        Constant.setZapInstall(zapInstallDir.toString());
        Constant.setZapHome(zapHomeDir.toString());
        // When
        new Constant();
        // Then
        assertHomeFile("config.xml", defaultContents("config.xml"));
        assertHomeFile("log4j.properties", defaultContents("log4j.properties"));
        assertHomeDirs();
        assertThat(Files.walk(zapHomeDir).count(), is(equalTo(8L)));
    }

    @Test
    public void shouldRestoreDefaultConfigFileIfOneInHomeIsMalformed() throws IOException {
        // Given
        String malformedConfig = "not a valid config";
        homeFile("config.xml", malformedConfig);
        Constant.setZapInstall(zapInstallDir.toString());
        Constant.setZapHome(zapHomeDir.toString());
        // When
        new Constant();
        // Then
        assertHomeFile("config.xml", defaultContents("config.xml"));
        assertHomeFile(getNameBackupMalformedConfig(), malformedConfig);
    }

    private void assertHomeFile(String name, String contents) throws IOException {
        Path file = zapHomeDir.resolve(name);
        assertThat(Files.exists(file), is(true));
        assertThat(contents(file), is(equalTo(contents)));
    }

    private void assertHomeDirs() {
        assertThat(Files.isDirectory(zapHomeDir.resolve("dirbuster")), is(true));
        assertThat(Files.isDirectory(zapHomeDir.resolve("fuzzers")), is(true));
        assertThat(Files.isDirectory(zapHomeDir.resolve("plugin")), is(true));
        assertThat(Files.isDirectory(zapHomeDir.resolve("session")), is(true));
    }

    private static String defaultContents(String name) throws IOException {
        try (InputStream is =
                Constant.class.getResourceAsStream("/org/zaproxy/zap/resources/" + name)) {
            if (is == null) {
                throw new IOException("File not found: " + name);
            }
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }

    private void installationFile(String name, String contents) throws IOException {
        createFile(zapInstallDir.resolve(name), contents);
    }

    private static void createFile(Path file, String contents) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, contents.getBytes(StandardCharsets.UTF_8));
    }

    private void homeFile(String name, String contents) throws IOException {
        createFile(zapHomeDir.resolve(name), contents);
    }

    private static String contents(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private String getNameBackupMalformedConfig() throws IOException {
        Optional<Path> file =
                Files.list(zapHomeDir)
                        .filter(
                                f -> {
                                    String name = f.getFileName().toString();
                                    return name.startsWith("config-") && name.endsWith(".xml.bak");
                                })
                        .findFirst();

        if (file.isPresent()) {
            return file.get().getFileName().toString();
        }
        return null;
    }
}
