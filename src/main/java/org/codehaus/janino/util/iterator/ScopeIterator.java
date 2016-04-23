
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2006, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino.util.iterator;

import java.util.*;

import org.codehaus.janino.*;

/**
 * An {@link java.util.Iterator} that iterates over scoped statements.
 * This iterator is used to iterate over the objects that implement
 * the Scope interface until there is no longer an enclosing scope.
 */
public class ScopeIterator implements Iterator {
    protected Java.Scope s;

    // Create ScopeIterator starting with the Scope that encloses
    // the given Statement.

    public ScopeIterator(Java.Statement stmt) { s = stmt.getEnclosingScope(); }

    // Create ScopeIterator starting with the given Scope.

    public ScopeIterator(Java.Scope es) { s = es; }

    public boolean hasNext() { return (s instanceof Java.Scope); }
    public Object  next()    { return (s = s.getEnclosingScope()); }
    public void    remove()  { throw new RuntimeException("unsupported remove()"); }
}

