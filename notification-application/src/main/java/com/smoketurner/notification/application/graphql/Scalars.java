/*
 * Copyright © 2018 Smoke Turner, LLC (contact@smoketurner.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.notification.application.graphql;

import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.EnumValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.NullValue;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLScalarType;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Portions copied from
 * https://github.com/leangen/graphql-spqr/blob/master/src/main/java/io/leangen/graphql/util/Scalars.java
 */
public class Scalars {

  private static Coercing<Object, Object> MAP_SCALAR_COERCION =
      new Coercing<Object, Object>() {
        @Override
        public Object serialize(Object dataFetcherResult) {
          return dataFetcherResult;
        }

        @Override
        public Object parseValue(Object input) {
          if (input instanceof Map) {
            return input;
          }
          throw valueParsingException(input, Map.class);
        }

        @Nullable
        @Override
        public Object parseLiteral(Object input) {
          return parseObjectValue(literalOrException(input, ObjectValue.class));
        }
      };

  public static GraphQLScalarType graphQLMapScalar(String name) {
    return new GraphQLScalarType(
        name, "Built-in scalar for map-like structures", MAP_SCALAR_COERCION);
  }

  @Nullable
  private static Object parseObjectValue(Value<?> value) {
    if (value instanceof StringValue) {
      return ((StringValue) value).getValue();
    }
    if (value instanceof IntValue) {
      return ((IntValue) value).getValue();
    }
    if (value instanceof FloatValue) {
      return ((FloatValue) value).getValue();
    }
    if (value instanceof BooleanValue) {
      return ((BooleanValue) value).isValue();
    }
    if (value instanceof EnumValue) {
      return ((EnumValue) value).getName();
    }
    if (value instanceof NullValue) {
      return null;
    }
    if (value instanceof ArrayValue) {
      return ((ArrayValue) value)
          .getValues()
          .stream()
          .map(Scalars::parseObjectValue)
          .collect(Collectors.toList());
    }
    if (value instanceof ObjectValue) {
      final Map<String, Object> map = new LinkedHashMap<>();
      ((ObjectValue) value)
          .getObjectFields()
          .forEach(field -> map.put(field.getName(), parseObjectValue(field.getValue())));
      return map;
    }
    // Should never happen, as it would mean the variable was not replaced by the parser
    throw new CoercingParseLiteralException(
        "Unknown scalar AST type: " + value.getClass().getName());
  }

  public static <T extends Value<?>> T literalOrException(Object input, Class<T> valueType) {
    if (valueType.isInstance(input)) {
      return valueType.cast(input);
    }
    throw new CoercingParseLiteralException(errorMessage(input, valueType));
  }

  public static CoercingParseLiteralException literalParsingException(
      Object input, Class<?>... allowedTypes) {
    return new CoercingParseLiteralException(errorMessage(input, allowedTypes));
  }

  public static CoercingParseValueException valueParsingException(
      Object input, Class<?>... allowedTypes) {
    return new CoercingParseValueException(errorMessage(input, allowedTypes));
  }

  public static String errorMessage(Object input, Class<?>... allowedTypes) {
    String types =
        Arrays.stream(allowedTypes)
            .map(type -> "'" + type.getSimpleName() + "'")
            .collect(Collectors.joining(" or "));
    return String.format(
        "Expected %stype %s but was '%s'",
        input instanceof Value ? "AST " : "",
        types,
        input == null ? "null" : input.getClass().getSimpleName());
  }
}
