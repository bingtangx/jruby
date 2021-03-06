package org.jruby.compiler.ir.instructions.jruby;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.targets.JVM;
import org.jruby.RubyArray;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class CheckArgsArrayArityInstr extends Instr {
    public final int required;
    public final int opt;
    public final int rest;
    private Operand argsArray;

    public CheckArgsArrayArityInstr(Operand argsArray, int required, int opt, int rest) {
        super(Operation.CHECK_ARGS_ARRAY_ARITY);
        
        this.required = required;
        this.opt = opt;
        this.rest = rest;
        this.argsArray = argsArray;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { argsArray };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        argsArray = argsArray.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + argsArray + ", " +  required + ", " + opt + ", " + rest + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new CheckArgsArrayArityInstr(argsArray.cloneForInlining(ii), required, opt, rest);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        RubyArray args = (RubyArray) argsArray.retrieve(context, self, currDynScope, temp);
        int numArgs = args.size();
        if ((numArgs < required) || ((rest == -1) && (numArgs > (required + opt)))) {
            Arity.raiseArgumentError(context.getRuntime(), numArgs, required, required + opt);
        }
        return null;
    }
}
