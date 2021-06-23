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
package org.sonarsource.slang.persistence;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.TreeMetaDataProvider;
import org.sonarsource.slang.persistence.conversion.DeserializationContext;
import org.sonarsource.slang.persistence.conversion.JsonTreeConverter;
import org.sonarsource.slang.persistence.conversion.SerializationContext;

public final class JsonTree {

  private JsonTree() {
  }

  public static String toJson(Tree tree) {
    TreeMetaData metaData = tree.metaData();
    TreeMetaDataProvider provider = new TreeMetaDataProvider(metaData.commentsInside(), metaData.tokens());
    SerializationContext ctx = new SerializationContext(JsonTreeConverter.POLYMORPHIC_CONVERTER);
    return Json.object()
      .add("treeMetaData", JsonTreeConverter.TREE_METADATA_PROVIDER_TO_JSON.apply(ctx, provider))
      .add("tree", ctx.toJson(tree))
      .toString();
  }

  public static Tree fromJson(String json) {
    JsonObject root = Json.parse(json).asObject();
    JsonObject treeMetaData = root.get("treeMetaData").asObject();
    DeserializationContext ctx = new DeserializationContext(JsonTreeConverter.POLYMORPHIC_CONVERTER);
    TreeMetaDataProvider metaDataProvider = JsonTreeConverter.TREE_METADATA_PROVIDER_FROM_JSON.apply(ctx, treeMetaData);
    ctx = ctx.withMetaDataProvider(metaDataProvider);
    return ctx.fieldToNullableObject(root, "tree", Tree.class);
  }

}
