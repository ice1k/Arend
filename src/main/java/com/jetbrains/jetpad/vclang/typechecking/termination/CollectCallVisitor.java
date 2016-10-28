package com.jetbrains.jetpad.vclang.typechecking.termination;

/*Generated by MPS */

import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.SubstVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.Clause;
import java.util.Set;
import java.util.HashSet;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import java.util.HashMap;
import com.jetbrains.jetpad.vclang.term.expr.AppExpression;
import com.jetbrains.jetpad.vclang.term.expr.ProjExpression;
import java.util.Map;
import com.jetbrains.jetpad.vclang.term.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.FunCallExpression;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ConstructorClause;
import com.jetbrains.jetpad.vclang.term.expr.LetExpression;
import com.jetbrains.jetpad.vclang.term.expr.LetClause;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.term.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.FieldCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.internal.FieldSet;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.LamExpression;
import com.jetbrains.jetpad.vclang.term.expr.PiExpression;
import com.jetbrains.jetpad.vclang.term.expr.SigmaExpression;
import com.jetbrains.jetpad.vclang.term.expr.TupleExpression;
import com.jetbrains.jetpad.vclang.term.expr.NewExpression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.ErrorExpression;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.term.expr.InferenceReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.OfTypeExpression;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.termination.CollectCallVisitor.ParameterVector;

public class CollectCallVisitor implements ElimTreeNodeVisitor<ParameterVector, Void>, ExpressionVisitor<ParameterVector, Void> {
  private Set<BaseCallMatrix> myCollectedCalls = new HashSet<>();
  private FunctionDefinition myDefinition;

  public CollectCallVisitor(FunctionDefinition def) {
    myDefinition = def;
    ParameterVector pv = new ParameterVector(def);
    if (def.getElimTree() != null) {
      def.getElimTree().accept(this, pv);
    }
  }

  public Set<BaseCallMatrix> getResult() {
    return myCollectedCalls;
  }

  private BaseCallMatrix.R compare(Expression argument, Expression sample) {
    // collect parts 
    HashMap<Expression, BaseCallMatrix.R> parts = new HashMap<>();
    collectParts(sample, parts, BaseCallMatrix.R.Equal);
    // strip currentExpression of App & Proj calls 
    boolean f;
    do {
      f = false;
      if (argument instanceof AppExpression) {
        argument = argument.getFunction();
        f = true;
      }
      if (argument instanceof ProjExpression) {
        argument = ((ProjExpression) argument).getExpression();
        f = true;
      }
    } while (f);

    // now try to find argument among parts 
    for (Expression part : parts.keySet()) {
      if (part.equals(argument)) {
        return parts.get(part);
      }
    }

    return BaseCallMatrix.R.Unknown;
  }

  private void collectParts(Expression e, Map<Expression, BaseCallMatrix.R> parts, BaseCallMatrix.R r) throws IllegalStateException {
    if (e instanceof ConCallExpression) {
      parts.put(e, r);
      ConCallExpression cce = (ConCallExpression) e;
      for (Expression dca : cce.getDefCallArguments()) {
        collectParts(dca, parts, BaseCallMatrix.R.LessThan);
      }
    } else if (e instanceof ReferenceExpression) {
      parts.put(e, r);
    } else {
      throw new IllegalStateException("Implementation of termination checker assumes that expressions collected from patterns consist of constructors and references to variables");
    }
  }

  private void collectCall(DefCallExpression expression, ParameterVector vector) {
    BaseCallMatrix cm = new CallMatrix(myDefinition, expression);
    assert (cm.getHeight() == vector.getHeight());
    for (int i = 0; i < vector.getHeight(); i++) {
      for (int j = 0; j < expression.getDefCallArguments().size(); j++) {
        cm.set(i, j, compare(expression.getDefCallArguments().get(j), vector.getParameter(i)));
      }
    }
    myCollectedCalls.add(cm);
  }

  @Override
  public Void visitFunCall(FunCallExpression expression, ParameterVector vector) {
    collectCall(expression, vector);

    for (Expression argument : expression.getDefCallArguments()) {
      argument.accept(this, vector);
    }
    return null;
  }

  @Override
  public Void visitConCall(ConCallExpression expression, ParameterVector vector) {
    Constructor constructor = expression.getDefinition();
    if (constructor.getDataType().getCondition(constructor) != null) {
      collectCall(expression, vector);
    }
    for (Expression argument : expression.getDefCallArguments()) {
      argument.accept(this, vector);
    }
    return null;
  }

