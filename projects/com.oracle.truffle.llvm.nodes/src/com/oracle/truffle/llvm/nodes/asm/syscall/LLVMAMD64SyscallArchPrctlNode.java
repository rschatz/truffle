/*
 * Copyright (c) 2017, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.truffle.llvm.nodes.asm.syscall;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.IntValueProfile;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMAddressStoreNode;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMAddressStoreNodeGen;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMTruffleObject;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.VoidType;

public abstract class LLVMAMD64SyscallArchPrctlNode extends LLVMAMD64SyscallOperationNode {
    private final IntValueProfile profile = IntValueProfile.createIdentityProfile();

    public LLVMAMD64SyscallArchPrctlNode() {
        super("arch_prctl");
    }

    @Specialization
    protected long doOp(VirtualFrame frame, long code, long addr,
                    @Cached("getContextReference()") ContextReference<LLVMContext> context,
                    @Cached("createAddressStoreNode()") LLVMAddressStoreNode store) {
        return exec(frame, code, addr, context, store);
    }

    @Specialization
    protected long doOp(VirtualFrame frame, long code, LLVMAddress addr,
                    @Cached("getContextReference()") ContextReference<LLVMContext> context,
                    @Cached("createAddressStoreNode()") LLVMAddressStoreNode store) {
        return exec(frame, code, addr, context, store);
    }

    @Specialization
    protected long doOp(VirtualFrame frame, long code, LLVMTruffleObject addr,
                    @Cached("getContextReference()") ContextReference<LLVMContext> context,
                    @Cached("createAddressStoreNode()") LLVMAddressStoreNode store) {
        return exec(frame, code, addr, context, store);
    }

    @TruffleBoundary
    protected LLVMAddressStoreNode createAddressStoreNode() {
        return LLVMAddressStoreNodeGen.create(new PointerType(VoidType.INSTANCE));
    }

    private long exec(VirtualFrame frame, long code, Object addr, ContextReference<LLVMContext> context, LLVMAddressStoreNode store) throws AssertionError {
        switch (profile.profile((int) code)) {
            case LLVMAMD64ArchPrctl.ARCH_SET_FS:
                context.get().setThreadLocalStorage(addr);
                break;
            case LLVMAMD64ArchPrctl.ARCH_GET_FS: {
                Object tls = getContextReference().get().getThreadLocalStorage();
                store.executeWithTarget(frame, addr, tls);
                break;
            }
            default:
                CompilerDirectives.transferToInterpreter();
                throw new AssertionError(String.format("not implemented: arch_prcntl(0x%04x)", code));
        }
        return 0;
    }
}
