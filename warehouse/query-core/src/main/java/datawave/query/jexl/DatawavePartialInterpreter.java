package datawave.query.jexl;

import datawave.query.collections.FunctionalSet;
import datawave.query.jexl.functions.ContentFunctions;
import datawave.query.jexl.functions.FunctionJexlNodeVisitor;
import datawave.query.jexl.functions.JexlFunctionArgumentDescriptorFactory;
import datawave.query.jexl.functions.arguments.JexlArgumentDescriptor;
import datawave.query.jexl.nodes.ExceededOrThresholdMarkerJexlNode;
import datawave.query.jexl.nodes.QueryPropertyMarker;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.jexl2.parser.ASTAndNode;
import org.apache.commons.jexl2.parser.ASTAssignment;
import org.apache.commons.jexl2.parser.ASTEQNode;
import org.apache.commons.jexl2.parser.ASTERNode;
import org.apache.commons.jexl2.parser.ASTFunctionNode;
import org.apache.commons.jexl2.parser.ASTGENode;
import org.apache.commons.jexl2.parser.ASTGTNode;
import org.apache.commons.jexl2.parser.ASTIdentifier;
import org.apache.commons.jexl2.parser.ASTIfStatement;
import org.apache.commons.jexl2.parser.ASTLENode;
import org.apache.commons.jexl2.parser.ASTLTNode;
import org.apache.commons.jexl2.parser.ASTNENode;
import org.apache.commons.jexl2.parser.ASTNRNode;
import org.apache.commons.jexl2.parser.ASTNotNode;
import org.apache.commons.jexl2.parser.ASTOrNode;
import org.apache.commons.jexl2.parser.ASTReference;
import org.apache.commons.jexl2.parser.JexlNode;
import org.apache.commons.jexl2.parser.JexlNodes;
import org.apache.log4j.Logger;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Set;

/**
 * Extended so that calls to a function node, which can return a collection of 'hits' instead of a Boolean, can be evaluated as true/false based on the size of
 * the hit collection. Also, if the member 'arithmetic' has been set to a HitListArithmetic, then the returned hits will be added to the hitSet of the
 * arithmetic.
 *
 * Also added in the ability to count attributes pulled from the ValueTuples which contribute to the positive evaluation.
 *
 *
 */
public class DatawavePartialInterpreter extends DatawaveInterpreter {
    
    private static final Logger log = Logger.getLogger(DatawavePartialInterpreter.class);
    private final Set<String> incompleteFields;
    
    public DatawavePartialInterpreter(JexlEngine jexl, JexlContext aContext, boolean strictFlag, boolean silentFlag, Set<String> incompleteFields) {
        super(jexl, aContext, strictFlag, silentFlag);
        this.incompleteFields = incompleteFields;
    }
    
    public enum MATCH {
        TRUE, FALSE, UNKNOWN;
        
        public static MATCH valueFor(boolean value) {
            return (value ? TRUE : FALSE);
        }
        
        public MATCH negate() {
            switch (this) {
                case UNKNOWN:
                    return UNKNOWN;
                case FALSE:
                    return TRUE;
                default:
                    return FALSE;
            }
        }
    }
    
