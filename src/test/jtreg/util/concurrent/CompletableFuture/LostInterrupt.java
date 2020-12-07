/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

/**
 * @test
 * @bug 8254350
 * @run main LostInterrupt
 */

// TODO: Rewrite as a CompletableFuture tck test

public class LostInterrupt {

    public static void main(String[] args) throws Exception {
        ForkJoinPool executor = new ForkJoinPool(1);
        try {
            for (int i = 0; i < 10_000; i++) {
                var future = new CompletableFuture<String>();
                executor.execute(() -> future.complete("foo"));

                Thread.currentThread().interrupt();
                try {
                    String result = future.get();

                    if (!Thread.interrupted())
                        throw new AssertionError("lost interrupt at run " + i);
                } catch (InterruptedException expected) {
                    if (Thread.interrupted())
                        throw new AssertionError(
                            "interrupt status should be cleared at run " + i);
                }
            }
        } finally {
            executor.shutdown();
        }
    }
}
