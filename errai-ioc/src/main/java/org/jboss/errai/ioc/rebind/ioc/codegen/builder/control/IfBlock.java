package org.jboss.errai.ioc.rebind.ioc.codegen.builder.control;

import org.jboss.errai.ioc.rebind.ioc.codegen.BlockStatement;
import org.jboss.errai.ioc.rebind.ioc.codegen.Statement;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
public class IfBlock extends AbstractBlockConditional {
    protected Statement elseBlock;
    protected IfBlock elseIf;

    public IfBlock(Statement condition, Statement block) {
        super(condition, new BlockStatement(block));
    }
    
    public IfBlock(Statement condition, Statement block, IfBlock elseIf) {
        this(condition, block);
        this.elseIf = elseIf;
    }
    
    public void setElseBlock(Statement elseBlock) {
        this.elseBlock = elseBlock;
    }
    
    public String generate() {
        StringBuilder builder = new StringBuilder("if ");
        if (getCondition() != null) {
            builder.append("(").append(getCondition().generate()).append(") ");
        }
        builder.append("{\n");

        if (getBlock() != null) {
            builder.append(getBlock().generate());
        }

        builder.append("\n} ");

        if (elseIf != null) {
            builder.append("else ").append(elseIf.generate());
            return builder.toString();
        }
        
        if (elseBlock != null) {
            builder.append("else { ").append(elseBlock.generate()).append("\n} ");
            return builder.toString();
        }

        return builder.toString();
    }
}
