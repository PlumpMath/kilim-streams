/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package tests.stream2;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import stream2.Collector;
import stream2.Collectors;
import stream2.LambdaTestHelpers;
import stream2.OpTestCase;
import stream2.Stream;
import stream2.StreamTestDataProvider;
import stream2.TestData;

import org.testng.annotations.Test;
import static stream2.Arrays2.proxy;

import static stream2.LambdaTestHelpers.countTo;
import static stream2.LambdaTestHelpers.countToj;
import static stream2.LambdaTestHelpers.mDoubler;
import static stream2.LambdaTestHelpers.mId;
import static stream2.LambdaTestHelpers.mZero;
import static stream2.LambdaTestHelpers.pEven;
import static stream2.LambdaTestHelpers.pFalse;
import static stream2.LambdaTestHelpers.pOdd;
import static stream2.LambdaTestHelpers.pTrue;

/**
 * GroupByOpTest
 *
 */
@Test
public class GroupByOpTest extends OpTestCase {

    public void testBypassCollect() {
        @SuppressWarnings("unchecked")
        Collector<Integer, Map<Boolean, List<Integer>>, Map<Boolean, List<Integer>>> collector
                = (Collector<Integer, Map<Boolean, List<Integer>>, Map<Boolean, List<Integer>>>) Collectors.groupingBy(LambdaTestHelpers.forPredicate(pEven, true, false));

        Map<Boolean, List<Integer>> m = collector.supplier().get();
        int[] ints = countTo(10).stream().mapToInt(e -> (int) e).toArray();
        for (int i : ints)
            collector.accumulator().accept(m, i);

        assertEquals(2, m.keySet().size());
        for(Collection<Integer> group : m.values()) {
            int count = 0;
            Stream<Integer> stream = proxy(group).stream();
            stream2.Iterator<Integer> it = stream.iterator();
            while (it.hasNext()) {
                it.next();
                ++count;
            }
            assertEquals(5, count);
        }
    }

    public void testGroupBy() {
        Map<Boolean,List<Integer>> result = countToj(10).stream().collect(Collectors.groupingBy(LambdaTestHelpers.forPredicate(pEven, true, false)));

        assertEquals(2, result.keySet().size());
        for(Collection<Integer> group : result.values()) {
            int count = 0;
            Stream<Integer> stream = proxy(group).stream();
            stream2.Iterator<Integer> it = stream.iterator();
            while (it.hasNext()) {
                it.next();
                ++count;
            }
            assertEquals(5, count);
        }
    }

    static class MapperData<T, K> {
        Function<T, K> m;
        int expectedSize;

        MapperData(Function<T, K> m, int expectedSize) {
            this.m = m;
            this.expectedSize = expectedSize;
        }
    }

    List<MapperData<Integer, ?>> getMapperData(TestData.OfRef<Integer> data) {
        int uniqueSize = data.into(new HashSet<>()).size();

        return Arrays.asList(
            new MapperData<>(mId, uniqueSize),
            new MapperData<>(mZero, Math.min(1, data.size())),
            new MapperData<>(mDoubler, uniqueSize),
            new MapperData<>(LambdaTestHelpers.compose(mId, mDoubler), uniqueSize),
            new MapperData<>(LambdaTestHelpers.compose(mDoubler, mDoubler), uniqueSize),

            new MapperData<>(LambdaTestHelpers.forPredicate(pFalse, true, false), Math.min(1, uniqueSize)),
            new MapperData<>(LambdaTestHelpers.forPredicate(pTrue, true, false), Math.min(1, uniqueSize)),
            new MapperData<>(LambdaTestHelpers.forPredicate(pEven, true, false), Math.min(2, uniqueSize)),
            new MapperData<>(LambdaTestHelpers.forPredicate(pOdd, true, false), Math.min(2, uniqueSize))
        );
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testOps(String name, TestData.OfRef<Integer> data) {
        // @@@ More things to test here:
        //     - Every value in data is present in right bucket
        //     - Total number of values equals size of data

        for (MapperData<Integer, ?> md : getMapperData(data)) {
            Collector<Integer, ?, Map<Object, List<Integer>>> tab = Collectors.groupingBy(md.m);
            Map<Object, List<Integer>> result =
                    withData(data)
                    .terminal(s -> s, s -> s.collect(tab))
                    .resultAsserter((act, exp, ord, par) -> {
                        if (par & !ord) {
                            GroupByOpTest.assertMultiMapEquals(act, exp);
                        }
                        else {
                            GroupByOpTest.assertObjectEquals(act, exp);
                        }
                    })
                    .exercise();
            assertEquals(result.keySet().size(), md.expectedSize);
        }
    }

    static void assertObjectEquals(Object a, Object b) {
        assertTrue(Objects.equals(a, b));
    }

    static <K, V> void assertMultiMapEquals(Map<K, ? extends Collection<V>> a, Map<K, ? extends Collection<V>> b) {
        assertTrue(multiMapEquals(a, b));
    }

    static<K, V> boolean multiMapEquals(Map<K, ? extends Collection<V>> a, Map<K, ? extends Collection<V>> b) {
        if (!Objects.equals(a.keySet(), b.keySet())) {
            return false;
        }

        for (K k : a.keySet()) {
            Set<V> as = new HashSet<>(a.get(k));
            Set<V> bs = new HashSet<>(b.get(k));
            if (!Objects.equals(as, bs)) {
                return false;
            }
        }

        return true;
    }
}
