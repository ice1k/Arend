package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.typeclass.ClassView;

import java.util.Arrays;
import java.util.List;

public class CompositeInstancePool implements ClassViewInstancePool {
  private final List<ClassViewInstancePool> myPools;

  public CompositeInstancePool(ClassViewInstancePool... pools) {
    myPools = Arrays.asList(pools);
  }

  @Override
  public Expression getInstance(Expression classifyingExpression, ClassView classView) {
    for (ClassViewInstancePool pool : myPools) {
      Expression expr = pool.getInstance(classifyingExpression, classView);
      if (expr != null) {
        return expr;
      }
    }
    return null;
  }

  @Override
  public Expression getInstance(Expression classifyingExpression, ClassDefinition classDef) {
    for (ClassViewInstancePool pool : myPools) {
      Expression expr = pool.getInstance(classifyingExpression, classDef);
      if (expr != null) {
        return expr;
      }
    }
    return null;
  }
}
