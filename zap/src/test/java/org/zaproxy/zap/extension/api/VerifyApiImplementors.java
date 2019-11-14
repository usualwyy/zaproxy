/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2019 The ZAP Development Team
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
package org.zaproxy.zap.extension.api;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.BeforeClass;
import org.junit.Test;
import org.parosproxy.paros.Constant;
import org.zaproxy.zap.extension.api.API.RequestType;
import org.zaproxy.zap.utils.I18N;

/**
 * Verifies that the {@link ApiElement}s and its parameters of all core {@link ApiImplementor}s have
 * resource keys for the descriptions.
 */
public class VerifyApiImplementors {

    private static I18N i18n;
    private static List<String> missingKeys;

    @BeforeClass
    public static void setup() throws Exception {
        i18n = new I18N(Locale.ENGLISH);
        Constant.messages = i18n;
        missingKeys = new ArrayList<>();
    }

    @Test
    public void shouldHaveDescriptionsForAllApis() {
        // Given / When
        List<ApiImplementor> apis = ApiGeneratorUtils.getAllImplementors();
        apis.sort((a, b) -> a.getPrefix().compareTo(b.getPrefix()));
        // Then
        for (ApiImplementor api : apis) {
            checkKey(api.getDescriptionKey());
            checkApiElements(api, api.getApiActions(), API.RequestType.action);
            checkApiElements(api, api.getApiOthers(), API.RequestType.other);
            checkApiElements(api, api.getApiViews(), API.RequestType.view);
        }
        assertThat(missingKeys, is(empty()));
    }

    private static void checkApiElements(
            ApiImplementor api, List<? extends ApiElement> elements, RequestType type) {
        elements.sort((a, b) -> a.getName().compareTo(b.getName()));
        for (ApiElement element : elements) {
            String baseKey = api.getPrefix() + ".api." + type + "." + element.getName();
            String key = element.getDescriptionTag();
            if (key == null || key.isEmpty()) {
                key = baseKey;
            }

            checkKey(key);

            checkParameters(baseKey + ".param.", element.getMandatoryParamNames());
            checkParameters(baseKey + ".param.", element.getOptionalParamNames());
        }
    }

    private static void checkParameters(String keyPrefix, List<String> params) {
        if (params == null || params.isEmpty()) {
            return;
        }

        for (String param : params) {
            checkKey(keyPrefix + param);
        }
    }

    private static void checkKey(String key) {
        if (!i18n.containsKey(key)) {
            missingKeys.add(key);
        }
    }
}