    /**
     * Determine whether a node expression contains an incomplete fieldname
     * <p>
     * Incomplete nodes contain at least one field that is both in the context and in the set of incomplete fields
     * 
     * @param node
     * @return true if incomplete fieldname found, false otherwise
     */
    private boolean isIncomplete(JexlNode node) {
        for (String field : JexlASTHelper.getIdentifierNames(node)) {
            field = JexlASTHelper.deconstructIdentifier(field);
            // if the field is not in the context, we cannot evaluate it.
            if (incompleteFields.contains(field) && context.has(field)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * This convenience method can be used to interpret the result of the script.execute() result which calls the interpret method below. If the result is false
     * but it involves incomplete fields, then MATCH.UNKNOWN is returned. Other with MATCH.TRUE or MATCH.FALSE is returned as appropriate
     *
     * @param node
     * @param scriptExecuteResult
     * @return the MATCH value
     */
    public MATCH getMatched(JexlNode node, Object scriptExecuteResult) {
        return getMatched(node, scriptExecuteResult, false);
    }
    
    public MATCH getMatched(JexlNode node, Object scriptExecuteResult, boolean negated) {
        MATCH matched = getMatched(scriptExecuteResult);
        
        // now determine whether the result should actually be unknown
        if ((matched == (negated ? MATCH.TRUE : MATCH.FALSE)) && isIncomplete(node)) {
            matched = MATCH.UNKNOWN;
        }
        
        return matched;
    }
    
    public MATCH getMatched(Object scriptExecuteResult) {
        MATCH matched = MATCH.FALSE;
        if (scriptExecuteResult != null) {
            if (MATCH.class.isAssignableFrom(scriptExecuteResult.getClass())) {
                matched = (MATCH) scriptExecuteResult;
            } else if (Boolean.class.isAssignableFrom(scriptExecuteResult.getClass())) {
                matched = MATCH.valueFor(isMatched(scriptExecuteResult));
            } else {
                matched = MATCH.valueFor(arithmetic.toBoolean(scriptExecuteResult));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Unable to process null result from JEXL evaluation");
            }
        }
        return matched;
    }
    
    public static boolean isMatched(Object scriptExecuteResult) {
        if (scriptExecuteResult != null && DatawavePartialInterpreter.MATCH.class.isAssignableFrom(scriptExecuteResult.getClass())) {
            DatawavePartialInterpreter.MATCH match = (DatawavePartialInterpreter.MATCH) scriptExecuteResult;
            switch (match) {
                case FALSE:
                    return false;
                case UNKNOWN:
                case TRUE:
                    return true;
                default:
                    throw new IllegalStateException("Unexpected value: " + match);
            }
        } else {
            return DatawaveInterpreter.isMatched(scriptExecuteResult);
        }
    }
    
    /**
     * More broadly, what happens when a phrase function is queried against an incomplete field?
     * <p>
     * Form A: content:phrase(FIELD, tfmap, Field A, Field B)
     * <p>
     * Form B: content:phrase(tfmap, Field A, Field B) && ...
     *
     * @param node
     *            an ASTFunctionNode
     * @param data
     *            some data
     * @return a result
     */
    @Override
    public Object visit(ASTFunctionNode node, Object data) {
        
        // get the namespace
        FunctionJexlNodeVisitor visitor = new FunctionJexlNodeVisitor();
        visitor.visit(node, null);
        
        // content functions should always have the field as the first argument
        if (visitor.namespace().equals(ContentFunctions.CONTENT_FUNCTION_NAMESPACE)) {
            JexlNode first = visitor.args().get(0);
            if (first instanceof ASTIdentifier) {
                String field = JexlASTHelper.deconstructIdentifier((ASTIdentifier) first);
                if (context.has(field) && incompleteFields.contains(field)) {
                    return MATCH.UNKNOWN;
                }
            }
        } else {
            // fall back to descriptor for non-content functions
            JexlArgumentDescriptor descriptor = JexlFunctionArgumentDescriptorFactory.F.getArgumentDescriptor(node);
            Set<String> fields = descriptor.fields(null, null);
            
            boolean incomplete = false;
            for (String field : fields) {
                if (context.has(field) && incompleteFields.contains(field)) {
                    incomplete = true;
                    break;
                }
            }
            
            if (incomplete) {
                return MATCH.UNKNOWN;
            }
        }
        
        // if no determination could be made about an UNKNOWN status, fallback to a normal visit
        Object result = super.visit(node, data);
        
        if (result instanceof Collection) {
            if (hasSiblings(node)) {
                return result;
            } else {
                return ((Collection<?>) result).isEmpty();
            }
        }
        
        return getMatched(node, result);
    }
    
    @Override
    public Object visit(ASTEQNode node, Object data) {
        return getMatched(node, super.visit(node, data));
    }
    
    @Override
    public Object visit(ASTERNode node, Object data) {
        return getMatched(node, super.visit(node, data));
    }
    
    @Override
    public Object visit(ASTOrNode node, Object data) {
        Deque<JexlNode> children = new ArrayDeque<>();
        Deque<JexlNode> stack = new ArrayDeque<>();
        stack.push(node);
        
        boolean allIdentifiers = true;
        
        // iterative depth-first traversal of tree to avoid stack
        // overflow when traversing large or'd lists
        JexlNode current;
        JexlNode child;
        while (!stack.isEmpty()) {
            current = stack.pop();
            
            if (current instanceof ASTOrNode) {
                for (int i = current.jjtGetNumChildren() - 1; i >= 0; i--) {
                    child = JexlASTHelper.dereference(current.jjtGetChild(i));
                    stack.push(child);
                }
            } else {
                children.push(current);
                if (allIdentifiers && !(current instanceof ASTIdentifier)) {
                    allIdentifiers = false;
                }
            }
        }
        
        // If all ASTIdentifiers, then traverse the whole queue. Otherwise we can attempt to short circuit.
        Object result = null;
        if (allIdentifiers) {
            // Likely within a function and must visit every child in the stack.
            // Failure to do so will short circuit value aggregation leading to incorrect function evaluation.
            while (!children.isEmpty()) {
                // Child nodes were put onto the stack left to right. PollLast to evaluate left to right.
                result = interpretOr(children.pollLast().jjtAccept(this, data), result);
            }
        } else {
            // We are likely within a normal union and can short circuit
            while (getMatched(result) == MATCH.FALSE && !children.isEmpty()) {
                // Child nodes were put onto the stack left to right. PollLast to evaluate left to right.
                result = interpretOr(children.pollLast().jjtAccept(this, data), result);
            }
        }
        
        return result;
    }
    
    /**
     * Interpret the result of a union via pairwise element evaluation
     *
     * @param left
     *            the current element
     * @param right
     *            the current evaluation state of this union
     * @return the updated evaluation state of this union
     */
    @Override
    public Object interpretOr(Object left, Object right) {
        FunctionalSet leftFunctionalSet = null;
        FunctionalSet rightFunctionalSet = null;
        if (left == null)
            left = FunctionalSet.empty();
        if (!(left instanceof Collection)) {
            try {
                MATCH leftValue = getMatched(left);
                if (leftValue == MATCH.TRUE || leftValue == MATCH.UNKNOWN) {
                    return leftValue;
                }
            } catch (ArithmeticException xrt) {
                throw new RuntimeException(left.toString() + " MATCH coercion error", xrt);
            }
        } else {
            leftFunctionalSet = new FunctionalSet();
            leftFunctionalSet.addAll((Collection) left);
        }
        if (right == null)
            right = FunctionalSet.empty();
        if (!(right instanceof Collection)) {
            try {
                MATCH rightValue = getMatched(right);
                if (rightValue == MATCH.TRUE) {
                    return Boolean.TRUE;
                }
            } catch (ArithmeticException xrt) {
                throw new RuntimeException(right.toString() + " MATCH coercion error", xrt);
            }
        } else {
            rightFunctionalSet = new FunctionalSet();
            rightFunctionalSet.addAll((Collection) right);
        }
        // when an identifier is expanded by the data model within a Function node, the results of the matches
        // for both (all?) fields must be gathered into a single collection to be returned.
        if (leftFunctionalSet != null && rightFunctionalSet != null) { // add left and right
            FunctionalSet functionalSet = new FunctionalSet(leftFunctionalSet);
            functionalSet.addAll(rightFunctionalSet);
            return functionalSet;
        } else if (leftFunctionalSet != null) {
            return leftFunctionalSet;
        } else if (rightFunctionalSet != null) {
            return rightFunctionalSet;
        } else {
            return getMatchedOr(left, right);
        }
    }
    
    private MATCH getMatchedOr(Object left, Object right) {
        left = getMatched(left);
        right = getMatched(right);
        if (left == MATCH.TRUE || right == MATCH.TRUE) {
            return MATCH.TRUE;
        } else if (left == MATCH.FALSE && right == MATCH.FALSE) {
            return MATCH.FALSE;
        } else {
            return MATCH.UNKNOWN;
        }
    }
    
    @Override
    public Object visit(ASTAndNode node, Object data) {
        // we could have arrived here after the node was dereferenced
        if (QueryPropertyMarker.findInstance(node).isType(ExceededOrThresholdMarkerJexlNode.class)) {
            return getMatched(node, visitExceededOrThresholdMarker(node));
        }
        
        // check for the special case of a range (conjunction of a G/GE and a L/LE node) and reinterpret as a function
        Object evaluation = evaluateRange(node);
        if (evaluation != null) {
            return getMatched(node, evaluation);
        }
        
        // values for intersection
        FunctionalSet functionalSet = new FunctionalSet();
        for (JexlNode child : JexlNodes.children(node)) {
            
            Object o = child.jjtAccept(this, data);
            if (o == null) {
                o = FunctionalSet.empty();
            }
            
            if (o instanceof Collection) {
                if (((Collection<?>) o).isEmpty()) {
                    return Boolean.FALSE;
                } else {
                    functionalSet.addAll((Collection<?>) o);
                }
            } else if (o instanceof MATCH) {
                if (o.equals(MATCH.FALSE)) {
                    return MATCH.FALSE;
                }
            } else {
                try {
                    boolean value = arithmetic.toBoolean(o);
                    if (!value) {
                        return Boolean.FALSE;
                    }
                } catch (RuntimeException xrt) {
                    throw new JexlException(child, "boolean coercion error", xrt);
                }
            }
        }
        
        if (!functionalSet.isEmpty()) {
            return functionalSet;
        } else {
            return MATCH.TRUE;
        }
    }
    
    @Override
    public Object visit(ASTAssignment node, Object data) {
        super.visit(node, data);
        return MATCH.TRUE;
    }
    
    @Override
    public Object visit(ASTGENode node, Object data) {
        return getMatched(node, super.visit(node, data));
    }
    
    @Override
    public Object visit(ASTGTNode node, Object data) {
        return getMatched(node, super.visit(node, data));
    }
    
    @Override
    public Object visit(ASTIfStatement node, Object data) {
        Object expression = node.jjtGetChild(0).jjtAccept(this, data);
        if (getMatched(expression) == MATCH.UNKNOWN) {
            return MATCH.UNKNOWN;
        }
        return super.visit(node, data);
    }
    
    @Override
    public Object visit(ASTLENode node, Object data) {
        return getMatched(node, super.visit(node, data));
    }
    
    @Override
    public Object visit(ASTLTNode node, Object data) {
        return getMatched(node, super.visit(node, data));
    }
    
    @Override
    public Object visit(ASTNENode node, Object data) {
        return getMatched(node, super.visit(node, data), false);
    }
    
    @Override
    public Object visit(ASTNRNode node, Object data) {
        return getMatched(node, super.visit(node, data), false);
    }
    
    @Override
    public Object visit(ASTNotNode node, Object data) {
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        return getMatched(val).negate();
    }
    
    @Override
    public Object visit(ASTReference node, Object data) {
        if (QueryPropertyMarker.findInstance(node).isType(ExceededOrThresholdMarkerJexlNode.class)) {
            return getMatched(node, visitExceededOrThresholdMarker(node));
        } else {
            return super.visit(node, data);
        }
    }
}
