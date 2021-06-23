/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.slang.visitors;

import java.util.ArrayList;
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.ModifierTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TreePrinter {

  private TreePrinter() {
  }

  public static String tree2string(List<Tree> trees) {
    return trees.stream().map(TreePrinter::tree2string).collect(Collectors.joining("\n"));
  }

  public static String tree2string(Tree tree) {
    StringBuilder sb = new StringBuilder();
    TreeVisitor<TreeContext> visitor = new TreeVisitor<>();
    visitor.register(Tree.class, (ctx, t) -> {
      IntStream.range(0, ctx.ancestors().size()).forEach(i -> sb.append("  "));
      sb.append(t.getClass().getSimpleName());
      if (t instanceof BinaryExpressionTree) {
        sb.append(" ").append(((BinaryExpressionTree) t).operator().name());
      } else if (t instanceof AssignmentExpressionTree) {
        sb.append(" ").append(((AssignmentExpressionTree) t).operator().name());
      } else if (t instanceof LiteralTree) {
        sb.append(" ").append(((LiteralTree) t).value());
      } else if (t instanceof IdentifierTree) {
        sb.append(" ").append(((IdentifierTree) t).name());
      } else if (t instanceof NativeTree) {
        sb.append(" ").append(((NativeTree) t).nativeKind());
      } else if (t instanceof ModifierTree) {
        sb.append(" ").append(((ModifierTree) t).kind());
      }
      sb.append("\n");
    });
    visitor.scan(new TreeContext(), tree);
    return sb.toString();
  }

  public static String table(Tree tree) {
    Table table = new Table("AST node class", "first…last tokens", "line:col");
    addAstNode(table, tree, 0);
    return table.toString();
  }

  private static void addAstNode(Table table, Tree node, int indentSize) {
    String indent = repeat(' ', indentSize);
    boolean hasChildren = !node.children().isEmpty();
    table.add(indent + kind(node) + (hasChildren ? " {" : ""),
        firstToLastTokens(node.metaData().tokens()),
        toLineColumn(node));
    for (Tree child : node.children()) {
      addAstNode(table, child, indentSize + 2);
    }
    if (hasChildren) {
      table.add(indent + "}", "", "");
    }
  }

  private static String firstToLastTokens(List<Token> tokens) {
    if (tokens.isEmpty()) {
      return "";
    } else {
      String firstToken = escapeWhiteSpace(tokens.get(0).text());
      String lastToken = escapeWhiteSpace(tokens.get(tokens.size() - 1).text());
      if (tokens.size() == 1) {
        return truncate(firstToken, 23);
      } else {
        return truncateFromLeft(firstToken, 10) +
            " … " + truncateFromRight(lastToken, 10);
      }
    }
  }

  private static String truncate(String text, int maxSize) {
    if (text.length() > maxSize) {
      return truncateFromLeft(text, maxSize / 2) +
          "…" + truncateFromRight(text, maxSize - (maxSize / 2) - 1);
    } else {
      return text;
    }
  }

  private static String truncateFromLeft(String text, int maxSize) {
    return text.length() > maxSize ? text.substring(0, maxSize) : text;
  }

  private static String truncateFromRight(String text, int maxSize) {
    return text.length() > maxSize ? text.substring(text.length() - maxSize) : text;
  }

  private static String escapeWhiteSpace(String text) {
    return text.replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  public static String kind(Tree node) {
    if (node instanceof NativeTree) {
      return "?" + ((NativeTree) node).nativeKind().toString() + "?";
    } else {
      return node.getClass().getSimpleName().replaceFirst("Impl$", "");
    }
  }

  private static String toLineColumn(Tree node) {
    return toLineColumn(node.textRange());
  }

  private static String toLineColumn(TextRange range) {
    return toLineColumn(range.start()) + " … " + toLineColumn(range.end());
  }

  private static String toLineColumn(TextPointer pointer) {
    return pointer.line() + ":" + (pointer.lineOffset() + 1);
  }

  private static String repeat(char filler, int count) {
    return new String(new char[count]).replace('\0', filler);
  }

  public static class Table {

    private final int colCount;
    private final String[] columnNames;
    private final int[] colWidths;
    private final List<String[]> rows;

    public Table(String... columnNames) {
      colCount = columnNames.length;
      colWidths = new int[colCount];
      this.columnNames = columnNames;
      for (int col = 0; col < colCount; col++) {
        colWidths[col] = this.columnNames[col].length();
      }
      rows = new ArrayList<>();
    }

    public void add(Object... columnValues) {
      if (columnValues.length != colCount) {
        throw new IllegalArgumentException("columnValues.length (" + columnNames.length + ") must be " + colCount);
      }
      String[] row = new String[colCount];
      for (int col = 0; col < colCount; col++) {
        row[col] = String.valueOf(columnValues[col]);
        if (colWidths[col] < row[col].length()) {
          colWidths[col] = row[col].length();
        }
      }
      rows.add(row);
    }

    @Override
    public String toString() {
      StringBuilder out = new StringBuilder();
      for (int col = 0; col < colCount; col++) {
        appendCol(out, columnNames[col], col, ' ');
      }
      out.append('\n');
      for (int col = 0; col < colCount; col++) {
        appendCol(out, "", col, '-');
      }
      for (String[] row : rows) {
        out.append('\n');
        for (int col = 0; col < colCount; col++) {
          appendCol(out, row[col], col, ' ');
        }
      }
      return out.toString();
    }

    private void appendCol(StringBuilder out, String value, int col, char filler) {
      int start = out.length();
      out.append(value);
      fill(out, filler, start + colWidths[col]);
      if (col + 1 < colCount) {
        out.append(filler).append('|').append(filler);
      }
    }

    private static void fill(StringBuilder out, char filler, int endExcluded) {
      while (out.length() < endExcluded) {
        out.append(filler);
      }
    }

  }

}
