package com.example.template.shared.utils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.springframework.beans.BeanUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public final class BeanUtilities {
  private static final Map<Class<?>, PropertyDescriptor[]> beanPropsMap = new ConcurrentHashMap<>();
  private static final Map<Class<?>, PropertyDescriptor[]> writableStringPropsMap =
      new ConcurrentHashMap<>();
  private static final Map<Class<?>, RecordInfo> recordInfoMap = new ConcurrentHashMap<>();

  private BeanUtilities() {}

  public static void trim(@NonNull Object bean) {
    var type = bean.getClass();
    assert !type.isRecord() : "Use BeanUtilities::trimRecord to trim records";

    try {
      for (var descriptor : getStringPropertyDescriptors(type, true)) {
        var value = (String) descriptor.getReadMethod().invoke(bean);
        if (value == null) {
          continue;
        }

        descriptor.getWriteMethod().invoke(bean, value.trim());
      }
    } catch (InvocationTargetException | IllegalAccessException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T extends Record> T trimRecord(@NonNull T record) {
    try {
      var recordInfo = geRecordInfo(record.getClass());
      var components = recordInfo.components();
      var length = components.length;
      var args = new Object[length];
      for (var i = 0; i < length; i++) {
        args[i] = components[i].getAccessor().invoke(record);
        if (components[i].getType() == String.class && args[i] != null) {
          args[i] = ((String) args[i]).trim();
        }
      }

      return (T) recordInfo.constructor().newInstance(args);
    } catch (InvocationTargetException
        | NoSuchMethodException
        | IllegalAccessException
        | InstantiationException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  public static MultiValueMap<String, String> toParams(@NonNull Object bean) {
    var descriptors = getStringPropertyDescriptors(bean.getClass(), false);
    var params = new LinkedMultiValueMap<String, String>(descriptors.length);
    for (var descriptor : descriptors) {
      var readMethod = descriptor.getReadMethod();
      if (readMethod == null || "class".equals(descriptor.getName())) {
        continue;
      }

      var name = descriptor.getName();
      Object value;
      try {
        value = readMethod.invoke(bean);
      } catch (IllegalAccessException | InvocationTargetException ex) {
        throw new IllegalArgumentException(ex);
      }

      if (value == null) {
        continue;
      }

      var valueStr = value.toString();
      if (valueStr.isEmpty()) {
        continue;
      }

      if (value.getClass().isArray()) {
        addParamsFromIterator(params, name, new ReflectionArrayIterator(value));
      } else if (value instanceof Iterable) {
        addParamsFromIterator(params, name, ((Iterable<?>) value).iterator());
      } else {
        params.add(name, value.toString());
      }
    }

    return params;
  }

  private static PropertyDescriptor[] getStringPropertyDescriptors(
      Class<?> type, boolean writableString) {
    var propsMap = writableString ? writableStringPropsMap : beanPropsMap;
    var descriptors = propsMap.get(type);
    if (descriptors != null) {
      return descriptors;
    }

    descriptors = BeanUtils.getPropertyDescriptors(type);

    var found = new ArrayList<PropertyDescriptor>(descriptors.length);
    for (var descriptor : descriptors) {
      if (writableString
          && (!String.class.equals(descriptor.getPropertyType())
              || descriptor.getWriteMethod() == null)) {
        continue;
      }

      if (descriptor.getReadMethod() != null) {
        found.add(descriptor);
      }
    }

    descriptors = new PropertyDescriptor[found.size()];
    found.toArray(descriptors);
    writableStringPropsMap.put(type, descriptors);

    return descriptors;
  }

  @SuppressWarnings("UnusedVariable")
  private static record RecordInfo(RecordComponent[] components, Constructor<?> constructor) {}

  private static RecordInfo geRecordInfo(Class<? extends Record> type)
      throws NoSuchMethodException {
    var recordInfo = recordInfoMap.get(type);
    if (recordInfo != null) {
      return recordInfo;
    }

    var components = type.getRecordComponents();
    var types = Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new);

    recordInfo = new RecordInfo(components, type.getDeclaredConstructor(types));
    recordInfoMap.put(type, recordInfo);

    return recordInfo;
  }

  private static void addParamsFromIterator(
      MultiValueMap<String, String> params, String name, Iterator<?> it) {
    while (it.hasNext()) {
      var value = it.next();
      if (value == null) {
        continue;
      }

      var valueStr = value.toString();
      if (valueStr.isEmpty()) {
        continue;
      }

      params.add(name, valueStr);
    }
  }
}
