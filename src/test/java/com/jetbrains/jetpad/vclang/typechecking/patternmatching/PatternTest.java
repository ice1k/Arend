package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.core.pattern.BindingPattern;
import com.jetbrains.jetpad.vclang.core.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.core.pattern.EmptyPattern;
import com.jetbrains.jetpad.vclang.core.pattern.Pattern;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;
import com.jetbrains.jetpad.vclang.util.Pair;
import org.junit.Test;

import java.util.*;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Interval;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Nat;
import static org.junit.Assert.*;

public class PatternTest extends TypeCheckingTestCase {
  private boolean checkPatterns(List<? extends Concrete.Pattern<Position>> patternArgs, List<Pattern> patterns, Map<Referable, Binding> expected, Map<Referable, Binding> actual, boolean hasImplicit) {
    int i = 0, j = 0;
    for (; i < patternArgs.size() && j < patterns.size(); i++, j++) {
      Concrete.Pattern<Position> pattern1 = patternArgs.get(i);
      if (pattern1 instanceof Concrete.EmptyPattern) {
        while (hasImplicit && patterns.get(j) instanceof BindingPattern) {
          j++;
        }
        assertTrue(patterns.get(j) instanceof EmptyPattern);
        for (j++; j < patterns.size(); j++) {
          assertTrue(patterns.get(j) instanceof BindingPattern);
        }
        return false;
      } else
      if (pattern1 instanceof Concrete.NamePattern) {
        Referable referable = ((Concrete.NamePattern) pattern1).getReferable();
        while (hasImplicit && patterns.get(j) instanceof BindingPattern && expected.get(referable) != ((BindingPattern) patterns.get(j)).getBinding()) {
          j++;
        }
        assertTrue(patterns.get(j) instanceof BindingPattern);
        actual.put(referable, ((BindingPattern) patterns.get(j)).getBinding());
      } else
      if (pattern1 instanceof Concrete.ConstructorPattern) {
        while (hasImplicit && patterns.get(j) instanceof BindingPattern) {
          j++;
        }
        assertTrue(patterns.get(j) instanceof ConstructorPattern);

        Concrete.ConstructorPattern<Position> conPattern1 = (Concrete.ConstructorPattern<Position>) pattern1;
        ConstructorPattern conPattern2 = (ConstructorPattern) patterns.get(j);
        assertEquals(conPattern1.getConstructor(), conPattern2.getConstructor().getReferable());
        checkPatterns(conPattern1.getPatterns(), conPattern2.getArguments(), expected, actual, hasImplicit);
      } else {
        throw new IllegalStateException();
      }
    }

    assertEquals(patternArgs.size(), i);
    assertEquals(patterns.size(), j);
    return true;
  }

  private void checkPatterns(List<? extends Concrete.Pattern<Position>> patternArgs, List<Pattern> patterns, Map<Referable, Binding> expected, boolean hasImplicit) {
    Map<Referable, Binding> actual = new HashMap<>();
    boolean withoutEmpty = checkPatterns(patternArgs, patterns, expected, actual, hasImplicit);
    assertEquals(expected, withoutEmpty ? actual : null);

    Stack<Pattern> patternStack = new Stack<>();
    for (int i = patterns.size() - 1; i >= 0; i--) {
      patternStack.push(patterns.get(i));
    }

    DependentLink last = null;
    while (!patternStack.isEmpty()) {
      Pattern pattern = patternStack.pop();
      if (pattern instanceof BindingPattern) {
        DependentLink link = ((BindingPattern) pattern).getBinding();
        if (last != null) {
          assertEquals(last.getNext(), link);
        }
        last = link;
      } else
      if (pattern instanceof ConstructorPattern) {
        for (int i = ((ConstructorPattern) pattern).getArguments().size() - 1; i >= 0; i--) {
          patternStack.push(((ConstructorPattern) pattern).getArguments().get(i));
        }
      } else
      if (pattern instanceof EmptyPattern) {
        break;
      }
    }
  }

