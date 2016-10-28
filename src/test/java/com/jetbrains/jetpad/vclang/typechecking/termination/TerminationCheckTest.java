/*
 * Copyright 2003-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.jetpad.vclang.typechecking.termination;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by user on 10/28/16.
 */
public class TerminationCheckTest {

  @Test
  public void test312() {
    TestVertex h = new TestVertex("h", "hx", "hy");
    TestVertex f = new TestVertex("f", "fx", "fy");
    TestVertex g = new TestVertex("g", "gx", "gy");
    Set<BaseCallMatrix> cms = new HashSet<>();
    cms.add(new TestCallMatrix("h-h-1", h, h, '<', 0, '=', 1));
    cms.add(new TestCallMatrix("h-h-2", h, h, '=', 0, '<', 1));
    cms.add(new TestCallMatrix("f-f", f, f, '?', '<', 1));
    cms.add(new TestCallMatrix("f-h", f, h, '?', '?'));
    cms.add(new TestCallMatrix("f-g", f, g, '<', 0, '=', 1));
    cms.add(new TestCallMatrix("g-f", g, f, '=', 0, '=', 1));
    cms.add(new TestCallMatrix("g-g", g, g, '<', 0, '?'));
    cms.add(new TestCallMatrix("g-h", g, h, '?', '?'));
    System.out.println(CallGraph.calculateClosure(new CallGraph(cms)));
  }

}
