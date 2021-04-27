package org.arend.ext.core.expr;

import org.arend.ext.core.definition.CoreDefinition;
import org.arend.ext.core.level.CoreLevel;
import org.arend.ext.core.level.CoreSort;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CoreDefCallExpression extends CoreExpression {
  @NotNull CoreDefinition getDefinition();
  @NotNull CoreLevel getPLevel();
  @NotNull CoreLevel getHLevel();
  @NotNull List<? extends CoreExpression> getDefCallArguments();
}