  @Test
  public void threeVars() {
    Concrete.FunctionDefinition<Position> fun = (Concrete.FunctionDefinition<Position>) resolveNamesDef(
      "\\function f (n m k : Nat) => \\elim n, m, k\n" +
      "  | suc n, zero, suc k => k");
    List<Concrete.Pattern<Position>> patternsArgs = ((Concrete.ElimFunctionBody<Position>) fun.getBody()).getClauses().get(0).getPatterns();
    Pair<List<Pattern>, Map<Referable, Binding>> res = new PatternTypechecking<>(new ProxyErrorReporter<>(fun, errorReporter), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE)).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, Nat()), param(null, Nat())), fun.getBody(), false);
    assertNotNull(res);
    assertEquals(0, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void nestedPatterns() {
    Concrete.FunctionDefinition<Position> fun = (Concrete.FunctionDefinition<Position>) resolveNamesDef(
      "\\function f (n m k : Nat) => \\elim n, m, k\n" +
      "  | suc (suc (suc n)), zero, suc (suc (suc (suc zero))) => n");
    List<Concrete.Pattern<Position>> patternsArgs = ((Concrete.ElimFunctionBody<Position>) fun.getBody()).getClauses().get(0).getPatterns();
    Pair<List<Pattern>, Map<Referable, Binding>> res = new PatternTypechecking<>(new ProxyErrorReporter<>(fun, errorReporter), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE)).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, Nat()), param(null, Nat())), fun.getBody(), false);
    assertNotNull(res);
    assertEquals(0, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void incorrectType() {
    Concrete.FunctionDefinition<Position> fun = (Concrete.FunctionDefinition<Position>) resolveNamesDef(
      "\\function f (n : Nat) (m : Nat -> Nat) (k : Nat) => \\elim n, m, k\n" +
      "  | suc n, zero, suc k => k");
    List<Concrete.Pattern<Position>> patternsArgs = ((Concrete.ElimFunctionBody<Position>) fun.getBody()).getClauses().get(0).getPatterns();
    Pair<List<Pattern>, Map<Referable, Binding>> res = new PatternTypechecking<>(new ProxyErrorReporter<>(fun, errorReporter), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE)).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, Pi(Nat(), Nat())), param(null, Nat())), fun.getBody(), false);
    assertNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void incorrectDataType() {
    Concrete.ClassDefinition<Position> classDef = resolveNamesModule(
      "\\data D | con\n" +
      "\\function f (n : Nat) (d : D) (k : Nat) => \\elim n, d, k\n" +
      "  | suc n, zero, suc k => k");
    Concrete.DataDefinition<Position> dataDef = (Concrete.DataDefinition<Position>) ((Concrete.DefineStatement<Position>) classDef.getGlobalStatements().get(0)).getDefinition();
    Concrete.FunctionDefinition<Position> funDef = (Concrete.FunctionDefinition<Position>) ((Concrete.DefineStatement<Position>) classDef.getGlobalStatements().get(1)).getDefinition();
    DataDefinition data = new DataDefinition(dataDef);
    data.setParameters(EmptyDependentLink.getInstance());
    data.setSort(Sort.STD);
    data.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    List<Concrete.Pattern<Position>> patternsArgs = ((Concrete.ElimFunctionBody<Position>) funDef.getBody()).getClauses().get(0).getPatterns();
    Pair<List<Pattern>, Map<Referable, Binding>> res = new PatternTypechecking<>(new ProxyErrorReporter<>(funDef, errorReporter), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE)).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, new DataCallExpression(data, Sort.STD, Collections.emptyList())), param(null, Nat())), funDef.getBody(), false);
    assertNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void tooManyPatterns() {
    Concrete.FunctionDefinition<Position> fun = (Concrete.FunctionDefinition<Position>) resolveNamesDef(
      "\\function f (n m k : Nat) => \\elim n, m, k\n" +
      "  | suc n m, zero, suc k => k");
    List<Concrete.Pattern<Position>> patternsArgs = ((Concrete.ElimFunctionBody<Position>) fun.getBody()).getClauses().get(0).getPatterns();
    Pair<List<Pattern>, Map<Referable, Binding>> res = new PatternTypechecking<>(new ProxyErrorReporter<>(fun, errorReporter), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE)).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, Nat()), param(null, Nat())), fun.getBody(), false);
    assertNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void interval() {
    Concrete.FunctionDefinition<Position> fun = (Concrete.FunctionDefinition<Position>) resolveNamesDef(
      "\\function f (n : Nat) (i : I) => \\elim n, i\n" +
      "  | zero, i => zero");
    List<Concrete.Pattern<Position>> patternsArgs = ((Concrete.ElimFunctionBody<Position>) fun.getBody()).getClauses().get(0).getPatterns();
    Pair<List<Pattern>, Map<Referable, Binding>> res = new PatternTypechecking<>(new ProxyErrorReporter<>(fun, errorReporter), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE)).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, Interval())), fun.getBody(), false);
    assertNotNull(res);
    assertEquals(0, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void intervalFail() {
    Concrete.FunctionDefinition<Position> fun = (Concrete.FunctionDefinition<Position>) resolveNamesDef(
      "\\function f (n : Nat) (i : I) => \\elim n, i\n" +
      "  | zero, left => zero");
    List<Concrete.Pattern<Position>> patternsArgs = ((Concrete.ElimFunctionBody<Position>) fun.getBody()).getClauses().get(0).getPatterns();
    Pair<List<Pattern>, Map<Referable, Binding>> res = new PatternTypechecking<>(new ProxyErrorReporter<>(fun, errorReporter), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE)).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, Interval())), fun.getBody(), false);
    assertNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void emptyDataType() {
    Concrete.ClassDefinition<Position> classDef = resolveNamesModule(
      "\\data D\n" +
      "\\function f (n : Nat) (d : D) (k : Nat) => \\elim n, d, k\n" +
      "  | suc n, (), k => k");
    Concrete.DataDefinition<Position> dataDef = (Concrete.DataDefinition<Position>) ((Concrete.DefineStatement<Position>) classDef.getGlobalStatements().get(0)).getDefinition();
    Concrete.FunctionDefinition<Position> funDef = (Concrete.FunctionDefinition<Position>) ((Concrete.DefineStatement<Position>) classDef.getGlobalStatements().get(1)).getDefinition();
    DataDefinition data = new DataDefinition(dataDef);
    data.setParameters(EmptyDependentLink.getInstance());
    data.setSort(Sort.STD);
    data.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    List<Concrete.Pattern<Position>> patternsArgs = ((Concrete.ElimFunctionBody<Position>) funDef.getBody()).getClauses().get(0).getPatterns();
    Pair<List<Pattern>, Map<Referable, Binding>> res = new PatternTypechecking<>(new ProxyErrorReporter<>(funDef, errorReporter), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE)).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, new DataCallExpression(data, Sort.STD, Collections.emptyList())), param(null, Nat())), funDef.getBody(), false);
    assertNotNull(res);
    assertEquals(0, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void emptyDataTypeWarning() {
    Concrete.ClassDefinition<Position> classDef = resolveNamesModule(
      "\\data D\n" +
      "\\function f (n : Nat) (d : D) (k : Nat) => \\elim n, d, k\n" +
      "  | suc n, (), suc k => k");
    Concrete.DataDefinition<Position> dataDef = (Concrete.DataDefinition<Position>) ((Concrete.DefineStatement<Position>) classDef.getGlobalStatements().get(0)).getDefinition();
    Concrete.FunctionDefinition<Position> funDef = (Concrete.FunctionDefinition<Position>) ((Concrete.DefineStatement<Position>) classDef.getGlobalStatements().get(1)).getDefinition();
    DataDefinition data = new DataDefinition(dataDef);
    data.setParameters(EmptyDependentLink.getInstance());
    data.setSort(Sort.STD);
    data.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    List<Concrete.Pattern<Position>> patternsArgs = ((Concrete.ElimFunctionBody<Position>) funDef.getBody()).getClauses().get(0).getPatterns();
    Pair<List<Pattern>, Map<Referable, Binding>> res = new PatternTypechecking<>(new ProxyErrorReporter<>(funDef, errorReporter), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE)).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, new DataCallExpression(data, Sort.STD, Collections.emptyList())), param(null, Nat())), funDef.getBody(), false);
    assertNotNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void elimBefore() {
    typeCheckDef(
      "\\function if (n : Nat) {A : \\Type} (a a' : A) : A => \\elim n\n" +
      "  | zero => a\n" +
      "  | suc _ => a'");
  }

  @Test
  public void elimAfter() {
    typeCheckDef(
      "\\function if {A : \\Type} (a a' : A) (n : Nat) : A => \\elim n\n" +
      "  | zero => a\n" +
      "  | suc _ => a'");
  }

  @Test
  public void dependentElim() {
    typeCheckModule(
      "\\function if {A : \\Type} (n : Nat) (a a' : A) : A => \\elim n\n" +
      "  | zero => a\n" +
      "  | suc _ => a'\n" +
      "\\function f (n : Nat) (x : if n Nat (Nat -> Nat)) : Nat => \\elim n\n" +
      "  | zero => x\n" +
      "  | suc _ => x 0");
  }

  @Test
  public void elimLess() {
    typeCheckModule(
      "\\data D Nat \\with | suc n => dsuc\n" +
      "\\function tests (n : Nat) (d : D n) : Nat => \\elim n, d\n" +
      "  | suc n => 0\n" +
      "  | zero => 0", 1);
  }

  @Test
  public void withoutElimLess() {
    typeCheckModule(
      "\\data D Nat \\with | suc n => dsuc\n" +
      "\\function tests (n : Nat) (d : D n) : Nat\n" +
      "  | suc n => 0\n" +
      "  | zero => 0", 2);
  }

  @Test
  public void elimMore() {
    typeCheckModule(
      "\\function tests (n m : Nat) : Nat => \\elim n\n" +
      "  | suc n, zero => 0\n" +
      "  | zero, suc m => 0", 1);
  }

  @Test
  public void elimEvenMore() {
    typeCheckModule(
      "\\function tests (n m : Nat) : Nat => \\elim n\n" +
      "  | suc n, zero => 0\n" +
      "  | zero, zero => 0\n" +
      "  | suc n, suc m => 0\n" +
      "  | zero, suc m => 0", 1);
  }

  @Test
  public void withoutElimMore() {
    typeCheckModule(
      "\\function tests (n : Nat) : Nat\n" +
        "  | suc n, zero => 0\n" +
        "  | zero, suc m => 0", 2);
  }

  @Test
  public void implicitParameter() {
    typeCheckModule(
      "\\function tests {n : Nat} (m : Nat) : Nat\n" +
      "  | {suc n}, zero => 0\n" +
      "  | {zero}, suc m => 0\n" +
      "  | {suc n}, suc m => 0\n" +
      "  | {zero}, zero => 0");
  }

  @Test
  public void skipImplicitParameter() {
    typeCheckModule(
      "\\function tests {n : Nat} (m : Nat) : Nat\n" +
      "  | suc m => 0\n" +
      "  | zero => 0");
  }

  @Test
  public void implicitParameterError() {
    typeCheckModule(
      "\\function tests {n : Nat} : Nat\n" +
      "  | suc n => 0\n" +
      "  | zero => 0", 2);
  }

  @Test
  public void withThis() {
    typeCheckModule(
      "\\function tests (n : Nat) : Nat\n" +
      "  | suc n => 0\n" +
      "  | zero => 0", "");
  }

  @Test
  public void withThisAndElim() {
    typeCheckModule(
      "\\function tests (n : Nat) : Nat => \\elim n\n" +
      "  | suc n => 0\n" +
      "  | zero => 0", "");
  }

  @Test
  public void nonEliminatedAvailable() {
    typeCheckModule(
      "\\function tests {n : Nat} (m : Nat) : Nat => \\elim m\n" +
      "  | suc m => m\n" +
      "  | zero => n");
  }

  @Test
  public void explicitAvailable() {
    typeCheckModule(
      "\\function tests {n : Nat} (m : Nat) : Nat\n" +
      "  | {n}, suc m => n\n" +
      "  | {k}, zero => k");
  }

  @Test
  public void eliminateOverridden() {
    typeCheckModule(
      "\\function f (n : Nat) (n : Nat) : Nat => \\elim n\n" +
      "  | suc _ => 2\n" +
      "  | zero => n\n" +
      "\\function g : f 1 0 = 1 => path (\\lam _ => 1)");
  }

  @Test
  public void redundantPattern() {
    typeCheckDef(
      "\\function f (n : Nat) : Nat\n" +
      "  | _ => 0\n" +
      "  | zero => 1", 1);
  }
}
