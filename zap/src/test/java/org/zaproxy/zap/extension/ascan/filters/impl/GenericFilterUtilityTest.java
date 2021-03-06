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
package org.zaproxy.zap.extension.ascan.filters.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zaproxy.zap.WithConfigsTest;
import org.zaproxy.zap.extension.ascan.filters.FilterCriteria;
import org.zaproxy.zap.extension.ascan.filters.FilterResult;
import org.zaproxy.zap.model.StructuralNode;

/** @author KSASAN preetkaran20@gmail.com */
public class GenericFilterUtilityTest extends WithConfigsTest {

    private AbstractGenericScanFilter<String, String> abstractGenericScanFilter;

    @Before
    public void init() {
        abstractGenericScanFilter =
                new AbstractGenericScanFilter<String, String>() {

                    @Override
                    public FilterResult isFiltered(StructuralNode node) {
                        return null;
                    }

                    @Override
                    public String getFilterType() {
                        return null;
                    }
                };
    }

    @Test
    public void testEmptyFilterValuesIncludeCriteria() {
        // Given
        List<String> genericFilterData = new ArrayList<String>();
        abstractGenericScanFilter.setFilterData(genericFilterData);

        Set<String> values = new HashSet<>();
        values.add("Dummy");

        // When
        FilterResult filterResult = abstractGenericScanFilter.isFiltered(values);

        // Then
        Assert.assertEquals(filterResult.isFiltered(), false);
    }

    @Test
    public void testIncludeCriteriaWithSameValues() {
        // Given
        List<String> genericFilterData = new ArrayList<String>();
        genericFilterData.add("Dummy");

        abstractGenericScanFilter.setFilterData(genericFilterData);

        Set<String> values = new HashSet<>();
        values.add("Dummy");

        // When
        FilterResult filterResult = abstractGenericScanFilter.isFiltered(values);

        // Then
        Assert.assertEquals(filterResult.isFiltered(), false);
    }

    @Test
    public void testIncludeCriteriaWithMoreFilterValues() {
        // Given
        List<String> genericFilterData = new ArrayList<String>();
        genericFilterData.add("Dummy");
        genericFilterData.add("Dummy1");

        abstractGenericScanFilter.setFilterData(genericFilterData);

        Set<String> values = new HashSet<>();
        values.add("Dummy");

        // When
        FilterResult filterResult = abstractGenericScanFilter.isFiltered(values);

        // Then
        Assert.assertEquals(filterResult.isFiltered(), false);
    }

    @Test
    public void testIncludeCriteriaWithMoreValues() {
        // Given
        List<String> genericFilterData = new ArrayList<String>();
        genericFilterData.add("Dummy");

        abstractGenericScanFilter.setFilterData(genericFilterData);
        Set<String> values = new HashSet<>();
        values.add("Dummy");
        values.add("Dummy1");

        // When
        FilterResult filterResult = abstractGenericScanFilter.isFiltered(values);

        // Then
        Assert.assertEquals(filterResult.isFiltered(), false);
    }

    @Test
    public void testShouldFailWhenValuesAndFilterDataValuesMismatchIncludeCriteria() {
        // Given
        List<String> genericFilterData = new ArrayList<String>();
        genericFilterData.add("Dummy");

        abstractGenericScanFilter.setFilterData(genericFilterData);

        Set<String> values = new HashSet<>();
        values.add("Dummy1");

        // When
        FilterResult filterResult = abstractGenericScanFilter.isFiltered(values);

        // Then
        Assert.assertEquals(filterResult.isFiltered(), true);
    }

    @Test
    public void testEmptyValuesInExcludeCriteria() {
        // Given
        List<String> genericFilterData = new ArrayList<String>();
        genericFilterData.add("Dummy");

        abstractGenericScanFilter.setFilterData(genericFilterData);
        abstractGenericScanFilter.setFilterCriteria(FilterCriteria.EXCLUDE);

        Set<String> values = new HashSet<>();

        // When
        FilterResult filterResult = abstractGenericScanFilter.isFiltered(values);

        // Then
        Assert.assertEquals(filterResult.isFiltered(), false);
    }

    @Test
    public void testShouldFailWhenSameValuesInExcludeCriteria() {
        // Given
        List<String> genericFilterData = new ArrayList<String>();
        genericFilterData.add("Dummy");

        abstractGenericScanFilter.setFilterData(genericFilterData);
        abstractGenericScanFilter.setFilterCriteria(FilterCriteria.EXCLUDE);

        Set<String> values = new HashSet<>();
        values.add("Dummy");

        // When
        FilterResult filterResult = abstractGenericScanFilter.isFiltered(values);

        // Then
        Assert.assertEquals(filterResult.isFiltered(), true);
    }

    @Test
    public void testShouldFailInExcludeCriteriaWithMoreFilterValues() {
        // Given
        List<String> genericFilterData = new ArrayList<String>();
        genericFilterData.add("Dummy");
        genericFilterData.add("Dummy1");

        abstractGenericScanFilter.setFilterData(genericFilterData);
        abstractGenericScanFilter.setFilterCriteria(FilterCriteria.EXCLUDE);

        Set<String> values = new HashSet<>();
        values.add("Dummy");

        // When
        FilterResult filterResult = abstractGenericScanFilter.isFiltered(values);

        // Then
        Assert.assertEquals(filterResult.isFiltered(), true);
    }

    @Test
    public void testShouldFailWhenExcludeCriteriaWithMoreValues() {
        // Given
        List<String> genericFilterData = new ArrayList<String>();
        genericFilterData.add("Dummy");

        abstractGenericScanFilter.setFilterData(genericFilterData);
        abstractGenericScanFilter.setFilterCriteria(FilterCriteria.EXCLUDE);

        Set<String> values = new HashSet<>();
        values.add("Dummy");
        values.add("Dummy1");

        // When
        FilterResult filterResult = abstractGenericScanFilter.isFiltered(values);

        // Then
        Assert.assertEquals(filterResult.isFiltered(), true);
    }

    @Test
    public void testShouldPassForValuesAndFilterValuesMismatchInExcludeCriteria() {
        // Given
        List<String> genericFilterData = new ArrayList<String>();
        genericFilterData.add("Dummy");

        abstractGenericScanFilter.setFilterData(genericFilterData);
        abstractGenericScanFilter.setFilterCriteria(FilterCriteria.EXCLUDE);

        Set<String> values = new HashSet<>();
        values.add("Dummy1");

        // When
        FilterResult filterResult = abstractGenericScanFilter.isFiltered(values);

        // Then
        Assert.assertEquals(filterResult.isFiltered(), false);
    }
}
