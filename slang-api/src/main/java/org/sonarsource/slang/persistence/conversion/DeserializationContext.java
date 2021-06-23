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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.sonarsource.slang.api.NativeKind;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.TreeMetaDataProvider;

public class DeserializationContext {

  private static final int MAX_ILLEGAL_ELEMENT_TEXT_LENGTH = 80;

  private final PolymorphicConverter polymorphicConverter;

  private final Deque<String> jsonPath = new LinkedList<>();

  private TreeMetaDataProvider metaDataProvider = null;

  public DeserializationContext(PolymorphicConverter polymorphicConverter) {
    this.polymorphicConverter = polymorphicConverter;
  }

  public DeserializationContext withMetaDataProvider(TreeMetaDataProvider metaDataProvider) {
    this.metaDataProvider = metaDataProvider;
    return this;
  }

  public void pushPath(String fieldName) {
    jsonPath.addLast(fieldName);
  }

  public void popPath() {
    jsonPath.removeLast();
  }

  public String path() {
    return String.join("/", jsonPath);
  }

  public TreeMetaData metaData(JsonObject json) {
    return RangeConverter.resolveMetaData(metaDataProvider, fieldToString(json, "metaData"));
  }

  public RuntimeException newIllegalMemberException(String message, @Nullable Object illegalElement) {
    String elementText = String.valueOf(illegalElement);
    elementText = elementText.substring(0, Math.min(elementText.length(), MAX_ILLEGAL_ELEMENT_TEXT_LENGTH));
    return new IllegalStateException(message + " at '" + path() + "' member: " + elementText);
  }

  @Nullable
  public <T extends Tree> T fieldToNullableObject(JsonObject parent, String fieldName, Class<T> expectedClass) {
    JsonValue json = parent.get(fieldName);
    if (json == null || Json.NULL.equals(json)) {
      return null;
    }
    return object(json, fieldName, expectedClass);
  }

  public <T extends Tree> T fieldToObject(JsonObject parent, String fieldName, Class<T> expectedClass) {
    JsonValue json = parent.get(fieldName);
    if (json == null || Json.NULL.equals(json)) {
      throw newIllegalMemberException("Unexpected null value for field '" + fieldName + "'", json);
    }
    return object(json, fieldName, expectedClass);
  }

  public NativeKind fieldToNativeKind(JsonObject parent, String fieldName) {
    return StringNativeKind.of(fieldToString(parent, fieldName, ""));
  }

  public <T extends Enum<T>> T fieldToEnum(JsonObject parent, String fieldName, Class<T> enumType) {
    return Enum.valueOf(enumType, fieldToString(parent, fieldName));
  }

  public <T extends Enum<T>> T fieldToEnum(JsonObject parent, String fieldName, String defaultValue, Class<T> enumType) {
    return Enum.valueOf(enumType, fieldToString(parent, fieldName, defaultValue));
  }

  public <T extends Tree> List<T> fieldToObjectList(JsonObject parent, String fieldName, Class<T> expectedClass) {
    return objectList(parent.get(fieldName), fieldName + "[]", expectedClass);
  }

  public <T extends Tree> List<T> objectList(@Nullable JsonValue value, String memberName, Class<T> expectedClass) {
    return objectList(value, jsonChild -> object(jsonChild, memberName, expectedClass));
  }

  public <T> List<T> objectList(@Nullable JsonValue value, BiFunction<DeserializationContext, JsonObject, T> converter) {
    return objectList(value, jsonChild -> converter.apply(this, jsonChild));
  }

  private <T> List<T> objectList(@Nullable JsonValue value, Function<JsonObject, T> converter) {
    if (value == null || value.isNull()) {
      return Collections.emptyList();
    }
    if (!value.isArray()) {
      throw newIllegalMemberException("Expect Array instead of " + value.getClass().getSimpleName(), value);
    }
    List<T> result = new ArrayList<>();
    for (JsonValue jsonValue : value.asArray()) {
      result.add(converter.apply(jsonValue.asObject()));
    }
    return result;
  }

  public String fieldToNullableString(JsonObject json, String fieldName) {
    JsonValue value = json.get(fieldName);
    if (value == null || Json.NULL.equals(value)) {
      return null;
    }
    return fieldToString(json, fieldName);
  }

  public String fieldToString(JsonObject json, String fieldName) {
    JsonValue value = json.get(fieldName);
    if (value == null || Json.NULL.equals(value)) {
      throw newIllegalMemberException("Missing non-null value for field '" + fieldName + "'", json);
    }
    if (!value.isString()) {
      throw newIllegalMemberException("Expect String instead of '" + value.getClass().getSimpleName() +
        "' for field '" + fieldName + "'", json);
    }
    return value.asString();
  }

  public String fieldToString(JsonObject json, String fieldName, String defaultValue){
    return json.getString(fieldName, defaultValue);
  }

  public TextRange fieldToRange(JsonObject json, String fieldName) {
    return RangeConverter.parse(fieldToString(json, fieldName));
  }

  public Token fieldToToken(JsonObject json, String fieldName) {
    return RangeConverter.resolveToken(metaDataProvider, fieldToString(json, fieldName));
  }

  @Nullable
  public Token fieldToNullableToken(JsonObject json, String fieldName) {
    return RangeConverter.resolveToken(metaDataProvider, fieldToNullableString(json, fieldName));
  }

  private <T> T object(JsonValue json, String memberName, Class<T> expectedClass) {
    pushPath(memberName);
    if (!json.isObject()) {
      throw newIllegalMemberException("Unexpected value for Tree", json);
    }
    JsonObject jsonObject = json.asObject();
    String jsonType = fieldToString(jsonObject, SerializationContext.TYPE_ATTRIBUTE);
    T object = polymorphicConverter.fromJson(this, jsonType, jsonObject, memberName, expectedClass);
    popPath();
    return object;
  }
}
