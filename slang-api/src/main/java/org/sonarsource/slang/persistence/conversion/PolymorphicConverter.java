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
package org.sonarsource.slang.persistence.conversion;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.HashMap;
import java.util.function.BiFunction;
import javax.annotation.Nullable;

public class PolymorphicConverter {

  @FunctionalInterface
  public interface Serialize<T> extends BiFunction<SerializationContext, T, JsonValue> {
  }

  @FunctionalInterface
  public interface Deserialize<T> extends BiFunction<DeserializationContext, JsonObject, T> {
  }

  private final HashMap<Class<?>, Serialize<?>> toJsonConverter = new HashMap<>();
  private final HashMap<String, Deserialize<?>> fromJsonConverter = new HashMap<>();
  private final HashMap<Class<?>, String> jsonTypeByJavaClass = new HashMap<>();

  public <T> void register(Class<T> treeClass, String jsonType, Serialize<T> treeToJson, Deserialize<T> jsonToTree) {
    toJsonConverter.put(treeClass, treeToJson);
    fromJsonConverter.put(jsonType, jsonToTree);
    jsonTypeByJavaClass.put(treeClass, jsonType);
  }

  public String getJsonType(Object object) {
    return jsonTypeByJavaClass.get(object.getClass());
  }

  public <T> JsonValue toJson(SerializationContext ctx, @Nullable T object) {
    if (object == null) {
      return Json.NULL;
    }
    Class<?> objectClass = object.getClass();
    Serialize<T> converter = (Serialize<T>) toJsonConverter.get(objectClass);
    if (converter == null) {
      throw new IllegalStateException("Unsupported tree class: " + objectClass.getName());
    }
    return converter.apply(ctx, object);
  }

  public <T> T fromJson(DeserializationContext ctx, String jsonType, JsonObject json, String memberName, Class<T> expectedClass) {
    ctx.pushPath(jsonType);
    Deserialize<?> converter = fromJsonConverter.get(jsonType);
    if (converter == null) {
      throw ctx.newIllegalMemberException("Invalid '@type' value", jsonType);
    }
    Object object = converter.apply(ctx, json);
    if (!expectedClass.isInstance(object)) {
      throw ctx.newIllegalMemberException("Unexpected '" + object.getClass().getName() + "' type for member '" + memberName + "'" +
        " instead of '" + expectedClass.getName() + "'", json);
    }
    ctx.popPath();
    return expectedClass.cast(object);
  }

}