  @Override
  public Void visitApp(AppExpression expression, ParameterVector vector) {
    expression.getFunction().accept(this, vector);

    for (Expression e : expression.getArguments()) {
      e.accept(this, vector);
    }
    return null;
  }
  @Override
  public Void visitBranch(BranchElimTreeNode node, ParameterVector vector) {
    for (ConstructorClause clause : node.getConstructorClauses()) {
      clause.getChild().accept(this, new ParameterVector(vector, clause));
    }
    return null;
  }

  @Override
  public Void visitLet(LetExpression expression, ParameterVector vector) {
    expression.getExpression().accept(this, vector);

    for (LetClause lc : expression.getClauses()) {
      lc.getResultType().toExpression().accept(this, vector);
      visitDependendLink(lc.getParameters(), vector);
      lc.getElimTree().accept(this, vector);
    }
    return null;
  }

  @Override
  public Void visitLeaf(LeafElimTreeNode node, ParameterVector vector) {
    node.getExpression().accept(this, vector);
    return null;
  }


  @Override
  public Void visitDataCall(DataCallExpression expression, ParameterVector vector) {
    for (Expression e : expression.getDefCallArguments()) {
      e.accept(this, vector);
    }
    return null;
  }

  @Override
  public Void visitFieldCall(FieldCallExpression expression, ParameterVector vector) {
    for (Expression e : expression.getDefCallArguments()) {
      e.accept(this, vector);
    }
    return null;
  }

  @Override
  public Void visitClassCall(ClassCallExpression expression, ParameterVector vector) {
    for (Map.Entry<ClassField, FieldSet.Implementation> i : expression.getFieldSet().getImplemented()) {
      i.getValue().term.accept(this, vector);
    }
    return null;
  }

  private Void visitDependendLink(DependentLink link, ParameterVector vector) {
    for (; link.hasNext(); link = link.getNext()) {
      link = link.getNextTyped(null);
      link.getType().toExpression().accept(this, vector);
    }
    return null;
  }

  @Override
  public Void visitLam(LamExpression expression, ParameterVector vector) {
    visitDependendLink(expression.getParameters(), vector);
    expression.getBody().accept(this, vector);
    return null;
  }

  @Override
  public Void visitPi(PiExpression expression, ParameterVector vector) {
    visitDependendLink(expression.getParameters(), vector);
    expression.getCodomain().accept(this, vector);
    return null;
  }

  @Override
  public Void visitSigma(SigmaExpression expression, ParameterVector vector) {
    visitDependendLink(expression.getParameters(), vector);
    return null;
  }

  @Override
  public Void visitTuple(TupleExpression expression, ParameterVector vector) {
    for (Expression e : expression.getFields()) {
      e.accept(this, vector);
    }

    expression.getType().accept(this, vector);
    return null;
  }

  @Override
  public Void visitProj(ProjExpression expression, ParameterVector vector) {
    expression.getExpression().accept(this, vector);
    return null;
  }

  @Override
  public Void visitNew(NewExpression expression, ParameterVector vector) {
    expression.getExpression().accept(this, vector);
    return null;
  }

  @Override
  public Void visitUniverse(UniverseExpression expression, ParameterVector vector) {
    return null;
  }

  @Override
  public Void visitError(ErrorExpression expression, ParameterVector vector) {
    return null;
  }

  @Override
  public Void visitEmpty(EmptyElimTreeNode node, ParameterVector vector) {
    return null;
  }

  @Override
  public Void visitReference(ReferenceExpression expression, ParameterVector vector) {
    return null;
  }

  @Override
  public Void visitInferenceReference(InferenceReferenceExpression expression, ParameterVector vector) {
    throw new IllegalStateException();
  }

  @Override
  public Void visitOfType(OfTypeExpression expression, ParameterVector vector) {
    throw new IllegalStateException();
  }

  public static class ParameterVector {
    private Expression[] myParts;

    public ParameterVector(FunctionDefinition def) {
      int paramCount = def.getNumberOfRequiredArguments();
      myParts = new Expression[paramCount];

      int i = 0;
      DependentLink dl = def.getParameters();
      while (!(dl instanceof EmptyDependentLink)) {
        myParts[i] = new ReferenceExpression(dl);

        dl = dl.getNext();
        i++;
      }
    }

    public ParameterVector(ParameterVector pv, SubstVisitor sv) {
      myParts = new Expression[pv.myParts.length];
      for (int i = 0; i < pv.myParts.length; i++) {
        myParts[i] = pv.myParts[i].accept(sv, null);
      }
    }

    public ParameterVector(ParameterVector pv, Clause c) {
      this(pv, new SubstVisitor(c.getSubst(), new LevelSubstitution()));
    }

    Expression getParameter(int i) {
      return myParts[i];
    }

    int getHeight() {
      return myParts.length;
    }

  }

}
